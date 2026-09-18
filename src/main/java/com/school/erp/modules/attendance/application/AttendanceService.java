package com.school.erp.modules.attendance.application;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
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
import com.school.erp.modules.academic.domain.Teacher;
import com.school.erp.modules.academic.application.AcademicHierarchyService;
import com.school.erp.modules.academic.infrastructure.ClassTeacherMappingRepository;
import com.school.erp.modules.academic.infrastructure.SubjectTeacherMappingRepository;
import com.school.erp.modules.academic.infrastructure.TeacherRepository;
import com.school.erp.modules.auth.application.SchoolUserPrincipal;
import com.school.erp.modules.attendance.api.dto.AttendanceRecordResponse;
import com.school.erp.modules.attendance.api.dto.AttendanceSummaryResponse;
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
import com.school.erp.modules.students.domain.StudentStatus;
import com.school.erp.modules.students.infrastructure.StudentClassAssignmentRepository;
import com.school.erp.modules.students.infrastructure.StudentRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AttendanceService {

	private static final String MODULE_NAME = "ATTENDANCE";
	public static final List<String> ATTENDANCE_REPORT_HEADERS = List.of(
			"Date",
			"Admission Number",
			"Student Name",
			"Status",
			"Remarks");

	private final AcademicHierarchyService academicHierarchyService;
	private final StudentClassAssignmentRepository studentClassAssignmentRepository;
	private final StudentRepository studentRepository;
	private final AttendanceRecordRepository attendanceRecordRepository;
	private final TeacherRepository teacherRepository;
	private final ClassTeacherMappingRepository classTeacherMappingRepository;
	private final SubjectTeacherMappingRepository subjectTeacherMappingRepository;
	private final AuditLogService auditLogService;

	@Transactional(readOnly = true)
	public List<AttendanceStudentResponse> getStudents(UUID academicYearId, UUID classId, UUID sectionId) {
		return getStudents(academicYearId, classId, sectionId, LocalDate.now());
	}

	@Transactional(readOnly = true)
	public List<AttendanceStudentResponse> getStudents(
			UUID academicYearId,
			UUID classId,
			UUID sectionId,
			LocalDate attendanceDate) {
		validateHierarchy(academicYearId, classId, sectionId);
		validateTeacherAttendanceScope(academicYearId, classId, sectionId);
		return eligibleAssignments(academicYearId, classId, sectionId, resolveAttendanceDate(attendanceDate)).stream()
				.map(this::toStudentResponse)
				.toList();
	}

	@Transactional
	public DailyAttendanceResponse saveDaily(DailyAttendanceRequest request) {
		Hierarchy hierarchy = validateHierarchy(request.academicYearId(), request.classId(), request.sectionId());
		validateTeacherAttendanceScope(request.academicYearId(), request.classId(), request.sectionId());
		Map<UUID, StudentClassAssignment> assignmentByStudent = eligibleAssignments(
				request.academicYearId(),
				request.classId(),
				request.sectionId(),
				request.attendanceDate()).stream()
				.collect(Collectors.toMap(
						assignment -> assignment.getStudent().getId(),
						Function.identity(),
						(first, second) -> first,
						LinkedHashMap::new));
		validateRequestStudents(request.records(), assignmentByStudent);

		List<UUID> requestedStudentIds = request.records().stream()
				.map(DailyAttendanceRecordRequest::studentId)
				.toList();
		Map<UUID, AttendanceRecord> existingByStudent = attendanceRecordRepository.findByStudentIdsAndDate(
				requestedStudentIds,
				request.attendanceDate()).stream()
				.collect(Collectors.toMap(
						record -> record.getStudent().getId(),
						Function.identity(),
						(first, second) -> first,
						LinkedHashMap::new));

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
		validateTeacherAttendanceScope(academicYearId, classId, sectionId);
		Map<UUID, String> rollNumbers = new java.util.HashMap<>();
		for (StudentClassAssignment assignment : eligibleAssignments(academicYearId, classId, sectionId, attendanceDate)) {
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
		BigDecimal percentage = AttendanceSummaryCalculator.percentage(total, present, late, halfDay);
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
	public AttendanceSummaryResponse getClassSummary(
			UUID academicYearId,
			UUID classId,
			UUID sectionId,
			LocalDate fromDate,
			LocalDate toDate) {
		validateRequiredDateRange(fromDate, toDate);
		Hierarchy hierarchy = validateHierarchy(academicYearId, classId, sectionId);
		validateTeacherAttendanceScope(academicYearId, classId, sectionId);
		List<AttendanceRecord> records = attendanceRecordRepository.findClassSummaryRecords(
				academicYearId,
				classId,
				sectionId,
				fromDate,
				toDate);
		return toSummaryResponse(hierarchy, fromDate, toDate, records);
	}

	@Transactional(readOnly = true)
	public AttendanceSummaryResponse getMonthlySummary(
			UUID academicYearId,
			UUID classId,
			UUID sectionId,
			int year,
			int month) {
		YearMonth selectedMonth;
		try {
			selectedMonth = YearMonth.of(year, month);
		}
		catch (RuntimeException ex) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Month must be between 1 and 12.");
		}
		return getClassSummary(
				academicYearId,
				classId,
				sectionId,
				selectedMonth.atDay(1),
				selectedMonth.atEndOfMonth());
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
		List<Map<String, Object>> rows = reportRows(academicYearId, classId, sectionId, fromDate, toDate);
		StringBuilder csv = new StringBuilder(String.join(",", ATTENDANCE_REPORT_HEADERS)).append('\n');
		for (Map<String, Object> row : rows) {
			csv.append(row.get("Date")).append(',')
					.append(escape((String) row.get("Admission Number"))).append(',')
					.append(escape((String) row.get("Student Name"))).append(',')
					.append(row.get("Status")).append(',')
					.append(escape((String) row.get("Remarks"))).append('\n');
		}
		audit(
				"AttendanceRecord",
				sectionId,
				"ATTENDANCE_EXPORTED",
				null,
				Map.of("academicYearId", academicYearId, "classId", classId, "sectionId", sectionId, "fromDate", fromDate, "toDate", toDate));
		return csv.toString().getBytes(StandardCharsets.UTF_8);
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> reportRows(UUID academicYearId, UUID classId, UUID sectionId, LocalDate fromDate, LocalDate toDate) {
		validateRequiredDateRange(fromDate, toDate);
		validateHierarchy(academicYearId, classId, sectionId);
		validateTeacherAttendanceScope(academicYearId, classId, sectionId);
		return attendanceRecordRepository.findForExport(
				academicYearId,
				classId,
				sectionId,
				fromDate,
				toDate).stream()
				.map(this::toReportRow)
				.toList();
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

	private List<StudentClassAssignment> eligibleAssignments(
			UUID academicYearId,
			UUID classId,
			UUID sectionId,
			LocalDate attendanceDate) {
		return studentClassAssignmentRepository.findEligibleByHierarchyOnDate(
				academicYearId,
				classId,
				sectionId,
				resolveAttendanceDate(attendanceDate),
				StudentStatus.INACTIVE);
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
		return AttendanceSummaryCalculator.count(records, status, AttendanceRecord::getStatus);
	}

	private BigDecimal attendancePercentage(List<AttendanceRecord> records) {
		return AttendanceSummaryCalculator.percentage(records, AttendanceRecord::getStatus);
	}

	private AttendanceSummaryResponse toSummaryResponse(
			Hierarchy hierarchy,
			LocalDate fromDate,
			LocalDate toDate,
			List<AttendanceRecord> records) {
		return new AttendanceSummaryResponse(
				hierarchy.academicYear().getId(),
				hierarchy.academicYear().getName(),
				hierarchy.classEntity().getId(),
				hierarchy.classEntity().getName(),
				hierarchy.section().getId(),
				hierarchy.section().getName(),
				fromDate,
				toDate,
				studentClassAssignmentRepository.countEligibleStudents(
						hierarchy.academicYear().getId(),
						hierarchy.classEntity().getId(),
						hierarchy.section().getId(),
						toDate,
						StudentStatus.INACTIVE),
				records.size(),
				count(records, AttendanceStatus.PRESENT),
				count(records, AttendanceStatus.ABSENT),
				count(records, AttendanceStatus.LATE),
				count(records, AttendanceStatus.HALF_DAY),
				count(records, AttendanceStatus.LEAVE),
				attendancePercentage(records));
	}

	private Map<String, Object> toReportRow(AttendanceRecord record) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("Date", record.getAttendanceDate());
		row.put("Admission Number", record.getStudent().getAdmissionNumber());
		row.put("Student Name", record.getStudent().getDisplayName());
		row.put("Status", record.getStatus());
		row.put("Remarks", record.getRemarks());
		return row;
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

	private void validateRequiredDateRange(LocalDate fromDate, LocalDate toDate) {
		if (fromDate == null || toDate == null) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "From date and to date are required.");
		}
		validateDateRange(fromDate, toDate);
	}

	private LocalDate resolveAttendanceDate(LocalDate attendanceDate) {
		return attendanceDate == null ? LocalDate.now() : attendanceDate;
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

	private void validateTeacherAttendanceScope(UUID academicYearId, UUID classId, UUID sectionId) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null
				|| !authentication.isAuthenticated()
				|| authentication instanceof AnonymousAuthenticationToken
				|| !hasAuthority(authentication, "ROLE_TEACHER")
				|| hasAnyAuthority(authentication, "ROLE_SUPER_ADMIN", "ROLE_ADMIN", "ROLE_PRINCIPAL")) {
			return;
		}
		UUID userAccountId = currentUserAccountId(authentication);
		if (userAccountId == null) {
			throw new BusinessException(ErrorCode.FORBIDDEN, "Teacher attendance account is not linked to a user id.");
		}
		Teacher teacher = teacherRepository.findByUserAccountIdAndDeletedFalse(userAccountId)
				.orElseThrow(() -> new BusinessException(
						ErrorCode.FORBIDDEN,
						"Teacher attendance account is not linked to a teacher profile."));
		boolean classTeacher = classTeacherMappingRepository
				.existsByTeacherIdAndClassEntityIdAndSectionIdAndActiveTrueAndDeletedFalse(
						teacher.getId(),
						classId,
						sectionId);
		boolean subjectTeacher = subjectTeacherMappingRepository
				.existsByTeacherIdAndClassEntityIdAndSectionIdAndActiveTrueAndDeletedFalse(
						teacher.getId(),
						classId,
						sectionId);
		if (!classTeacher && !subjectTeacher) {
			throw new BusinessException(
					ErrorCode.FORBIDDEN,
					"Teacher is not assigned to the selected class and division.");
		}
	}

	private boolean hasAuthority(Authentication authentication, String authority) {
		return authentication.getAuthorities().stream()
				.anyMatch(granted -> granted.getAuthority().equals(authority));
	}

	private boolean hasAnyAuthority(Authentication authentication, String... authorities) {
		for (String authority : authorities) {
			if (hasAuthority(authentication, authority)) {
				return true;
			}
		}
		return false;
	}

	private UUID currentUserAccountId(Authentication authentication) {
		Object principal = authentication.getPrincipal();
		if (principal instanceof SchoolUserPrincipal userPrincipal) {
			return userPrincipal.getId();
		}
		return parseUuid(authentication.getName());
	}

	private UUID parseUuid(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		try {
			return UUID.fromString(value);
		}
		catch (IllegalArgumentException ex) {
			return null;
		}
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
