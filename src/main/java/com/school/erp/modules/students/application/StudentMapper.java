package com.school.erp.modules.students.application;

import java.util.Comparator;
import java.util.List;

import com.school.erp.modules.students.api.dto.ClassSectionAssignmentRequest;
import com.school.erp.modules.students.api.dto.ClassSectionAssignmentResponse;
import com.school.erp.modules.students.api.dto.ParentGuardianRequest;
import com.school.erp.modules.students.api.dto.ParentMappingResponse;
import com.school.erp.modules.students.api.dto.StudentAdmissionRequest;
import com.school.erp.modules.students.api.dto.StudentDocumentResponse;
import com.school.erp.modules.students.api.dto.StudentProfileRequest;
import com.school.erp.modules.students.api.dto.StudentResponse;
import com.school.erp.modules.students.api.dto.StudentSummaryResponse;
import com.school.erp.modules.students.domain.ParentGuardian;
import com.school.erp.modules.students.domain.Student;
import com.school.erp.modules.students.domain.StudentClassAssignment;
import com.school.erp.modules.students.domain.StudentDocument;
import com.school.erp.modules.students.domain.StudentParent;

import org.springframework.stereotype.Component;

@Component
public class StudentMapper {

	public Student toStudent(StudentAdmissionRequest request) {
		StudentProfileRequest profile = request.profile();
		Student student = new Student(
				request.admissionNumber(),
				profile.firstName(),
				profile.dateOfBirth(),
				profile.gender(),
				profile.admissionDate());
		updateStudentProfile(student, profile);
		return student;
	}

	public void updateStudentProfile(Student student, StudentProfileRequest profile) {
		student.updateProfile(
				profile.firstName(),
				profile.middleName(),
				profile.lastName(),
				profile.dateOfBirth(),
				profile.gender(),
				profile.bloodGroup(),
				profile.email(),
				profile.phoneNumber(),
				profile.admissionDate(),
				profile.previousSchool(),
				profile.addressLine1(),
				profile.addressLine2(),
				profile.city(),
				profile.state(),
				profile.postalCode(),
				profile.country());
	}

	public ParentGuardian toParentGuardian(ParentGuardianRequest request) {
		ParentGuardian parent = new ParentGuardian(
				request.firstName(),
				request.lastName(),
				request.email(),
				request.phoneNumber());
		updateParentGuardian(parent, request);
		return parent;
	}

	public void updateParentGuardian(ParentGuardian parent, ParentGuardianRequest request) {
		parent.updateProfile(
				request.firstName(),
				request.lastName(),
				request.email(),
				request.phoneNumber(),
				request.alternatePhoneNumber(),
				request.occupation(),
				request.addressLine1(),
				request.addressLine2(),
				request.city(),
				request.state(),
				request.postalCode(),
				request.country(),
				request.userAccountId());
	}

	public StudentResponse toProfileResponse(Student student) {
		List<ClassSectionAssignmentResponse> assignments = student.getClassAssignments().stream()
				.filter(assignment -> !assignment.isDeleted())
				.sorted(assignmentOrder())
				.map(this::toClassSectionAssignmentResponse)
				.toList();
		return new StudentResponse(
				student.getId(),
				student.getAdmissionNumber(),
				student.getFirstName(),
				student.getMiddleName(),
				student.getLastName(),
				student.getDisplayName(),
				student.getFullName(),
				student.getDateOfBirth(),
				student.getGender(),
				student.getBloodGroup(),
				student.getEmail(),
				student.getPhoneNumber(),
				student.getStatus(),
				student.getAdmissionDate(),
				student.getPreviousSchool(),
				student.getAddressLine1(),
				student.getAddressLine2(),
				student.getCity(),
				student.getState(),
				student.getPostalCode(),
				student.getCountry(),
				student.getPhotoStorageKey(),
				student.getPhotoUrl(),
				student.getPhotoContentType(),
				student.getPhotoFileName(),
				parentResponses(student),
				documentResponses(student),
				student.getCurrentAssignment().map(this::toClassSectionAssignmentResponse).orElse(null),
				assignments,
				student.getCreatedAt(),
				student.getUpdatedAt());
	}

	public StudentSummaryResponse toSummaryResponse(Student student) {
		StudentClassAssignment assignment = student.getCurrentAssignment().orElse(null);
		return new StudentSummaryResponse(
				student.getId(),
				student.getAdmissionNumber(),
				student.getDisplayName(),
				student.getStatus(),
				student.getAdmissionDate(),
				assignment == null || assignment.getAcademicYearEntity() == null
						? null
						: assignment.getAcademicYearEntity().getId(),
				assignment == null || assignment.getClassEntity() == null
						? null
						: assignment.getClassEntity().getId(),
				assignment == null || assignment.getSectionEntity() == null
						? null
						: assignment.getSectionEntity().getId(),
				assignment == null ? null : assignment.getClassName(),
				assignment == null ? null : assignment.getSectionName(),
				assignment == null ? null : assignment.getRollNumber());
	}

	public ParentMappingResponse toParentMappingResponse(StudentParent mapping) {
		ParentGuardian parent = mapping.getParent();
		return new ParentMappingResponse(
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
				parent.getOccupation(),
				parent.getAddressLine1(),
				parent.getAddressLine2(),
				parent.getCity(),
				parent.getState(),
				parent.getPostalCode(),
				parent.getCountry(),
				parent.getUserAccountId());
	}

	public StudentDocumentResponse toStudentDocumentResponse(StudentDocument document) {
		return new StudentDocumentResponse(
				document.getId(),
				document.getDocumentType(),
				document.getDocumentNumber(),
				document.getFileName(),
				document.getContentType(),
				document.getFileSize(),
				document.getStorageKey(),
				document.getFileUrl(),
				document.getVerificationStatus(),
				document.getRemarks(),
				document.getVerifiedAt(),
				document.getVerifiedBy(),
				document.getCreatedAt());
	}

	public ClassSectionAssignmentResponse toClassSectionAssignmentResponse(StudentClassAssignment assignment) {
		return new ClassSectionAssignmentResponse(
				assignment.getId(),
				assignment.getAcademicYearEntity() == null ? null : assignment.getAcademicYearEntity().getId(),
				assignment.getClassEntity() == null ? null : assignment.getClassEntity().getId(),
				assignment.getSectionEntity() == null ? null : assignment.getSectionEntity().getId(),
				assignment.getAcademicYear(),
				assignment.getClassName(),
				assignment.getSectionName(),
				assignment.getRollNumber(),
				assignment.getEffectiveFrom(),
				assignment.getEffectiveTo(),
				assignment.isActive(),
				assignment.getCreatedAt());
	}

	private List<ParentMappingResponse> parentResponses(Student student) {
		return student.getParents().stream()
				.filter(mapping -> !mapping.isDeleted())
				.sorted(Comparator.comparing(StudentParent::isPrimaryContact).reversed()
						.thenComparing(mapping -> mapping.getRelationType().name()))
				.map(this::toParentMappingResponse)
				.toList();
	}

	private List<StudentDocumentResponse> documentResponses(Student student) {
		return student.getDocuments().stream()
				.filter(document -> !document.isDeleted())
				.sorted(Comparator.comparing((StudentDocument document) -> document.getDocumentType().name())
						.thenComparing(StudentDocument::getFileName))
				.map(this::toStudentDocumentResponse)
				.toList();
	}

	private Comparator<StudentClassAssignment> assignmentOrder() {
		return Comparator.comparing(StudentClassAssignment::isActive).reversed()
				.thenComparing(
						StudentClassAssignment::getEffectiveFrom,
						Comparator.nullsLast(Comparator.reverseOrder()));
	}
}
