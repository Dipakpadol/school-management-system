package com.school.erp.modules.portal.application;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.school.erp.common.api.PageRequestDto;
import com.school.erp.common.api.PageResponse;
import com.school.erp.common.exception.BusinessException;
import com.school.erp.common.exception.ErrorCode;
import com.school.erp.common.exception.ResourceNotFoundException;
import com.school.erp.modules.academic.domain.ClassTeacherMapping;
import com.school.erp.modules.academic.domain.SubjectTeacherMapping;
import com.school.erp.modules.academic.domain.Teacher;
import com.school.erp.modules.academic.infrastructure.ClassTeacherMappingRepository;
import com.school.erp.modules.academic.infrastructure.SubjectTeacherMappingRepository;
import com.school.erp.modules.academic.infrastructure.TeacherRepository;
import com.school.erp.modules.attendance.api.dto.AttendanceStudentResponse;
import com.school.erp.modules.attendance.api.dto.DailyAttendanceRequest;
import com.school.erp.modules.attendance.api.dto.DailyAttendanceResponse;
import com.school.erp.modules.attendance.api.dto.StudentAttendanceHistoryResponse;
import com.school.erp.modules.attendance.api.dto.StudentAttendanceSummaryResponse;
import com.school.erp.modules.attendance.application.AttendanceService;
import com.school.erp.modules.attendance.domain.AttendanceStatus;
import com.school.erp.modules.auth.application.SchoolUserPrincipal;
import com.school.erp.modules.communications.api.dto.CommunicationResponse;
import com.school.erp.modules.communications.application.CommunicationMapper;
import com.school.erp.modules.communications.domain.CommunicationAudienceType;
import com.school.erp.modules.communications.infrastructure.CommunicationRecordRepository;
import com.school.erp.modules.exams.api.dto.ExamStudentResponse;
import com.school.erp.modules.exams.api.dto.MarksEntryRequest;
import com.school.erp.modules.exams.api.dto.MarksEntryResponse;
import com.school.erp.modules.exams.api.dto.StudentExamResultsResponse;
import com.school.erp.modules.exams.application.ExamService;
import com.school.erp.modules.fees.api.dto.StudentFeeSummaryResponse;
import com.school.erp.modules.fees.application.FeeService;
import com.school.erp.modules.hostel.api.dto.HostelAllocationResponse;
import com.school.erp.modules.hostel.application.HostelService;
import com.school.erp.modules.library.api.dto.LibraryFineResponse;
import com.school.erp.modules.library.api.dto.LibraryLoanResponse;
import com.school.erp.modules.library.application.LibraryService;
import com.school.erp.modules.library.domain.LibraryMemberType;
import com.school.erp.modules.library.domain.LibraryMembership;
import com.school.erp.modules.library.infrastructure.LibraryMembershipRepository;
import com.school.erp.modules.portal.api.dto.PortalLibraryMembershipResponse;
import com.school.erp.modules.portal.api.dto.PortalLibrarySummaryResponse;
import com.school.erp.modules.portal.api.dto.PortalParentContactResponse;
import com.school.erp.modules.portal.api.dto.PortalStudentDashboardResponse;
import com.school.erp.modules.portal.api.dto.PortalStudentDocumentResponse;
import com.school.erp.modules.portal.api.dto.PortalStudentProfileResponse;
import com.school.erp.modules.portal.api.dto.PortalTeacherDashboardResponse;
import com.school.erp.modules.portal.api.dto.PortalTeacherScopeResponse;
import com.school.erp.modules.students.api.dto.ClassSectionAssignmentResponse;
import com.school.erp.modules.students.api.dto.StudentSummaryResponse;
import com.school.erp.modules.students.application.StudentMapper;
import com.school.erp.modules.students.domain.ParentGuardian;
import com.school.erp.modules.students.domain.Student;
import com.school.erp.modules.students.domain.StudentClassAssignment;
import com.school.erp.modules.students.domain.StudentDocument;
import com.school.erp.modules.students.domain.StudentParent;
import com.school.erp.modules.students.infrastructure.StudentRepository;
import com.school.erp.modules.teachers.api.dto.TeacherAssignmentResponse;
import com.school.erp.modules.teachers.api.dto.TeacherResponse;
import com.school.erp.modules.teachers.application.TeacherMapper;
import com.school.erp.modules.teachers.domain.TeacherAcademicAssignment;
import com.school.erp.modules.teachers.domain.TeacherAssignmentStatus;
import com.school.erp.modules.teachers.domain.TeacherAssignmentType;
import com.school.erp.modules.teachers.infrastructure.TeacherAcademicAssignmentRepository;
import com.school.erp.modules.transport.api.dto.StudentTransportAssignmentResponse;
import com.school.erp.modules.transport.application.TransportService;

import org.springframework.data.domain.Page;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PortalService {

	private static final String ROLE_STUDENT = "ROLE_STUDENT";
	private static final String ROLE_PARENT = "ROLE_PARENT";
	private static final String ROLE_TEACHER = "ROLE_TEACHER";

	private final StudentRepository studentRepository;
	private final StudentMapper studentMapper;
	private final AttendanceService attendanceService;
	private final FeeService feeService;
	private final ExamService examService;
	private final HostelService hostelService;
	private final TransportService transportService;
	private final LibraryService libraryService;
	private final LibraryMembershipRepository libraryMembershipRepository;
	private final CommunicationRecordRepository communicationRecordRepository;
	private final CommunicationMapper communicationMapper;
	private final TeacherRepository teacherRepository;
	private final TeacherMapper teacherMapper;
	private final TeacherAcademicAssignmentRepository teacherAcademicAssignmentRepository;
	private final ClassTeacherMappingRepository classTeacherMappingRepository;
	private final SubjectTeacherMappingRepository subjectTeacherMappingRepository;

	@Transactional(readOnly = true)
	public PortalStudentDashboardResponse studentDashboard(UUID academicYearId, PageRequestDto pageRequest) {
		Student student = currentStudent();
		return studentDashboard(student, academicYearId, pageRequest);
	}

	@Transactional(readOnly = true)
	public PortalStudentProfileResponse studentProfile() {
		return toStudentProfile(currentStudent());
	}

	@Transactional(readOnly = true)
	public StudentAttendanceHistoryResponse studentAttendance(
			UUID academicYearId,
			LocalDate fromDate,
			LocalDate toDate,
			AttendanceStatus status,
			PageRequestDto pageRequest,
			String sort) {
		return attendanceFor(currentStudent(), academicYearId, fromDate, toDate, status, pageRequest, sort);
	}

	@Transactional(readOnly = true)
	public StudentAttendanceSummaryResponse studentAttendanceSummary(UUID academicYearId) {
		Student student = currentStudent();
		return attendanceService.getStudentSummary(student.getId(), requireAcademicYear(student, academicYearId));
	}

	@Transactional(readOnly = true)
	public StudentExamResultsResponse studentExams(UUID academicYearId, UUID examTypeId, UUID examScheduleId) {
		Student student = currentStudent();
		return examService.getStudentProfileExamResults(student.getId(), resolveAcademicYear(student, academicYearId), examTypeId, examScheduleId);
	}

	@Transactional(readOnly = true)
	public StudentFeeSummaryResponse studentFees(UUID academicYearId) {
		Student student = currentStudent();
		return feeService.studentFeeSummary(student.getId(), resolveAcademicYear(student, academicYearId));
	}

	@Transactional(readOnly = true)
	public PortalLibrarySummaryResponse studentLibrary(PageRequestDto pageRequest) {
		return libraryForStudent(currentStudent().getId(), pageRequest);
	}

	@Transactional(readOnly = true)
	public HostelAllocationResponse studentHostel(UUID academicYearId) {
		Student student = currentStudent();
		return hostelService.currentStudentAllocation(student.getId(), resolveAcademicYear(student, academicYearId)).orElse(null);
	}

	@Transactional(readOnly = true)
	public StudentTransportAssignmentResponse studentTransport(UUID academicYearId) {
		Student student = currentStudent();
		return transportService.currentStudentAssignment(student.getId(), resolveAcademicYear(student, academicYearId)).orElse(null);
	}

	@Transactional(readOnly = true)
	public PageResponse<CommunicationResponse> studentCommunications(PageRequestDto pageRequest) {
		return communicationsForStudent(currentStudent(), pageRequest);
	}

	@Transactional(readOnly = true)
	public List<StudentSummaryResponse> parentChildren() {
		requireRole(ROLE_PARENT);
		UUID userId = currentUserId();
		return studentRepository.findChildrenByParentUserAccountId(userId).stream()
				.map(studentMapper::toSummaryResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public PortalStudentDashboardResponse parentChildDashboard(UUID childId, UUID academicYearId, PageRequestDto pageRequest) {
		Student student = currentParentChild(childId);
		return studentDashboard(student, academicYearId, pageRequest);
	}

	@Transactional(readOnly = true)
	public PortalStudentProfileResponse parentChildProfile(UUID childId) {
		return toStudentProfile(currentParentChild(childId));
	}

	@Transactional(readOnly = true)
	public StudentAttendanceHistoryResponse parentChildAttendance(
			UUID childId,
			UUID academicYearId,
			LocalDate fromDate,
			LocalDate toDate,
			AttendanceStatus status,
			PageRequestDto pageRequest,
			String sort) {
		return attendanceFor(currentParentChild(childId), academicYearId, fromDate, toDate, status, pageRequest, sort);
	}

	@Transactional(readOnly = true)
	public StudentAttendanceSummaryResponse parentChildAttendanceSummary(UUID childId, UUID academicYearId) {
		Student student = currentParentChild(childId);
		return attendanceService.getStudentSummary(student.getId(), requireAcademicYear(student, academicYearId));
	}

	@Transactional(readOnly = true)
	public StudentExamResultsResponse parentChildExams(UUID childId, UUID academicYearId, UUID examTypeId, UUID examScheduleId) {
		Student student = currentParentChild(childId);
		return examService.getStudentProfileExamResults(student.getId(), resolveAcademicYear(student, academicYearId), examTypeId, examScheduleId);
	}

	@Transactional(readOnly = true)
	public StudentFeeSummaryResponse parentChildFees(UUID childId, UUID academicYearId) {
		Student student = currentParentChild(childId);
		return feeService.studentFeeSummary(student.getId(), resolveAcademicYear(student, academicYearId));
	}

	@Transactional(readOnly = true)
	public PortalLibrarySummaryResponse parentChildLibrary(UUID childId, PageRequestDto pageRequest) {
		return libraryForStudent(currentParentChild(childId).getId(), pageRequest);
	}

	@Transactional(readOnly = true)
	public HostelAllocationResponse parentChildHostel(UUID childId, UUID academicYearId) {
		Student student = currentParentChild(childId);
		return hostelService.currentStudentAllocation(student.getId(), resolveAcademicYear(student, academicYearId)).orElse(null);
	}

	@Transactional(readOnly = true)
	public StudentTransportAssignmentResponse parentChildTransport(UUID childId, UUID academicYearId) {
		Student student = currentParentChild(childId);
		return transportService.currentStudentAssignment(student.getId(), resolveAcademicYear(student, academicYearId)).orElse(null);
	}

	@Transactional(readOnly = true)
	public PageResponse<CommunicationResponse> parentChildCommunications(UUID childId, PageRequestDto pageRequest) {
		return communicationsForParentChild(currentParentChild(childId), pageRequest);
	}

	@Transactional(readOnly = true)
	public PortalTeacherDashboardResponse teacherDashboard(UUID academicYearId, PageRequestDto pageRequest) {
		Teacher teacher = currentTeacher();
		List<TeacherAssignmentResponse> assignments = teacherAcademicAssignments(teacher, academicYearId);
		List<PortalTeacherScopeResponse> scopes = teacherScopes(teacher, academicYearId);
		return new PortalTeacherDashboardResponse(
				teacherProfile(teacher, scopes),
				assignments,
				scopes,
				communications(Set.of(CommunicationAudienceType.TEACHERS, CommunicationAudienceType.STAFF), null, null, pageRequest));
	}

	@Transactional(readOnly = true)
	public TeacherResponse teacherProfile() {
		Teacher teacher = currentTeacher();
		return teacherProfile(teacher, teacherScopes(teacher, null));
	}

	@Transactional(readOnly = true)
	public List<PortalTeacherScopeResponse> teacherAssignments(UUID academicYearId) {
		return teacherScopes(currentTeacher(), academicYearId);
	}

	@Transactional(readOnly = true)
	public List<ExamStudentResponse> teacherStudents(UUID academicYearId, UUID classId, UUID sectionId) {
		Teacher teacher = currentTeacher();
		ensureTeacherClassScope(teacher, academicYearId, classId, sectionId);
		return examService.getStudents(academicYearId, classId, sectionId);
	}

	@Transactional(readOnly = true)
	public DailyAttendanceResponse teacherDailyAttendance(UUID academicYearId, UUID classId, UUID sectionId, LocalDate date) {
		Teacher teacher = currentTeacher();
		ensureTeacherClassScope(teacher, academicYearId, classId, sectionId);
		return attendanceService.getDaily(academicYearId, classId, sectionId, date);
	}

	@Transactional
	public DailyAttendanceResponse teacherSaveDaily(DailyAttendanceRequest request) {
		Teacher teacher = currentTeacher();
		ensureTeacherClassScope(teacher, request.academicYearId(), request.classId(), request.sectionId());
		return attendanceService.saveDaily(request);
	}

	@Transactional(readOnly = true)
	public MarksEntryResponse teacherMarks(
			UUID academicYearId,
			UUID classId,
			UUID sectionId,
			UUID examScheduleId,
			UUID subjectId) {
		Teacher teacher = currentTeacher();
		ensureTeacherSubjectScope(teacher, academicYearId, classId, sectionId, subjectId);
		return examService.getMarks(academicYearId, classId, sectionId, examScheduleId, subjectId);
	}

	@Transactional
	public MarksEntryResponse teacherSaveMarks(MarksEntryRequest request) {
		Teacher teacher = currentTeacher();
		ensureTeacherSubjectScope(teacher, request.academicYearId(), request.classId(), request.sectionId(), request.subjectId());
		return examService.saveMarks(request);
	}

	@Transactional(readOnly = true)
	public PageResponse<CommunicationResponse> teacherCommunications(PageRequestDto pageRequest) {
		currentTeacher();
		return communications(Set.of(CommunicationAudienceType.TEACHERS, CommunicationAudienceType.STAFF), null, null, pageRequest);
	}

	private PortalStudentDashboardResponse studentDashboard(Student student, UUID academicYearId, PageRequestDto pageRequest) {
		UUID resolvedAcademicYearId = resolveAcademicYear(student, academicYearId);
		return new PortalStudentDashboardResponse(
				toStudentProfile(student),
				attendanceFor(student, resolvedAcademicYearId, null, null, null, pageRequest, null),
				feeService.studentFeeSummary(student.getId(), resolvedAcademicYearId),
				examService.getStudentProfileExamResults(student.getId(), resolvedAcademicYearId, null, null),
				libraryForStudent(student.getId(), pageRequest),
				hostelService.currentStudentAllocation(student.getId(), resolvedAcademicYearId).orElse(null),
				transportService.currentStudentAssignment(student.getId(), resolvedAcademicYearId).orElse(null),
				communicationsForStudent(student, pageRequest));
	}

	private StudentAttendanceHistoryResponse attendanceFor(
			Student student,
			UUID academicYearId,
			LocalDate fromDate,
			LocalDate toDate,
			AttendanceStatus status,
			PageRequestDto pageRequest,
			String sort) {
		return attendanceService.getStudentAttendanceHistory(
				student.getId(),
				resolveAcademicYear(student, academicYearId),
				fromDate,
				toDate,
				status,
				pageOrDefault(pageRequest),
				sort);
	}

	private PortalLibrarySummaryResponse libraryForStudent(UUID studentId, PageRequestDto pageRequest) {
		return librarySummary(libraryMembershipRepository
				.findFirstByMemberTypeAndStudentIdAndActiveTrueAndDeletedFalseOrderByStartDateDesc(
						LibraryMemberType.STUDENT,
						studentId), pageRequest);
	}

	private PortalLibrarySummaryResponse librarySummary(Optional<LibraryMembership> membership, PageRequestDto pageRequest) {
		PageRequestDto resolvedPage = pageOrDefault(pageRequest);
		if (membership.isEmpty()) {
			return new PortalLibrarySummaryResponse(null, emptyPage(resolvedPage), emptyPage(resolvedPage));
		}
		LibraryMembership value = membership.get();
		return new PortalLibrarySummaryResponse(
				new PortalLibraryMembershipResponse(
						value.getId(),
						value.getMemberType(),
						value.getMembershipNumber(),
						value.getStartDate(),
						value.getExpiryDate(),
						value.isActive()),
				libraryService.loans(null, value.getId(), null, null, value.getMemberType(), null, null, false, resolvedPage),
				libraryService.fines(null, value.getId(), null, null, resolvedPage));
	}

	private PageResponse<CommunicationResponse> communicationsForStudent(Student student, PageRequestDto pageRequest) {
		StudentClassAssignment assignment = currentAssignment(student).orElse(null);
		return communications(
				Set.of(CommunicationAudienceType.STUDENTS),
				classId(assignment),
				sectionId(assignment),
				pageRequest);
	}

	private PageResponse<CommunicationResponse> communicationsForParentChild(Student student, PageRequestDto pageRequest) {
		StudentClassAssignment assignment = currentAssignment(student).orElse(null);
		return communications(
				Set.of(CommunicationAudienceType.PARENTS),
				classId(assignment),
				sectionId(assignment),
				pageRequest);
	}

	private PageResponse<CommunicationResponse> communications(
			Set<CommunicationAudienceType> audienceTypes,
			UUID classId,
			UUID sectionId,
			PageRequestDto pageRequest) {
		Page<CommunicationResponse> page = communicationRecordRepository
				.findVisibleForPortal(audienceTypes, classId, sectionId, Instant.now(), pageOrDefault(pageRequest).toPageable("publishedAt"))
				.map(communicationMapper::toResponse);
		return new PageResponse<>(
				page.getContent(),
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages(),
				page.isFirst(),
				page.isLast());
	}

	private Student currentStudent() {
		requireRole(ROLE_STUDENT);
		UUID userId = currentUserId();
		Student linked = studentRepository.findByUserAccountIdAndDeletedFalse(userId)
				.orElseThrow(() -> new BusinessException(
						ErrorCode.FORBIDDEN,
						"Authenticated user is not linked to a student profile."));
		return loadStudentProfile(linked.getId());
	}

	private Student currentParentChild(UUID childId) {
		requireRole(ROLE_PARENT);
		UUID userId = currentUserId();
		boolean linked = studentRepository.findChildrenByParentUserAccountId(userId).stream()
				.anyMatch(student -> student.getId().equals(childId));
		if (!linked) {
			throw new BusinessException(ErrorCode.FORBIDDEN, "Requested child is not linked to this parent account.");
		}
		return loadStudentProfile(childId);
	}

	private Teacher currentTeacher() {
		requireRole(ROLE_TEACHER);
		UUID userId = currentUserId();
		return teacherRepository.findByUserAccountIdAndDeletedFalse(userId)
				.orElseThrow(() -> new BusinessException(
						ErrorCode.FORBIDDEN,
						"Authenticated user is not linked to a teacher profile."));
	}

	private void ensureTeacherClassScope(Teacher teacher, UUID academicYearId, UUID classId, UUID sectionId) {
		boolean mappedAsClassTeacher = classTeacherMappingRepository
				.existsByTeacherIdAndClassEntityIdAndSectionIdAndActiveTrueAndDeletedFalse(teacher.getId(), classId, sectionId);
		boolean mappedAsSubjectTeacher = subjectTeacherMappingRepository
				.existsByTeacherIdAndClassEntityIdAndSectionIdAndActiveTrueAndDeletedFalse(teacher.getId(), classId, sectionId);
		boolean assigned = teacherAcademicAssignmentsRaw(teacher, academicYearId).stream()
				.anyMatch(assignment -> assignment.isActive()
						&& matchesClassScope(assignment, classId, sectionId));
		if (!mappedAsClassTeacher && !mappedAsSubjectTeacher && !assigned) {
			throw new BusinessException(ErrorCode.FORBIDDEN, "Teacher is not assigned to the selected class and division.");
		}
	}

	private void ensureTeacherSubjectScope(Teacher teacher, UUID academicYearId, UUID classId, UUID sectionId, UUID subjectId) {
		boolean mapped = subjectTeacherMappingRepository
				.existsByTeacherIdAndClassEntityIdAndSectionIdAndSubjectIdAndActiveTrueAndDeletedFalse(
						teacher.getId(),
						classId,
						sectionId,
						subjectId);
		boolean assigned = teacherAcademicAssignmentsRaw(teacher, academicYearId).stream()
				.anyMatch(assignment -> assignment.isActive()
						&& assignment.getAssignmentType() == TeacherAssignmentType.SUBJECT_TEACHER
						&& matchesClassScope(assignment, classId, sectionId)
						&& assignment.getSubject() != null
						&& assignment.getSubject().getId().equals(subjectId));
		if (!mapped && !assigned) {
			throw new BusinessException(ErrorCode.FORBIDDEN, "Teacher is not assigned to the selected subject.");
		}
	}

	private boolean matchesClassScope(TeacherAcademicAssignment assignment, UUID classId, UUID sectionId) {
		return assignment.getClassEntity() != null
				&& assignment.getSection() != null
				&& assignment.getClassEntity().getId().equals(classId)
				&& assignment.getSection().getId().equals(sectionId);
	}

	private List<TeacherAssignmentResponse> teacherAcademicAssignments(Teacher teacher, UUID academicYearId) {
		return teacherAcademicAssignmentsRaw(teacher, academicYearId).stream()
				.filter(assignment -> assignment.getStatus() == TeacherAssignmentStatus.ACTIVE)
				.map(teacherMapper::toAssignmentResponse)
				.toList();
	}

	private List<TeacherAcademicAssignment> teacherAcademicAssignmentsRaw(Teacher teacher, UUID academicYearId) {
		if (academicYearId == null) {
			return teacherAcademicAssignmentRepository.findByTeacherIdAndDeletedFalseOrderByCreatedAtDesc(teacher.getId());
		}
		return teacherAcademicAssignmentRepository.findByTeacherIdAndAcademicYearIdAndDeletedFalseOrderByCreatedAtDesc(
				teacher.getId(),
				academicYearId);
	}

	private List<PortalTeacherScopeResponse> teacherScopes(Teacher teacher, UUID academicYearId) {
		LinkedHashMap<String, PortalTeacherScopeResponse> scopes = new LinkedHashMap<>();
		for (TeacherAcademicAssignment assignment : teacherAcademicAssignmentsRaw(teacher, academicYearId)) {
			if (assignment.isActive() && assignment.getClassEntity() != null && assignment.getSection() != null) {
				addScope(scopes, new PortalTeacherScopeResponse(
						"TEACHER_ASSIGNMENT",
						assignment.getAssignmentType(),
						assignment.getAcademicYear().getId(),
						assignment.getAcademicYear().getName(),
						assignment.getClassEntity().getId(),
						assignment.getClassEntity().getName(),
						assignment.getSection().getId(),
						assignment.getSection().getName(),
						assignment.getSubject() == null ? null : assignment.getSubject().getId(),
						assignment.getSubject() == null ? null : assignment.getSubject().getName()));
			}
		}
		for (ClassTeacherMapping mapping : classMappings(teacher, academicYearId)) {
			addScope(scopes, new PortalTeacherScopeResponse(
					"ACADEMIC_MAPPING",
					TeacherAssignmentType.CLASS_TEACHER,
					mapping.getClassEntity().getAcademicYear().getId(),
					mapping.getClassEntity().getAcademicYear().getName(),
					mapping.getClassEntity().getId(),
					mapping.getClassEntity().getName(),
					mapping.getSection().getId(),
					mapping.getSection().getName(),
					null,
					null));
		}
		for (SubjectTeacherMapping mapping : subjectMappings(teacher, academicYearId)) {
			addScope(scopes, new PortalTeacherScopeResponse(
					"ACADEMIC_MAPPING",
					TeacherAssignmentType.SUBJECT_TEACHER,
					mapping.getClassEntity().getAcademicYear().getId(),
					mapping.getClassEntity().getAcademicYear().getName(),
					mapping.getClassEntity().getId(),
					mapping.getClassEntity().getName(),
					mapping.getSection().getId(),
					mapping.getSection().getName(),
					mapping.getSubject().getId(),
					mapping.getSubject().getName()));
		}
		return List.copyOf(scopes.values());
	}

	private List<ClassTeacherMapping> classMappings(Teacher teacher, UUID academicYearId) {
		if (academicYearId == null) {
			return classTeacherMappingRepository.findByTeacherIdAndActiveTrueAndDeletedFalseOrderByEffectiveFromDesc(teacher.getId());
		}
		return classTeacherMappingRepository.findByTeacherIdAndClassEntityAcademicYearIdAndActiveTrueAndDeletedFalseOrderByEffectiveFromDesc(
				teacher.getId(),
				academicYearId);
	}

	private List<SubjectTeacherMapping> subjectMappings(Teacher teacher, UUID academicYearId) {
		if (academicYearId == null) {
			return subjectTeacherMappingRepository.findByTeacherIdAndActiveTrueAndDeletedFalseOrderByEffectiveFromDesc(teacher.getId());
		}
		return subjectTeacherMappingRepository.findByTeacherIdAndClassEntityAcademicYearIdAndActiveTrueAndDeletedFalseOrderByEffectiveFromDesc(
				teacher.getId(),
				academicYearId);
	}

	private void addScope(LinkedHashMap<String, PortalTeacherScopeResponse> scopes, PortalTeacherScopeResponse scope) {
		scopes.putIfAbsent(scopeKey(scope), scope);
	}

	private String scopeKey(PortalTeacherScopeResponse scope) {
		return scope.assignmentType()
				+ ":"
				+ scope.academicYearId()
				+ ":"
				+ scope.classId()
				+ ":"
				+ scope.sectionId()
				+ ":"
				+ scope.subjectId();
	}

	private TeacherResponse teacherProfile(Teacher teacher, List<PortalTeacherScopeResponse> scopes) {
		long assignedClasses = scopes.stream()
				.filter(scope -> scope.classId() != null && scope.sectionId() != null)
				.map(scope -> scope.classId() + ":" + scope.sectionId())
				.distinct()
				.count();
		long assignedSubjects = scopes.stream()
				.filter(scope -> scope.subjectId() != null)
				.map(scope -> scope.classId() + ":" + scope.sectionId() + ":" + scope.subjectId())
				.distinct()
				.count();
		return teacherMapper.toTeacherResponse(teacher, assignedClasses, assignedSubjects);
	}

	private PortalStudentProfileResponse toStudentProfile(Student student) {
		List<ClassSectionAssignmentResponse> assignments = student.getClassAssignments().stream()
				.filter(assignment -> !assignment.isDeleted())
				.sorted(Comparator.comparing(StudentClassAssignment::isActive).reversed()
						.thenComparing(
								StudentClassAssignment::getEffectiveFrom,
								Comparator.nullsLast(Comparator.reverseOrder())))
				.map(studentMapper::toClassSectionAssignmentResponse)
				.toList();
		return new PortalStudentProfileResponse(
				student.getId(),
				student.getAdmissionNumber(),
				student.getFirstName(),
				student.getMiddleName(),
				student.getLastName(),
				student.getDisplayName(),
				student.getDateOfBirth(),
				student.getGender(),
				student.getBloodGroup(),
				student.getEmail(),
				student.getPhoneNumber(),
				student.getStatus(),
				student.getAdmissionDate(),
				student.getPhotoUrl(),
				student.getPhotoContentType(),
				student.getPhotoFileName(),
				currentAssignment(student).map(studentMapper::toClassSectionAssignmentResponse).orElse(null),
				assignments,
				student.getParents().stream()
						.filter(mapping -> !mapping.isDeleted())
						.sorted(Comparator.comparing(StudentParent::isPrimaryContact).reversed()
								.thenComparing(mapping -> mapping.getRelationType().name()))
						.map(this::toParentContact)
						.toList(),
				student.getDocuments().stream()
						.filter(document -> !document.isDeleted())
						.sorted(Comparator.comparing((StudentDocument document) -> document.getDocumentType().name())
								.thenComparing(StudentDocument::getFileName))
						.map(this::toStudentDocument)
						.toList(),
				student.getCreatedAt(),
				student.getUpdatedAt());
	}

	private PortalParentContactResponse toParentContact(StudentParent mapping) {
		ParentGuardian parent = mapping.getParent();
		return new PortalParentContactResponse(
				mapping.getId(),
				parent.getId(),
				mapping.getRelationType(),
				mapping.isPrimaryContact(),
				mapping.isEmergencyContact(),
				mapping.isPickupAllowed(),
				parent.getDisplayName(),
				parent.getFirstName(),
				parent.getLastName(),
				parent.getEmail(),
				parent.getPhoneNumber(),
				parent.getAlternatePhoneNumber(),
				parent.getOccupation());
	}

	private PortalStudentDocumentResponse toStudentDocument(StudentDocument document) {
		return new PortalStudentDocumentResponse(
				document.getId(),
				document.getDocumentType(),
				document.getDocumentNumber(),
				document.getFileName(),
				document.getContentType(),
				document.getFileSize(),
				document.getFileUrl(),
				document.getVerificationStatus(),
				document.getRemarks(),
				document.getVerifiedAt(),
				document.getCreatedAt());
	}

	private Student loadStudentProfile(UUID studentId) {
		return studentRepository.findProfileByIdAndDeletedFalse(studentId)
				.orElseThrow(() -> new ResourceNotFoundException("Student", studentId));
	}

	private UUID resolveAcademicYear(Student student, UUID academicYearId) {
		return academicYearId == null ? currentAcademicYearId(student) : academicYearId;
	}

	private UUID requireAcademicYear(Student student, UUID academicYearId) {
		UUID resolved = resolveAcademicYear(student, academicYearId);
		if (resolved == null) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Academic year is required.");
		}
		return resolved;
	}

	private UUID currentAcademicYearId(Student student) {
		return currentAssignment(student)
				.map(this::academicYearId)
				.orElse(null);
	}

	private Optional<StudentClassAssignment> currentAssignment(Student student) {
		return student.getCurrentAssignment();
	}

	private UUID academicYearId(StudentClassAssignment assignment) {
		return assignment == null || assignment.getAcademicYearEntity() == null
				? null
				: assignment.getAcademicYearEntity().getId();
	}

	private UUID classId(StudentClassAssignment assignment) {
		return assignment == null || assignment.getClassEntity() == null
				? null
				: assignment.getClassEntity().getId();
	}

	private UUID sectionId(StudentClassAssignment assignment) {
		return assignment == null || assignment.getSectionEntity() == null
				? null
				: assignment.getSectionEntity().getId();
	}

	private PageRequestDto pageOrDefault(PageRequestDto pageRequest) {
		return pageRequest == null ? new PageRequestDto(null, null, null, null) : pageRequest;
	}

	private <T> PageResponse<T> emptyPage(PageRequestDto pageRequest) {
		return new PageResponse<>(List.of(), pageRequest.page(), pageRequest.size(), 0, 0, true, true);
	}

	private void requireRole(String roleAuthority) {
		Authentication authentication = currentAuthentication();
		if (authentication.getAuthorities().stream().noneMatch(authority -> authority.getAuthority().equals(roleAuthority))) {
			throw new BusinessException(ErrorCode.FORBIDDEN, "Portal access is not allowed for this account.");
		}
	}

	private UUID currentUserId() {
		Authentication authentication = currentAuthentication();
		Object principal = authentication.getPrincipal();
		if (principal instanceof SchoolUserPrincipal userPrincipal) {
			return userPrincipal.getId();
		}
		if (principal instanceof Jwt jwt) {
			return parseUuid(jwt.getSubject())
					.orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN, "Authenticated user id is invalid."));
		}
		return parseUuid(authentication.getName())
				.orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN, "Authenticated user id is required."));
	}

	private Authentication currentAuthentication() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null
				|| !authentication.isAuthenticated()
				|| authentication instanceof AnonymousAuthenticationToken) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED, "Authentication is required.");
		}
		return authentication;
	}

	private Optional<UUID> parseUuid(String value) {
		if (!StringUtils.hasText(value)) {
			return Optional.empty();
		}
		try {
			return Optional.of(UUID.fromString(value));
		}
		catch (IllegalArgumentException ex) {
			return Optional.empty();
		}
	}
}
