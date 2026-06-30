package com.school.erp.modules.teachers.application;

import com.school.erp.modules.academic.domain.ClassTeacherMapping;
import com.school.erp.modules.academic.domain.SubjectTeacherMapping;
import com.school.erp.modules.academic.domain.Teacher;
import com.school.erp.modules.teachers.api.dto.TeacherAcademicMappingResponse;
import com.school.erp.modules.teachers.api.dto.TeacherAssignmentResponse;
import com.school.erp.modules.teachers.api.dto.TeacherDocumentResponse;
import com.school.erp.modules.teachers.api.dto.TeacherResponse;
import com.school.erp.modules.teachers.domain.TeacherAcademicAssignment;
import com.school.erp.modules.teachers.domain.TeacherAssignmentType;
import com.school.erp.modules.teachers.domain.TeacherDocument;

import org.springframework.stereotype.Component;

@Component
public class TeacherMapper {

	public TeacherResponse toTeacherResponse(Teacher teacher, long assignedClassesCount, long assignedSubjectsCount) {
		return new TeacherResponse(
				teacher.getId(),
				teacher.getEmployeeNumber(),
				teacher.getEmployeeNumber(),
				teacher.getFirstName(),
				teacher.getMiddleName(),
				teacher.getLastName(),
				teacher.getDisplayName(),
				teacher.getGender(),
				teacher.getDateOfBirth(),
				teacher.getMobileNumber(),
				teacher.getPhoneNumber(),
				teacher.getEmail(),
				teacher.getQualification(),
				teacher.getExperienceYears(),
				teacher.getJoiningDate(),
				teacher.getStatus(),
				teacher.getUserAccountId(),
				assignedClassesCount,
				assignedSubjectsCount,
				teacher.getCreatedAt(),
				teacher.getUpdatedAt());
	}

	public TeacherAssignmentResponse toAssignmentResponse(TeacherAcademicAssignment assignment) {
		return new TeacherAssignmentResponse(
				assignment.getId(),
				assignment.getTeacher().getId(),
				assignment.getTeacher().getDisplayName(),
				assignment.getAcademicYear().getId(),
				assignment.getAcademicYear().getName(),
				assignment.getAssignmentType(),
				assignment.getClassEntity() == null ? null : assignment.getClassEntity().getId(),
				assignment.getClassEntity() == null ? null : assignment.getClassEntity().getName(),
				assignment.getSection() == null ? null : assignment.getSection().getId(),
				assignment.getSection() == null ? null : assignment.getSection().getName(),
				assignment.getSubject() == null ? null : assignment.getSubject().getId(),
				assignment.getSubject() == null ? null : assignment.getSubject().getName(),
				assignment.getStatus(),
				assignment.getCreatedAt(),
				assignment.getUpdatedAt());
	}

	public TeacherDocumentResponse toDocumentResponse(TeacherDocument document) {
		return new TeacherDocumentResponse(
				document.getId(),
				document.getTeacher().getId(),
				document.getDocumentType(),
				document.getFileName(),
				document.getFileUrl(),
				document.getFilePath(),
				document.getUploadedAt(),
				document.getStatus(),
				document.getCreatedAt(),
				document.getUpdatedAt());
	}

	public TeacherAcademicMappingResponse toClassTeacherMapping(ClassTeacherMapping mapping) {
		return new TeacherAcademicMappingResponse(
				mapping.getId(),
				"ACADEMIC_MANAGEMENT",
				TeacherAssignmentType.CLASS_TEACHER,
				mapping.getClassEntity().getAcademicYear().getId(),
				mapping.getClassEntity().getAcademicYear().getName(),
				mapping.getClassEntity().getId(),
				mapping.getClassEntity().getName(),
				mapping.getSection().getId(),
				mapping.getSection().getName(),
				null,
				null,
				mapping.getEffectiveFrom(),
				mapping.getEffectiveTo(),
				mapping.isActive());
	}

	public TeacherAcademicMappingResponse toSubjectTeacherMapping(SubjectTeacherMapping mapping) {
		return new TeacherAcademicMappingResponse(
				mapping.getId(),
				"ACADEMIC_MANAGEMENT",
				TeacherAssignmentType.SUBJECT_TEACHER,
				mapping.getClassEntity().getAcademicYear().getId(),
				mapping.getClassEntity().getAcademicYear().getName(),
				mapping.getClassEntity().getId(),
				mapping.getClassEntity().getName(),
				mapping.getSection().getId(),
				mapping.getSection().getName(),
				mapping.getSubject().getId(),
				mapping.getSubject().getName(),
				mapping.getEffectiveFrom(),
				mapping.getEffectiveTo(),
				mapping.isActive());
	}
}
