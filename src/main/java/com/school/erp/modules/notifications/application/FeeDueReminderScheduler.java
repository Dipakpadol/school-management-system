package com.school.erp.modules.notifications.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.school.erp.modules.fees.domain.FeeAssignmentStatus;
import com.school.erp.modules.fees.domain.StudentFeeAssignment;
import com.school.erp.modules.fees.domain.StudentFeeInstallment;
import com.school.erp.modules.fees.infrastructure.StudentFeeAssignmentRepository;
import com.school.erp.modules.notifications.domain.NotificationChannel;
import com.school.erp.modules.notifications.domain.NotificationLog;
import com.school.erp.modules.notifications.domain.NotificationStatus;
import com.school.erp.modules.students.domain.ParentGuardian;
import com.school.erp.modules.students.domain.Student;
import com.school.erp.modules.students.domain.StudentParent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class FeeDueReminderScheduler {

	private static final Logger LOGGER = LoggerFactory.getLogger(FeeDueReminderScheduler.class);
	private static final String REFERENCE_TYPE = "STUDENT_FEE_ASSIGNMENT";
	private static final String NO_RECIPIENT = "N/A";

	private final NotificationProperties properties;
	private final StudentFeeAssignmentRepository assignmentRepository;
	private final NotificationService notificationService;

	@Scheduled(
			cron = "${notifications.scheduler.fee-due-cron:0 0 9 * * *}",
			zone = "${notifications.scheduler.zone:Asia/Kolkata}")
	public void run() {
		if (!properties.getScheduler().isEnabled()) {
			return;
		}
		ReminderSummary summary = processDueFeeReminders();
		LOGGER.info(
				"Fee due reminder scheduler completed totalDue={} sent={} failed={} skipped={} duplicatesSkipped={}",
				summary.totalDue(),
				summary.sent(),
				summary.failed(),
				summary.skipped(),
				summary.duplicatesSkipped());
	}

	public ReminderSummary processDueFeeReminders() {
		if (!properties.getScheduler().isEnabled()) {
			return ReminderSummary.empty();
		}
		ZoneId zone = ZoneId.of(properties.getScheduler().getZone());
		LocalDate today = LocalDate.now(zone);
		Instant startOfToday = today.atStartOfDay(zone).toInstant();
		Instant endOfToday = today.atTime(LocalTime.MAX).atZone(zone).toInstant();
		List<StudentFeeAssignment> assignments = assignmentRepository.findDueReminderCandidates(
				today,
				List.of(
						FeeAssignmentStatus.PENDING,
						FeeAssignmentStatus.PARTIALLY_PAID,
						FeeAssignmentStatus.OVERDUE));

		int sent = 0;
		int failed = 0;
		int skipped = 0;
		int duplicatesSkipped = 0;

		for (StudentFeeAssignment assignment : assignments) {
			ReminderMessage message = reminderMessage(assignment, today);
			for (NotificationChannel channel : enabledChannels()) {
				if (alreadySentToday(assignment.getId(), channel, startOfToday, endOfToday)) {
					duplicatesSkipped++;
					continue;
				}
				Optional<String> recipient = recipientFor(assignment.getStudent(), channel);
				if (recipient.isEmpty()) {
					notificationService.recordSkipped(
							channel,
							NO_RECIPIENT,
							message.subject(),
							message.body(),
							REFERENCE_TYPE,
							assignment.getId(),
							"No recipient contact available");
					skipped++;
					continue;
				}
				NotificationLog log = notificationService.sendNotification(
						channel,
						recipient.get(),
						message.subject(),
						message.body(),
						REFERENCE_TYPE,
						assignment.getId());
				if (log.getStatus() == NotificationStatus.SENT) {
					sent++;
				}
				else if (log.getStatus() == NotificationStatus.FAILED) {
					failed++;
				}
				else {
					skipped++;
				}
			}
		}

		return new ReminderSummary(assignments.size(), sent, failed, skipped, duplicatesSkipped);
	}

	private List<NotificationChannel> enabledChannels() {
		java.util.ArrayList<NotificationChannel> channels = new java.util.ArrayList<>();
		if (properties.getScheduler().isEmailEnabled()) {
			channels.add(NotificationChannel.EMAIL);
		}
		if (properties.getScheduler().isSmsEnabled()) {
			channels.add(NotificationChannel.SMS);
		}
		if (properties.getScheduler().isWhatsappEnabled()) {
			channels.add(NotificationChannel.WHATSAPP);
		}
		return channels;
	}

	private boolean alreadySentToday(UUID assignmentId, NotificationChannel channel, Instant startOfToday, Instant endOfToday) {
		return notificationService.wasSentBetween(REFERENCE_TYPE, assignmentId, channel, startOfToday, endOfToday);
	}

	private Optional<String> recipientFor(Student student, NotificationChannel channel) {
		return switch (channel) {
			case EMAIL -> firstParentContact(student, true).or(() -> studentContact(student.getEmail()));
			case SMS, WHATSAPP -> firstParentContact(student, false).or(() -> studentContact(student.getPhoneNumber()));
			case PUSH -> Optional.empty();
		};
	}

	private Optional<String> firstParentContact(Student student, boolean email) {
		return student.getParents().stream()
				.filter(mapping -> !mapping.isDeleted())
				.sorted(Comparator.comparing(StudentParent::isPrimaryContact).reversed())
				.map(StudentParent::getParent)
				.filter(parent -> parent != null && !parent.isDeleted())
				.map(parent -> contact(parent, email))
				.filter(StringUtils::hasText)
				.findFirst();
	}

	private String contact(ParentGuardian parent, boolean email) {
		return email ? parent.getEmail() : firstText(parent.getPhoneNumber(), parent.getAlternatePhoneNumber());
	}

	private Optional<String> studentContact(String value) {
		return StringUtils.hasText(value) ? Optional.of(value.trim()) : Optional.empty();
	}

	private ReminderMessage reminderMessage(StudentFeeAssignment assignment, LocalDate today) {
		LocalDate dueDate = assignment.orderedInstallments().stream()
				.filter(installment -> !installment.isDeleted())
				.filter(installment -> installment.getDueDate() != null && !installment.getDueDate().isAfter(today))
				.filter(installment -> installment.getBalanceAmount().signum() > 0)
				.map(StudentFeeInstallment::getDueDate)
				.min(Comparator.naturalOrder())
				.orElse(today);
		String studentName = assignment.getStudent().getDisplayName();
		String amount = money(assignment.getBalanceAmount());
		String subject = "Fee due reminder - " + studentName;
		String body = "Dear Parent, fee payment is due for %s. Pending amount: %s. Due date: %s. %s"
				.formatted(studentName, amount, dueDate, properties.getSchoolName());
		return new ReminderMessage(subject, body);
	}

	private String money(BigDecimal value) {
		return value == null ? "0.00" : value.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
	}

	private String firstText(String primary, String fallback) {
		return StringUtils.hasText(primary) ? primary.trim() : StringUtils.hasText(fallback) ? fallback.trim() : fallback;
	}

	private record ReminderMessage(String subject, String body) {
	}

	public record ReminderSummary(
			int totalDue,
			int sent,
			int failed,
			int skipped,
			int duplicatesSkipped) {

		static ReminderSummary empty() {
			return new ReminderSummary(0, 0, 0, 0, 0);
		}
	}
}
