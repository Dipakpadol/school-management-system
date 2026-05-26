package com.school.erp.common.modules.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.audit.application.AuditLogService;
import com.school.erp.common.exception.BusinessException;
import com.school.erp.common.importexport.CsvExportService;
import com.school.erp.common.importexport.CsvImportService;
import com.school.erp.common.importexport.ExcelExportService;
import com.school.erp.common.importexport.ExcelImportService;
import com.school.erp.common.importexport.ImportErrorReportStore;
import com.school.erp.common.importexport.PdfExportService;
import com.school.erp.common.importexport.TemplateGeneratorService;
import com.school.erp.common.modules.api.dto.ModuleRecordRequest;
import com.school.erp.common.modules.api.dto.ModuleRecordResponse;
import com.school.erp.common.modules.domain.ModuleRecord;
import com.school.erp.common.modules.infrastructure.ModuleRecordRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ModuleRecordServiceTest {

	@Mock
	private ModuleRecordRepository moduleRecordRepository;

	@Mock
	private AuditLogService auditLogService;

	@Mock
	private ExcelImportService excelImportService;

	@Mock
	private CsvImportService csvImportService;

	@Mock
	private ExcelExportService excelExportService;

	@Mock
	private CsvExportService csvExportService;

	@Mock
	private PdfExportService pdfExportService;

	@Mock
	private TemplateGeneratorService templateGeneratorService;

	private ModuleRecordService moduleRecordService;

	@BeforeEach
	void setUp() {
		moduleRecordService = new ModuleRecordService(
				moduleRecordRepository,
				new ModuleRecordMapper(),
				auditLogService,
				excelImportService,
				csvImportService,
				excelExportService,
				csvExportService,
				pdfExportService,
				templateGeneratorService,
				new ImportErrorReportStore());
	}

	@Test
	void createNormalizesModuleTypeAndCodeAndWritesAudit() {
		when(moduleRecordRepository.existsByModuleNameAndRecordTypeAndCodeIgnoreCaseAndDeletedFalse(
				"ACADEMIC", "CLASS_SUBJECTS", "MATH")).thenReturn(false);
		when(moduleRecordRepository.save(any(ModuleRecord.class))).thenAnswer(invocation -> {
			ModuleRecord record = invocation.getArgument(0);
			ReflectionTestUtils.setField(record, "id", UUID.randomUUID());
			return record;
		});

		ModuleRecordResponse response = moduleRecordService.create(
				"academic",
				"class-subjects",
				new ModuleRecordRequest("math", "Mathematics", null, null, null, null, null, null, null, true));

		assertThat(response.moduleName()).isEqualTo("ACADEMIC");
		assertThat(response.recordType()).isEqualTo("CLASS_SUBJECTS");
		assertThat(response.code()).isEqualTo("MATH");
		assertThat(response.status()).isEqualTo("ACTIVE");
		verify(auditLogService).record(any());

		ArgumentCaptor<ModuleRecord> captor = ArgumentCaptor.forClass(ModuleRecord.class);
		verify(moduleRecordRepository).save(captor.capture());
		assertThat(captor.getValue().getName()).isEqualTo("Mathematics");
	}

	@Test
	void updateRejectsDuplicateCodeWithinModuleAndType() {
		UUID id = UUID.randomUUID();
		ModuleRecord record = new ModuleRecord(
				"ACADEMIC",
				"SUBJECTS",
				new ModuleRecordRequest("SCI", "Science", null, null, null, null, null, null, null, true));
		ReflectionTestUtils.setField(record, "id", id);
		when(moduleRecordRepository.findByIdAndModuleNameAndRecordTypeAndDeletedFalse(id, "ACADEMIC", "SUBJECTS"))
				.thenReturn(Optional.of(record));
		when(moduleRecordRepository.existsByModuleNameAndRecordTypeAndCodeIgnoreCaseAndIdNotAndDeletedFalse(
				"ACADEMIC", "SUBJECTS", "MATH", id)).thenReturn(true);

		assertThatThrownBy(() -> moduleRecordService.update(
				"academic",
				"subjects",
				id,
				new ModuleRecordRequest("MATH", "Mathematics", null, null, null, null, null, null, null, true)))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("Code already exists");
	}
}
