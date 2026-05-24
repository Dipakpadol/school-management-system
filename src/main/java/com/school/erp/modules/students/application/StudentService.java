package com.school.erp.modules.students.application;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.school.erp.common.api.PageRequestDto;
import com.school.erp.common.api.PageResponse;
import com.school.erp.common.audit.application.AuditLogEvent;
import com.school.erp.common.audit.application.AuditLogService;
import com.school.erp.common.exception.BusinessException;
import com.school.erp.common.exception.ErrorCode;
import com.school.erp.common.exception.ResourceNotFoundException;
import com.school.erp.modules.students.api.dto.ClassSectionAssignmentRequest;
import com.school.erp.modules.students.api.dto.ParentGuardianRequest;
import com.school.erp.modules.students.api.dto.ParentMappingRequest;
import com.school.erp.modules.students.api.dto.StudentAdmissionRequest;
import com.school.erp.modules.students.api.dto.StudentDocumentRequest;
import com.school.erp.modules.students.api.dto.StudentProfileRequest;
import com.school.erp.modules.students.api.dto.StudentResponse;
import com.school.erp.modules.students.api.dto.StudentSearchRequest;
import com.school.erp.modules.students.api.dto.StudentStatusUpdateRequest;
import com.school.erp.modules.students.api.dto.StudentSummaryResponse;
import com.school.erp.modules.students.domain.ParentGuardian;
import com.school.erp.modules.students.domain.Student;
import com.school.erp.modules.students.domain.StudentClassAssignment;
import com.school.erp.modules.students.domain.StudentDocument;
import com.school.erp.modules.students.domain.StudentParent;
import com.school.erp.modules.students.domain.StudentStatus;
import com.school.erp.modules.students.infrastructure.ParentGuardianRepository;
import com.school.erp.modules.students.infrastructure.StudentRepository;
import com.school.erp.modules.students.infrastructure.StudentSpecifications;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StudentService {

	private static final String MODULE_NAME = "STUDENTS";
	private static final String ENTITY_NAME = "Student";

	private final StudentRepository studentRepository;
	private final ParentGuardianRepository parentGuardianRepository;
	private final StudentMapper studentMapper;
	private final AuditLogService auditLogService;

	@Transactional
	public StudentResponse admitStudent(StudentAdmissionRequest request) {
		validateAdmissionRequest(request);
		ensureAdmissionNumberAvailable(request.admissionNumber());

		Student student = studentMapper.toStudent(request);
		request.parents().forEach(parent -> addParentMapping(student, parent));
		assignClassSection(student, request.classAssignment());
		optionalDocuments(request.documents()).forEach(document -> addDocument(student, document));

		StudentResponse response = studentMapper.toProfileResponse(studentRepository.save(student));
		auditStudent(response.id(), "CREATE", null, response);
		return response;
	}

	@Transactional(readOnly = true)
	public StudentResponse getStudentProfile(UUID studentId) {
		return studentMapper.toProfileResponse(loadProfile(studentId));
	}

	@Transactional
	public StudentResponse updateStudentProfile(UUID studentId, StudentProfileRequest request) {
		Student student = loadProfile(studentId);
		StudentResponse oldValue = studentMapper.toProfileResponse(student);
		studentMapper.updateStudentProfile(student, request);
		StudentResponse response = studentMapper.toProfileResponse(student);
		auditStudent(studentId, "UPDATE", oldValue, response);
		return response;
	}

	@Transactional
	public StudentResponse addParent(UUID studentId, ParentMappingRequest request) {
		Student student = loadProfile(studentId);
		StudentResponse oldValue = studentMapper.toProfileResponse(student);
		validateParentRequest(request.parent());
		addParentMapping(student, request);
		StudentResponse response = studentMapper.toProfileResponse(student);
		auditStudent(studentId, "PARENT_ADDED", oldValue, response);
		return response;
	}

	@Transactional
	public StudentResponse updateParent(UUID studentId, UUID parentMappingId, ParentMappingRequest request) {
		Student student = loadProfile(studentId);
		StudentResponse oldValue = studentMapper.toProfileResponse(student);
		validateParentRequest(request.parent());
		StudentParent mapping = findParentMapping(student, parentMappingId);
		ParentGuardian parent = resolveParent(request.parent());
		if (request.primaryContact()) {
			student.getParents().forEach(StudentParent::markSecondary);
		}
		mapping.update(
				parent,
				request.relationType(),
				request.primaryContact(),
				request.emergencyContact(),
				request.pickupAllowed());
		StudentResponse response = studentMapper.toProfileResponse(student);
		auditStudent(studentId, "PARENT_UPDATED", oldValue, response);
		return response;
	}

	@Transactional
	public StudentResponse deleteParent(UUID studentId, UUID parentMappingId) {
		Student student = loadProfile(studentId);
		StudentResponse oldValue = studentMapper.toProfileResponse(student);
		findParentMapping(student, parentMappingId).softDelete(currentActor());
		StudentResponse response = studentMapper.toProfileResponse(student);
		auditStudent(studentId, "PARENT_DELETED", oldValue, response);
		return response;
	}

	@Transactional
	public StudentResponse addDocument(UUID studentId, StudentDocumentRequest request) {
		Student student = loadProfile(studentId);
		StudentResponse oldValue = studentMapper.toProfileResponse(student);
		addDocument(student, request);
		StudentResponse response = studentMapper.toProfileResponse(student);
		auditStudent(studentId, "DOCUMENT_ADDED", oldValue, response);
		return response;
	}

	@Transactional
	public StudentResponse updateDocument(UUID studentId, UUID documentId, StudentDocumentRequest request) {
		Student student = loadProfile(studentId);
		StudentResponse oldValue = studentMapper.toProfileResponse(student);
		validateDocumentRequest(request);
		findDocument(student, documentId).update(
				request.documentType(),
				request.documentNumber(),
				request.fileName(),
				request.contentType(),
				request.fileSize(),
				request.storageKey(),
				request.fileUrl(),
				request.remarks());
		StudentResponse response = studentMapper.toProfileResponse(student);
		auditStudent(studentId, "DOCUMENT_UPDATED", oldValue, response);
		return response;
	}

	@Transactional
	public StudentResponse deleteDocument(UUID studentId, UUID documentId) {
		Student student = loadProfile(studentId);
		StudentResponse oldValue = studentMapper.toProfileResponse(student);
		findDocument(student, documentId).softDelete(currentActor());
		StudentResponse response = studentMapper.toProfileResponse(student);
		auditStudent(studentId, "DOCUMENT_DELETED", oldValue, response);
		return response;
	}

	@Transactional
	public StudentResponse assignClassSection(UUID studentId, ClassSectionAssignmentRequest request) {
		Student student = loadProfile(studentId);
		StudentResponse oldValue = studentMapper.toProfileResponse(student);
		assignClassSection(student, request);
		StudentResponse response = studentMapper.toProfileResponse(student);
		auditStudent(studentId, "CLASS_ASSIGNMENT_CHANGED", oldValue, response);
		return response;
	}

	@Transactional
	public StudentResponse updateClassAssignment(UUID studentId, UUID assignmentId, ClassSectionAssignmentRequest request) {
		Student student = loadProfile(studentId);
		StudentResponse oldValue = studentMapper.toProfileResponse(student);
		StudentClassAssignment assignment = findClassAssignment(student, assignmentId);
		if (assignment.isActive()) {
			student.getClassAssignments().stream()
					.filter(existing -> !existing.getId().equals(assignmentId))
					.filter(StudentClassAssignment::isActive)
					.forEach(existing -> existing.deactivate(request.effectiveFrom().minusDays(1)));
		}
		assignment.update(
				request.academicYear(),
				request.className(),
				request.sectionName(),
				request.rollNumber(),
				request.effectiveFrom(),
				null,
				true);
		StudentResponse response = studentMapper.toProfileResponse(student);
		auditStudent(studentId, "CLASS_ASSIGNMENT_UPDATED", oldValue, response);
		return response;
	}

	@Transactional
	public StudentResponse deleteClassAssignment(UUID studentId, UUID assignmentId) {
		Student student = loadProfile(studentId);
		StudentResponse oldValue = studentMapper.toProfileResponse(student);
		findClassAssignment(student, assignmentId).softDelete(currentActor());
		StudentResponse response = studentMapper.toProfileResponse(student);
		auditStudent(studentId, "CLASS_ASSIGNMENT_DELETED", oldValue, response);
		return response;
	}

	@Transactional
	public StudentResponse updateStatus(UUID studentId, StudentStatusUpdateRequest request) {
		Student student = loadProfile(studentId);
		StudentResponse oldValue = studentMapper.toProfileResponse(student);
		student.changeStatus(request.status());
		StudentResponse response = studentMapper.toProfileResponse(student);
		auditStudent(studentId, "STATUS_CHANGE", oldValue, response);
		return response;
	}

	@Transactional
	public StudentResponse activate(UUID studentId) {
		return updateStatus(studentId, new StudentStatusUpdateRequest(StudentStatus.ACTIVE));
	}

	@Transactional
	public StudentResponse deactivate(UUID studentId) {
		return updateStatus(studentId, new StudentStatusUpdateRequest(StudentStatus.INACTIVE));
	}

	@Transactional
	public void deleteStudent(UUID studentId) {
		Student student = loadProfile(studentId);
		StudentResponse oldValue = studentMapper.toProfileResponse(student);
		student.softDelete(currentActor());
		auditStudent(studentId, "DELETE", oldValue, Map.of("deleted", true, "studentId", studentId));
	}

	@Transactional
	public void permanentlyDeleteStudent(UUID studentId) {
		Student student = loadProfile(studentId);
		StudentResponse oldValue = studentMapper.toProfileResponse(student);
		studentRepository.delete(student);
		auditStudent(studentId, "PERMANENT_DELETE", oldValue, Map.of("permanentlyDeleted", true, "studentId", studentId));
	}

	private Student loadProfile(UUID studentId) {
		return studentRepository.findProfileByIdAndDeletedFalse(studentId)
				.orElseThrow(() -> new ResourceNotFoundException("Student", studentId));
	}

	@Transactional(readOnly = true)
	public PageResponse<StudentSummaryResponse> search(StudentSearchRequest request, PageRequestDto pageRequest) {
		validateSearchRequest(request);
		return PageResponse.from(
				studentRepository.findAll(StudentSpecifications.matching(request), pageRequest.toPageable("admissionNumber")),
				studentMapper::toSummaryResponse);
	}

	private void addParentMapping(Student student, ParentMappingRequest request) {
		validateParentRequest(request.parent());
		ParentGuardian parent = resolveParent(request.parent());
		if (student.hasParentMapping(parent, request.relationType())) {
			throw new BusinessException(
					ErrorCode.CONFLICT,
					"Parent is already mapped to this student with relation: " + request.relationType());
		}
		student.addParent(
				parent,
				request.relationType(),
				request.primaryContact(),
				request.emergencyContact(),
				request.pickupAllowed());
	}

	private StudentParent findParentMapping(Student student, UUID mappingId) {
		return student.getParents().stream()
				.filter(mapping -> !mapping.isDeleted())
				.filter(mapping -> mapping.getId().equals(mappingId))
				.findFirst()
				.orElseThrow(() -> new ResourceNotFoundException("Student parent mapping", mappingId));
	}

	private StudentDocument findDocument(Student student, UUID documentId) {
		return student.getDocuments().stream()
				.filter(document -> !document.isDeleted())
				.filter(document -> document.getId().equals(documentId))
				.findFirst()
				.orElseThrow(() -> new ResourceNotFoundException("Student document", documentId));
	}

	private StudentClassAssignment findClassAssignment(Student student, UUID assignmentId) {
		return student.getClassAssignments().stream()
				.filter(assignment -> !assignment.isDeleted())
				.filter(assignment -> assignment.getId().equals(assignmentId))
				.findFirst()
				.orElseThrow(() -> new ResourceNotFoundException("Student class assignment", assignmentId));
	}

	private ParentGuardian resolveParent(ParentGuardianRequest request) {
		if (StringUtils.hasText(request.email())) {
			return parentGuardianRepository.findByEmailIgnoreCaseAndDeletedFalse(request.email())
					.map(parent -> {
						studentMapper.updateParentGuardian(parent, request);
						return parent;
					})
					.orElseGet(() -> parentGuardianRepository.save(studentMapper.toParentGuardian(request)));
		}
		return parentGuardianRepository.save(studentMapper.toParentGuardian(request));
	}

	private void assignClassSection(Student student, ClassSectionAssignmentRequest request) {
		student.assignClassSection(
				request.academicYear(),
				request.className(),
				request.sectionName(),
				request.rollNumber(),
				request.effectiveFrom());
	}

	private void addDocument(Student student, StudentDocumentRequest request) {
		validateDocumentRequest(request);
		student.addDocument(
				request.documentType(),
				request.documentNumber(),
				request.fileName(),
				request.contentType(),
				request.fileSize(),
				request.storageKey(),
				request.fileUrl(),
				request.remarks());
	}

	private void ensureAdmissionNumberAvailable(String admissionNumber) {
		if (studentRepository.existsByAdmissionNumberIgnoreCaseAndDeletedFalse(admissionNumber.trim())) {
			throw new BusinessException(ErrorCode.CONFLICT, "Admission number already exists: " + admissionNumber);
		}
	}

	private void validateAdmissionRequest(StudentAdmissionRequest request) {
		long primaryContacts = request.parents().stream()
				.filter(ParentMappingRequest::primaryContact)
				.count();
		if (primaryContacts != 1) {
			throw new BusinessException(
					ErrorCode.BUSINESS_RULE_VIOLATION,
					"Exactly one primary parent or guardian contact is required.");
		}
		request.parents().forEach(parent -> validateParentRequest(parent.parent()));
		optionalDocuments(request.documents()).forEach(this::validateDocumentRequest);
	}

	private void validateParentRequest(ParentGuardianRequest request) {
		if (!StringUtils.hasText(request.email()) && !StringUtils.hasText(request.phoneNumber())) {
			throw new BusinessException(
					ErrorCode.BUSINESS_RULE_VIOLATION,
					"Parent or guardian email or phone number is required.");
		}
	}

	private void validateDocumentRequest(StudentDocumentRequest request) {
		if (!StringUtils.hasText(request.storageKey()) && !StringUtils.hasText(request.fileUrl())) {
			throw new BusinessException(
					ErrorCode.BUSINESS_RULE_VIOLATION,
					"Document storage key or file URL is required.");
		}
	}

	private void validateSearchRequest(StudentSearchRequest request) {
		if (request.admittedFrom() != null
				&& request.admittedTo() != null
				&& request.admittedFrom().isAfter(request.admittedTo())) {
			throw new BusinessException(
					ErrorCode.VALIDATION_ERROR,
					"Admission start date must be before or equal to admission end date.");
		}
	}

	private List<StudentDocumentRequest> optionalDocuments(List<StudentDocumentRequest> documents) {
		return documents == null ? List.of() : documents;
	}

	private String currentActor() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			return "system";
		}
		return authentication.getName();
	}

	private void auditStudent(UUID studentId, String action, Object oldValue, Object newValue) {
		auditLogService.record(new AuditLogEvent(
				MODULE_NAME,
				ENTITY_NAME,
				studentId == null ? null : studentId.toString(),
				action,
				oldValue,
				newValue));
	}
}
