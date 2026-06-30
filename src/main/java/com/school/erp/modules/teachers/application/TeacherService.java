package com.school.erp.modules.teachers.application;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.school.erp.common.audit.application.AuditLogEvent;
import com.school.erp.common.audit.application.AuditLogService;
import com.school.erp.common.exception.BusinessException;
import com.school.erp.common.exception.ErrorCode;
import com.school.erp.common.exception.ResourceNotFoundException;
import com.school.erp.modules.academic.api.dto.AcademicYearResponse;
import com.school.erp.modules.academic.api.dto.AssignClassTeacherRequest;
import com.school.erp.modules.academic.api.dto.AssignSubjectTeacherRequest;
import com.school.erp.modules.academic.application.AcademicHierarchyService;
import com.school.erp.modules.academic.domain.AcademicYear;
import com.school.erp.modules.academic.domain.ClassEntity;
import com.school.erp.modules.academic.domain.ClassTeacherMapping;
import com.school.erp.modules.academic.domain.SectionEntity;
import com.school.erp.modules.academic.domain.Subject;
import com.school.erp.modules.academic.domain.SubjectTeacherMapping;
import com.school.erp.modules.academic.domain.Teacher;
import com.school.erp.modules.academic.domain.TeacherStatus;
import com.school.erp.modules.academic.infrastructure.ClassTeacherMappingRepository;
import com.school.erp.modules.academic.infrastructure.SubjectTeacherMappingRepository;
import com.school.erp.modules.academic.infrastructure.TeacherRepository;
import com.school.erp.modules.teachers.api.dto.TeacherAcademicMappingResponse;
import com.school.erp.modules.teachers.api.dto.TeacherAssignmentRequest;
import com.school.erp.modules.teachers.api.dto.TeacherAssignmentResponse;
import com.school.erp.modules.teachers.api.dto.TeacherDocumentRequest;
import com.school.erp.modules.teachers.api.dto.TeacherDocumentResponse;
import com.school.erp.modules.teachers.api.dto.TeacherProfileResponse;
import com.school.erp.modules.teachers.api.dto.TeacherRequest;
import com.school.erp.modules.teachers.api.dto.TeacherResponse;
import com.school.erp.modules.teachers.domain.TeacherAcademicAssignment;
import com.school.erp.modules.teachers.domain.TeacherAssignmentStatus;
import com.school.erp.modules.teachers.domain.TeacherAssignmentType;
import com.school.erp.modules.teachers.domain.TeacherDocument;
import com.school.erp.modules.teachers.domain.TeacherDocumentStatus;
import com.school.erp.modules.teachers.infrastructure.TeacherAcademicAssignmentRepository;
import com.school.erp.modules.teachers.infrastructure.TeacherDocumentRepository;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TeacherService {

	private static final String MODULE_NAME = "TEACHERS";

	private final TeacherRepository teacherRepository;
	private final TeacherAcademicAssignmentRepository assignmentRepository;
	private final TeacherDocumentRepository documentRepository;
	private final ClassTeacherMappingRepository classTeacherMappingRepository;
	private final SubjectTeacherMappingRepository subjectTeacherMappingRepository;
	private final AcademicHierarchyService academicHierarchyService;
	private final TeacherMapper teacherMapper;
	private final AuditLogService auditLogService;

	@Transactional(readOnly = true)
	public List<AcademicYearResponse> academicYears() {
		return academicHierarchyService.getAcademicYears();
	}

	@Transactional(readOnly = true)
	public List<TeacherResponse> teachers(UUID academicYearId) {
		if (academicYearId != null) {
			academicHierarchyService.loadAcademicYear(academicYearId);
		}
		return teacherRepository.findAllByDeletedFalseOrderByFirstNameAscLastNameAsc().stream()
				.map(teacher -> teacherMapper.toTeacherResponse(
						teacher,
						assignedClassesCount(teacher.getId(), academicYearId),
						assignedSubjectsCount(teacher.getId(), academicYearId)))
				.toList();
	}

	@Transactional
	public TeacherResponse createTeacher(TeacherRequest request) {
		validateEmployeeCodeAvailable(request.employeeCode(), null);
		Teacher teacher = new Teacher(
				request.employeeCode(),
				request.firstName(),
				request.lastName(),
				request.email(),
				request.mobileNumber());
		teacher.updateProfile(
				request.employeeCode(),
				request.firstName(),
				request.middleName(),
				request.lastName(),
				request.gender(),
				request.dateOfBirth(),
				request.mobileNumber(),
				request.email(),
				request.qualification(),
				request.experienceYears(),
				request.joiningDate(),
				statusOrActive(request.status()),
				request.userId());
		TeacherResponse response = teacherMapper.toTeacherResponse(teacherRepository.save(teacher), 0, 0);
		audit("Teacher", response.id(), "TEACHER_CREATED", null, response);
		return response;
	}

	@Transactional
	public TeacherResponse updateTeacher(UUID teacherId, TeacherRequest request) {
		Teacher teacher = loadTeacher(teacherId);
		TeacherResponse oldValue = teacherMapper.toTeacherResponse(teacher, 0, 0);
		validateEmployeeCodeAvailable(request.employeeCode(), teacherId);
		teacher.updateProfile(
				request.employeeCode(),
				request.firstName(),
				request.middleName(),
				request.lastName(),
				request.gender(),
				request.dateOfBirth(),
				request.mobileNumber(),
				request.email(),
				request.qualification(),
				request.experienceYears(),
				request.joiningDate(),
				statusOrActive(request.status()),
				request.userId());
		TeacherResponse response = teacherMapper.toTeacherResponse(
				teacher,
				assignedClassesCount(teacherId, null),
				assignedSubjectsCount(teacherId, null));
		audit("Teacher", teacherId, "TEACHER_UPDATED", oldValue, response);
		return response;
	}

	@Transactional(readOnly = true)
	public TeacherResponse getTeacher(UUID teacherId) {
		Teacher teacher = loadTeacher(teacherId);
		return teacherMapper.toTeacherResponse(
				teacher,
				assignedClassesCount(teacherId, null),
				assignedSubjectsCount(teacherId, null));
	}

	@Transactional
	public TeacherResponse deleteTeacher(UUID teacherId) {
		Teacher teacher = loadTeacher(teacherId);
		TeacherResponse oldValue = teacherMapper.toTeacherResponse(
				teacher,
				assignedClassesCount(teacherId, null),
				assignedSubjectsCount(teacherId, null));
		teacher.softDelete(currentActor());
		audit("Teacher", teacherId, "TEACHER_DELETED", oldValue, Map.of("deleted", true, "teacherId", teacherId));
		return oldValue;
	}

	@Transactional(readOnly = true)
	public TeacherProfileResponse profile(UUID teacherId, UUID academicYearId) {
		Teacher teacher = loadTeacher(teacherId);
		if (academicYearId != null) {
			academicHierarchyService.loadAcademicYear(academicYearId);
		}
		List<TeacherAssignmentResponse> assignments = assignmentRows(teacherId, academicYearId).stream()
				.map(teacherMapper::toAssignmentResponse)
				.toList();
		List<TeacherAcademicMappingResponse> classMappings = classMappings(teacherId, academicYearId).stream()
				.map(teacherMapper::toClassTeacherMapping)
				.toList();
		List<TeacherAcademicMappingResponse> subjectMappings = subjectMappings(teacherId, academicYearId).stream()
				.map(teacherMapper::toSubjectTeacherMapping)
				.toList();
		return new TeacherProfileResponse(
				teacherMapper.toTeacherResponse(teacher, classMappings.size(), subjectMappings.size()),
				assignments,
				classMappings,
				subjectMappings,
				documents(teacherId),
				"Teacher attendance module is not configured yet.",
				"Payroll module is not configured yet.",
				"Teacher notifications are available through the notification module when recipients are configured.");
	}

	@Transactional(readOnly = true)
	public List<TeacherAssignmentResponse> assignments(UUID teacherId, UUID academicYearId) {
		loadTeacher(teacherId);
		if (academicYearId != null) {
			academicHierarchyService.loadAcademicYear(academicYearId);
		}
		return assignmentRows(teacherId, academicYearId).stream()
				.map(teacherMapper::toAssignmentResponse)
				.toList();
	}

	@Transactional
	public TeacherAssignmentResponse createAssignment(UUID teacherId, TeacherAssignmentRequest request) {
		ResolvedTeacherAssignment resolved = resolveAssignment(teacherId, request);
		TeacherAcademicAssignment assignment = assignmentRepository.save(new TeacherAcademicAssignment(
				resolved.teacher(),
				resolved.academicYear(),
				request.assignmentType(),
				resolved.classEntity(),
				resolved.section(),
				resolved.subject(),
				statusOrActive(request.status())));
		if (assignment.isActive()) {
			syncAcademicMapping(assignment, request.effectiveFrom());
		}
		TeacherAssignmentResponse response = teacherMapper.toAssignmentResponse(assignment);
		audit("TeacherAcademicAssignment", response.id(), "TEACHER_ASSIGNMENT_ADDED", null, response);
		return response;
	}

	@Transactional
	public TeacherAssignmentResponse updateAssignment(UUID teacherId, UUID assignmentId, TeacherAssignmentRequest request) {
		TeacherAcademicAssignment assignment = loadAssignment(assignmentId);
		ensureAssignmentBelongsToTeacher(assignment, teacherId);
		TeacherAssignmentResponse oldValue = teacherMapper.toAssignmentResponse(assignment);
		ResolvedTeacherAssignment resolved = resolveAssignment(teacherId, request);
		assignment.update(
				resolved.teacher(),
				resolved.academicYear(),
				request.assignmentType(),
				resolved.classEntity(),
				resolved.section(),
				resolved.subject(),
				statusOrActive(request.status()));
		if (assignment.isActive()) {
			syncAcademicMapping(assignment, request.effectiveFrom());
		}
		TeacherAssignmentResponse response = teacherMapper.toAssignmentResponse(assignment);
		audit("TeacherAcademicAssignment", assignmentId, "TEACHER_ASSIGNMENT_UPDATED", oldValue, response);
		return response;
	}

	@Transactional
	public TeacherAssignmentResponse deleteAssignment(UUID teacherId, UUID assignmentId) {
		TeacherAcademicAssignment assignment = loadAssignment(assignmentId);
		ensureAssignmentBelongsToTeacher(assignment, teacherId);
		TeacherAssignmentResponse oldValue = teacherMapper.toAssignmentResponse(assignment);
		assignment.softDelete(currentActor());
		audit("TeacherAcademicAssignment", assignmentId, "TEACHER_ASSIGNMENT_DELETED", oldValue, Map.of("deleted", true, "assignmentId", assignmentId));
		return oldValue;
	}

	@Transactional(readOnly = true)
	public List<TeacherDocumentResponse> documents(UUID teacherId) {
		loadTeacher(teacherId);
		return documentRepository.findByTeacherIdAndDeletedFalseOrderByUploadedAtDesc(teacherId).stream()
				.map(teacherMapper::toDocumentResponse)
				.toList();
	}

	@Transactional
	public TeacherDocumentResponse createDocument(UUID teacherId, TeacherDocumentRequest request) {
		Teacher teacher = loadTeacher(teacherId);
		validateDocumentLocation(request);
		TeacherDocument document = documentRepository.save(new TeacherDocument(
				teacher,
				request.documentType(),
				request.fileName(),
				request.fileUrl(),
				request.filePath(),
				documentStatusOrActive(request.status())));
		TeacherDocumentResponse response = teacherMapper.toDocumentResponse(document);
		audit("TeacherDocument", response.id(), "TEACHER_DOCUMENT_UPLOADED", null, response);
		return response;
	}

	@Transactional
	public TeacherDocumentResponse updateDocument(UUID teacherId, UUID documentId, TeacherDocumentRequest request) {
		TeacherDocument document = loadDocument(documentId);
		ensureDocumentBelongsToTeacher(document, teacherId);
		validateDocumentLocation(request);
		TeacherDocumentResponse oldValue = teacherMapper.toDocumentResponse(document);
		document.update(
				request.documentType(),
				request.fileName(),
				request.fileUrl(),
				request.filePath(),
				documentStatusOrActive(request.status()));
		TeacherDocumentResponse response = teacherMapper.toDocumentResponse(document);
		audit("TeacherDocument", documentId, "TEACHER_DOCUMENT_UPDATED", oldValue, response);
		return response;
	}

	@Transactional
	public TeacherDocumentResponse deleteDocument(UUID teacherId, UUID documentId) {
		TeacherDocument document = loadDocument(documentId);
		ensureDocumentBelongsToTeacher(document, teacherId);
		TeacherDocumentResponse oldValue = teacherMapper.toDocumentResponse(document);
		document.softDelete(currentActor());
		audit("TeacherDocument", documentId, "TEACHER_DOCUMENT_DELETED", oldValue, Map.of("deleted", true, "documentId", documentId));
		return oldValue;
	}

	private ResolvedTeacherAssignment resolveAssignment(UUID teacherId, TeacherAssignmentRequest request) {
		Teacher teacher = loadActiveTeacher(teacherId);
		AcademicYear academicYear = academicHierarchyService.loadAcademicYear(request.academicYearId());
		ClassEntity classEntity = request.classId() == null ? null : academicHierarchyService.loadClass(request.classId());
		if (classEntity != null && !classEntity.getAcademicYear().getId().equals(academicYear.getId())) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Class does not belong to the selected academic year.");
		}
		SectionEntity section = request.sectionId() == null
				? null
				: academicHierarchyService.loadSection(request.sectionId());
		if (section != null) {
			if (classEntity == null) {
				throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Class is required when section is selected.");
			}
			if (!section.getClassEntity().getId().equals(classEntity.getId())) {
				throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Section does not belong to the selected class.");
			}
		}
		Subject subject = request.subjectId() == null ? null : academicHierarchyService.loadSubject(request.subjectId());
		validateAssignmentShape(request.assignmentType(), classEntity, section, subject);
		return new ResolvedTeacherAssignment(teacher, academicYear, classEntity, section, subject);
	}

	private void validateAssignmentShape(
			TeacherAssignmentType type,
			ClassEntity classEntity,
			SectionEntity section,
			Subject subject) {
		if (type == TeacherAssignmentType.CLASS_TEACHER && (classEntity == null || section == null)) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Class and section are required for class teacher assignment.");
		}
		if (type == TeacherAssignmentType.SUBJECT_TEACHER
				&& (classEntity == null || section == null || subject == null)) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Class, section, and subject are required for subject teacher assignment.");
		}
	}

	private void syncAcademicMapping(TeacherAcademicAssignment assignment, LocalDate effectiveFrom) {
		LocalDate startDate = effectiveFrom == null ? LocalDate.now() : effectiveFrom;
		if (assignment.getAssignmentType() == TeacherAssignmentType.CLASS_TEACHER) {
			academicHierarchyService.assignClassTeacher(
					assignment.getClassEntity().getId(),
					assignment.getSection().getId(),
					new AssignClassTeacherRequest(assignment.getTeacher().getId(), startDate));
			audit("ClassTeacherMapping", assignment.getId(), "CLASS_TEACHER_MAPPED", null, teacherMapper.toAssignmentResponse(assignment));
		}
		else if (assignment.getAssignmentType() == TeacherAssignmentType.SUBJECT_TEACHER) {
			academicHierarchyService.assignSubjectTeacher(
					assignment.getClassEntity().getId(),
					assignment.getSection().getId(),
					assignment.getSubject().getId(),
					new AssignSubjectTeacherRequest(assignment.getTeacher().getId(), startDate));
			audit("SubjectTeacherMapping", assignment.getId(), "SUBJECT_TEACHER_MAPPED", null, teacherMapper.toAssignmentResponse(assignment));
		}
	}

	private long assignedClassesCount(UUID teacherId, UUID academicYearId) {
		long count = classMappings(teacherId, academicYearId).size();
		count += assignmentRows(teacherId, academicYearId).stream()
				.filter(assignment -> assignment.getAssignmentType() == TeacherAssignmentType.CLASS_TEACHER
						|| assignment.getAssignmentType() == TeacherAssignmentType.COORDINATOR)
				.count();
		return count;
	}

	private long assignedSubjectsCount(UUID teacherId, UUID academicYearId) {
		long count = subjectMappings(teacherId, academicYearId).size();
		count += assignmentRows(teacherId, academicYearId).stream()
				.filter(assignment -> assignment.getAssignmentType() == TeacherAssignmentType.SUBJECT_TEACHER)
				.count();
		return count;
	}

	private List<TeacherAcademicAssignment> assignmentRows(UUID teacherId, UUID academicYearId) {
		if (academicYearId == null) {
			return assignmentRepository.findByTeacherIdAndDeletedFalseOrderByCreatedAtDesc(teacherId);
		}
		return assignmentRepository.findByTeacherIdAndAcademicYearIdAndDeletedFalseOrderByCreatedAtDesc(teacherId, academicYearId);
	}

	private List<ClassTeacherMapping> classMappings(UUID teacherId, UUID academicYearId) {
		if (academicYearId == null) {
			return classTeacherMappingRepository.findByTeacherIdAndActiveTrueAndDeletedFalseOrderByEffectiveFromDesc(teacherId);
		}
		return classTeacherMappingRepository
				.findByTeacherIdAndClassEntityAcademicYearIdAndActiveTrueAndDeletedFalseOrderByEffectiveFromDesc(
						teacherId,
						academicYearId);
	}

	private List<SubjectTeacherMapping> subjectMappings(UUID teacherId, UUID academicYearId) {
		if (academicYearId == null) {
			return subjectTeacherMappingRepository.findByTeacherIdAndActiveTrueAndDeletedFalseOrderByEffectiveFromDesc(teacherId);
		}
		return subjectTeacherMappingRepository
				.findByTeacherIdAndClassEntityAcademicYearIdAndActiveTrueAndDeletedFalseOrderByEffectiveFromDesc(
						teacherId,
						academicYearId);
	}

	private void validateEmployeeCodeAvailable(String employeeCode, UUID excludedTeacherId) {
		if (!StringUtils.hasText(employeeCode)) {
			return;
		}
		teacherRepository.findByEmployeeNumberIgnoreCaseAndDeletedFalse(employeeCode)
				.filter(existing -> excludedTeacherId == null || !existing.getId().equals(excludedTeacherId))
				.ifPresent(existing -> {
					throw new BusinessException(ErrorCode.CONFLICT, "Teacher employee code already exists.");
				});
	}

	private void validateDocumentLocation(TeacherDocumentRequest request) {
		if (!StringUtils.hasText(request.fileUrl()) && !StringUtils.hasText(request.filePath())) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Document file URL or file path is required.");
		}
	}

	private Teacher loadTeacher(UUID teacherId) {
		return teacherRepository.findByIdAndDeletedFalse(teacherId)
				.orElseThrow(() -> new ResourceNotFoundException("Teacher", teacherId));
	}

	private Teacher loadActiveTeacher(UUID teacherId) {
		Teacher teacher = loadTeacher(teacherId);
		if (!teacher.isActive()) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Teacher is inactive.");
		}
		return teacher;
	}

	private TeacherAcademicAssignment loadAssignment(UUID assignmentId) {
		return assignmentRepository.findByIdAndDeletedFalse(assignmentId)
				.orElseThrow(() -> new ResourceNotFoundException("Teacher assignment", assignmentId));
	}

	private TeacherDocument loadDocument(UUID documentId) {
		return documentRepository.findByIdAndDeletedFalse(documentId)
				.orElseThrow(() -> new ResourceNotFoundException("Teacher document", documentId));
	}

	private void ensureAssignmentBelongsToTeacher(TeacherAcademicAssignment assignment, UUID teacherId) {
		if (!assignment.getTeacher().getId().equals(teacherId)) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Assignment does not belong to the selected teacher.");
		}
	}

	private void ensureDocumentBelongsToTeacher(TeacherDocument document, UUID teacherId) {
		if (!document.getTeacher().getId().equals(teacherId)) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Document does not belong to the selected teacher.");
		}
	}

	private TeacherStatus statusOrActive(TeacherStatus status) {
		return status == null ? TeacherStatus.ACTIVE : status;
	}

	private TeacherAssignmentStatus statusOrActive(TeacherAssignmentStatus status) {
		return status == null ? TeacherAssignmentStatus.ACTIVE : status;
	}

	private TeacherDocumentStatus documentStatusOrActive(TeacherDocumentStatus status) {
		return status == null ? TeacherDocumentStatus.ACTIVE : status;
	}

	private String currentActor() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			return "system";
		}
		return authentication.getName();
	}

	private void audit(String entityName, UUID entityId, String action, Object oldValue, Object newValue) {
		auditLogService.record(new AuditLogEvent(
				MODULE_NAME,
				entityName,
				entityId == null ? null : entityId.toString(),
				action,
				oldValue,
				newValue == null ? Map.of() : newValue));
	}

	private record ResolvedTeacherAssignment(
			Teacher teacher,
			AcademicYear academicYear,
			ClassEntity classEntity,
			SectionEntity section,
			Subject subject) {
	}
}
