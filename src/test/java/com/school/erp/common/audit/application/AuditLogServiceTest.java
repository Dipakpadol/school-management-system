package com.school.erp.common.audit.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.erp.common.api.PageRequestDto;
import com.school.erp.common.api.PageResponse;
import com.school.erp.common.audit.api.dto.AuditLogDto;
import com.school.erp.common.audit.api.dto.AuditLogSearchRequest;
import com.school.erp.common.audit.domain.AuditLog;
import com.school.erp.common.audit.infrastructure.AuditLogRepository;
import com.school.erp.common.exception.BusinessException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

	@Mock
	private AuditLogRepository auditLogRepository;

	private AuditLogService auditLogService;

	@BeforeEach
	void setUp() {
		auditLogService = new AuditLogService(auditLogRepository, new AuditLogMapper(), new ObjectMapper());
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void recordSerializesPayloadAndPersistsCurrentActor() {
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken("admin@school.test", "n/a", List.of()));
		when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> {
			AuditLog auditLog = invocation.getArgument(0);
			ReflectionTestUtils.setField(auditLog, "id", UUID.randomUUID());
			ReflectionTestUtils.setField(auditLog, "createdAt", Instant.now());
			return auditLog;
		});

		AuditLogDto response = auditLogService.record(new AuditLogEvent(
				"STUDENTS",
				"Student",
				"student-1",
				"CREATE",
				null,
				Map.of("admissionNumber", "ADM-001")));

		assertThat(response.moduleName()).isEqualTo("STUDENTS");
		assertThat(response.entityName()).isEqualTo("Student");
		assertThat(response.action()).isEqualTo("CREATE");
		assertThat(response.performedBy()).isEqualTo("admin@school.test");
		assertThat(response.newValue()).contains("\"admissionNumber\":\"ADM-001\"");

		ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
		verify(auditLogRepository).save(captor.capture());
		assertThat(captor.getValue().getResourceType()).isEqualTo("Student");
		assertThat(captor.getValue().getStatus()).isEqualTo("SUCCESS");
	}

	@Test
	void searchUsesPerformedAtDescendingByDefault() {
		AuditLog auditLog = new AuditLog("FEES", "FeeReceipt", "receipt-1", "PAYMENT_COLLECTED", null, "{}", "system", Instant.now(), null);
		ReflectionTestUtils.setField(auditLog, "id", UUID.randomUUID());
		ReflectionTestUtils.setField(auditLog, "createdAt", Instant.now());
		when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(auditLog)));

		PageResponse<AuditLogDto> response = auditLogService.search(
				new AuditLogSearchRequest(null, "FEES", null, null, null, null, null, null),
				new PageRequestDto(0, 20, null, null));

		assertThat(response.totalElements()).isEqualTo(1);

		ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
		verify(auditLogRepository).findAll(any(Specification.class), pageableCaptor.capture());
		Sort.Order order = pageableCaptor.getValue().getSort().getOrderFor("performedAt");
		assertThat(order).isNotNull();
		assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
	}

	@Test
	void searchRejectsInvalidDateRange() {
		Instant later = Instant.parse("2026-05-24T10:00:00Z");
		Instant earlier = Instant.parse("2026-05-24T09:00:00Z");

		assertThatThrownBy(() -> auditLogService.search(
				new AuditLogSearchRequest(null, null, null, null, null, null, later, earlier),
				new PageRequestDto(0, 20, null, null)))
				.isInstanceOf(BusinessException.class);

		verify(auditLogRepository, never()).findAll(any(Specification.class), any(Pageable.class));
	}
}
