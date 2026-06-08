package com.school.erp.modules.attendance.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.school.erp.common.audit.application.AuditLogEvent;
import com.school.erp.common.audit.application.AuditLogService;
import com.school.erp.common.api.PageRequestDto;
import com.school.erp.common.api.PageResponse;
import com.school.erp.common.exception.BusinessException;
import com.school.erp.common.exception.ErrorCode;
import com.school.erp.common.exception.ResourceNotFoundException;
import com.school.erp.modules.academic.domain.AcademicYear;
import com.school.erp.modules.academic.domain.ClassEntity;
import com.school.erp.modules.academic.domain.SectionEntity;
import com.school.erp.modules.academic.application.AcademicHierarchyService;
import com.school.erp.modules.attendance.api.dto.AttendanceRecordResponse;
import com.school.erp.modules.attendance.api.dto.AttendanceStudentResponse;
import com.school.erp.modules.attendance.api.dto.DailyAttendanceRecordRequest;
import com.school.erp.modules.attendance.api.dto.DailyAttendanceRequest;
import com.school.erp.modules.attendance.api.dto.DailyAttendanceResponse;
import com.school.erp.modules.attendance.api.dto.StudentAttendanceHistoryRecordResponse;
import com.school.erp.modules.attendance.api.dto.StudentAttendanceHistoryResponse;
import com.school.erp.modules.attendance.api.dto.StudentAttendanceSummaryResponse;
import com.school.erp.modules.attendance.domain.AttendanceRecord;
import com.school.erp.modules.attendance.domain.AttendanceStatus;
import com.school.erp.modules.attendance.infrastructure.AttendanceRecordRepository;
import com.school.erp.modules.students.domain.Student;
import com.school.erp.modules.students.domain.StudentClassAssignment;
import com.school.erp.modules.students.infrastructure.StudentClassAssignmentRepository;
import com.school.erp.modules.students.infrastructure.StudentRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AttendanceService {

	private static final String MODULE_NAME = "ATTENDANCE";

	private final AcademicHierarchyService academicHierarchyService;
	private final StudentClassAssignmentRepository studentClassAssignmentRepository;
	private final StudentRepository studentRepository;
	private final AttendanceRecordRepository attendanceRecordRepository;
	private final AuditLogService auditLogService;

	@Transactional(readOnly = true)
	public List<AttendanceStudentResponse> getStudents(UUID academicYearId, UUID classId, UUID sectionId) {
		validateHierarchy(academicYearId, classId, sectionId);
		return activeAssignments(academicYearId, classId, sectionId).stream()
				.map(this::toStudentResponse)
				.toList();
	}

	@Transactional
	public DailyAttendanceResponse saveDaily(DailyAttendanceRequest request) {
		Hierarchy hierarchy = validateHierarchy(request.academicYearId(), request.classId(), request.sectionId());
		Map<UUID, StudentClassAssignment> assignmentByStudent = activeAssignments(
				request.academicYearId(),
				request.classId(),
				request.sectionId()).stream()
				.collect(Collectors.toMap(
						assignment -> assignment.getStudent().getId(),
						Function.identity(),
						(first, second) -> first,
						LinkedHashMap::new));
		validateRequestStudents(request.records(), assignmentByStudent);

		Map<UUID, AttendanceRecord> existingByStudent = attendanceRecordRepository.findDailyRecords(
				request.academicYearId(),
				request.classId(),
				request.sectionId(),
				request.attendanceDate()).stream()
				.collect(Collectors.toMap(record -> record.getStudent().getId(), Function.identity()));

		for (DailyAttendanceRecordRequest recordRequest : request.records()) {
			AttendanceRecord record = existingByStudent.get(recordRequest.studentId());
			if (record == null) {
				Student student = assignmentByStudent.get(recordRequest.studentId()).getStudent();
				attendanceRecordRepository.save(new AttendanceRecord(
						hierarchy.academicYear(),
						hierarchy.classEntity(),
						hierarchy.section(),
						student,
						request.attendanceDate(),
						recordRequest.status(),
						recordRequest.remarks()));
			}
			else {
				record.update(recordRequest.status(), recordRequest.remarks());
			}
		}

		DailyAttendanceResponse response = getDaily(
				request.academicYearId(),
				request.classId(),
				request.sectionId(),
				request.attendanceDate());
		audit(
				"AttendanceRecord",
				request.sectionId(),
				"ATTENDANCE_SAVED",
				null,
				Map.of(
						"academicYearId", request.academicYearId(),
						"classId", request.classId(),
						"sectionId", request.sectionId(),
						"attendanceDate", request.attendanceDate(),
						"records", request.records().size()));
		return response;
	}

	@Transactional(readOnly = true)
	public DailyAttendanceResponse getDaily(UUID academicYearId, UUID classId, UUID sectionId, LocalDate attendanceDate) {
		validateHierarchy(academicYearId, classId, sectionId);
		Map<UUID, String> rollNumbers = new java.util.HashMap<>();
		for (StudentClassAssignment assignment : activeAssignments(academicYearId, classId, sectionId)) {
			rollNumbers.put(assignment.getStudent().getId(), assignment.getRollNumber());
		}
		List<AttendanceRecordResponse> records = attendanceRecordRepository.findDailyRecords(
				academicYearId,
				classId,
				sectionId,
				attendanceDate).stream()
				.map(record -> toRecordResponse(record, rollNumbers.get(record.getStudent().getId())))
				.toList();
		return toDailyResponse(academicYearId, classId, sectionId, attendanceDate, records);
	}

	@Transactional(readOnly = true)
	public StudentAttendanceSummaryResponse getStudentSummary(UUID studentId, UUID academicYearId) {
		AcademicYear academicYear = academicHierarchyService.loadAcademicYear(academicYearId);
		Student student = studentRepository.findByIdAndDeletedFalse(studentId)
				.orElseThrow(() -> new ResourceNotFoundException("Student", studentId));
		List<AttendanceRecord> records = attendanceRecordRepository.findByStudentAndAcademicYear(studentId, academicYearId);
		long total = records.size();
		long present = count(records, AttendanceStatus.PRESENT);
		long absent = count(records, AttendanceStatus.ABSENT);
		long late = count(records, AttendanceStatus.LATE);
		long halfDay = count(records, AttendanceStatus.HALF_DAY);
		long leave = count(records, AttendanceStatus.LEAVE);
		BigDecimal percentage = total == 0
				? BigDecimal.ZERO
				: BigDecimal.valueOf(present + late)
						.add(BigDecimal.valueOf(halfDay).multiply(BigDecimal.valueOf(0.5)))
						.multiply(BigDecimal.valueOf(100))
						.divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
		return new StudentAttendanceSummaryResponse(
				studentId,
				student.getDisplayName(),
				academicYear.getId(),
				total,
				present,
				absent,
				late,
				halfDay,
				leave,
				percentage);
	}

	@Transactional(readOnly = true)
	public StudentAttendanceHistoryResponse getStudentAttendanceHistory(
			UUID studentId,
			UUID academicYearId,
			LocalDate fromDate,
			LocalDate toDate,
			AttendanceStatus status,
			PageRequestDto pageRequest,
			String sort) {
		validateDateRange(fromDate, toDate);
		AcademicYear selectedYear = academicYearId == null ? null : academicHierarchyService.loadAcademicYear(academicYearId);
		Student student = studentRepository.findProfileByIdAndDeletedFalse(studentId)
				.orElseThrow(() -> new ResourceNotFoundException("Student", studentId));
		Pageable pageable = studentHistoryPageable(pageRequest, sort);
		Page<AttendanceRecord> page = attendanceRecordRepository.findStudentHistory(
				studentId,
				academicYearId,
				fromDate,
				toDate,
				status,
				pageable);
		List<AttendanceRecord> summaryRecords = attendanceRecordRepository.findStudentHistoryForSummary(
				studentId,
				academicYearId,
				fromDate,
				toDate,
				status);
		AttendanceContext context = attendanceContext(student, selectedYear, summaryRecords);
		return new StudentAttendanceHistoryResponse(
				studentId,
				student.getDisplayName(),
				context.academicYearId(),
				context.academicYear(),
				context.classId(),
				context.className(),
				context.sectionId(),
				context.sectionName(),
				summaryRecords.size(),
				count(summaryRecords, AttendanceStatus.PRESENT),
				count(summaryRecords, AttendanceStatus.ABSENT),
				count(summaryRecords, AttendanceStatus.LATE),
				count(summaryRecords, AttendanceStatus.HALF_DAY),
				count(summaryRecords, AttendanceStatus.LEAVE),
				attendancePercentage(summaryRecords),
				PageResponse.from(page, this::toHistoryRecordResponse));
	}

	@Transactional(readOnly = true)
	public byte[] exportStudentAttendanceHistory(
			UUID studentId,
			UUID academicYearId,
			LocalDate fromDate,
			LocalDate toDate,
			AttendanceStatus status) {
		validateDateRange(fromDate, toDate);
		if (academicYearId != null) {
			academicHierarchyService.loadAcademicYear(academicYearId);
		}
		Student student = studentRepository.findByIdAndDeletedFalse(studentId)
				.orElseThrow(() -> new ResourceNotFoundException("Student", studentId));
		List<AttendanceRecord> records = attendanceRecordRepository.findStudentHistoryForSummary(
				studentId,
				academicYearId,
				fromDate,
				toDate,
				status);
		StringBuilder csv = new StringBuilder("Student,Admission Number,Date,Status,Remarks,Marked By,Marked At,Updated By,Updated At\n");
		for (AttendanceRecord record : records) {
			csv.append(escape(student.getDisplayName())).append(',')
					.append(escape(student.getAdmissionNumber())).append(',')
					.append(record.getAttendanceDate()).append(',')
					.append(record.getStatus()).append(',')
					.append(escape(record.getRemarks())).append(',')
					.append(escape(record.getCreatedBy())).append(',')
					.append(record.getCreatedAt()).append(',')
					.append(escape(record.getUpdatedBy())).append(',')
					.append(record.getUpdatedAt()).append('\n');
		}
		return csv.toString().getBytes(StandardCharsets.UTF_8);
	}

	@Transactional(readOnly = true)
	public byte[] export(UUID academicYearId, UUID classId, UUID sectionId, LocalDate fromDate, LocalDate toDate) {
		if (fromDate.isAfter(toDate)) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "From date must be before or equal to to date.");
		}
		validateHierarchy(academicYearId, classId, sectionId);
		StringBuilder csv = new StringBuilder("Date,Admission Number,Student Name,Status,Remarks\n");
		for (AttendanceRecord record : attendanceRecordRepository.findForExport(
				academicYearId,
				classId,
				sectionId,
				fromDate,
				toDate)) {
			csv.append(record.getAttendanceDate()).append(',')
					.append(escape(record.getStudent().getAdmissionNumber())).append(',')
					.append(escape(record.getStudent().getDisplayName())).append(',')
					.append(record.getStatus()).append(',')
					.append(escape(record.getRemarks())).append('\n');
		}
		audit(
				"AttendanceRecord",
				sectionId,
				"ATTENDANCE_EXPORTED",
				null,
				Map.of("academicYearId", academicYearId, "classId", classId, "sectionId", sectionId, "fromDate", fromDate, "toDate", toDate));
		return csv.toString().getBytes(StandardCharsets.UTF_8);
	}

	private Hierarchy validateHierarchy(UUID academicYearId, UUID classId, UUID sectionId) {
		AcademicYear academicYear = academicHierarchyService.loadAcademicYear(academicYearId);
		ClassEntity classEntity = academicHierarchyService.loadClass(classId);
		if (!classEntity.getAcademicYear().getId().equals(academicYearId)) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Class does not belong to the selected academic year.");
		}
		SectionEntity section = academicHierarchyService.loadSectionForClass(classId, sectionId);
		return new Hierarchy(academicYear, classEntity, section);
	}

	private List<StudentClassAssignment> activeAssignments(UUID academicYearId, UUID classId, UUID sectionId) {
		return studentClassAssignmentRepository.findActiveByHierarchy(academicYearId, classId, sectionId);
	}

	private void validateRequestStudents(
			List<DailyAttendanceRecordRequest> records,
			Map<UUID, StudentClassAssignment> assignmentByStudent) {
		java.util.Set<UUID> seen = new java.util.HashSet<>();
		for (DailyAttendanceRecordRequest record : records) {
			if (!seen.add(record.studentId())) {
				throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Duplicate attendance record for student: " + record.studentId());
			}
			if (!assignmentByStudent.containsKey(record.studentId())) {
				throw new BusinessException(
						ErrorCode.BUSINESS_RULE_VIOLATION,
						"Student does not belong to selected academic year, class, and division.");
			}
		}
	}

	private DailyAttendanceResponse toDailyResponse(
			UUID academicYearId,
			UUID classId,
			UUID sectionId,
			LocalDate attendanceDate,
			List<AttendanceRecordResponse> records) {
		return new DailyAttendanceResponse(
				academicYearId,
				classId,
				sectionId,
				attendanceDate,
				records.size(),
				records.stream().filter(record -> record.status() == AttendanceStatus.PRESENT).count(),
				records.stream().filter(record -> record.status() == AttendanceStatus.ABSENT).count(),
				records.stream().filter(record -> record.status() == AttendanceStatus.LATE).count(),
				records.stream().filter(record -> record.status() == AttendanceStatus.HALF_DAY).count(),
				records.stream().filter(record -> record.status() == AttendanceStatus.LEAVE).count(),
				records);
	}

	private AttendanceRecordResponse toRecordResponse(AttendanceRecord record, String rollNumber) {
		return new AttendanceRecordResponse(
				record.getId(),
				record.getAcademicYear().getId(),
				record.getClassEntity().getId(),
				record.getSection().getId(),
				record.getAttendanceDate(),
				toStudentResponse(record.getStudent(), rollNumber),
				record.getStatus(),
				record.getRemarks());
	}

	private AttendanceStudentResponse toStudentResponse(StudentClassAssignment assignment) {
		return toStudentResponse(assignment.getStudent(), assignment.getRollNumber());
	}

	private AttendanceStudentResponse toStudentResponse(Student student, String rollNumber) {
		return new AttendanceStudentResponse(
				student.getId(),
				student.getAdmissionNumber(),
				rollNumber,
				student.getFirstName(),
				student.getMiddleName(),
				student.getLastName(),
				student.getDisplayName(),
				student.getStatus());
	}

	private long count(List<AttendanceRecord> records, AttendanceStatus status) {
		return records.stream().filter(record -> record.getStatus() == status).count();
	}

	private BigDecimal attendancePercentage(List<AttendanceRecord> records) {
		long total = records.size();
		if (total == 0) {
			return BigDecimal.ZERO;
		}
		return BigDecimal.valueOf(count(records, AttendanceStatus.PRESENT) + count(records, AttendanceStatus.LATE))
				.add(BigDecimal.valueOf(count(records, AttendanceStatus.HALF_DAY)).multiply(BigDecimal.valueOf(0.5)))
				.multiply(BigDecimal.valueOf(100))
				.divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
	}

	private StudentAttendanceHistoryRecordResponse toHistoryRecordResponse(AttendanceRecord record) {
		return new StudentAttendanceHistoryRecordResponse(
				record.getAttendanceDate(),
				record.getStatus(),
				record.getRemarks(),
				record.getCreatedBy(),
				record.getCreatedAt(),
				record.getUpdatedBy(),
				record.getUpdatedAt());
	}

	private AttendanceContext attendanceContext(
			Student student,
			AcademicYear selectedYear,
			List<AttendanceRecord> records) {
		if (!records.isEmpty()) {
			AttendanceRecord first = records.getFirst();
			return new AttendanceContext(
					first.getAcademicYear().getId(),
					first.getAcademicYear().getName(),
					first.getClassEntity().getId(),
					first.getClassEntity().getName(),
					first.getSection().getId(),
					first.getSection().getName());
		}
		StudentClassAssignment assignment = student.getClassAssignments().stream()
				.filter(existing -> !existing.isDeleted())
				.filter(existing -> selectedYear == null || existing.isForAcademicYear(selectedYear))
				.filter(StudentClassAssignment::isActive)
				.findFirst()
				.or(() -> student.getCurrentAssignment())
				.orElse(null);
		if (assignment == null) {
			return new AttendanceContext(
					selectedYear == null ? null : selectedYear.getId(),
					selectedYear == null ? null : selectedYear.getName(),
					null,
					null,
					null,
					null);
		}
		return new AttendanceContext(
				assignment.getAcademicYearEntity() == null ? selectedYear == null ? null : selectedYear.getId() : assignment.getAcademicYearEntity().getId(),
				assignment.getAcademicYearEntity() == null ? assignment.getAcademicYear() : assignment.getAcademicYearEntity().getName(),
				assignment.getClassEntity() == null ? null : assignment.getClassEntity().getId(),
				assignment.getClassName(),
				assignment.getSectionEntity() == null ? null : assignment.getSectionEntity().getId(),
				assignment.getSectionName());
	}

	private void validateDateRange(LocalDate fromDate, LocalDate toDate) {
		if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "From date must be before or equal to to date.");
		}
	}

	private Pageable studentHistoryPageable(PageRequestDto pageRequest, String sort) {
		int page = pageRequest == null ? 0 : pageRequest.page();
		int size = pageRequest == null ? 20 : pageRequest.size();
		Sort.Direction direction = Sort.Direction.DESC;
		String property = "attendanceDate";
		if (StringUtils.hasText(sort)) {
			String[] parts = sort.split(",");
			property = attendanceSortProperty(parts[0]);
			if (parts.length > 1) {
				direction = sortDirection(parts[1], direction);
			}
		}
		else if (pageRequest != null && StringUtils.hasText(pageRequest.sortBy())) {
			property = attendanceSortProperty(pageRequest.sortBy());
			direction = pageRequest.direction();
		}
		return PageRequest.of(page, size, Sort.by(direction, property));
	}

	private Sort.Direction sortDirection(String value, Sort.Direction defaultDirection) {
		try {
			return Sort.Direction.fromString(value);
		}
		catch (IllegalArgumentException ex) {
			return defaultDirection;
		}
	}

	private String attendanceSortProperty(String value) {
		if (!StringUtils.hasText(value)) {
			return "attendanceDate";
		}
		return switch (value.trim()) {
			case "status" -> "status";
			case "markedAt", "createdAt" -> "createdAt";
			case "updatedAt" -> "updatedAt";
			default -> "attendanceDate";
		};
	}

	private String escape(String value) {
		if (value == null) {
			return "";
		}
		if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
			return "\"" + value.replace("\"", "\"\"") + "\"";
		}
		return value;
	}

	private void audit(String entityName, UUID entityId, String action, Object oldValue, Object newValue) {
		auditLogService.record(new AuditLogEvent(
				MODULE_NAME,
				entityName,
				entityId == null ? null : entityId.toString(),
				action,
				oldValue,
				newValue));
	}

	private record Hierarchy(AcademicYear academicYear, ClassEntity classEntity, SectionEntity section) {
	}

	private record AttendanceContext(
			UUID academicYearId,
			String academicYear,
			UUID classId,
			String className,
			UUID sectionId,
			String sectionName) {
	}
}
