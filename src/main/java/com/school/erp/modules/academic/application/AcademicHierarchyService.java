package com.school.erp.modules.academic.application;

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
import com.school.erp.modules.academic.api.dto.ClassResponse;
import com.school.erp.modules.academic.api.dto.ClassSectionTeachersResponse;
import com.school.erp.modules.academic.api.dto.SectionResponse;
import com.school.erp.modules.academic.domain.AcademicYear;
import com.school.erp.modules.academic.domain.ClassEntity;
import com.school.erp.modules.academic.domain.ClassTeacherMapping;
import com.school.erp.modules.academic.domain.SectionEntity;
import com.school.erp.modules.academic.domain.Subject;
import com.school.erp.modules.academic.domain.SubjectTeacherMapping;
import com.school.erp.modules.academic.domain.Teacher;
import com.school.erp.modules.academic.infrastructure.AcademicYearRepository;
import com.school.erp.modules.academic.infrastructure.ClassEntityRepository;
import com.school.erp.modules.academic.infrastructure.ClassTeacherMappingRepository;
import com.school.erp.modules.academic.infrastructure.SectionEntityRepository;
import com.school.erp.modules.academic.infrastructure.SubjectRepository;
import com.school.erp.modules.academic.infrastructure.SubjectTeacherMappingRepository;
import com.school.erp.modules.academic.infrastructure.TeacherRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AcademicHierarchyService {

	private static final String MODULE_NAME = "ACADEMIC";

	private final AcademicYearRepository academicYearRepository;
	private final ClassEntityRepository classEntityRepository;
	private final SectionEntityRepository sectionEntityRepository;
	private final TeacherRepository teacherRepository;
	private final SubjectRepository subjectRepository;
	private final ClassTeacherMappingRepository classTeacherMappingRepository;
	private final SubjectTeacherMappingRepository subjectTeacherMappingRepository;
	private final AcademicHierarchyMapper mapper;
	private final AuditLogService auditLogService;

	@Transactional(readOnly = true)
	public List<AcademicYearResponse> getAcademicYears() {
		return academicYearRepository.findAllByDeletedFalseOrderByStartDateDescNameAsc().stream()
				.map(mapper::toAcademicYearResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<ClassResponse> getClasses(UUID academicYearId) {
		loadAcademicYear(academicYearId);
		return classEntityRepository.findByAcademicYearIdAndDeletedFalseOrderByDisplayOrderAscNameAsc(academicYearId).stream()
				.map(mapper::toClassResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<SectionResponse> getSections(UUID classId) {
		loadClass(classId);
		return sectionEntityRepository.findByClassEntityIdAndDeletedFalseOrderByDisplayOrderAscNameAsc(classId).stream()
				.map(mapper::toSectionResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public ClassSectionTeachersResponse getTeacherDetails(UUID classId, UUID sectionId) {
		ClassEntity classEntity = loadClass(classId);
		SectionEntity section = loadSectionForClass(classId, sectionId);
		ClassTeacherMapping classTeacher = classTeacherMappingRepository
				.findByClassEntityIdAndSectionIdAndActiveTrueAndDeletedFalse(classId, sectionId)
				.orElse(null);
		List<SubjectTeacherMapping> subjectTeachers = subjectTeacherMappingRepository
				.findByClassEntityIdAndSectionIdAndActiveTrueAndDeletedFalseOrderBySubjectNameAscTeacherFirstNameAsc(classId, sectionId);
		return mapper.toTeacherDetails(classEntity, section, classTeacher, subjectTeachers);
	}

	@Transactional
	public ClassSectionTeachersResponse assignClassTeacher(
			UUID classId,
			UUID sectionId,
			AssignClassTeacherRequest request) {
		ClassEntity classEntity = loadClass(classId);
		SectionEntity section = loadSectionForClass(classId, sectionId);
		Teacher teacher = loadActiveTeacher(request.teacherId());
		ClassSectionTeachersResponse oldValue = getTeacherDetails(classId, sectionId);

		classTeacherMappingRepository.findByClassEntityIdAndSectionIdAndActiveTrueAndDeletedFalse(classId, sectionId)
				.ifPresent(existing -> existing.deactivate(request.effectiveFrom().minusDays(1)));
		ClassTeacherMapping mapping = classTeacherMappingRepository.save(new ClassTeacherMapping(
				classEntity,
				section,
				teacher,
				request.effectiveFrom()));

		ClassSectionTeachersResponse response = getTeacherDetails(classId, sectionId);
		audit("ClassTeacherMapping", mapping.getId(), "CLASS_TEACHER_ASSIGNED", oldValue, response);
		return response;
	}

	@Transactional
	public ClassSectionTeachersResponse assignSubjectTeacher(
			UUID classId,
			UUID sectionId,
			UUID subjectId,
			AssignSubjectTeacherRequest request) {
		ClassEntity classEntity = loadClass(classId);
		SectionEntity section = loadSectionForClass(classId, sectionId);
		Subject subject = loadSubject(subjectId);
		Teacher teacher = loadActiveTeacher(request.teacherId());
		if (subjectTeacherMappingRepository
				.findByClassEntityIdAndSectionIdAndSubjectIdAndTeacherIdAndActiveTrueAndDeletedFalse(
						classId,
						sectionId,
						subjectId,
						request.teacherId())
				.isPresent()) {
			throw new BusinessException(
					ErrorCode.CONFLICT,
					"Teacher is already assigned to this subject for the selected class and section.");
		}

		ClassSectionTeachersResponse oldValue = getTeacherDetails(classId, sectionId);
		SubjectTeacherMapping mapping = subjectTeacherMappingRepository.save(new SubjectTeacherMapping(
				classEntity,
				section,
				subject,
				teacher,
				request.effectiveFrom()));
		ClassSectionTeachersResponse response = getTeacherDetails(classId, sectionId);
		audit("SubjectTeacherMapping", mapping.getId(), "SUBJECT_TEACHER_ASSIGNED", oldValue, response);
		return response;
	}

	public AcademicYear loadAcademicYear(UUID academicYearId) {
		return academicYearRepository.findByIdAndDeletedFalse(academicYearId)
				.orElseThrow(() -> new ResourceNotFoundException("Academic year", academicYearId));
	}

	public ClassEntity loadClass(UUID classId) {
		return classEntityRepository.findByIdAndDeletedFalse(classId)
				.orElseThrow(() -> new ResourceNotFoundException("Class", classId));
	}

	public SectionEntity loadSectionForClass(UUID classId, UUID sectionId) {
		SectionEntity section = sectionEntityRepository.findByIdAndDeletedFalse(sectionId)
				.orElseThrow(() -> new ResourceNotFoundException("Section", sectionId));
		if (!section.getClassEntity().getId().equals(classId)) {
			throw new BusinessException(
					ErrorCode.BUSINESS_RULE_VIOLATION,
					"Section does not belong to the selected class.");
		}
		return section;
	}

	public Subject loadSubject(UUID subjectId) {
		Subject subject = subjectRepository.findByIdAndDeletedFalse(subjectId)
				.orElseThrow(() -> new ResourceNotFoundException("Subject", subjectId));
		if (!subject.isActive()) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Subject is inactive.");
		}
		return subject;
	}

	public Teacher loadActiveTeacher(UUID teacherId) {
		Teacher teacher = teacherRepository.findByIdAndDeletedFalse(teacherId)
				.orElseThrow(() -> new ResourceNotFoundException("Teacher", teacherId));
		if (!teacher.isActive()) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Teacher is inactive.");
		}
		return teacher;
	}

	public ClassEntity resolveClass(AcademicYear academicYear, String classNameOrCode) {
		return classEntityRepository.findByAcademicYearIdAndNameIgnoreCaseAndDeletedFalse(academicYear.getId(), classNameOrCode)
				.or(() -> classEntityRepository.findByAcademicYearIdAndCodeIgnoreCaseAndDeletedFalse(academicYear.getId(), classNameOrCode))
				.orElseThrow(() -> new ResourceNotFoundException("Class", classNameOrCode));
	}

	public SectionEntity resolveSection(ClassEntity classEntity, String sectionNameOrCode) {
		return sectionEntityRepository.findByClassEntityIdAndCodeIgnoreCaseAndDeletedFalse(classEntity.getId(), sectionNameOrCode)
				.or(() -> sectionEntityRepository.findByClassEntityIdAndNameIgnoreCaseAndDeletedFalse(classEntity.getId(), sectionNameOrCode))
				.or(() -> sectionEntityRepository.findByClassEntityIdAndNameIgnoreCaseAndDeletedFalse(
						classEntity.getId(),
						"Division " + sectionNameOrCode))
				.orElseThrow(() -> new ResourceNotFoundException("Section", sectionNameOrCode));
	}

	public AcademicYear resolveAcademicYear(String academicYearNameOrCode) {
		return academicYearRepository.findByNameIgnoreCaseAndDeletedFalse(academicYearNameOrCode)
				.or(() -> academicYearRepository.findByCodeIgnoreCaseAndDeletedFalse(academicYearNameOrCode))
				.orElseThrow(() -> new ResourceNotFoundException("Academic year", academicYearNameOrCode));
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
}
