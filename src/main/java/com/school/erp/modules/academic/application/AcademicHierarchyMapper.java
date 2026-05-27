package com.school.erp.modules.academic.application;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.school.erp.modules.academic.api.dto.AcademicYearResponse;
import com.school.erp.modules.academic.api.dto.ClassResponse;
import com.school.erp.modules.academic.api.dto.ClassSectionTeachersResponse;
import com.school.erp.modules.academic.api.dto.SectionResponse;
import com.school.erp.modules.academic.api.dto.SubjectTeacherResponse;
import com.school.erp.modules.academic.api.dto.TeacherSummaryResponse;
import com.school.erp.modules.academic.domain.AcademicYear;
import com.school.erp.modules.academic.domain.ClassEntity;
import com.school.erp.modules.academic.domain.ClassTeacherMapping;
import com.school.erp.modules.academic.domain.SectionEntity;
import com.school.erp.modules.academic.domain.Subject;
import com.school.erp.modules.academic.domain.SubjectTeacherMapping;
import com.school.erp.modules.academic.domain.Teacher;

import org.springframework.stereotype.Component;

@Component
public class AcademicHierarchyMapper {

	public AcademicYearResponse toAcademicYearResponse(AcademicYear academicYear) {
		return new AcademicYearResponse(
				academicYear.getId(),
				academicYear.getCode(),
				academicYear.getName(),
				academicYear.getStartDate(),
				academicYear.getEndDate(),
				academicYear.isActive());
	}

	public ClassResponse toClassResponse(ClassEntity classEntity) {
		return new ClassResponse(
				classEntity.getId(),
				classEntity.getAcademicYear().getId(),
				classEntity.getCode(),
				classEntity.getName(),
				classEntity.getDisplayOrder(),
				classEntity.isActive());
	}

	public SectionResponse toSectionResponse(SectionEntity section) {
		return new SectionResponse(
				section.getId(),
				section.getClassEntity().getId(),
				section.getCode(),
				section.getName(),
				section.getCapacity(),
				section.getDisplayOrder(),
				section.isActive());
	}

	public TeacherSummaryResponse toTeacherSummary(Teacher teacher) {
		return new TeacherSummaryResponse(
				teacher.getId(),
				teacher.getEmployeeNumber(),
				teacher.getDisplayName(),
				teacher.getEmail(),
				teacher.getPhoneNumber());
	}

	public ClassSectionTeachersResponse toTeacherDetails(
			ClassEntity classEntity,
			SectionEntity section,
			ClassTeacherMapping classTeacherMapping,
			List<SubjectTeacherMapping> subjectTeacherMappings) {
		return new ClassSectionTeachersResponse(
				classEntity.getId(),
				classEntity.getName(),
				section.getId(),
				section.getName(),
				classTeacherMapping == null ? null : toTeacherSummary(classTeacherMapping.getTeacher()),
				toSubjectTeacherResponses(subjectTeacherMappings));
	}

	private List<SubjectTeacherResponse> toSubjectTeacherResponses(List<SubjectTeacherMapping> mappings) {
		Map<UUID, SubjectTeacherBucket> buckets = new LinkedHashMap<>();
		for (SubjectTeacherMapping mapping : mappings) {
			Subject subject = mapping.getSubject();
			buckets.computeIfAbsent(
					subject.getId(),
					id -> new SubjectTeacherBucket(subject.getId(), subject.getCode(), subject.getName()))
					.teachers()
					.add(toTeacherSummary(mapping.getTeacher()));
		}
		return buckets.values().stream()
				.map(bucket -> new SubjectTeacherResponse(
						bucket.subjectId(),
						bucket.subjectCode(),
						bucket.subjectName(),
						List.copyOf(bucket.teachers())))
				.toList();
	}

	private record SubjectTeacherBucket(
			UUID subjectId,
			String subjectCode,
			String subjectName,
			List<TeacherSummaryResponse> teachers) {

		private SubjectTeacherBucket(UUID subjectId, String subjectCode, String subjectName) {
			this(subjectId, subjectCode, subjectName, new ArrayList<>());
		}
	}
}
