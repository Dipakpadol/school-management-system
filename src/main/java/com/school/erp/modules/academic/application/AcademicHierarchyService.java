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
import com.school.erp.modules.academic.api.dto.AcademicYearRequest;
import com.school.erp.modules.academic.api.dto.AcademicYearResponse;
import com.school.erp.modules.academic.api.dto.AssignClassTeacherRequest;
import com.school.erp.modules.academic.api.dto.AssignSubjectTeacherRequest;
import com.school.erp.modules.academic.api.dto.ClassRequest;
import com.school.erp.modules.academic.api.dto.ClassResponse;
import com.school.erp.modules.academic.api.dto.ClassSectionTeachersResponse;
import com.school.erp.modules.academic.api.dto.DivisionRequest;
import com.school.erp.modules.academic.api.dto.DivisionResponse;
import com.school.erp.modules.academic.api.dto.DivisionSubjectRequest;
import com.school.erp.modules.academic.api.dto.DivisionSubjectResponse;
import com.school.erp.modules.academic.api.dto.SectionResponse;
import com.school.erp.modules.academic.api.dto.SubjectResponse;
import com.school.erp.modules.academic.domain.AcademicYear;
import com.school.erp.modules.academic.api.dto.TeacherSummaryResponse;
import com.school.erp.modules.academic.domain.ClassEntity;
import com.school.erp.modules.academic.domain.ClassTeacherMapping;
import com.school.erp.modules.academic.domain.DivisionSubject;
import com.school.erp.modules.academic.domain.SectionEntity;
import com.school.erp.modules.academic.domain.Subject;
import com.school.erp.modules.academic.domain.SubjectTeacherMapping;
import com.school.erp.modules.academic.domain.Teacher;
import com.school.erp.modules.academic.infrastructure.AcademicYearRepository;
import com.school.erp.modules.academic.infrastructure.ClassEntityRepository;
import com.school.erp.modules.academic.infrastructure.ClassTeacherMappingRepository;
import com.school.erp.modules.academic.infrastructure.DivisionSubjectRepository;
import com.school.erp.modules.academic.infrastructure.SectionEntityRepository;
import com.school.erp.modules.academic.infrastructure.SubjectRepository;
import com.school.erp.modules.academic.infrastructure.SubjectTeacherMappingRepository;
import com.school.erp.modules.academic.infrastructure.TeacherRepository;
import com.school.erp.modules.students.infrastructure.StudentClassAssignmentRepository;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
	private final DivisionSubjectRepository divisionSubjectRepository;
	private final ClassTeacherMappingRepository classTeacherMappingRepository;
	private final SubjectTeacherMappingRepository subjectTeacherMappingRepository;
	private final StudentClassAssignmentRepository studentClassAssignmentRepository;
	private final AcademicHierarchyMapper mapper;
	private final AuditLogService auditLogService;

	@Transactional(readOnly = true)
	public List<AcademicYearResponse> getAcademicYears() {
		return academicYearRepository.findAllByDeletedFalseOrderByStartDateDescNameAsc().stream()
				.map(mapper::toAcademicYearResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public AcademicYearResponse getAcademicYear(UUID academicYearId) {
		return mapper.toAcademicYearResponse(loadAcademicYear(academicYearId));
	}

	@Transactional(readOnly = true)
	public AcademicYearResponse getCurrentAcademicYear() {
		return mapper.toAcademicYearResponse(resolveCurrentAcademicYear());
	}

	@Transactional
	public AcademicYearResponse createAcademicYear(AcademicYearRequest request) {
		validateAcademicYearRequest(request, null);
		String code = academicYearCode(request);
		if (academicYearRepository.findByCodeIgnoreCaseAndDeletedFalse(code).isPresent()) {
			throw new BusinessException(ErrorCode.CONFLICT, "Academic year code already exists: " + code);
		}
		if (academicYearRepository.findByNameIgnoreCaseAndDeletedFalse(request.name()).isPresent()) {
			throw new BusinessException(ErrorCode.CONFLICT, "Academic year name already exists: " + request.name());
		}
		AcademicYear academicYear = new AcademicYear(code, request.name(), request.startDate(), request.endDate());
		academicYear.update(code, request.name(), request.startDate(), request.endDate(), request.active(), request.description());
		applyCurrentFlag(academicYear, request.current(), null);
		AcademicYearResponse response = mapper.toAcademicYearResponse(academicYearRepository.save(academicYear));
		audit("AcademicYear", response.id(), "CREATE", null, response);
		return response;
	}

	@Transactional
	public AcademicYearResponse updateAcademicYear(UUID academicYearId, AcademicYearRequest request) {
		AcademicYear academicYear = loadAcademicYear(academicYearId);
		AcademicYearResponse oldValue = mapper.toAcademicYearResponse(academicYear);
		validateAcademicYearRequest(request, academicYearId);
		String code = academicYearCode(request);
		academicYearRepository.findByCodeIgnoreCaseAndDeletedFalse(code)
				.filter(existing -> !existing.getId().equals(academicYearId))
				.ifPresent(existing -> {
					throw new BusinessException(ErrorCode.CONFLICT, "Academic year code already exists: " + code);
				});
		academicYearRepository.findByNameIgnoreCaseAndDeletedFalse(request.name())
				.filter(existing -> !existing.getId().equals(academicYearId))
				.ifPresent(existing -> {
					throw new BusinessException(ErrorCode.CONFLICT, "Academic year name already exists: " + request.name());
				});
		academicYear.update(code, request.name(), request.startDate(), request.endDate(), request.active(), request.description());
		applyCurrentFlag(academicYear, request.current(), academicYearId);
		AcademicYearResponse response = mapper.toAcademicYearResponse(academicYear);
		audit("AcademicYear", academicYearId, "UPDATE", oldValue, response);
		return response;
	}

	@Transactional
	public AcademicYearResponse setCurrentAcademicYear(UUID academicYearId) {
		AcademicYear academicYear = loadAcademicYear(academicYearId);
		AcademicYearResponse oldValue = mapper.toAcademicYearResponse(academicYear);
		if (!academicYear.isActive()) {
			throw new BusinessException(
					ErrorCode.BUSINESS_RULE_VIOLATION,
					"Only an active academic year can be marked current.");
		}
		academicYearRepository.clearCurrentYearExcept(academicYearId);
		academicYear.markCurrent();
		AcademicYearResponse response = mapper.toAcademicYearResponse(academicYear);
		audit("AcademicYear", academicYearId, "CURRENT_CHANGED", oldValue, response);
		return response;
	}

	@Transactional
	public AcademicYearResponse deleteAcademicYear(UUID academicYearId) {
		AcademicYear academicYear = loadAcademicYear(academicYearId);
		AcademicYearResponse oldValue = mapper.toAcademicYearResponse(academicYear);
		if (classEntityRepository.countByAcademicYearIdAndDeletedFalse(academicYearId) > 0) {
			throw new BusinessException(
					ErrorCode.BUSINESS_RULE_VIOLATION,
					"Academic year has classes. Delete or move classes before deleting the academic year.");
		}
		academicYear.clearCurrent();
		academicYear.softDelete(currentActor());
		audit("AcademicYear", academicYearId, "DELETE", oldValue, Map.of("deleted", true, "academicYearId", academicYearId));
		return mapper.toAcademicYearResponse(academicYear);
	}

	@Transactional(readOnly = true)
	public List<ClassResponse> getClasses(UUID academicYearId) {
		loadAcademicYear(academicYearId);
		return classEntityRepository.findByAcademicYearIdAndDeletedFalseOrderByDisplayOrderAscNameAsc(academicYearId).stream()
				.map(mapper::toClassResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public ClassResponse getClass(UUID classId) {
		return mapper.toClassResponse(loadClass(classId));
	}

	@Transactional
	public ClassResponse createClass(UUID academicYearId, ClassRequest request) {
		AcademicYear academicYear = loadAcademicYear(academicYearId);
		validateClassRequest(request, academicYearId, null);
		String code = classCode(request);
		ClassEntity classEntity = new ClassEntity(academicYear, code, request.name(), request.displayOrder());
		classEntity.update(code, request.name(), request.displayOrder(), request.active());
		ClassResponse response = mapper.toClassResponse(classEntityRepository.save(classEntity));
		audit("Class", response.id(), "CREATE", null, response);
		return response;
	}

	@Transactional
	public ClassResponse updateClass(UUID classId, ClassRequest request) {
		ClassEntity classEntity = loadClass(classId);
		ClassResponse oldValue = mapper.toClassResponse(classEntity);
		validateClassRequest(request, classEntity.getAcademicYear().getId(), classId);
		classEntity.update(classCode(request), request.name(), request.displayOrder(), request.active());
		ClassResponse response = mapper.toClassResponse(classEntity);
		audit("Class", classId, "UPDATE", oldValue, response);
		return response;
	}

	@Transactional
	public ClassResponse deleteClass(UUID classId) {
		ClassEntity classEntity = loadClass(classId);
		ClassResponse oldValue = mapper.toClassResponse(classEntity);
		if (sectionEntityRepository.countByClassEntityIdAndDeletedFalse(classId) > 0) {
			throw new BusinessException(
					ErrorCode.BUSINESS_RULE_VIOLATION,
					"Class has divisions. Delete divisions before deleting the class.");
		}
		classEntity.softDelete(currentActor());
		audit("Class", classId, "DELETE", oldValue, Map.of("deleted", true, "classId", classId));
		return mapper.toClassResponse(classEntity);
	}

	@Transactional(readOnly = true)
	public List<SectionResponse> getSections(UUID classId) {
		loadClass(classId);
		return sectionEntityRepository.findByClassEntityIdAndDeletedFalseOrderByDisplayOrderAscNameAsc(classId).stream()
				.map(mapper::toSectionResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<DivisionResponse> getDivisions(UUID classId) {
		loadClass(classId);
		return sectionEntityRepository.findByClassEntityIdAndDeletedFalseOrderByDisplayOrderAscNameAsc(classId).stream()
				.map(this::toDivisionResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public DivisionResponse getDivision(UUID divisionId) {
		return toDivisionResponse(loadSection(divisionId));
	}

	@Transactional
	public DivisionResponse createDivision(UUID classId, DivisionRequest request) {
		ClassEntity classEntity = loadClass(classId);
		validateDivisionRequest(request, classId, null);
		String code = divisionCode(request);
		SectionEntity section = new SectionEntity(classEntity, code, request.name(), request.capacity(), request.displayOrder());
		section.update(code, request.name(), request.capacity(), request.displayOrder(), request.active());
		SectionEntity saved = sectionEntityRepository.save(section);
		if (request.classTeacherId() != null) {
			assignClassTeacherInternal(classEntity, saved, request.classTeacherId(), LocalDate.now());
		}
		DivisionResponse response = toDivisionResponse(saved);
		audit("Division", response.id(), "CREATE", null, response);
		return response;
	}

	@Transactional
	public DivisionResponse updateDivision(UUID divisionId, DivisionRequest request) {
		SectionEntity section = loadSection(divisionId);
		DivisionResponse oldValue = toDivisionResponse(section);
		validateDivisionRequest(request, section.getClassEntity().getId(), divisionId);
		section.update(divisionCode(request), request.name(), request.capacity(), request.displayOrder(), request.active());
		if (request.classTeacherId() != null) {
			assignClassTeacherInternal(section.getClassEntity(), section, request.classTeacherId(), LocalDate.now());
		}
		DivisionResponse response = toDivisionResponse(section);
		audit("Division", divisionId, "UPDATE", oldValue, response);
		return response;
	}

	@Transactional
	public DivisionResponse deleteDivision(UUID divisionId) {
		SectionEntity section = loadSection(divisionId);
		DivisionResponse oldValue = toDivisionResponse(section);
		if (studentClassAssignmentRepository.countBySectionId(divisionId) > 0) {
			throw new BusinessException(
					ErrorCode.BUSINESS_RULE_VIOLATION,
					"Division has student assignment history. Move or archive assignments before deleting the division.");
		}
		section.softDelete(currentActor());
		audit("Division", divisionId, "DELETE", oldValue, Map.of("deleted", true, "divisionId", divisionId));
		return toDivisionResponse(section);
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

		ClassTeacherMapping mapping = assignClassTeacherInternal(classEntity, section, teacher.getId(), request.effectiveFrom());

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

	@Transactional(readOnly = true)
	public List<SubjectResponse> getSubjects() {
		return subjectRepository.findAllByDeletedFalseAndActiveTrueOrderByNameAsc().stream()
				.map(mapper::toSubjectResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<TeacherSummaryResponse> getTeachers() {
		return teacherRepository.findAllByDeletedFalseAndActiveTrueOrderByFirstNameAscLastNameAsc().stream()
				.map(mapper::toTeacherSummary)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<DivisionSubjectResponse> getDivisionSubjects(UUID divisionId) {
		loadSection(divisionId);
		return divisionSubjectRepository.findBySectionIdAndDeletedFalseOrderBySubjectNameAsc(divisionId).stream()
				.map(mapper::toDivisionSubjectResponse)
				.toList();
	}

	@Transactional
	public DivisionSubjectResponse addDivisionSubject(UUID divisionId, DivisionSubjectRequest request) {
		SectionEntity section = loadSection(divisionId);
		Subject subject = resolveSubject(request);
		if (divisionSubjectRepository.existsBySectionIdAndSubjectIdAndDeletedFalse(divisionId, subject.getId())) {
			throw new BusinessException(ErrorCode.CONFLICT, "Subject is already assigned to this division.");
		}
		Teacher teacher = request.teacherId() == null ? null : loadActiveTeacher(request.teacherId());
		DivisionSubject mapping = divisionSubjectRepository.save(new DivisionSubject(section, subject, teacher));
		if (request.active() != null && !request.active()) {
			mapping.deactivate();
		}
		DivisionSubjectResponse response = mapper.toDivisionSubjectResponse(mapping);
		audit("DivisionSubject", mapping.getId(), "SUBJECT_ASSIGNED", null, response);
		return response;
	}

	@Transactional
	public DivisionSubjectResponse updateDivisionSubject(UUID divisionId, UUID divisionSubjectId, DivisionSubjectRequest request) {
		DivisionSubject mapping = loadDivisionSubjectForDivision(divisionId, divisionSubjectId);
		DivisionSubjectResponse oldValue = mapper.toDivisionSubjectResponse(mapping);
		Teacher teacher = request.teacherId() == null ? null : loadActiveTeacher(request.teacherId());
		mapping.update(teacher, request.active() == null || request.active());
		DivisionSubjectResponse response = mapper.toDivisionSubjectResponse(mapping);
		audit("DivisionSubject", divisionSubjectId, "SUBJECT_UPDATED", oldValue, response);
		return response;
	}

	@Transactional
	public DivisionSubjectResponse deleteDivisionSubject(UUID divisionId, UUID divisionSubjectId) {
		DivisionSubject mapping = loadDivisionSubjectForDivision(divisionId, divisionSubjectId);
		DivisionSubjectResponse oldValue = mapper.toDivisionSubjectResponse(mapping);
		mapping.softDelete(currentActor());
		DivisionSubjectResponse response = mapper.toDivisionSubjectResponse(mapping);
		audit("DivisionSubject", divisionSubjectId, "SUBJECT_REMOVED", oldValue, Map.of("deleted", true, "divisionSubjectId", divisionSubjectId));
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
		SectionEntity section = loadSection(sectionId);
		if (!section.getClassEntity().getId().equals(classId)) {
			throw new BusinessException(
					ErrorCode.BUSINESS_RULE_VIOLATION,
					"Section does not belong to the selected class.");
		}
		return section;
	}

	public SectionEntity loadSection(UUID sectionId) {
		return sectionEntityRepository.findByIdAndDeletedFalse(sectionId)
				.orElseThrow(() -> new ResourceNotFoundException("Division", sectionId));
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

	private DivisionResponse toDivisionResponse(SectionEntity section) {
		ClassTeacherMapping classTeacher = classTeacherMappingRepository
				.findByClassEntityIdAndSectionIdAndActiveTrueAndDeletedFalse(section.getClassEntity().getId(), section.getId())
				.orElse(null);
		return mapper.toDivisionResponse(
				section,
				studentClassAssignmentRepository.countActiveBySectionId(section.getId()),
				classTeacher,
				divisionSubjectRepository.findBySectionIdAndDeletedFalseOrderBySubjectNameAsc(section.getId()));
	}

	private ClassTeacherMapping assignClassTeacherInternal(
			ClassEntity classEntity,
			SectionEntity section,
			UUID teacherId,
			LocalDate effectiveFrom) {
		Teacher teacher = loadActiveTeacher(teacherId);
		classTeacherMappingRepository.findByClassEntityIdAndSectionIdAndActiveTrueAndDeletedFalse(
				classEntity.getId(),
				section.getId())
				.ifPresent(existing -> existing.deactivate(effectiveFrom.minusDays(1)));
		return classTeacherMappingRepository.save(new ClassTeacherMapping(
				classEntity,
				section,
				teacher,
				effectiveFrom));
	}

	private DivisionSubject loadDivisionSubjectForDivision(UUID divisionId, UUID divisionSubjectId) {
		DivisionSubject mapping = divisionSubjectRepository.findDetailedByIdAndDeletedFalse(divisionSubjectId)
				.orElseThrow(() -> new ResourceNotFoundException("Division subject", divisionSubjectId));
		if (!mapping.getSection().getId().equals(divisionId)) {
			throw new BusinessException(
					ErrorCode.BUSINESS_RULE_VIOLATION,
					"Subject mapping does not belong to the selected division.");
		}
		return mapping;
	}

	private Subject resolveSubject(DivisionSubjectRequest request) {
		if (request.subjectId() != null) {
			return loadSubject(request.subjectId());
		}
		if (!StringUtils.hasText(request.subjectCode()) || !StringUtils.hasText(request.subjectName())) {
			throw new BusinessException(
					ErrorCode.VALIDATION_ERROR,
					"Subject id or subject code and name are required.");
		}
		return subjectRepository.findByCodeIgnoreCaseAndDeletedFalse(request.subjectCode())
				.or(() -> subjectRepository.findByNameIgnoreCaseAndDeletedFalse(request.subjectName()))
				.orElseGet(() -> subjectRepository.save(new Subject(
						request.subjectCode(),
						request.subjectName(),
						request.description())));
	}

	private void validateAcademicYearRequest(AcademicYearRequest request, UUID excludedId) {
		if (request.startDate().isAfter(request.endDate())) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Academic year start date must be before end date.");
		}
		if (Boolean.TRUE.equals(request.current()) && !request.active()) {
			throw new BusinessException(
					ErrorCode.BUSINESS_RULE_VIOLATION,
					"Only an active academic year can be marked current.");
		}
		if (request.active() && academicYearRepository.existsOverlappingActiveYear(request.startDate(), request.endDate(), excludedId)) {
			throw new BusinessException(
					ErrorCode.CONFLICT,
					"Academic year overlaps an existing active academic year.");
		}
	}

	private void validateClassRequest(ClassRequest request, UUID academicYearId, UUID excludedClassId) {
		String code = classCode(request);
		classEntityRepository.findByAcademicYearIdAndCodeIgnoreCaseAndDeletedFalse(academicYearId, code)
				.filter(existing -> !existing.getId().equals(excludedClassId))
				.ifPresent(existing -> {
					throw new BusinessException(ErrorCode.CONFLICT, "Class code already exists in this academic year: " + code);
				});
		classEntityRepository.findByAcademicYearIdAndNameIgnoreCaseAndDeletedFalse(academicYearId, request.name())
				.filter(existing -> !existing.getId().equals(excludedClassId))
				.ifPresent(existing -> {
					throw new BusinessException(ErrorCode.CONFLICT, "Class name already exists in this academic year: " + request.name());
				});
	}

	private void validateDivisionRequest(DivisionRequest request, UUID classId, UUID excludedDivisionId) {
		String code = divisionCode(request);
		sectionEntityRepository.findByClassEntityIdAndCodeIgnoreCaseAndDeletedFalse(classId, code)
				.filter(existing -> !existing.getId().equals(excludedDivisionId))
				.ifPresent(existing -> {
					throw new BusinessException(ErrorCode.CONFLICT, "Division code already exists in this class: " + code);
				});
		sectionEntityRepository.findByClassEntityIdAndNameIgnoreCaseAndDeletedFalse(classId, request.name())
				.filter(existing -> !existing.getId().equals(excludedDivisionId))
				.ifPresent(existing -> {
					throw new BusinessException(ErrorCode.CONFLICT, "Division name already exists in this class: " + request.name());
				});
	}

	private String academicYearCode(AcademicYearRequest request) {
		return StringUtils.hasText(request.code()) ? request.code().trim() : request.name().trim();
	}

	private AcademicYear resolveCurrentAcademicYear() {
		LocalDate today = LocalDate.now();
		return academicYearRepository.findFirstByCurrentYearTrueAndDeletedFalseOrderByStartDateDescNameAsc()
				.or(() -> academicYearRepository
						.findFirstByActiveTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqualAndDeletedFalseOrderByStartDateDescNameAsc(
								today,
								today))
				.or(() -> academicYearRepository.findFirstByActiveTrueAndDeletedFalseOrderByStartDateDescNameAsc())
				.orElseThrow(() -> new ResourceNotFoundException("Current academic year", today));
	}

	private void applyCurrentFlag(AcademicYear academicYear, Boolean current, UUID excludedId) {
		if (current == null) {
			return;
		}
		if (current) {
			if (!academicYear.isActive()) {
				throw new BusinessException(
						ErrorCode.BUSINESS_RULE_VIOLATION,
						"Only an active academic year can be marked current.");
			}
			academicYearRepository.clearCurrentYearExcept(excludedId);
			academicYear.markCurrent();
			return;
		}
		academicYear.clearCurrent();
	}

	private String classCode(ClassRequest request) {
		return StringUtils.hasText(request.code()) ? request.code().trim() : request.name().trim();
	}

	private String divisionCode(DivisionRequest request) {
		return StringUtils.hasText(request.code()) ? request.code().trim() : request.name().trim();
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
}
