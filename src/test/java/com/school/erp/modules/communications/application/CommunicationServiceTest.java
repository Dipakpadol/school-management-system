package com.school.erp.modules.communications.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.api.PageRequestDto;
import com.school.erp.common.audit.application.AuditLogService;
import com.school.erp.common.exception.BusinessException;
import com.school.erp.common.exception.ErrorCode;
import com.school.erp.modules.academic.application.AcademicHierarchyService;
import com.school.erp.modules.academic.domain.AcademicYear;
import com.school.erp.modules.academic.domain.ClassEntity;
import com.school.erp.modules.academic.domain.SectionEntity;
import com.school.erp.modules.communications.api.dto.CommunicationRequest;
import com.school.erp.modules.communications.domain.CommunicationAudienceType;
import com.school.erp.modules.communications.domain.CommunicationRecord;
import com.school.erp.modules.communications.domain.CommunicationStatus;
import com.school.erp.modules.communications.domain.CommunicationType;
import com.school.erp.modules.communications.infrastructure.CommunicationRecordRepository;
import com.school.erp.modules.staff.domain.EmploymentStatus;
import com.school.erp.modules.staff.infrastructure.StaffRepository;
import com.school.erp.modules.students.infrastructure.StudentClassAssignmentRepository;
import com.school.erp.modules.students.domain.StudentStatus;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CommunicationServiceTest {

	@Mock
	private CommunicationRecordRepository communicationRepository;

	@Mock
	private StaffRepository staffRepository;

	@Mock
	private StudentClassAssignmentRepository studentClassAssignmentRepository;

	@Mock
	private AcademicHierarchyService academicHierarchyService;

	@Mock
	private AuditLogService auditLogService;

	private CommunicationService communicationService;

	@BeforeEach
	void setUp() {
		communicationService = new CommunicationService(
				communicationRepository,
				staffRepository,
				studentClassAssignmentRepository,
				academicHierarchyService,
				new CommunicationMapper(),
				auditLogService);
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void communicationsReturnsFilteredHistoryPage() {
		CommunicationRecord record = new CommunicationRecord(
				CommunicationType.CIRCULAR,
				"Fee reminder",
				"Submit instalment before month end",
				CommunicationAudienceType.PARENTS,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				CommunicationStatus.PUBLISHED);
		setId(record, UUID.randomUUID());
		when(communicationRepository.search(eq(CommunicationType.CIRCULAR), eq(CommunicationStatus.PUBLISHED), any()))
				.thenReturn(new PageImpl<>(List.of(record)));

		var response = communicationService.communications(
				CommunicationType.CIRCULAR,
				CommunicationStatus.PUBLISHED,
				new PageRequestDto(0, 10, null, null));

		assertThat(response.content()).hasSize(1);
		assertThat(response.content().getFirst().type()).isEqualTo(CommunicationType.CIRCULAR);
		assertThat(response.content().getFirst().audienceType()).isEqualTo(CommunicationAudienceType.PARENTS);
	}

	@Test
	void createRejectsSectionWithoutClass() {
		CommunicationRequest request = request(CommunicationAudienceType.ALL, null, UUID.randomUUID(), CommunicationType.NOTICE);

		assertThatThrownBy(() -> communicationService.create(request))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.VALIDATION_ERROR);
	}

	@Test
	void createEventAcceptsDivisionAudienceAndStoresEventFields() {
		authenticate("COMMUNICATION_CREATE");
		UUID academicYearId = UUID.randomUUID();
		UUID classId = UUID.randomUUID();
		UUID sectionId = UUID.randomUUID();
		when(academicHierarchyService.loadAcademicYear(academicYearId)).thenReturn(mock(AcademicYear.class));
		when(academicHierarchyService.loadClass(classId)).thenReturn(mock(ClassEntity.class));
		when(academicHierarchyService.loadSectionForClass(classId, sectionId)).thenReturn(mock(SectionEntity.class));
		when(communicationRepository.save(any(CommunicationRecord.class))).thenAnswer(invocation -> {
			CommunicationRecord saved = invocation.getArgument(0);
			setId(saved, UUID.randomUUID());
			return saved;
		});

		var response = communicationService.create(new CommunicationRequest(
				CommunicationType.EVENT,
				"Science fair",
				"Division fair schedule",
				CommunicationAudienceType.DIVISION,
				academicYearId,
				classId,
				sectionId,
				Instant.parse("2026-09-05T04:00:00Z"),
				Instant.parse("2026-09-07T12:00:00Z"),
				Instant.parse("2026-09-07T04:30:00Z"),
				Instant.parse("2026-09-07T07:30:00Z"),
				"Auditorium",
				CommunicationStatus.DRAFT));

		assertThat(response.type()).isEqualTo(CommunicationType.EVENT);
		assertThat(response.audienceType()).isEqualTo(CommunicationAudienceType.DIVISION);
		assertThat(response.classId()).isEqualTo(classId);
		assertThat(response.sectionId()).isEqualTo(sectionId);
		assertThat(response.location()).isEqualTo("Auditorium");
	}

	@Test
	void publishStaffCommunicationStoresRecipientCount() {
		UUID communicationId = UUID.randomUUID();
		CommunicationRecord record = new CommunicationRecord(
				CommunicationType.ANNOUNCEMENT,
				"Staff meeting",
				"Monthly staff meeting",
				CommunicationAudienceType.STAFF,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				CommunicationStatus.DRAFT);
		setId(record, communicationId);
		when(communicationRepository.findByIdAndDeletedFalse(communicationId)).thenReturn(Optional.of(record));
		when(staffRepository.countByStatusAndDeletedFalse(EmploymentStatus.ACTIVE)).thenReturn(12L);

		var response = communicationService.publish(communicationId);

		assertThat(response.status()).isEqualTo(CommunicationStatus.PUBLISHED);
		assertThat(response.recipientCount()).isEqualTo(12);
		assertThat(response.publishedAt()).isNotNull();
	}

	@Test
	void publishAllAudienceCombinesActiveStaffAndEligibleStudents() {
		UUID communicationId = UUID.randomUUID();
		CommunicationRecord record = new CommunicationRecord(
				CommunicationType.ANNOUNCEMENT,
				"School reopening",
				"Regular classes resume tomorrow",
				CommunicationAudienceType.ALL,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				CommunicationStatus.DRAFT);
		setId(record, communicationId);
		when(communicationRepository.findByIdAndDeletedFalse(communicationId)).thenReturn(Optional.of(record));
		when(staffRepository.countByStatusAndDeletedFalse(EmploymentStatus.ACTIVE)).thenReturn(18L);
		when(studentClassAssignmentRepository.countEligibleStudents(
				isNull(),
				isNull(),
				isNull(),
				any(LocalDate.class),
				eq(StudentStatus.INACTIVE)))
				.thenReturn(420L);

		var response = communicationService.publish(communicationId);

		assertThat(response.status()).isEqualTo(CommunicationStatus.PUBLISHED);
		assertThat(response.recipientCount()).isEqualTo(438);
	}

	@Test
	void createRejectsAuthenticatedUsersWithoutCommunicationMutationAuthority() {
		authenticate("COMMUNICATION_READ");

		assertThatThrownBy(() -> communicationService.create(request(
				CommunicationAudienceType.ALL,
				null,
				null,
				CommunicationType.NOTICE)))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.FORBIDDEN);
	}

	private CommunicationRequest request(
			CommunicationAudienceType audienceType,
			UUID classId,
			UUID sectionId,
			CommunicationType type) {
		return new CommunicationRequest(
				type,
				"Notice",
				"Exam timing update",
				audienceType,
				null,
				classId,
				sectionId,
				Instant.parse("2026-09-01T03:30:00Z"),
				null,
				null,
				null,
				null,
				CommunicationStatus.DRAFT);
	}

	private void setId(Object entity, UUID id) {
		ReflectionTestUtils.setField(entity, "id", id);
	}

	private void authenticate(String... authorities) {
		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
				"communication-user",
				null,
				java.util.Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList()));
	}
}
