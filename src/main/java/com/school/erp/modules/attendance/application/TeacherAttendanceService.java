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

import com.school.erp.common.api.PageRequestDto;
import com.school.erp.common.api.PageResponse;
import com.school.erp.common.audit.application.AuditLogEvent;
import com.school.erp.common.audit.application.AuditLogService;
import com.school.erp.common.exception.BusinessException;
import com.school.erp.common.exception.ErrorCode;
import com.school.erp.common.exception.ResourceNotFoundException;
import com.school.erp.modules.academic.application.AcademicHierarchyService;
import com.school.erp.modules.academic.domain.AcademicYear;
import com.school.erp.modules.academic.domain.Teacher;
import com.school.erp.modules.academic.infrastructure.TeacherRepository;
import com.school.erp.modules.attendance.api.dto.TeacherAttendanceHistoryRecordResponse;
import com.school.erp.modules.attendance.api.dto.TeacherAttendanceHistoryResponse;
import com.school.erp.modules.attendance.api.dto.TeacherAttendanceRecordResponse;
import com.school.erp.modules.attendance.api.dto.TeacherAttendanceTeacherResponse;
import com.school.erp.modules.attendance.api.dto.TeacherDailyAttendanceRecordRequest;
import com.school.erp.modules.attendance.api.dto.TeacherDailyAttendanceRequest;
import com.school.erp.modules.attendance.api.dto.TeacherDailyAttendanceResponse;
import com.school.erp.modules.attendance.domain.AttendanceStatus;
import com.school.erp.modules.attendance.domain.TeacherAttendanceRecord;
import com.school.erp.modules.attendance.infrastructure.TeacherAttendanceRecordRepository;
import com.school.erp.modules.teachers.domain.TeacherAssignmentStatus;
import com.school.erp.modules.teachers.infrastructure.TeacherAcademicAssignmentRepository;

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
public class TeacherAttendanceService {

	private static final String MODULE_NAME = "TEACHER_ATTENDANCE";

	private final AcademicHierarchyService academicHierarchyService;
	private final TeacherRepository teacherRepository;
	private final TeacherAcademicAssignmentRepository teacherAssignmentRepository;
	private final TeacherAttendanceRecordRepository attendanceRecordRepository;
	private final AuditLogService auditLogService;

	@Transactional(readOnly = true)
	public List<TeacherAttendanceTeacherResponse> getTeachers(UUID academicYearId) {
		academicHierarchyService.loadAcademicYear(academicYearId);
		return attendanceTeachers(academicYearId).stream()
				.map(this::toTeacherResponse)
				.toList();
	}

	@Transactional
	public TeacherDailyAttendanceResponse saveDaily(TeacherDailyAttendanceRequest request) {
		AcademicYear academicYear = academicHierarchyService.loadAcademicYear(request.academicYearId());
		Map<UUID, Teacher> teachersById = attendanceTeachers(request.academicYearId()).stream()
				.collect(Collectors.toMap(
						Teacher::getId,
						Function.identity(),
						(first, second) -> first,
						LinkedHashMap::new));
		validateRequestTeachers(request.records(), teachersById);

		Map<UUID, TeacherAttendanceRecord> existingByTeacher = attendanceRecordRepository
				.findDailyRecords(request.academicYearId(), request.attendanceDate()).stream()
				.collect(Collectors.toMap(record -> record.getTeacher().getId(), Function.identity()));

		for (TeacherDailyAttendanceRecordRequest recordRequest : request.records()) {
			TeacherAttendanceRecord record = existingByTeacher.get(recordRequest.teacherId());
			if (record == null) {
				TeacherAttendanceRecord saved = attendanceRecordRepository.save(new TeacherAttendanceRecord(
						academicYear,
						teachersById.get(recordRequest.teacherId()),
						request.attendanceDate(),
						recordRequest.status(),
						recordRequest.remarks()));
				audit(
						"TeacherAttendanceRecord",
						saved.getId(),
						"TEACHER_ATTENDANCE_CREATED",
						null,
						toRecordResponse(saved));
			}
			else {
				TeacherAttendanceRecordResponse oldValue = toRecordResponse(record);
				record.update(recordRequest.status(), recordRequest.remarks());
				audit(
						"TeacherAttendanceRecord",
						record.getId(),
						"TEACHER_ATTENDANCE_UPDATED",
						oldValue,
						toRecordResponse(record));
			}
		}

		TeacherDailyAttendanceResponse response = getDaily(request.academicYearId(), request.attendanceDate());
		audit(
				"TeacherAttendanceDaily",
				request.academicYearId(),
				"TEACHER_ATTENDANCE_SAVED",
				null,
				Map.of(
						"academicYearId", request.academicYearId(),
						"attendanceDate", request.attendanceDate(),
						"records", request.records().size()));
		return response;
	}

	@Transactional(readOnly = true)
	public TeacherDailyAttendanceResponse getDaily(UUID academicYearId, LocalDate attendanceDate) {
		academicHierarchyService.loadAcademicYear(academicYearId);
		List<TeacherAttendanceRecordResponse> records = attendanceRecordRepository
				.findDailyRecords(academicYearId, attendanceDate).stream()
				.map(this::toRecordResponse)
				.toList();
		return toDailyResponse(academicYearId, attendanceDate, records);
	}

	@Transactional(readOnly = true)
	public TeacherAttendanceHistoryResponse getTeacherAttendanceHistory(
			UUID teacherId,
			UUID academicYearId,
			LocalDate fromDate,
			LocalDate toDate,
			AttendanceStatus status,
			PageRequestDto pageRequest,
			String sort) {
		validateDateRange(fromDate, toDate);
		AcademicYear selectedYear = academicYearId == null ? null : academicHierarchyService.loadAcademicYear(academicYearId);
		Teacher teacher = loadTeacher(teacherId);
		Pageable pageable = historyPageable(pageRequest, sort);
		Page<TeacherAttendanceRecord> page = attendanceRecordRepository.findTeacherHistory(
				teacherId,
				academicYearId,
				fromDate,
				toDate,
				status,
				pageable);
		List<TeacherAttendanceRecord> summaryRecords = attendanceRecordRepository.findTeacherHistoryForSummary(
				teacherId,
				academicYearId,
				fromDate,
				toDate,
				status);
		AttendanceContext context = attendanceContext(selectedYear, summaryRecords);
		return new TeacherAttendanceHistoryResponse(
				teacherId,
				teacher.getDisplayName(),
				teacher.getEmployeeNumber(),
				context.academicYearId(),
				context.academicYear(),
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
	public byte[] export(UUID academicYearId, LocalDate fromDate, LocalDate toDate) {
		validateDateRange(fromDate, toDate);
		academicHierarchyService.loadAcademicYear(academicYearId);
		StringBuilder csv = new StringBuilder("Date,Employee Number,Teacher Name,Status,Remarks\n");
		for (TeacherAttendanceRecord record : attendanceRecordRepository.findForExport(
				academicYearId,
				fromDate,
				toDate)) {
			csv.append(record.getAttendanceDate()).append(',')
					.append(escape(record.getTeacher().getEmployeeNumber())).append(',')
					.append(escape(record.getTeacher().getDisplayName())).append(',')
					.append(record.getStatus()).append(',')
					.append(escape(record.getRemarks())).append('\n');
		}
		audit(
				"TeacherAttendanceExport",
				academicYearId,
				"TEACHER_ATTENDANCE_EXPORTED",
				null,
				Map.of("academicYearId", academicYearId, "fromDate", fromDate, "toDate", toDate));
		return csv.toString().getBytes(StandardCharsets.UTF_8);
	}

	private void validateRequestTeachers(
			List<TeacherDailyAttendanceRecordRequest> records,
			Map<UUID, Teacher> teachersById) {
		java.util.Set<UUID> seen = new java.util.HashSet<>();
		for (TeacherDailyAttendanceRecordRequest record : records) {
			if (!seen.add(record.teacherId())) {
				throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Duplicate attendance record for teacher: " + record.teacherId());
			}
			if (!teachersById.containsKey(record.teacherId())) {
				throw new BusinessException(
						ErrorCode.BUSINESS_RULE_VIOLATION,
						"Teacher is inactive or does not exist.");
			}
		}
	}

	private List<Teacher> attendanceTeachers(UUID academicYearId) {
		List<Teacher> assignedTeachers = teacherAssignmentRepository.findActiveTeachersByAcademicYearId(
				academicYearId,
				TeacherAssignmentStatus.ACTIVE);
		if (!assignedTeachers.isEmpty()) {
			return assignedTeachers;
		}
		return teacherRepository.findAllByDeletedFalseAndActiveTrueOrderByFirstNameAscLastNameAsc();
	}

	private TeacherDailyAttendanceResponse toDailyResponse(
			UUID academicYearId,
			LocalDate attendanceDate,
			List<TeacherAttendanceRecordResponse> records) {
		return new TeacherDailyAttendanceResponse(
				academicYearId,
				attendanceDate,
				records.size(),
				records.stream().filter(record -> record.status() == AttendanceStatus.PRESENT).count(),
				records.stream().filter(record -> record.status() == AttendanceStatus.ABSENT).count(),
				records.stream().filter(record -> record.status() == AttendanceStatus.LATE).count(),
				records.stream().filter(record -> record.status() == AttendanceStatus.HALF_DAY).count(),
				records.stream().filter(record -> record.status() == AttendanceStatus.LEAVE).count(),
				records);
	}

	private TeacherAttendanceRecordResponse toRecordResponse(TeacherAttendanceRecord record) {
		return new TeacherAttendanceRecordResponse(
				record.getId(),
				record.getAcademicYear().getId(),
				record.getAttendanceDate(),
				toTeacherResponse(record.getTeacher()),
				record.getStatus(),
				record.getRemarks());
	}

	private TeacherAttendanceTeacherResponse toTeacherResponse(Teacher teacher) {
		return new TeacherAttendanceTeacherResponse(
				teacher.getId(),
				teacher.getEmployeeNumber(),
				teacher.getFirstName(),
				teacher.getMiddleName(),
				teacher.getLastName(),
				teacher.getDisplayName(),
				teacher.getEmail(),
				teacher.getMobileNumber(),
				teacher.getStatus());
	}

	private TeacherAttendanceHistoryRecordResponse toHistoryRecordResponse(TeacherAttendanceRecord record) {
		return new TeacherAttendanceHistoryRecordResponse(
				record.getAttendanceDate(),
				record.getStatus(),
				record.getRemarks(),
				record.getCreatedBy(),
				record.getCreatedAt(),
				record.getUpdatedBy(),
				record.getUpdatedAt());
	}

	private long count(List<TeacherAttendanceRecord> records, AttendanceStatus status) {
		return records.stream().filter(record -> record.getStatus() == status).count();
	}

	private BigDecimal attendancePercentage(List<TeacherAttendanceRecord> records) {
		long total = records.size();
		if (total == 0) {
			return BigDecimal.ZERO;
		}
		return BigDecimal.valueOf(count(records, AttendanceStatus.PRESENT) + count(records, AttendanceStatus.LATE))
				.add(BigDecimal.valueOf(count(records, AttendanceStatus.HALF_DAY)).multiply(BigDecimal.valueOf(0.5)))
				.multiply(BigDecimal.valueOf(100))
				.divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
	}

	private AttendanceContext attendanceContext(AcademicYear selectedYear, List<TeacherAttendanceRecord> records) {
		if (!records.isEmpty()) {
			TeacherAttendanceRecord first = records.getFirst();
			return new AttendanceContext(first.getAcademicYear().getId(), first.getAcademicYear().getName());
		}
		return new AttendanceContext(
				selectedYear == null ? null : selectedYear.getId(),
				selectedYear == null ? null : selectedYear.getName());
	}

	private void validateDateRange(LocalDate fromDate, LocalDate toDate) {
		if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "From date must be before or equal to to date.");
		}
	}

	private Pageable historyPageable(PageRequestDto pageRequest, String sort) {
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

	private Teacher loadTeacher(UUID teacherId) {
		return teacherRepository.findByIdAndDeletedFalse(teacherId)
				.orElseThrow(() -> new ResourceNotFoundException("Teacher", teacherId));
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

	private record AttendanceContext(UUID academicYearId, String academicYear) {
	}
}
