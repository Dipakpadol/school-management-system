package com.school.erp.modules.exams.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.school.erp.common.audit.application.AuditLogEvent;
import com.school.erp.common.audit.application.AuditLogService;
import com.school.erp.common.exception.BusinessException;
import com.school.erp.common.exception.ErrorCode;
import com.school.erp.common.exception.ResourceNotFoundException;
import com.school.erp.modules.academic.api.dto.DivisionSubjectResponse;
import com.school.erp.modules.academic.application.AcademicHierarchyService;
import com.school.erp.modules.academic.domain.AcademicYear;
import com.school.erp.modules.academic.domain.ClassEntity;
import com.school.erp.modules.academic.domain.SectionEntity;
import com.school.erp.modules.academic.domain.Subject;
import com.school.erp.modules.academic.infrastructure.DivisionSubjectRepository;
import com.school.erp.modules.exams.api.dto.ExamMarkResponse;
import com.school.erp.modules.exams.api.dto.ExamScheduleRequest;
import com.school.erp.modules.exams.api.dto.ExamScheduleResponse;
import com.school.erp.modules.exams.api.dto.ExamStudentResponse;
import com.school.erp.modules.exams.api.dto.ExamTypeRequest;
import com.school.erp.modules.exams.api.dto.ExamTypeResponse;
import com.school.erp.modules.exams.api.dto.GenerateResultRequest;
import com.school.erp.modules.exams.api.dto.MarksEntryRecordRequest;
import com.school.erp.modules.exams.api.dto.MarksEntryRequest;
import com.school.erp.modules.exams.api.dto.MarksEntryResponse;
import com.school.erp.modules.exams.api.dto.StudentResultResponse;
import com.school.erp.modules.exams.api.dto.SubjectResultResponse;
import com.school.erp.modules.exams.domain.ExamMark;
import com.school.erp.modules.exams.domain.ExamSchedule;
import com.school.erp.modules.exams.domain.ExamType;
import com.school.erp.modules.exams.infrastructure.ExamMarkRepository;
import com.school.erp.modules.exams.infrastructure.ExamScheduleRepository;
import com.school.erp.modules.exams.infrastructure.ExamTypeRepository;
import com.school.erp.modules.students.domain.Student;
import com.school.erp.modules.students.domain.StudentClassAssignment;
import com.school.erp.modules.students.infrastructure.StudentClassAssignmentRepository;
import com.school.erp.modules.students.infrastructure.StudentRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExamService {

	private static final String MODULE_NAME = "EXAMS";
	private static final BigDecimal PASS_PERCENTAGE = BigDecimal.valueOf(35);

	private final AcademicHierarchyService academicHierarchyService;
	private final StudentClassAssignmentRepository studentClassAssignmentRepository;
	private final StudentRepository studentRepository;
	private final DivisionSubjectRepository divisionSubjectRepository;
	private final ExamTypeRepository examTypeRepository;
	private final ExamScheduleRepository examScheduleRepository;
	private final ExamMarkRepository examMarkRepository;
	private final AuditLogService auditLogService;

	@Transactional(readOnly = true)
	public List<ExamStudentResponse> getStudents(UUID academicYearId, UUID classId, UUID sectionId) {
		validateHierarchy(academicYearId, classId, sectionId);
		return activeAssignments(academicYearId, classId, sectionId).stream()
				.map(this::toExamStudent)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<DivisionSubjectResponse> getSubjects(UUID classId, UUID sectionId) {
		academicHierarchyService.loadSectionForClass(classId, sectionId);
		return academicHierarchyService.getDivisionSubjects(sectionId);
	}

	@Transactional
	public ExamTypeResponse createType(ExamTypeRequest request) {
		validateTypeCode(request.code(), null);
		ExamType type = examTypeRepository.save(new ExamType(
				request.code(),
				request.name(),
				request.description(),
				request.displayOrder(),
				request.active()));
		ExamTypeResponse response = toTypeResponse(type);
		audit("ExamType", type.getId(), "CREATE", null, response);
		return response;
	}

	@Transactional
	public ExamTypeResponse updateType(UUID examTypeId, ExamTypeRequest request) {
		ExamType type = loadType(examTypeId);
		ExamTypeResponse oldValue = toTypeResponse(type);
		validateTypeCode(request.code(), examTypeId);
		type.update(request.code(), request.name(), request.description(), request.displayOrder(), request.active());
		ExamTypeResponse response = toTypeResponse(type);
		audit("ExamType", examTypeId, "UPDATE", oldValue, response);
		return response;
	}

	@Transactional(readOnly = true)
	public ExamTypeResponse getType(UUID examTypeId) {
		return toTypeResponse(loadType(examTypeId));
	}

	@Transactional(readOnly = true)
	public List<ExamTypeResponse> getTypes() {
		return examTypeRepository.findAllByDeletedFalseOrderByDisplayOrderAscNameAsc().stream()
				.map(this::toTypeResponse)
				.toList();
	}

	@Transactional
	public void deleteType(UUID examTypeId) {
		ExamType type = loadType(examTypeId);
		if (examScheduleRepository.countByExamTypeIdAndDeletedFalse(examTypeId) > 0) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Exam type is used in an exam schedule.");
		}
		ExamTypeResponse oldValue = toTypeResponse(type);
		type.softDelete("system");
		audit("ExamType", examTypeId, "DELETE", oldValue, Map.of("deleted", true, "examTypeId", examTypeId));
	}

	@Transactional
	public ExamScheduleResponse createSchedule(ExamScheduleRequest request) {
		Hierarchy hierarchy = validateHierarchy(request.academicYearId(), request.classId(), request.sectionId());
		ExamType examType = loadType(request.examTypeId());
		if (!examType.isActive()) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Exam type is inactive.");
		}
		Subject subject = validateSubjectAssigned(request.sectionId(), request.subjectId());
		validateDuplicateSchedule(request, null);
		ExamSchedule schedule = examScheduleRepository.save(new ExamSchedule(
				hierarchy.academicYear(),
				hierarchy.classEntity(),
				hierarchy.section(),
				examType,
				subject,
				request.examDate(),
				request.maxMarks(),
				request.status(),
				request.description()));
		ExamScheduleResponse response = toScheduleResponse(schedule);
		audit("ExamSchedule", schedule.getId(), "CREATE", null, response);
		return response;
	}

	@Transactional
	public ExamScheduleResponse updateSchedule(UUID scheduleId, ExamScheduleRequest request) {
		ExamSchedule schedule = loadSchedule(scheduleId);
		ExamScheduleResponse oldValue = toScheduleResponse(schedule);
		validateScheduleRequestMatchesExisting(schedule, request);
		validateDuplicateSchedule(request, scheduleId);
		schedule.update(request.examDate(), request.maxMarks(), request.status(), request.description());
		ExamScheduleResponse response = toScheduleResponse(schedule);
		audit("ExamSchedule", scheduleId, "UPDATE", oldValue, response);
		return response;
	}

	@Transactional(readOnly = true)
	public ExamScheduleResponse getSchedule(UUID scheduleId) {
		return toScheduleResponse(loadSchedule(scheduleId));
	}

	@Transactional(readOnly = true)
	public List<ExamScheduleResponse> getSchedules(UUID academicYearId, UUID classId, UUID sectionId) {
		return examScheduleRepository.search(academicYearId, classId, sectionId).stream()
				.map(this::toScheduleResponse)
				.toList();
	}

	@Transactional
	public void deleteSchedule(UUID scheduleId) {
		ExamSchedule schedule = loadSchedule(scheduleId);
		if (examMarkRepository.countByExamScheduleIdAndDeletedFalse(scheduleId) > 0) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Exam schedule already has marks.");
		}
		ExamScheduleResponse oldValue = toScheduleResponse(schedule);
		schedule.softDelete("system");
		audit("ExamSchedule", scheduleId, "DELETE", oldValue, Map.of("deleted", true, "scheduleId", scheduleId));
	}

	@Transactional
	public MarksEntryResponse saveMarks(MarksEntryRequest request) {
		Hierarchy hierarchy = validateHierarchy(request.academicYearId(), request.classId(), request.sectionId());
		ExamSchedule schedule = loadSchedule(request.examScheduleId());
		validateScheduleForMarks(schedule, request);
		Map<UUID, StudentClassAssignment> assignmentByStudent = activeAssignments(
				request.academicYearId(),
				request.classId(),
				request.sectionId()).stream()
				.collect(Collectors.toMap(
						assignment -> assignment.getStudent().getId(),
						Function.identity(),
						(first, second) -> first,
						LinkedHashMap::new));
		validateMarksRequest(request.records(), assignmentByStudent);
		Map<UUID, ExamMark> existingByStudent = examMarkRepository.findMarks(
				request.academicYearId(),
				request.classId(),
				request.sectionId(),
				request.examScheduleId(),
				request.subjectId()).stream()
				.collect(Collectors.toMap(mark -> mark.getStudent().getId(), Function.identity()));
		for (MarksEntryRecordRequest recordRequest : request.records()) {
			ExamMark mark = existingByStudent.get(recordRequest.studentId());
			if (mark == null) {
				Student student = assignmentByStudent.get(recordRequest.studentId()).getStudent();
				examMarkRepository.save(new ExamMark(
						hierarchy.academicYear(),
						hierarchy.classEntity(),
						hierarchy.section(),
						schedule,
						schedule.getSubject(),
						student,
						recordRequest.marksObtained(),
						recordRequest.maxMarks(),
						recordRequest.remarks()));
			}
			else {
				mark.update(recordRequest.marksObtained(), recordRequest.maxMarks(), recordRequest.remarks());
			}
		}
		MarksEntryResponse response = getMarks(
				request.academicYearId(),
				request.classId(),
				request.sectionId(),
				request.examScheduleId(),
				request.subjectId());
		audit("ExamMark", request.examScheduleId(), "MARKS_SAVED", null, Map.of("records", request.records().size()));
		return response;
	}

	@Transactional(readOnly = true)
	public MarksEntryResponse getMarks(
			UUID academicYearId,
			UUID classId,
			UUID sectionId,
			UUID examScheduleId,
			UUID subjectId) {
		validateHierarchy(academicYearId, classId, sectionId);
		List<ExamMarkResponse> records = examMarkRepository.findMarks(
				academicYearId,
				classId,
				sectionId,
				examScheduleId,
				subjectId).stream()
				.map(mark -> toMarkResponse(mark, null))
				.toList();
		return new MarksEntryResponse(academicYearId, classId, sectionId, examScheduleId, subjectId, records.size(), records);
	}

	@Transactional
	public List<StudentResultResponse> generateResults(GenerateResultRequest request) {
		validateHierarchy(request.academicYearId(), request.classId(), request.sectionId());
		List<StudentResultResponse> results = rankResults(examMarkRepository.findResultMarks(
				request.academicYearId(),
				request.classId(),
				request.sectionId(),
				request.examTypeId()));
		audit("ExamResult", request.sectionId(), "RESULT_GENERATED", null, Map.of(
				"academicYearId", request.academicYearId(),
				"classId", request.classId(),
				"sectionId", request.sectionId(),
				"examTypeId", request.examTypeId(),
				"students", results.size()));
		return results;
	}

	@Transactional(readOnly = true)
	public StudentResultResponse getStudentResult(UUID studentId, UUID academicYearId) {
		studentRepository.findByIdAndDeletedFalse(studentId)
				.orElseThrow(() -> new ResourceNotFoundException("Student", studentId));
		List<ExamMark> marks = examMarkRepository.findStudentMarks(studentId, academicYearId);
		if (marks.isEmpty()) {
			throw new ResourceNotFoundException("Student result", studentId);
		}
		return buildResult(marks, null);
	}

	@Transactional(readOnly = true)
	public byte[] exportReportCard(UUID studentId, UUID academicYearId) {
		StudentResultResponse result = getStudentResult(studentId, academicYearId);
		StringBuilder csv = new StringBuilder("Student,Admission Number,Subject,Marks,Max Marks,Grade,Passed\n");
		for (SubjectResultResponse subject : result.subjects()) {
			csv.append(escape(result.studentName())).append(',')
					.append(escape(result.admissionNumber())).append(',')
					.append(escape(subject.subjectName())).append(',')
					.append(subject.marksObtained()).append(',')
					.append(subject.maxMarks()).append(',')
					.append(subject.grade()).append(',')
					.append(subject.passed()).append('\n');
		}
		return csv.toString().getBytes(StandardCharsets.UTF_8);
	}

	private Hierarchy validateHierarchy(UUID academicYearId, UUID classId, UUID sectionId) {
		AcademicYear academicYear = academicHierarchyService.loadAcademicYear(academicYearId);
		ClassEntity classEntity = academicHierarchyService.loadClass(classId);
		if (!classEntity.getAcademicYear().getId().equals(academicYearId)) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Class does not belong to selected academic year.");
		}
		SectionEntity section = academicHierarchyService.loadSectionForClass(classId, sectionId);
		return new Hierarchy(academicYear, classEntity, section);
	}

	private List<StudentClassAssignment> activeAssignments(UUID academicYearId, UUID classId, UUID sectionId) {
		return studentClassAssignmentRepository.findActiveByHierarchy(academicYearId, classId, sectionId);
	}

	private Subject validateSubjectAssigned(UUID sectionId, UUID subjectId) {
		if (!divisionSubjectRepository.existsBySectionIdAndSubjectIdAndDeletedFalse(sectionId, subjectId)) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Subject is not assigned to the selected division.");
		}
		return academicHierarchyService.loadSubject(subjectId);
	}

	private void validateTypeCode(String code, UUID excludedId) {
		examTypeRepository.findByCodeIgnoreCaseAndDeletedFalse(code)
				.filter(existing -> !existing.getId().equals(excludedId))
				.ifPresent(existing -> {
					throw new BusinessException(ErrorCode.CONFLICT, "Exam type code already exists: " + code);
				});
	}

	private void validateDuplicateSchedule(ExamScheduleRequest request, UUID excludedId) {
		if (examScheduleRepository.existsDuplicate(
				request.academicYearId(),
				request.classId(),
				request.sectionId(),
				request.examTypeId(),
				request.subjectId(),
				excludedId)) {
			throw new BusinessException(ErrorCode.CONFLICT, "Exam schedule already exists for this exam type and subject.");
		}
	}

	private void validateScheduleRequestMatchesExisting(ExamSchedule schedule, ExamScheduleRequest request) {
		if (!schedule.getAcademicYear().getId().equals(request.academicYearId())
				|| !schedule.getClassEntity().getId().equals(request.classId())
				|| !schedule.getSection().getId().equals(request.sectionId())
				|| !schedule.getExamType().getId().equals(request.examTypeId())
				|| !schedule.getSubject().getId().equals(request.subjectId())) {
			throw new BusinessException(
					ErrorCode.BUSINESS_RULE_VIOLATION,
					"Academic year, class, division, exam type, and subject cannot be changed for an existing schedule.");
		}
	}

	private void validateScheduleForMarks(ExamSchedule schedule, MarksEntryRequest request) {
		if (!schedule.getAcademicYear().getId().equals(request.academicYearId())
				|| !schedule.getClassEntity().getId().equals(request.classId())
				|| !schedule.getSection().getId().equals(request.sectionId())
				|| !schedule.getSubject().getId().equals(request.subjectId())) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Exam schedule does not match selected hierarchy.");
		}
	}

	private void validateMarksRequest(
			List<MarksEntryRecordRequest> records,
			Map<UUID, StudentClassAssignment> assignmentByStudent) {
		java.util.Set<UUID> seen = new java.util.HashSet<>();
		for (MarksEntryRecordRequest record : records) {
			if (!seen.add(record.studentId())) {
				throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Duplicate marks record for student: " + record.studentId());
			}
			if (!assignmentByStudent.containsKey(record.studentId())) {
				throw new BusinessException(
						ErrorCode.BUSINESS_RULE_VIOLATION,
						"Student does not belong to selected academic year, class, and division.");
			}
			if (record.marksObtained().compareTo(record.maxMarks()) > 0) {
				throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Marks obtained cannot exceed max marks.");
			}
		}
	}

	private ExamType loadType(UUID examTypeId) {
		return examTypeRepository.findByIdAndDeletedFalse(examTypeId)
				.orElseThrow(() -> new ResourceNotFoundException("Exam type", examTypeId));
	}

	private ExamSchedule loadSchedule(UUID scheduleId) {
		return examScheduleRepository.findDetailedByIdAndDeletedFalse(scheduleId)
				.orElseThrow(() -> new ResourceNotFoundException("Exam schedule", scheduleId));
	}

	private ExamTypeResponse toTypeResponse(ExamType type) {
		return new ExamTypeResponse(
				type.getId(),
				type.getCode(),
				type.getName(),
				type.getDescription(),
				type.getDisplayOrder(),
				type.isActive());
	}

	private ExamScheduleResponse toScheduleResponse(ExamSchedule schedule) {
		return new ExamScheduleResponse(
				schedule.getId(),
				schedule.getAcademicYear().getId(),
				schedule.getAcademicYear().getName(),
				schedule.getClassEntity().getId(),
				schedule.getClassEntity().getName(),
				schedule.getSection().getId(),
				schedule.getSection().getName(),
				schedule.getExamType().getId(),
				schedule.getExamType().getName(),
				schedule.getSubject().getId(),
				schedule.getSubject().getName(),
				schedule.getExamDate(),
				schedule.getMaxMarks(),
				schedule.getStatus(),
				schedule.getDescription());
	}

	private ExamMarkResponse toMarkResponse(ExamMark mark, String rollNumber) {
		return new ExamMarkResponse(
				mark.getId(),
				mark.getAcademicYear().getId(),
				mark.getClassEntity().getId(),
				mark.getSection().getId(),
				mark.getExamSchedule().getId(),
				mark.getSubject().getId(),
				mark.getSubject().getName(),
				new ExamStudentResponse(
						mark.getStudent().getId(),
						mark.getStudent().getAdmissionNumber(),
						rollNumber,
						mark.getStudent().getDisplayName()),
				mark.getMarksObtained(),
				mark.getMaxMarks(),
				mark.getRemarks());
	}

	private ExamStudentResponse toExamStudent(StudentClassAssignment assignment) {
		Student student = assignment.getStudent();
		return new ExamStudentResponse(
				student.getId(),
				student.getAdmissionNumber(),
				assignment.getRollNumber(),
				student.getDisplayName());
	}

	private List<StudentResultResponse> rankResults(List<ExamMark> marks) {
		List<StudentResultResponse> unranked = marks.stream()
				.collect(Collectors.groupingBy(mark -> mark.getStudent().getId(), LinkedHashMap::new, Collectors.toList()))
				.values().stream()
				.map(studentMarks -> buildResult(studentMarks, null))
				.sorted(Comparator.comparing(StudentResultResponse::percentage).reversed())
				.toList();
		java.util.ArrayList<StudentResultResponse> ranked = new java.util.ArrayList<>();
		for (int index = 0; index < unranked.size(); index++) {
			UUID studentId = unranked.get(index).studentId();
			List<ExamMark> studentMarks = marks.stream()
					.filter(mark -> mark.getStudent().getId().equals(studentId))
					.toList();
			ranked.add(buildResult(studentMarks, index + 1));
		}
		return ranked;
	}

	private StudentResultResponse buildResult(List<ExamMark> marks, Integer rank) {
		ExamMark first = marks.get(0);
		BigDecimal total = marks.stream()
				.map(ExamMark::getMarksObtained)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal max = marks.stream()
				.map(ExamMark::getMaxMarks)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal percentage = max.compareTo(BigDecimal.ZERO) == 0
				? BigDecimal.ZERO
				: total.multiply(BigDecimal.valueOf(100)).divide(max, 2, RoundingMode.HALF_UP);
		List<SubjectResultResponse> subjects = marks.stream()
				.map(this::toSubjectResult)
				.toList();
		boolean passed = !subjects.isEmpty() && subjects.stream().allMatch(SubjectResultResponse::passed);
		return new StudentResultResponse(
				first.getStudent().getId(),
				first.getStudent().getAdmissionNumber(),
				first.getStudent().getDisplayName(),
				first.getAcademicYear().getId(),
				first.getClassEntity().getId(),
				first.getSection().getId(),
				total,
				max,
				percentage,
				grade(percentage),
				passed,
				rank,
				subjects);
	}

	private SubjectResultResponse toSubjectResult(ExamMark mark) {
		BigDecimal percentage = mark.getMaxMarks().compareTo(BigDecimal.ZERO) == 0
				? BigDecimal.ZERO
				: mark.getMarksObtained().multiply(BigDecimal.valueOf(100)).divide(mark.getMaxMarks(), 2, RoundingMode.HALF_UP);
		return new SubjectResultResponse(
				mark.getSubject().getId(),
				mark.getSubject().getName(),
				mark.getMarksObtained(),
				mark.getMaxMarks(),
				grade(percentage),
				percentage.compareTo(PASS_PERCENTAGE) >= 0);
	}

	private String grade(BigDecimal percentage) {
		if (percentage.compareTo(BigDecimal.valueOf(90)) >= 0) {
			return "A+";
		}
		if (percentage.compareTo(BigDecimal.valueOf(80)) >= 0) {
			return "A";
		}
		if (percentage.compareTo(BigDecimal.valueOf(70)) >= 0) {
			return "B";
		}
		if (percentage.compareTo(BigDecimal.valueOf(60)) >= 0) {
			return "C";
		}
		if (percentage.compareTo(PASS_PERCENTAGE) >= 0) {
			return "D";
		}
		return "F";
	}

	private String escape(String value) {
		if (!StringUtils.hasText(value)) {
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
}
