package com.school.erp.modules.exams.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import com.school.erp.modules.exams.api.dto.ExamScheduleSubjectRequest;
import com.school.erp.modules.exams.api.dto.ExamScheduleSubjectResponse;
import com.school.erp.modules.exams.api.dto.ExamStudentResponse;
import com.school.erp.modules.exams.api.dto.ExamTypeRequest;
import com.school.erp.modules.exams.api.dto.ExamTypeResponse;
import com.school.erp.modules.exams.api.dto.GenerateResultRequest;
import com.school.erp.modules.exams.api.dto.MarksEntryRecordRequest;
import com.school.erp.modules.exams.api.dto.MarksEntryRequest;
import com.school.erp.modules.exams.api.dto.MarksEntryResponse;
import com.school.erp.modules.exams.api.dto.StudentExamResultDetailResponse;
import com.school.erp.modules.exams.api.dto.StudentExamResultsResponse;
import com.school.erp.modules.exams.api.dto.StudentResultResponse;
import com.school.erp.modules.exams.api.dto.StudentSubjectExamResultResponse;
import com.school.erp.modules.exams.api.dto.SubjectResultResponse;
import com.school.erp.modules.exams.domain.ExamMark;
import com.school.erp.modules.exams.domain.ExamSchedule;
import com.school.erp.modules.exams.domain.ExamScheduleSubject;
import com.school.erp.modules.exams.domain.ExamType;
import com.school.erp.modules.exams.infrastructure.ExamMarkRepository;
import com.school.erp.modules.exams.infrastructure.ExamScheduleRepository;
import com.school.erp.modules.exams.infrastructure.ExamScheduleSubjectRepository;
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
	private final ExamScheduleSubjectRepository examScheduleSubjectRepository;
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
		List<ResolvedScheduleSubject> subjects = resolveScheduleSubjects(request);
		validateScheduleConflicts(hierarchy, request.examTypeId(), subjects, null);
		ExamSchedule schedule = new ExamSchedule(
				hierarchy.academicYear(),
				hierarchy.classEntity(),
				hierarchy.section(),
				examType,
				normalizeExamName(request.examName(), examType),
				request.status(),
				request.description());
		for (ResolvedScheduleSubject subject : subjects) {
			schedule.addSubject(
					subject.subject(),
					subject.examDate(),
					subject.startTime(),
					subject.endTime(),
					subject.room(),
					subject.maxMarks(),
					subject.passingMarks());
		}
		schedule.syncLegacySubjectFields();
		ExamSchedule saved = examScheduleRepository.save(schedule);
		ExamScheduleResponse response = toScheduleResponse(saved);
		audit("ExamSchedule", saved.getId(), "CREATE", null, response);
		for (ExamScheduleSubject subject : saved.activeSubjects()) {
			audit("ExamScheduleSubject", subject.getId(), "SUBJECT_ADDED", null, toScheduleSubjectResponse(subject));
		}
		return response;
	}

	@Transactional
	public ExamScheduleResponse updateSchedule(UUID scheduleId, ExamScheduleRequest request) {
		ExamSchedule schedule = loadSchedule(scheduleId);
		ExamScheduleResponse oldValue = toScheduleResponse(schedule);
		validateScheduleRequestMatchesExisting(schedule, request);
		ExamType examType = loadType(request.examTypeId());
		List<ResolvedScheduleSubject> requestedSubjects = resolveScheduleSubjects(request);
		validateScheduleConflicts(
				new Hierarchy(schedule.getAcademicYear(), schedule.getClassEntity(), schedule.getSection()),
				request.examTypeId(),
				requestedSubjects,
				scheduleId);
		schedule.update(normalizeExamName(request.examName(), examType), request.status(), request.description());
		syncScheduleSubjects(schedule, requestedSubjects);
		schedule.syncLegacySubjectFields();
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
		for (ExamScheduleSubject subject : schedule.activeSubjects()) {
			subject.softDelete("system");
		}
		schedule.softDelete("system");
		audit("ExamSchedule", scheduleId, "DELETE", oldValue, Map.of("deleted", true, "scheduleId", scheduleId));
	}

	@Transactional
	public MarksEntryResponse saveMarks(MarksEntryRequest request) {
		Hierarchy hierarchy = validateHierarchy(request.academicYearId(), request.classId(), request.sectionId());
		ExamSchedule schedule = loadSchedule(request.examScheduleId());
		ExamScheduleSubject scheduleSubject = validateScheduleForMarks(schedule, request);
		Map<UUID, StudentClassAssignment> assignmentByStudent = activeAssignments(
				request.academicYearId(),
				request.classId(),
				request.sectionId()).stream()
				.collect(Collectors.toMap(
						assignment -> assignment.getStudent().getId(),
						Function.identity(),
						(first, second) -> first,
						LinkedHashMap::new));
		validateMarksRequest(request.records(), assignmentByStudent, scheduleSubject);
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
						scheduleSubject.getSubject(),
						student,
						recordRequest.marksObtained(),
						scheduleSubject.getMaxMarks(),
						recordRequest.remarks()));
			}
			else {
				mark.update(recordRequest.marksObtained(), scheduleSubject.getMaxMarks(), recordRequest.remarks());
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
		ExamSchedule schedule = loadSchedule(examScheduleId);
		ExamScheduleSubject scheduleSubject = schedule.findSubject(subjectId)
				.orElseThrow(() -> new ResourceNotFoundException("Exam schedule subject", subjectId));
		List<ExamMarkResponse> records = examMarkRepository.findMarks(
				academicYearId,
				classId,
				sectionId,
				examScheduleId,
				subjectId).stream()
				.map(mark -> toMarkResponse(mark, null))
				.toList();
		return new MarksEntryResponse(
				academicYearId,
				classId,
				sectionId,
				examScheduleId,
				subjectId,
				scheduleSubject.getMaxMarks(),
				scheduleSubject.getPassingMarks(),
				records.size(),
				records);
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
	public StudentExamResultsResponse getStudentProfileExamResults(
			UUID studentId,
			UUID academicYearId,
			UUID examTypeId,
			UUID examScheduleId) {
		AcademicYear selectedYear = academicYearId == null ? null : academicHierarchyService.loadAcademicYear(academicYearId);
		if (examTypeId != null) {
			loadType(examTypeId);
		}
		if (examScheduleId != null) {
			loadSchedule(examScheduleId);
		}
		Student student = studentRepository.findProfileByIdAndDeletedFalse(studentId)
				.orElseThrow(() -> new ResourceNotFoundException("Student", studentId));
		List<ExamMark> marks = examMarkRepository.findStudentExamResults(
				studentId,
				academicYearId,
				examTypeId,
				examScheduleId);
		Map<ResultGroup, List<ExamMark>> marksByGroup = marks.stream()
				.collect(Collectors.groupingBy(this::resultGroup, LinkedHashMap::new, Collectors.toList()));
		Map<ResultGroup, List<ExamSchedule>> schedulesByGroup = new LinkedHashMap<>();
		for (StudentClassAssignment assignment : profileAssignments(student, academicYearId)) {
			for (ExamSchedule schedule : examScheduleRepository.findForStudentProfile(
					assignment.getAcademicYearEntity().getId(),
					assignment.getClassEntity().getId(),
					assignment.getSectionEntity().getId(),
					examTypeId,
					examScheduleId)) {
				ResultGroup group = resultGroup(schedule);
				schedulesByGroup.computeIfAbsent(group, ignored -> new java.util.ArrayList<>()).add(schedule);
			}
		}
		List<ResultGroup> groups = java.util.stream.Stream.concat(marksByGroup.keySet().stream(), schedulesByGroup.keySet().stream())
				.distinct()
				.sorted(this::compareResultGroups)
				.toList();
		List<StudentExamResultDetailResponse> results = groups.stream()
				.map(group -> buildProfileResult(
						group,
						marksByGroup.getOrDefault(group, List.of()),
						schedulesByGroup.getOrDefault(group, List.of()),
						studentId))
				.toList();
		ExamProfileContext context = examProfileContext(student, selectedYear, results);
		return new StudentExamResultsResponse(
				studentId,
				student.getDisplayName(),
				context.academicYearId(),
				context.academicYear(),
				context.classId(),
				context.className(),
				context.sectionId(),
				context.sectionName(),
				results);
	}

	@Transactional(readOnly = true)
	public byte[] exportStudentProfileReportCard(UUID studentId, UUID resultId, UUID academicYearId) {
		StudentExamResultsResponse response = getStudentProfileExamResults(studentId, academicYearId, resultId, null);
		StudentExamResultDetailResponse result = response.results().stream()
				.filter(candidate -> candidate.resultId().equals(resultId))
				.findFirst()
				.orElseThrow(() -> new ResourceNotFoundException("Student exam result", resultId));
		StringBuilder csv = new StringBuilder("Student,Exam,Subject,Marks,Max Marks,Grade,Status,Remarks,Exam Date\n");
		for (StudentSubjectExamResultResponse subject : result.subjectResults()) {
			csv.append(escape(response.studentName())).append(',')
					.append(escape(result.examName())).append(',')
					.append(escape(subject.subjectName())).append(',')
					.append(subject.marksObtained() == null ? "" : subject.marksObtained()).append(',')
					.append(subject.maxMarks()).append(',')
					.append(escape(subject.grade())).append(',')
					.append(result.passFailStatus()).append(',')
					.append(escape(subject.remarks())).append(',')
					.append(subject.examDate()).append('\n');
		}
		return csv.toString().getBytes(StandardCharsets.UTF_8);
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

	private void validateScheduleRequestMatchesExisting(ExamSchedule schedule, ExamScheduleRequest request) {
		if (!schedule.getAcademicYear().getId().equals(request.academicYearId())
				|| !schedule.getClassEntity().getId().equals(request.classId())
				|| !schedule.getSection().getId().equals(request.sectionId())
				|| !schedule.getExamType().getId().equals(request.examTypeId())) {
			throw new BusinessException(
					ErrorCode.BUSINESS_RULE_VIOLATION,
					"Academic year, class, division, and exam type cannot be changed for an existing schedule.");
		}
	}

	private ExamScheduleSubject validateScheduleForMarks(ExamSchedule schedule, MarksEntryRequest request) {
		if (!schedule.getAcademicYear().getId().equals(request.academicYearId())
				|| !schedule.getClassEntity().getId().equals(request.classId())
				|| !schedule.getSection().getId().equals(request.sectionId())) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Exam schedule does not match selected hierarchy.");
		}
		return examScheduleSubjectRepository.findByExamScheduleIdAndSubjectIdAndDeletedFalse(
						request.examScheduleId(),
						request.subjectId())
				.orElseThrow(() -> new BusinessException(
						ErrorCode.BUSINESS_RULE_VIOLATION,
						"Subject is not part of the selected exam schedule."));
	}

	private void validateMarksRequest(
			List<MarksEntryRecordRequest> records,
			Map<UUID, StudentClassAssignment> assignmentByStudent,
			ExamScheduleSubject scheduleSubject) {
		java.util.Set<UUID> seen = new HashSet<>();
		for (MarksEntryRecordRequest record : records) {
			if (!seen.add(record.studentId())) {
				throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Duplicate marks record for student: " + record.studentId());
			}
			if (!assignmentByStudent.containsKey(record.studentId())) {
				throw new BusinessException(
					ErrorCode.BUSINESS_RULE_VIOLATION,
					"Student does not belong to selected academic year, class, and division.");
			}
			if (record.marksObtained().compareTo(scheduleSubject.getMaxMarks()) > 0) {
				throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Marks obtained cannot exceed max marks.");
			}
		}
	}

	private List<ResolvedScheduleSubject> resolveScheduleSubjects(ExamScheduleRequest request) {
		List<ExamScheduleSubjectRequest> subjectRequests = normalizeSubjectRequests(request);
		if (subjectRequests.isEmpty()) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "At least one subject is required.");
		}
		java.util.Set<UUID> seen = new HashSet<>();
		List<ResolvedScheduleSubject> resolved = new ArrayList<>();
		for (ExamScheduleSubjectRequest subjectRequest : subjectRequests) {
			if (!seen.add(subjectRequest.subjectId())) {
				throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Duplicate subject is not allowed in the same exam schedule.");
			}
			validateScheduleSubjectRequest(subjectRequest);
			Subject subject = validateSubjectAssigned(request.sectionId(), subjectRequest.subjectId());
			resolved.add(new ResolvedScheduleSubject(
					subject,
					subjectRequest.examDate(),
					subjectRequest.startTime(),
					subjectRequest.endTime(),
					trimToNull(subjectRequest.room()),
					subjectRequest.maxMarks(),
					subjectRequest.passingMarks()));
		}
		return resolved;
	}

	private List<ExamScheduleSubjectRequest> normalizeSubjectRequests(ExamScheduleRequest request) {
		if (request.subjects() != null && !request.subjects().isEmpty()) {
			return request.subjects();
		}
		if (request.subjectId() != null || request.examDate() != null || request.maxMarks() != null) {
			return List.of(new ExamScheduleSubjectRequest(
					request.subjectId(),
					request.examDate(),
					request.startTime(),
					request.endTime(),
					request.room(),
					request.maxMarks(),
					request.passingMarks()));
		}
		return List.of();
	}

	private void validateScheduleSubjectRequest(ExamScheduleSubjectRequest request) {
		if (request.subjectId() == null) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Subject is required.");
		}
		if (request.examDate() == null) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Exam date is required for each subject.");
		}
		if (request.maxMarks() == null || request.maxMarks().compareTo(BigDecimal.ZERO) <= 0) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Max marks must be greater than 0.");
		}
		if (request.passingMarks() != null && request.passingMarks().compareTo(BigDecimal.ZERO) < 0) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Passing marks cannot be negative.");
		}
		if (request.passingMarks() != null && request.passingMarks().compareTo(request.maxMarks()) > 0) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Passing marks cannot be greater than max marks.");
		}
		if (request.startTime() != null && request.endTime() != null && !request.endTime().isAfter(request.startTime())) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Exam end time must be after start time.");
		}
	}

	private void validateScheduleConflicts(
			Hierarchy hierarchy,
			UUID examTypeId,
			List<ResolvedScheduleSubject> subjects,
			UUID excludedScheduleId) {
		validateInternalSubjectSlotConflicts(subjects);
		for (ResolvedScheduleSubject subject : subjects) {
			if (examScheduleRepository.existsDuplicateSubject(
					hierarchy.academicYear().getId(),
					hierarchy.classEntity().getId(),
					hierarchy.section().getId(),
					examTypeId,
					subject.subject().getId(),
					excludedScheduleId)) {
				throw new BusinessException(
						ErrorCode.CONFLICT,
						"Subject is already scheduled for this exam type, class, and division.");
			}
			if (examScheduleRepository.existsSubjectSlotConflict(
					hierarchy.academicYear().getId(),
					hierarchy.classEntity().getId(),
					hierarchy.section().getId(),
					subject.examDate(),
					subject.startTime(),
					subject.endTime(),
					excludedScheduleId)) {
				throw new BusinessException(
						ErrorCode.CONFLICT,
						"Exam schedule conflicts with another subject slot for the selected class and division.");
			}
		}
	}

	private void validateInternalSubjectSlotConflicts(List<ResolvedScheduleSubject> subjects) {
		for (int first = 0; first < subjects.size(); first++) {
			for (int second = first + 1; second < subjects.size(); second++) {
				if (sameDateConflict(subjects.get(first), subjects.get(second))) {
					throw new BusinessException(
							ErrorCode.CONFLICT,
							"Exam subjects in the same schedule cannot overlap on the same date.");
				}
			}
		}
	}

	private boolean sameDateConflict(ResolvedScheduleSubject first, ResolvedScheduleSubject second) {
		if (!Objects.equals(first.examDate(), second.examDate())) {
			return false;
		}
		return timeConflict(first.startTime(), first.endTime(), second.startTime(), second.endTime());
	}

	private boolean timeConflict(LocalTime firstStart, LocalTime firstEnd, LocalTime secondStart, LocalTime secondEnd) {
		if (firstStart == null || firstEnd == null || secondStart == null || secondEnd == null) {
			return true;
		}
		return firstStart.isBefore(secondEnd) && firstEnd.isAfter(secondStart);
	}

	private void syncScheduleSubjects(ExamSchedule schedule, List<ResolvedScheduleSubject> requestedSubjects) {
		Map<UUID, ExamScheduleSubject> existingBySubject = schedule.activeSubjects().stream()
				.collect(Collectors.toMap(
						subject -> subject.getSubject().getId(),
						Function.identity(),
						(first, second) -> first,
						LinkedHashMap::new));
		java.util.Set<UUID> requestedIds = requestedSubjects.stream()
				.map(subject -> subject.subject().getId())
				.collect(Collectors.toSet());
		for (ExamScheduleSubject existing : existingBySubject.values()) {
			if (!requestedIds.contains(existing.getSubject().getId())) {
				ExamScheduleSubjectResponse oldValue = toScheduleSubjectResponse(existing);
				existing.softDelete("system");
				audit("ExamScheduleSubject", existing.getId(), "SUBJECT_REMOVED", oldValue, Map.of("deleted", true));
			}
		}
		for (ResolvedScheduleSubject requested : requestedSubjects) {
			ExamScheduleSubject existing = existingBySubject.get(requested.subject().getId());
			if (existing == null) {
				ExamScheduleSubject added = schedule.addSubject(
						requested.subject(),
						requested.examDate(),
						requested.startTime(),
						requested.endTime(),
						requested.room(),
						requested.maxMarks(),
						requested.passingMarks());
				audit("ExamScheduleSubject", added.getId(), "SUBJECT_ADDED", null, toScheduleSubjectResponse(added));
			}
			else {
				ExamScheduleSubjectResponse oldValue = toScheduleSubjectResponse(existing);
				boolean maxChanged = different(existing.getMaxMarks(), requested.maxMarks());
				boolean passingChanged = different(existing.getPassingMarks(), requested.passingMarks());
				boolean slotChanged = !Objects.equals(existing.getExamDate(), requested.examDate())
						|| !Objects.equals(existing.getStartTime(), requested.startTime())
						|| !Objects.equals(existing.getEndTime(), requested.endTime())
						|| !Objects.equals(trimToNull(existing.getRoom()), requested.room());
				existing.update(
						requested.examDate(),
						requested.startTime(),
						requested.endTime(),
						requested.room(),
						requested.maxMarks(),
						requested.passingMarks());
				ExamScheduleSubjectResponse newValue = toScheduleSubjectResponse(existing);
				if (slotChanged) {
					audit("ExamScheduleSubject", existing.getId(), "SCHEDULE_SLOT_UPDATED", oldValue, newValue);
				}
				if (maxChanged) {
					audit("ExamScheduleSubject", existing.getId(), "MAX_MARKS_UPDATED", oldValue, newValue);
				}
				if (passingChanged) {
					audit("ExamScheduleSubject", existing.getId(), "PASSING_MARKS_UPDATED", oldValue, newValue);
				}
			}
		}
	}

	private boolean different(BigDecimal first, BigDecimal second) {
		if (first == null || second == null) {
			return !Objects.equals(first, second);
		}
		return first.compareTo(second) != 0;
	}

	private String normalizeExamName(String value, ExamType examType) {
		if (StringUtils.hasText(value)) {
			return value.trim();
		}
		return examType.getName();
	}

	private String trimToNull(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		return value.trim();
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
		List<ExamScheduleSubjectResponse> subjects = schedule.activeSubjects().stream()
				.map(this::toScheduleSubjectResponse)
				.toList();
		ExamScheduleSubjectResponse firstSubject = subjects.isEmpty() ? null : subjects.getFirst();
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
				schedule.getExamName(),
				subjects,
				firstSubject == null ? null : firstSubject.subjectId(),
				firstSubject == null ? null : firstSubject.subjectName(),
				firstSubject == null ? null : firstSubject.examDate(),
				firstSubject == null ? null : firstSubject.startTime(),
				firstSubject == null ? null : firstSubject.endTime(),
				firstSubject == null ? null : firstSubject.room(),
				firstSubject == null ? null : firstSubject.maxMarks(),
				firstSubject == null ? null : firstSubject.passingMarks(),
				schedule.getStatus(),
				schedule.getDescription());
	}

	private ExamScheduleSubjectResponse toScheduleSubjectResponse(ExamScheduleSubject subject) {
		return new ExamScheduleSubjectResponse(
				subject.getId(),
				subject.getSubject().getId(),
				subject.getSubject().getName(),
				subject.getExamDate(),
				subject.getStartTime(),
				subject.getEndTime(),
				subject.getRoom(),
				subject.getMaxMarks(),
				subject.getPassingMarks());
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
		List<ExamScheduleSubject> scheduledSubjects = scheduledSubjectsForMarks(marks);
		Map<UUID, ExamMark> marksBySubject = marks.stream()
				.collect(Collectors.toMap(
						mark -> mark.getSubject().getId(),
						Function.identity(),
						(firstMark, secondMark) -> firstMark,
						LinkedHashMap::new));
		BigDecimal max = scheduledSubjects.isEmpty()
				? marks.stream().map(ExamMark::getMaxMarks).reduce(BigDecimal.ZERO, BigDecimal::add)
				: scheduledSubjects.stream().map(ExamScheduleSubject::getMaxMarks).reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal total = scheduledSubjects.isEmpty()
				? marks.stream().map(ExamMark::getMarksObtained).reduce(BigDecimal.ZERO, BigDecimal::add)
				: scheduledSubjects.stream()
						.map(subject -> marksBySubject.get(subject.getSubject().getId()))
						.filter(Objects::nonNull)
						.map(ExamMark::getMarksObtained)
						.reduce(BigDecimal.ZERO, BigDecimal::add);
		List<SubjectResultResponse> subjects = scheduledSubjects.isEmpty()
				? marks.stream().map(this::toSubjectResult).toList()
				: scheduledSubjects.stream()
						.map(subject -> toSubjectResult(subject, marksBySubject.get(subject.getSubject().getId())))
						.toList();
		BigDecimal percentage = max.compareTo(BigDecimal.ZERO) == 0
				? BigDecimal.ZERO
				: total.multiply(BigDecimal.valueOf(100)).divide(max, 2, RoundingMode.HALF_UP);
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

	private List<ExamScheduleSubject> scheduledSubjectsForMarks(List<ExamMark> marks) {
		Map<UUID, ExamScheduleSubject> subjects = new LinkedHashMap<>();
		for (ExamMark mark : marks) {
			for (ExamScheduleSubject subject : mark.getExamSchedule().activeSubjects()) {
				subjects.putIfAbsent(subject.getSubject().getId(), subject);
			}
		}
		return subjects.values().stream()
				.sorted(Comparator.comparing(ExamScheduleSubject::getExamDate)
						.thenComparing(subject -> subject.getSubject().getName()))
				.toList();
	}

	private SubjectResultResponse toSubjectResult(ExamScheduleSubject scheduleSubject, ExamMark mark) {
		if (mark == null) {
			return new SubjectResultResponse(
					scheduleSubject.getSubject().getId(),
					scheduleSubject.getSubject().getName(),
					BigDecimal.ZERO,
					scheduleSubject.getMaxMarks(),
					"F",
					false);
		}
		BigDecimal percentage = scheduleSubject.getMaxMarks().compareTo(BigDecimal.ZERO) == 0
				? BigDecimal.ZERO
				: mark.getMarksObtained().multiply(BigDecimal.valueOf(100)).divide(scheduleSubject.getMaxMarks(), 2, RoundingMode.HALF_UP);
		return new SubjectResultResponse(
				scheduleSubject.getSubject().getId(),
				scheduleSubject.getSubject().getName(),
				mark.getMarksObtained(),
				scheduleSubject.getMaxMarks(),
				grade(percentage),
				subjectPassed(scheduleSubject, mark, percentage));
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
				subjectPassed(mark, percentage));
	}

	private boolean subjectPassed(ExamMark mark, BigDecimal percentage) {
		return mark.getExamSchedule()
				.findSubject(mark.getSubject().getId())
				.map(scheduleSubject -> subjectPassed(scheduleSubject, mark, percentage))
				.orElse(percentage.compareTo(PASS_PERCENTAGE) >= 0);
	}

	private boolean subjectPassed(ExamScheduleSubject scheduleSubject, ExamMark mark, BigDecimal percentage) {
		if (scheduleSubject.getPassingMarks() != null) {
			return mark.getMarksObtained().compareTo(scheduleSubject.getPassingMarks()) >= 0;
		}
		return percentage.compareTo(PASS_PERCENTAGE) >= 0;
	}

	private StudentExamResultDetailResponse buildProfileResult(
			ResultGroup group,
			List<ExamMark> marks,
			List<ExamSchedule> schedules,
			UUID studentId) {
		List<ExamSchedule> orderedSchedules = orderedSchedules(marks, schedules);
		ExamResultContext context = examResultContext(group, marks, orderedSchedules);
		Map<UUID, ExamMark> marksBySubject = marks.stream()
				.collect(Collectors.toMap(
						mark -> mark.getSubject().getId(),
						Function.identity(),
						(first, second) -> first,
						LinkedHashMap::new));
		List<ExamScheduleSubject> orderedSubjects = orderedScheduleSubjects(marks, orderedSchedules);
		List<StudentSubjectExamResultResponse> subjects = orderedSubjects.stream()
				.map(subject -> toProfileSubjectResult(subject, marksBySubject.get(subject.getSubject().getId())))
				.toList();
		BigDecimal totalMarks = subjects.stream()
				.map(StudentSubjectExamResultResponse::maxMarks)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal obtainedMarks = subjects.stream()
				.map(StudentSubjectExamResultResponse::marksObtained)
				.filter(java.util.Objects::nonNull)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		boolean complete = !subjects.isEmpty() && subjects.stream().allMatch(subject -> subject.marksObtained() != null);
		BigDecimal percentage = totalMarks.compareTo(BigDecimal.ZERO) == 0
				? BigDecimal.ZERO
				: obtainedMarks.multiply(BigDecimal.valueOf(100)).divide(totalMarks, 2, RoundingMode.HALF_UP);
		String grade = complete ? grade(percentage) : null;
		boolean passed = complete && orderedSubjects.stream()
				.allMatch(subject -> {
					ExamMark mark = marksBySubject.get(subject.getSubject().getId());
					if (mark == null) {
						return false;
					}
					BigDecimal subjectPercentage = subject.getMaxMarks().compareTo(BigDecimal.ZERO) == 0
							? BigDecimal.ZERO
							: mark.getMarksObtained().multiply(BigDecimal.valueOf(100)).divide(subject.getMaxMarks(), 2, RoundingMode.HALF_UP);
					return subjectPassed(subject, mark, subjectPercentage);
				});
		String status = complete ? passed ? "PASS" : "FAIL" : "PENDING";
		return new StudentExamResultDetailResponse(
				group.examTypeId(),
				group.academicYearId(),
				context.academicYear(),
				group.classId(),
				context.className(),
				group.sectionId(),
				context.sectionName(),
				group.examTypeId(),
				context.examType(),
				context.examName(),
				orderedSchedules.size() == 1 ? orderedSchedules.getFirst().getId() : null,
				subjects,
				totalMarks,
				obtainedMarks,
				percentage,
				grade,
				status,
				complete ? rankFor(group, studentId) : null,
				marks.stream()
						.map(ExamMark::getUpdatedAt)
						.filter(java.util.Objects::nonNull)
						.max(Instant::compareTo)
						.orElse(null));
	}

	private StudentSubjectExamResultResponse toProfileSubjectResult(ExamScheduleSubject scheduleSubject, ExamMark mark) {
		if (mark == null) {
			return new StudentSubjectExamResultResponse(
					scheduleSubject.getExamSchedule().getId(),
					scheduleSubject.getSubject().getId(),
					scheduleSubject.getSubject().getName(),
					scheduleSubject.getMaxMarks(),
					null,
					null,
					scheduleSubject.getExamSchedule().getDescription(),
					scheduleSubject.getExamDate());
		}
		BigDecimal percentage = scheduleSubject.getMaxMarks().compareTo(BigDecimal.ZERO) == 0
				? BigDecimal.ZERO
				: mark.getMarksObtained().multiply(BigDecimal.valueOf(100)).divide(scheduleSubject.getMaxMarks(), 2, RoundingMode.HALF_UP);
		return new StudentSubjectExamResultResponse(
				scheduleSubject.getExamSchedule().getId(),
				scheduleSubject.getSubject().getId(),
				scheduleSubject.getSubject().getName(),
				scheduleSubject.getMaxMarks(),
				mark.getMarksObtained(),
				grade(percentage),
				mark.getRemarks(),
				scheduleSubject.getExamDate());
	}

	private List<StudentClassAssignment> profileAssignments(Student student, UUID academicYearId) {
		return student.getClassAssignments().stream()
				.filter(assignment -> !assignment.isDeleted())
				.filter(assignment -> assignment.getAcademicYearEntity() != null)
				.filter(assignment -> assignment.getClassEntity() != null)
				.filter(assignment -> assignment.getSectionEntity() != null)
				.filter(assignment -> academicYearId == null || assignment.getAcademicYearEntity().getId().equals(academicYearId))
				.sorted(Comparator.comparing(StudentClassAssignment::isActive).reversed()
						.thenComparing(
								StudentClassAssignment::getEffectiveFrom,
								Comparator.nullsLast(Comparator.reverseOrder())))
				.toList();
	}

	private ResultGroup resultGroup(ExamMark mark) {
		return new ResultGroup(
				mark.getAcademicYear().getId(),
				mark.getClassEntity().getId(),
				mark.getSection().getId(),
				mark.getExamSchedule().getExamType().getId());
	}

	private ResultGroup resultGroup(ExamSchedule schedule) {
		return new ResultGroup(
				schedule.getAcademicYear().getId(),
				schedule.getClassEntity().getId(),
				schedule.getSection().getId(),
				schedule.getExamType().getId());
	}

	private int compareResultGroups(ResultGroup first, ResultGroup second) {
		int year = first.academicYearId().compareTo(second.academicYearId());
		if (year != 0) {
			return year;
		}
		return first.examTypeId().compareTo(second.examTypeId());
	}

	private List<ExamSchedule> orderedSchedules(List<ExamMark> marks, List<ExamSchedule> schedules) {
		Map<UUID, ExamSchedule> ordered = new LinkedHashMap<>();
		for (ExamSchedule schedule : schedules) {
			ordered.putIfAbsent(schedule.getId(), schedule);
		}
		for (ExamMark mark : marks) {
			ordered.putIfAbsent(mark.getExamSchedule().getId(), mark.getExamSchedule());
		}
		return ordered.values().stream()
				.sorted(Comparator.comparing(
								this::firstExamDate,
								Comparator.nullsLast(Comparator.naturalOrder()))
						.thenComparing(schedule -> schedule.getExamName() == null ? "" : schedule.getExamName()))
				.toList();
	}

	private List<ExamScheduleSubject> orderedScheduleSubjects(List<ExamMark> marks, List<ExamSchedule> schedules) {
		Map<UUID, ExamScheduleSubject> subjects = new LinkedHashMap<>();
		for (ExamSchedule schedule : schedules) {
			for (ExamScheduleSubject subject : schedule.activeSubjects()) {
				subjects.putIfAbsent(subject.getSubject().getId(), subject);
			}
		}
		for (ExamMark mark : marks) {
			mark.getExamSchedule()
					.findSubject(mark.getSubject().getId())
					.ifPresent(subject -> subjects.putIfAbsent(subject.getSubject().getId(), subject));
		}
		return subjects.values().stream()
				.sorted(Comparator.comparing(ExamScheduleSubject::getExamDate)
						.thenComparing(subject -> subject.getSubject().getName()))
				.toList();
	}

	private LocalDate firstExamDate(ExamSchedule schedule) {
		return schedule.activeSubjects().stream()
				.map(ExamScheduleSubject::getExamDate)
				.min(LocalDate::compareTo)
				.orElse(schedule.getExamDate());
	}

	private ExamResultContext examResultContext(
			ResultGroup group,
			List<ExamMark> marks,
			List<ExamSchedule> schedules) {
		if (!marks.isEmpty()) {
			ExamMark first = marks.getFirst();
			return new ExamResultContext(
					first.getAcademicYear().getName(),
					first.getClassEntity().getName(),
					first.getSection().getName(),
					first.getExamSchedule().getExamType().getName(),
					first.getExamSchedule().getExamName());
		}
		ExamSchedule first = schedules.getFirst();
		return new ExamResultContext(
				first.getAcademicYear().getName(),
				first.getClassEntity().getName(),
				first.getSection().getName(),
				first.getExamType().getName(),
				first.getExamName());
	}

	private ExamProfileContext examProfileContext(
			Student student,
			AcademicYear selectedYear,
			List<StudentExamResultDetailResponse> results) {
		if (!results.isEmpty()) {
			StudentExamResultDetailResponse first = results.getFirst();
			return new ExamProfileContext(
					first.academicYearId(),
					first.academicYear(),
					first.classId(),
					first.className(),
					first.sectionId(),
					first.sectionName());
		}
		StudentClassAssignment assignment = profileAssignments(student, selectedYear == null ? null : selectedYear.getId()).stream()
				.findFirst()
				.orElse(null);
		if (assignment == null) {
			return new ExamProfileContext(
					selectedYear == null ? null : selectedYear.getId(),
					selectedYear == null ? null : selectedYear.getName(),
					null,
					null,
					null,
					null);
		}
		return new ExamProfileContext(
				assignment.getAcademicYearEntity().getId(),
				assignment.getAcademicYearEntity().getName(),
				assignment.getClassEntity().getId(),
				assignment.getClassName(),
				assignment.getSectionEntity().getId(),
				assignment.getSectionName());
	}

	private Integer rankFor(ResultGroup group, UUID studentId) {
		List<StudentResultResponse> ranked = rankResults(examMarkRepository.findResultMarks(
				group.academicYearId(),
				group.classId(),
				group.sectionId(),
				group.examTypeId()));
		return ranked.stream()
				.filter(result -> result.studentId().equals(studentId))
				.map(StudentResultResponse::rank)
				.findFirst()
				.orElse(null);
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

	private record ResultGroup(UUID academicYearId, UUID classId, UUID sectionId, UUID examTypeId) {
	}

	private record ExamResultContext(
			String academicYear,
			String className,
			String sectionName,
			String examType,
			String examName) {
	}

	private record ResolvedScheduleSubject(
			Subject subject,
			LocalDate examDate,
			LocalTime startTime,
			LocalTime endTime,
			String room,
			BigDecimal maxMarks,
			BigDecimal passingMarks) {
	}

	private record ExamProfileContext(
			UUID academicYearId,
			String academicYear,
			UUID classId,
			String className,
			UUID sectionId,
			String sectionName) {
	}
}
