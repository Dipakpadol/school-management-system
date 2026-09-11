package com.school.erp.modules.academic.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.audit.application.AuditLogService;
import com.school.erp.common.exception.BusinessException;
import com.school.erp.common.exception.ErrorCode;
import com.school.erp.modules.academic.api.dto.AcademicYearRequest;
import com.school.erp.modules.academic.api.dto.AssignClassTeacherRequest;
import com.school.erp.modules.academic.api.dto.AssignSubjectTeacherRequest;
import com.school.erp.modules.academic.api.dto.ClassSectionTeachersResponse;
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
import com.school.erp.modules.academic.infrastructure.DivisionSubjectRepository;
import com.school.erp.modules.students.infrastructure.StudentClassAssignmentRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AcademicHierarchyServiceTest {

	@Mock
	private AcademicYearRepository academicYearRepository;

	@Mock
	private ClassEntityRepository classEntityRepository;

	@Mock
	private SectionEntityRepository sectionEntityRepository;

	@Mock
	private TeacherRepository teacherRepository;

	@Mock
	private SubjectRepository subjectRepository;

	@Mock
	private DivisionSubjectRepository divisionSubjectRepository;

	@Mock
	private ClassTeacherMappingRepository classTeacherMappingRepository;

	@Mock
	private SubjectTeacherMappingRepository subjectTeacherMappingRepository;

	@Mock
	private StudentClassAssignmentRepository studentClassAssignmentRepository;

	@Mock
	private AuditLogService auditLogService;

	private AcademicHierarchyService service;
	private AcademicYear academicYear;
	private ClassEntity classEntity;
	private SectionEntity section;

	@BeforeEach
	void setUp() {
		service = new AcademicHierarchyService(
				academicYearRepository,
				classEntityRepository,
				sectionEntityRepository,
				teacherRepository,
				subjectRepository,
				divisionSubjectRepository,
				classTeacherMappingRepository,
				subjectTeacherMappingRepository,
				studentClassAssignmentRepository,
				new AcademicHierarchyMapper(),
				auditLogService);
		academicYear = new AcademicYear("AY-2026-27", "2026-2027", LocalDate.of(2026, 4, 1), LocalDate.of(2027, 3, 31));
		classEntity = new ClassEntity(academicYear, "CLASS-6", "Class 6", 6);
		section = new SectionEntity(classEntity, "A", "Division A", 40, 1);
		setId(academicYear);
		setId(classEntity);
		setId(section);
	}

	@Test
	void createAcademicYearCanMarkYearCurrent() {
		when(academicYearRepository.existsOverlappingActiveYear(
				LocalDate.of(2028, 4, 1),
				LocalDate.of(2029, 3, 31),
				null))
				.thenReturn(false);
		when(academicYearRepository.findByCodeIgnoreCaseAndDeletedFalse("AY-2028-29")).thenReturn(Optional.empty());
		when(academicYearRepository.findByNameIgnoreCaseAndDeletedFalse("2028-2029")).thenReturn(Optional.empty());
		when(academicYearRepository.save(any(AcademicYear.class))).thenAnswer(invocation -> {
			AcademicYear year = invocation.getArgument(0);
			setId(year);
			return year;
		});

		var response = service.createAcademicYear(new AcademicYearRequest(
				"AY-2028-29",
				"2028-2029",
				LocalDate.of(2028, 4, 1),
				LocalDate.of(2029, 3, 31),
				true,
				true,
				"Next session"));

		assertThat(response.current()).isTrue();
		assertThat(response.active()).isTrue();
		verify(academicYearRepository).clearCurrentYearExcept(null);
	}

	@Test
	void getCurrentAcademicYearFallsBackToActiveYearContainingToday() {
		when(academicYearRepository.findFirstByCurrentYearTrueAndDeletedFalseOrderByStartDateDescNameAsc())
				.thenReturn(Optional.empty());
		when(academicYearRepository
				.findFirstByActiveTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqualAndDeletedFalseOrderByStartDateDescNameAsc(
						any(LocalDate.class),
						any(LocalDate.class)))
				.thenReturn(Optional.of(academicYear));

		var response = service.getCurrentAcademicYear();

		assertThat(response.id()).isEqualTo(academicYear.getId());
		assertThat(response.name()).isEqualTo("2026-2027");
	}

	@Test
	void setCurrentAcademicYearRejectsInactiveYear() {
		academicYear.deactivate();
		when(academicYearRepository.findByIdAndDeletedFalse(academicYear.getId())).thenReturn(Optional.of(academicYear));

		assertThatThrownBy(() -> service.setCurrentAcademicYear(academicYear.getId()))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.BUSINESS_RULE_VIOLATION);
	}

	@Test
	void deleteDivisionRejectsHistoricalStudentAssignments() {
		when(sectionEntityRepository.findByIdAndDeletedFalse(section.getId())).thenReturn(Optional.of(section));
		when(classTeacherMappingRepository.findByClassEntityIdAndSectionIdAndActiveTrueAndDeletedFalse(
				classEntity.getId(),
				section.getId()))
				.thenReturn(Optional.empty());
		when(divisionSubjectRepository.findBySectionIdAndDeletedFalseOrderBySubjectNameAsc(section.getId())).thenReturn(List.of());
		when(studentClassAssignmentRepository.countBySectionId(section.getId())).thenReturn(1L);

		assertThatThrownBy(() -> service.deleteDivision(section.getId()))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.BUSINESS_RULE_VIOLATION);
	}

	@Test
	void assignClassTeacherReplacesExistingActiveMapping() {
		Teacher oldTeacher = teacher("TCH-001", "Amit");
		Teacher newTeacher = teacher("TCH-002", "Meera");
		ClassTeacherMapping existing = new ClassTeacherMapping(classEntity, section, oldTeacher, LocalDate.of(2026, 4, 1));
		setId(existing);
		ClassTeacherMapping[] activeMapping = { existing };

		when(classEntityRepository.findByIdAndDeletedFalse(classEntity.getId())).thenReturn(Optional.of(classEntity));
		when(sectionEntityRepository.findByIdAndDeletedFalse(section.getId())).thenReturn(Optional.of(section));
		when(teacherRepository.findByIdAndDeletedFalse(newTeacher.getId())).thenReturn(Optional.of(newTeacher));
		when(classTeacherMappingRepository.findByClassEntityIdAndSectionIdAndActiveTrueAndDeletedFalse(
				classEntity.getId(),
				section.getId()))
				.thenAnswer(invocation -> Optional.ofNullable(activeMapping[0]));
		when(subjectTeacherMappingRepository
				.findByClassEntityIdAndSectionIdAndActiveTrueAndDeletedFalseOrderBySubjectNameAscTeacherFirstNameAsc(
						classEntity.getId(),
						section.getId()))
				.thenReturn(List.of());
		when(classTeacherMappingRepository.save(any(ClassTeacherMapping.class))).thenAnswer(invocation -> {
			ClassTeacherMapping mapping = invocation.getArgument(0);
			setId(mapping);
			activeMapping[0] = mapping;
			return mapping;
		});

		ClassSectionTeachersResponse response = service.assignClassTeacher(
				classEntity.getId(),
				section.getId(),
				new AssignClassTeacherRequest(newTeacher.getId(), LocalDate.of(2026, 5, 1)));

		assertThat(existing.isActive()).isFalse();
		assertThat(existing.getEffectiveTo()).isEqualTo(LocalDate.of(2026, 4, 30));
		assertThat(response.classTeacher().id()).isEqualTo(newTeacher.getId());
		verify(auditLogService).record(any());
	}

	@Test
	void assignSubjectTeacherAddsTeacherWithoutReplacingOtherSubjectTeachers() {
		Teacher teacher = teacher("TCH-003", "Arjun");
		Subject subject = new Subject("MATH", "Mathematics", "Core mathematics");
		setId(subject);
		List<SubjectTeacherMapping> activeMappings = new ArrayList<>();

		when(classEntityRepository.findByIdAndDeletedFalse(classEntity.getId())).thenReturn(Optional.of(classEntity));
		when(sectionEntityRepository.findByIdAndDeletedFalse(section.getId())).thenReturn(Optional.of(section));
		when(subjectRepository.findByIdAndDeletedFalse(subject.getId())).thenReturn(Optional.of(subject));
		when(teacherRepository.findByIdAndDeletedFalse(teacher.getId())).thenReturn(Optional.of(teacher));
		when(subjectTeacherMappingRepository
				.findByClassEntityIdAndSectionIdAndSubjectIdAndTeacherIdAndActiveTrueAndDeletedFalse(
						classEntity.getId(),
						section.getId(),
						subject.getId(),
						teacher.getId()))
				.thenReturn(Optional.empty());
		when(classTeacherMappingRepository.findByClassEntityIdAndSectionIdAndActiveTrueAndDeletedFalse(
				classEntity.getId(),
				section.getId()))
				.thenReturn(Optional.empty());
		when(subjectTeacherMappingRepository
				.findByClassEntityIdAndSectionIdAndActiveTrueAndDeletedFalseOrderBySubjectNameAscTeacherFirstNameAsc(
						classEntity.getId(),
						section.getId()))
				.thenAnswer(invocation -> List.copyOf(activeMappings));
		when(subjectTeacherMappingRepository.save(any(SubjectTeacherMapping.class))).thenAnswer(invocation -> {
			SubjectTeacherMapping mapping = invocation.getArgument(0);
			setId(mapping);
			activeMappings.add(mapping);
			return mapping;
		});

		ClassSectionTeachersResponse response = service.assignSubjectTeacher(
				classEntity.getId(),
				section.getId(),
				subject.getId(),
				new AssignSubjectTeacherRequest(teacher.getId(), LocalDate.of(2026, 4, 1)));

		assertThat(response.subjectTeachers()).hasSize(1);
		assertThat(response.subjectTeachers().getFirst().teachers()).hasSize(1);
		assertThat(response.subjectTeachers().getFirst().teachers().getFirst().id()).isEqualTo(teacher.getId());
		verify(auditLogService).record(any());
	}

	private Teacher teacher(String employeeNumber, String firstName) {
		Teacher teacher = new Teacher(employeeNumber, firstName, "Teacher", firstName.toLowerCase() + "@school.test", null);
		setId(teacher);
		return teacher;
	}

	private void setId(Object entity) {
		ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
	}
}
