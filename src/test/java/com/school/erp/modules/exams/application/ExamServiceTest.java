package com.school.erp.modules.exams.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.audit.application.AuditLogService;
import com.school.erp.common.exception.BusinessException;
import com.school.erp.common.exception.ErrorCode;
import com.school.erp.modules.academic.application.AcademicHierarchyService;
import com.school.erp.modules.academic.domain.AcademicYear;
import com.school.erp.modules.academic.domain.ClassEntity;
import com.school.erp.modules.academic.domain.SectionEntity;
import com.school.erp.modules.academic.domain.Subject;
import com.school.erp.modules.academic.infrastructure.DivisionSubjectRepository;
import com.school.erp.modules.exams.api.dto.ExamScheduleRequest;
import com.school.erp.modules.exams.api.dto.ExamScheduleSubjectRequest;
import com.school.erp.modules.exams.api.dto.GenerateResultRequest;
import com.school.erp.modules.exams.api.dto.MarksEntryRecordRequest;
import com.school.erp.modules.exams.api.dto.MarksEntryRequest;
import com.school.erp.modules.exams.domain.ExamMark;
import com.school.erp.modules.exams.domain.ExamSchedule;
import com.school.erp.modules.exams.domain.ExamScheduleStatus;
import com.school.erp.modules.exams.domain.ExamScheduleSubject;
import com.school.erp.modules.exams.domain.ExamType;
import com.school.erp.modules.exams.infrastructure.ExamMarkRepository;
import com.school.erp.modules.exams.infrastructure.ExamScheduleRepository;
import com.school.erp.modules.exams.infrastructure.ExamScheduleSubjectRepository;
import com.school.erp.modules.exams.infrastructure.ExamTypeRepository;
import com.school.erp.modules.students.domain.Gender;
import com.school.erp.modules.students.domain.Student;
import com.school.erp.modules.students.domain.StudentClassAssignment;
import com.school.erp.modules.students.infrastructure.StudentClassAssignmentRepository;
import com.school.erp.modules.students.infrastructure.StudentRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ExamServiceTest {

	@Mock
	private AcademicHierarchyService academicHierarchyService;

	@Mock
	private StudentClassAssignmentRepository studentClassAssignmentRepository;

	@Mock
	private StudentRepository studentRepository;

	@Mock
	private DivisionSubjectRepository divisionSubjectRepository;

	@Mock
	private ExamTypeRepository examTypeRepository;

	@Mock
	private ExamScheduleRepository examScheduleRepository;

	@Mock
	private ExamScheduleSubjectRepository examScheduleSubjectRepository;

	@Mock
	private ExamMarkRepository examMarkRepository;

	@Mock
	private AuditLogService auditLogService;

	private ExamService examService;
	private AcademicYear academicYear;
	private ClassEntity classEntity;
	private SectionEntity section;
	private ExamType examType;
	private Subject math;
	private Subject english;
	private Subject science;
	private Student student;
	private StudentClassAssignment assignment;
	private ExamSchedule schedule;

	@BeforeEach
	void setUp() {
		examService = new ExamService(
				academicHierarchyService,
				studentClassAssignmentRepository,
				studentRepository,
				divisionSubjectRepository,
				examTypeRepository,
				examScheduleRepository,
				examScheduleSubjectRepository,
				examMarkRepository,
				auditLogService);
		academicYear = new AcademicYear("AY-2026-27", "2026-2027", LocalDate.of(2026, 4, 1), LocalDate.of(2027, 3, 31));
		classEntity = new ClassEntity(academicYear, "CLASS-6", "Class 6", 6);
		section = new SectionEntity(classEntity, "A", "Division A", 40, 1);
		examType = new ExamType("MID", "Mid Term", null, 1, true);
		math = new Subject("MATH", "Mathematics", null);
		english = new Subject("ENG", "English", null);
		science = new Subject("SCI", "Science", null);
		student = new Student("ADM-2026-0001", "Aarav", LocalDate.of(2014, 8, 17), Gender.MALE, LocalDate.of(2026, 4, 1));
		setId(academicYear);
		setId(classEntity);
		setId(section);
		setId(examType);
		setId(math);
		setId(english);
		setId(science);
		setId(student);
		schedule = scheduleWithSubjects(
				subject(math, LocalDate.of(2026, 8, 10), "100.00", "35.00"));
		setId(schedule);
		schedule.activeSubjects().forEach(this::setId);
		assignment = student.assignClassSection(academicYear, classEntity, section, "23", LocalDate.of(2026, 4, 1));
	}

	@Test
	void createScheduleSupportsMultipleSubjects() {
		stubHierarchy();
		when(examTypeRepository.findByIdAndDeletedFalse(examType.getId())).thenReturn(Optional.of(examType));
		when(divisionSubjectRepository.existsBySectionIdAndSubjectIdAndDeletedFalse(section.getId(), math.getId())).thenReturn(true);
		when(divisionSubjectRepository.existsBySectionIdAndSubjectIdAndDeletedFalse(section.getId(), english.getId())).thenReturn(true);
		when(academicHierarchyService.loadSubject(math.getId())).thenReturn(math);
		when(academicHierarchyService.loadSubject(english.getId())).thenReturn(english);
		when(examScheduleRepository.save(any(ExamSchedule.class))).thenAnswer(invocation -> {
			ExamSchedule saved = invocation.getArgument(0);
			setId(saved);
			saved.activeSubjects().forEach(this::setId);
			return saved;
		});

		var response = examService.createSchedule(scheduleRequest(List.of(
				subject(math, LocalDate.of(2026, 8, 10), "50.00", "18.00"),
				subject(english, LocalDate.of(2026, 8, 11), "50.00", "18.00"))));

		assertThat(response.examName()).isEqualTo("Unit Test 1");
		assertThat(response.subjects()).hasSize(2);
		assertThat(response.subjects()).extracting("subjectName").containsExactly("Mathematics", "English");
		assertThat(response.subjects().getFirst().maxMarks()).isEqualByComparingTo("50.00");
		assertThat(response.subjects().getFirst().passingMarks()).isEqualByComparingTo("18.00");
	}

	@Test
	void createScheduleIncludesSubjectTimeAndRoom() {
		stubHierarchy();
		when(examTypeRepository.findByIdAndDeletedFalse(examType.getId())).thenReturn(Optional.of(examType));
		when(divisionSubjectRepository.existsBySectionIdAndSubjectIdAndDeletedFalse(section.getId(), math.getId())).thenReturn(true);
		when(academicHierarchyService.loadSubject(math.getId())).thenReturn(math);
		when(examScheduleRepository.save(any(ExamSchedule.class))).thenAnswer(invocation -> {
			ExamSchedule saved = invocation.getArgument(0);
			setId(saved);
			saved.activeSubjects().forEach(this::setId);
			return saved;
		});

		var response = examService.createSchedule(scheduleRequest(List.of(
				subject(
						math,
						LocalDate.of(2026, 8, 10),
						LocalTime.of(9, 0),
						LocalTime.of(11, 0),
						"Room 12",
						"50.00",
						"18.00"))));

		assertThat(response.subjects().getFirst().startTime()).isEqualTo(LocalTime.of(9, 0));
		assertThat(response.subjects().getFirst().endTime()).isEqualTo(LocalTime.of(11, 0));
		assertThat(response.subjects().getFirst().room()).isEqualTo("Room 12");
		assertThat(response.startTime()).isEqualTo(LocalTime.of(9, 0));
		assertThat(response.room()).isEqualTo("Room 12");
	}

	@Test
	void createScheduleRejectsEmptySubjectsList() {
		stubHierarchy();
		when(examTypeRepository.findByIdAndDeletedFalse(examType.getId())).thenReturn(Optional.of(examType));

		assertThatThrownBy(() -> examService.createSchedule(scheduleRequest(List.of())))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.VALIDATION_ERROR);
	}

	@Test
	void createScheduleRejectsDuplicateSubject() {
		stubHierarchy();
		when(examTypeRepository.findByIdAndDeletedFalse(examType.getId())).thenReturn(Optional.of(examType));
		when(divisionSubjectRepository.existsBySectionIdAndSubjectIdAndDeletedFalse(section.getId(), math.getId())).thenReturn(true);
		when(academicHierarchyService.loadSubject(math.getId())).thenReturn(math);

		assertThatThrownBy(() -> examService.createSchedule(scheduleRequest(List.of(
				subject(math, LocalDate.of(2026, 8, 10), "50.00", "18.00"),
				subject(math, LocalDate.of(2026, 8, 11), "50.00", "18.00")))))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.VALIDATION_ERROR);
	}

	@Test
	void createScheduleRejectsPassingMarksGreaterThanMaxMarks() {
		stubHierarchy();
		when(examTypeRepository.findByIdAndDeletedFalse(examType.getId())).thenReturn(Optional.of(examType));

		assertThatThrownBy(() -> examService.createSchedule(scheduleRequest(List.of(
				subject(math, LocalDate.of(2026, 8, 10), "50.00", "51.00")))))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.VALIDATION_ERROR);
	}

	@Test
	void createScheduleRejectsInvalidSubjectTimeRange() {
		stubHierarchy();
		when(examTypeRepository.findByIdAndDeletedFalse(examType.getId())).thenReturn(Optional.of(examType));

		assertThatThrownBy(() -> examService.createSchedule(scheduleRequest(List.of(
				subject(
						math,
						LocalDate.of(2026, 8, 10),
						LocalTime.of(11, 0),
						LocalTime.of(10, 0),
						"Room 12",
						"50.00",
						"18.00")))))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.VALIDATION_ERROR);
	}

	@Test
	void createScheduleRejectsExistingSlotConflict() {
		stubHierarchy();
		when(examTypeRepository.findByIdAndDeletedFalse(examType.getId())).thenReturn(Optional.of(examType));
		when(divisionSubjectRepository.existsBySectionIdAndSubjectIdAndDeletedFalse(section.getId(), math.getId())).thenReturn(true);
		when(academicHierarchyService.loadSubject(math.getId())).thenReturn(math);
		when(examScheduleRepository.existsSubjectSlotConflict(
				academicYear.getId(),
				classEntity.getId(),
				section.getId(),
				LocalDate.of(2026, 8, 10),
				LocalTime.of(9, 0),
				LocalTime.of(11, 0),
				null))
				.thenReturn(true);

		assertThatThrownBy(() -> examService.createSchedule(scheduleRequest(List.of(
				subject(
						math,
						LocalDate.of(2026, 8, 10),
						LocalTime.of(9, 0),
						LocalTime.of(11, 0),
						"Room 12",
						"50.00",
						"18.00")))))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.CONFLICT);
	}

	@Test
	void createScheduleRejectsSubjectNotMappedToSelectedSection() {
		stubHierarchy();
		when(examTypeRepository.findByIdAndDeletedFalse(examType.getId())).thenReturn(Optional.of(examType));
		when(divisionSubjectRepository.existsBySectionIdAndSubjectIdAndDeletedFalse(section.getId(), math.getId())).thenReturn(false);

		assertThatThrownBy(() -> examService.createSchedule(scheduleRequest(List.of(
				subject(math, LocalDate.of(2026, 8, 10), "50.00", "18.00")))))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.BUSINESS_RULE_VIOLATION);
	}

	@Test
	void updateScheduleAddsSubjectAndSoftDeletesRemovedSubject() {
		schedule = scheduleWithSubjects(
				subject(math, LocalDate.of(2026, 8, 10), "50.00", "18.00"),
				subject(english, LocalDate.of(2026, 8, 11), "50.00", "18.00"));
		setId(schedule);
		schedule.activeSubjects().forEach(this::setId);
		ExamScheduleSubject englishRow = schedule.findSubject(english.getId()).orElseThrow();
		when(examScheduleRepository.findDetailedByIdAndDeletedFalse(schedule.getId())).thenReturn(Optional.of(schedule));
		when(examTypeRepository.findByIdAndDeletedFalse(examType.getId())).thenReturn(Optional.of(examType));
		when(divisionSubjectRepository.existsBySectionIdAndSubjectIdAndDeletedFalse(section.getId(), math.getId())).thenReturn(true);
		when(divisionSubjectRepository.existsBySectionIdAndSubjectIdAndDeletedFalse(section.getId(), science.getId())).thenReturn(true);
		when(academicHierarchyService.loadSubject(math.getId())).thenReturn(math);
		when(academicHierarchyService.loadSubject(science.getId())).thenReturn(science);

		var response = examService.updateSchedule(schedule.getId(), scheduleRequest(List.of(
				subject(math, LocalDate.of(2026, 8, 10), "60.00", "21.00"),
				subject(science, LocalDate.of(2026, 8, 12), "40.00", "14.00"))));

		assertThat(englishRow.isDeleted()).isTrue();
		assertThat(schedule.findSubject(science.getId())).isPresent();
		assertThat(schedule.findSubject(math.getId()).orElseThrow().getMaxMarks()).isEqualByComparingTo("60.00");
		assertThat(response.subjects()).extracting("subjectName").containsExactly("Mathematics", "Science");
	}

	@Test
	void saveMarksUsesScheduledMaxMarksAndIgnoresRequestMaxMarks() {
		stubHierarchy();
		when(examScheduleRepository.findDetailedByIdAndDeletedFalse(schedule.getId())).thenReturn(Optional.of(schedule));
		ExamScheduleSubject scheduleSubject = schedule.findSubject(math.getId()).orElseThrow();
		when(examScheduleSubjectRepository.findByExamScheduleIdAndSubjectIdAndDeletedFalse(schedule.getId(), math.getId()))
				.thenReturn(Optional.of(scheduleSubject));
		when(studentClassAssignmentRepository.findActiveByHierarchy(academicYear.getId(), classEntity.getId(), section.getId()))
				.thenReturn(List.of(assignment));
		when(examMarkRepository.findMarks(academicYear.getId(), classEntity.getId(), section.getId(), schedule.getId(), math.getId()))
				.thenReturn(List.of());

		examService.saveMarks(new MarksEntryRequest(
				academicYear.getId(),
				classEntity.getId(),
				section.getId(),
				schedule.getId(),
				math.getId(),
				List.of(new MarksEntryRecordRequest(student.getId(), new BigDecimal("45.00"), new BigDecimal("999.00"), "Good"))));

		ArgumentCaptor<ExamMark> markCaptor = ArgumentCaptor.forClass(ExamMark.class);
		verify(examMarkRepository).save(markCaptor.capture());
		assertThat(markCaptor.getValue().getMaxMarks()).isEqualByComparingTo("100.00");
	}

	@Test
	void saveMarksRejectsMarksGreaterThanScheduledMaxMarks() {
		stubHierarchy();
		when(examScheduleRepository.findDetailedByIdAndDeletedFalse(schedule.getId())).thenReturn(Optional.of(schedule));
		ExamScheduleSubject scheduleSubject = schedule.findSubject(math.getId()).orElseThrow();
		when(examScheduleSubjectRepository.findByExamScheduleIdAndSubjectIdAndDeletedFalse(schedule.getId(), math.getId()))
				.thenReturn(Optional.of(scheduleSubject));
		when(studentClassAssignmentRepository.findActiveByHierarchy(academicYear.getId(), classEntity.getId(), section.getId()))
				.thenReturn(List.of(assignment));

		assertThatThrownBy(() -> examService.saveMarks(new MarksEntryRequest(
				academicYear.getId(),
				classEntity.getId(),
				section.getId(),
				schedule.getId(),
				math.getId(),
				List.of(new MarksEntryRecordRequest(student.getId(), new BigDecimal("101.00"), null, null)))))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.VALIDATION_ERROR);
	}

	@Test
	void generateResultsUsesScheduledPassingMarks() {
		schedule = scheduleWithSubjects(
				subject(math, LocalDate.of(2026, 8, 10), "50.00", "18.00"),
				subject(science, LocalDate.of(2026, 8, 12), "50.00", "18.00"));
		setId(schedule);
		schedule.activeSubjects().forEach(this::setId);
		ExamMark mathMark = mark(math, "40.00", "50.00");
		ExamMark scienceMark = mark(science, "17.00", "50.00");
		stubHierarchy();
		when(examMarkRepository.findResultMarks(academicYear.getId(), classEntity.getId(), section.getId(), examType.getId()))
				.thenReturn(List.of(mathMark, scienceMark));

		var results = examService.generateResults(new GenerateResultRequest(
				academicYear.getId(),
				classEntity.getId(),
				section.getId(),
				examType.getId()));

		assertThat(results).hasSize(1);
		var result = results.getFirst();
		assertThat(result.maxMarks()).isEqualByComparingTo("100.00");
		assertThat(result.totalMarks()).isEqualByComparingTo("57.00");
		assertThat(result.percentage()).isEqualByComparingTo("57.00");
		assertThat(result.passed()).isFalse();
		assertThat(result.subjects()).extracting("passed").containsExactly(true, false);
	}

	@Test
	void getStudentProfileExamResultsBuildsResultFromMarks() {
		ExamMark mark = mark(math, "88.00", "100.00");
		when(academicHierarchyService.loadAcademicYear(academicYear.getId())).thenReturn(academicYear);
		when(examTypeRepository.findByIdAndDeletedFalse(examType.getId())).thenReturn(Optional.of(examType));
		when(studentRepository.findProfileByIdAndDeletedFalse(student.getId())).thenReturn(Optional.of(student));
		when(examMarkRepository.findStudentExamResults(student.getId(), academicYear.getId(), examType.getId(), null))
				.thenReturn(List.of(mark));
		when(examScheduleRepository.findForStudentProfile(
				academicYear.getId(),
				classEntity.getId(),
				section.getId(),
				examType.getId(),
				null))
				.thenReturn(List.of(schedule));
		when(examMarkRepository.findResultMarks(academicYear.getId(), classEntity.getId(), section.getId(), examType.getId()))
				.thenReturn(List.of(mark));

		var response = examService.getStudentProfileExamResults(
				student.getId(),
				academicYear.getId(),
				examType.getId(),
				null);

		assertThat(response.results()).hasSize(1);
		var result = response.results().getFirst();
		assertThat(result.examType()).isEqualTo("Mid Term");
		assertThat(result.examName()).isEqualTo("Unit Test 1");
		assertThat(result.totalMarks()).isEqualByComparingTo("100.00");
		assertThat(result.obtainedMarks()).isEqualByComparingTo("88.00");
		assertThat(result.percentage()).isEqualByComparingTo("88.00");
		assertThat(result.grade()).isEqualTo("A");
		assertThat(result.passFailStatus()).isEqualTo("PASS");
		assertThat(result.rank()).isEqualTo(1);
		assertThat(result.subjectResults()).hasSize(1);
		assertThat(result.subjectResults().getFirst().subjectName()).isEqualTo("Mathematics");
		assertThat(result.subjectResults().getFirst().marksObtained()).isEqualByComparingTo("88.00");
	}

	@Test
	void getStudentProfileExamResultsShowsScheduledExamWhenMarksAreMissing() {
		when(examTypeRepository.findByIdAndDeletedFalse(examType.getId())).thenReturn(Optional.of(examType));
		when(studentRepository.findProfileByIdAndDeletedFalse(student.getId())).thenReturn(Optional.of(student));
		when(examMarkRepository.findStudentExamResults(student.getId(), null, examType.getId(), null))
				.thenReturn(List.of());
		when(examScheduleRepository.findForStudentProfile(
				academicYear.getId(),
				classEntity.getId(),
				section.getId(),
				examType.getId(),
				null))
				.thenReturn(List.of(schedule));

		var response = examService.getStudentProfileExamResults(
				student.getId(),
				null,
				examType.getId(),
				null);

		assertThat(response.results()).hasSize(1);
		var result = response.results().getFirst();
		assertThat(result.passFailStatus()).isEqualTo("PENDING");
		assertThat(result.grade()).isNull();
		assertThat(result.rank()).isNull();
		assertThat(result.subjectResults().getFirst().marksObtained()).isNull();
		verify(examMarkRepository, never()).findResultMarks(academicYear.getId(), classEntity.getId(), section.getId(), examType.getId());
	}

	private void stubHierarchy() {
		when(academicHierarchyService.loadAcademicYear(academicYear.getId())).thenReturn(academicYear);
		when(academicHierarchyService.loadClass(classEntity.getId())).thenReturn(classEntity);
		when(academicHierarchyService.loadSectionForClass(classEntity.getId(), section.getId())).thenReturn(section);
	}

	private ExamScheduleRequest scheduleRequest(List<ExamScheduleSubjectRequest> subjects) {
		return new ExamScheduleRequest(
				academicYear.getId(),
				classEntity.getId(),
				section.getId(),
				examType.getId(),
				"Unit Test 1",
				subjects,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				ExamScheduleStatus.SCHEDULED,
				null);
	}

	private ExamScheduleSubjectRequest subject(Subject subject, LocalDate examDate, String maxMarks, String passingMarks) {
		return subject(subject, examDate, null, null, null, maxMarks, passingMarks);
	}

	private ExamScheduleSubjectRequest subject(
			Subject subject,
			LocalDate examDate,
			LocalTime startTime,
			LocalTime endTime,
			String room,
			String maxMarks,
			String passingMarks) {
		return new ExamScheduleSubjectRequest(
				subject.getId(),
				examDate,
				startTime,
				endTime,
				room,
				new BigDecimal(maxMarks),
				passingMarks == null ? null : new BigDecimal(passingMarks));
	}

	private ExamSchedule scheduleWithSubjects(ExamScheduleSubjectRequest... subjects) {
		ExamSchedule examSchedule = new ExamSchedule(
				academicYear,
				classEntity,
				section,
				examType,
				"Unit Test 1",
				ExamScheduleStatus.COMPLETED,
				null);
		for (ExamScheduleSubjectRequest subjectRequest : subjects) {
			Subject selected = subjectRequest.subjectId().equals(math.getId()) ? math
					: subjectRequest.subjectId().equals(english.getId()) ? english
					: science;
			examSchedule.addSubject(
					selected,
					subjectRequest.examDate(),
					subjectRequest.startTime(),
					subjectRequest.endTime(),
					subjectRequest.room(),
					subjectRequest.maxMarks(),
					subjectRequest.passingMarks());
		}
		return examSchedule;
	}

	private ExamMark mark(Subject subject, String marks, String maxMarks) {
		ExamMark mark = new ExamMark(
				academicYear,
				classEntity,
				section,
				schedule,
				subject,
				student,
				new BigDecimal(marks),
				new BigDecimal(maxMarks),
				"Good");
		setId(mark);
		ReflectionTestUtils.setField(mark, "updatedAt", Instant.parse("2026-08-10T10:15:00Z"));
		return mark;
	}

	private void setId(Object entity) {
		ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
	}
}
