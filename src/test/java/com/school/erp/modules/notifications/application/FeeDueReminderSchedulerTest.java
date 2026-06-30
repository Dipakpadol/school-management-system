package com.school.erp.modules.notifications.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.school.erp.modules.fees.domain.FeeAssignmentStatus;
import com.school.erp.modules.fees.domain.FeeCategory;
import com.school.erp.modules.fees.domain.FeeStructure;
import com.school.erp.modules.fees.domain.StudentFeeAssignment;
import com.school.erp.modules.fees.infrastructure.StudentFeeAssignmentRepository;
import com.school.erp.modules.notifications.domain.NotificationChannel;
import com.school.erp.modules.notifications.domain.NotificationLog;
import com.school.erp.modules.notifications.domain.NotificationStatus;
import com.school.erp.modules.students.domain.Gender;
import com.school.erp.modules.students.domain.Student;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FeeDueReminderSchedulerTest {

	@Mock
	private StudentFeeAssignmentRepository assignmentRepository;

	@Mock
	private NotificationService notificationService;

	private NotificationProperties properties;
	private FeeDueReminderScheduler scheduler;

	@BeforeEach
	void setUp() {
		properties = new NotificationProperties();
		properties.getScheduler().setEnabled(true);
		properties.getScheduler().setEmailEnabled(true);
		properties.getScheduler().setSmsEnabled(false);
		properties.getScheduler().setWhatsappEnabled(false);
		scheduler = new FeeDueReminderScheduler(properties, assignmentRepository, notificationService);
	}

	@Test
	void schedulerDisabledDoesNothing() {
		properties.getScheduler().setEnabled(false);

		FeeDueReminderScheduler.ReminderSummary summary = scheduler.processDueFeeReminders();

		assertThat(summary.totalDue()).isZero();
		verify(assignmentRepository, never()).findDueReminderCandidates(any(LocalDate.class), any());
	}

	@Test
	void schedulerSendsEmailReminderForDueAssignment() {
		StudentFeeAssignment assignment = dueAssignment(true);
		NotificationLog sent = new NotificationLog(NotificationChannel.EMAIL, "parent@example.com", "Subject", "Message");
		sent.markSent("smtp", "smtp", "sent");
		when(assignmentRepository.findDueReminderCandidates(any(LocalDate.class), any())).thenReturn(List.of(assignment));
		when(notificationService.wasSentBetween(anyString(), any(UUID.class), any(NotificationChannel.class), any(Instant.class), any(Instant.class)))
				.thenReturn(false);
		when(notificationService.sendNotification(any(NotificationChannel.class), anyString(), anyString(), anyString(), anyString(), any(UUID.class)))
				.thenReturn(sent);

		FeeDueReminderScheduler.ReminderSummary summary = scheduler.processDueFeeReminders();

		assertThat(summary.totalDue()).isEqualTo(1);
		assertThat(summary.sent()).isEqualTo(1);
		assertThat(summary.failed()).isZero();
		assertThat(summary.skipped()).isZero();
		verify(notificationService).sendNotification(
				any(NotificationChannel.class),
				anyString(),
				anyString(),
				anyString(),
				anyString(),
				any(UUID.class));
	}

	@Test
	void duplicateSentReminderIsSkippedForSameDay() {
		StudentFeeAssignment assignment = dueAssignment(true);
		when(assignmentRepository.findDueReminderCandidates(any(LocalDate.class), any())).thenReturn(List.of(assignment));
		when(notificationService.wasSentBetween(anyString(), any(UUID.class), any(NotificationChannel.class), any(Instant.class), any(Instant.class)))
				.thenReturn(true);

		FeeDueReminderScheduler.ReminderSummary summary = scheduler.processDueFeeReminders();

		assertThat(summary.duplicatesSkipped()).isEqualTo(1);
		verify(notificationService, never()).sendNotification(
				any(NotificationChannel.class),
				anyString(),
				anyString(),
				anyString(),
				anyString(),
				any(UUID.class));
	}

	@Test
	void noContactDetailsCreatesSkippedLog() {
		StudentFeeAssignment assignment = dueAssignment(false);
		NotificationLog skipped = new NotificationLog(NotificationChannel.EMAIL, "N/A", "Subject", "Message");
		skipped.markSkipped("No recipient contact available");
		when(assignmentRepository.findDueReminderCandidates(any(LocalDate.class), any())).thenReturn(List.of(assignment));
		when(notificationService.wasSentBetween(anyString(), any(UUID.class), any(NotificationChannel.class), any(Instant.class), any(Instant.class)))
				.thenReturn(false);
		when(notificationService.recordSkipped(any(NotificationChannel.class), anyString(), anyString(), anyString(), anyString(), any(UUID.class), anyString()))
				.thenReturn(skipped);

		FeeDueReminderScheduler.ReminderSummary summary = scheduler.processDueFeeReminders();

		assertThat(summary.skipped()).isEqualTo(1);
		verify(notificationService).recordSkipped(
				any(NotificationChannel.class),
				anyString(),
				anyString(),
				anyString(),
				anyString(),
				any(UUID.class),
				anyString());
	}

	@Test
	void failedProviderResultIncrementsFailureCount() {
		StudentFeeAssignment assignment = dueAssignment(true);
		NotificationLog failed = new NotificationLog(NotificationChannel.EMAIL, "parent@example.com", "Subject", "Message");
		failed.markFailed("smtp", "SMTP failed");
		when(assignmentRepository.findDueReminderCandidates(any(LocalDate.class), any())).thenReturn(List.of(assignment));
		when(notificationService.wasSentBetween(anyString(), any(UUID.class), any(NotificationChannel.class), any(Instant.class), any(Instant.class)))
				.thenReturn(false);
		when(notificationService.sendNotification(any(NotificationChannel.class), anyString(), anyString(), anyString(), anyString(), any(UUID.class)))
				.thenReturn(failed);

		FeeDueReminderScheduler.ReminderSummary summary = scheduler.processDueFeeReminders();

		assertThat(summary.failed()).isEqualTo(1);
		assertThat(summary.sent()).isZero();
	}

	private StudentFeeAssignment dueAssignment(boolean withContact) {
		Student student = new Student(
				"ADM-2026-0001",
				"Aarav",
				LocalDate.of(2014, 8, 17),
				Gender.MALE,
				LocalDate.of(2026, 4, 1));
		if (withContact) {
			student.updateProfile(
					"Aarav",
					null,
					"Sharma",
					LocalDate.of(2014, 8, 17),
					Gender.MALE,
					null,
					"parent@example.com",
					"9999999999",
					LocalDate.of(2026, 4, 1),
					null,
					null,
					null,
					null,
					null,
					null,
					null);
		}
		setId(student);
		FeeCategory category = new FeeCategory("TUITION", "Tuition", null, 1);
		setId(category);
		FeeStructure structure = new FeeStructure("2026-2027", "Class 6", "A", "Annual Fee", null);
		structure.addItem(category, new BigDecimal("1000.00"), true, 1);
		structure.addInstallment(1, "First Term", LocalDate.now().minusDays(1), new BigDecimal("1000.00"));
		structure.activate();
		setId(structure);
		structure.getItems().forEach(this::setId);
		structure.getInstallments().forEach(this::setId);
		StudentFeeAssignment assignment = new StudentFeeAssignment(student, structure, LocalDate.now().minusDays(10), null);
		structure.orderedInstallments().forEach(installment -> assignment.addInstallment(
				installment.getSequenceNo(),
				installment.getTitle(),
				installment.getDueDate(),
				installment.getAmount()));
		setId(assignment);
		assignment.getInstallments().forEach(this::setId);
		assertThat(assignment.getStatus()).isIn(FeeAssignmentStatus.PENDING, FeeAssignmentStatus.OVERDUE);
		return assignment;
	}

	private void setId(Object entity) {
		ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
	}
}
