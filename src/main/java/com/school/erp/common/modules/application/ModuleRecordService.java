package com.school.erp.common.modules.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.school.erp.common.api.PageRequestDto;
import com.school.erp.common.api.PageResponse;
import com.school.erp.common.audit.application.AuditAction;
import com.school.erp.common.audit.application.AuditLogEvent;
import com.school.erp.common.audit.application.AuditLogService;
import com.school.erp.common.exception.BusinessException;
import com.school.erp.common.exception.ErrorCode;
import com.school.erp.common.exception.ResourceNotFoundException;
import com.school.erp.common.importexport.CsvExportService;
import com.school.erp.common.importexport.CsvImportService;
import com.school.erp.common.importexport.ExcelExportService;
import com.school.erp.common.importexport.ExcelImportService;
import com.school.erp.common.importexport.ImportErrorDto;
import com.school.erp.common.importexport.ImportErrorReportStore;
import com.school.erp.common.importexport.ImportResultDto;
import com.school.erp.common.importexport.ImportRow;
import com.school.erp.common.importexport.PdfExportService;
import com.school.erp.common.importexport.TemplateGeneratorService;
import com.school.erp.common.modules.api.dto.ModuleRecordCountResponse;
import com.school.erp.common.modules.api.dto.ModuleRecordRequest;
import com.school.erp.common.modules.api.dto.ModuleRecordResponse;
import com.school.erp.common.modules.api.dto.ModuleRecordSearchRequest;
import com.school.erp.common.modules.domain.ModuleRecord;
import com.school.erp.common.modules.infrastructure.ModuleRecordRepository;

import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ModuleRecordService {

	private static final List<String> HEADERS = List.of(
			"code",
			"name",
			"description",
			"status",
			"parentId",
			"ownerId",
			"recordDate",
			"amount",
			"metadataJson",
			"active");

	private final ModuleRecordRepository moduleRecordRepository;
	private final ModuleRecordMapper moduleRecordMapper;
	private final AuditLogService auditLogService;
	private final ExcelImportService excelImportService;
	private final CsvImportService csvImportService;
	private final ExcelExportService excelExportService;
	private final CsvExportService csvExportService;
	private final PdfExportService pdfExportService;
	private final TemplateGeneratorService templateGeneratorService;
	private final ImportErrorReportStore importErrorReportStore;

	@Transactional(readOnly = true)
	public PageResponse<ModuleRecordResponse> search(
			String moduleName,
			String recordType,
			ModuleRecordSearchRequest searchRequest,
			PageRequestDto pageRequest) {
		return PageResponse.from(
				moduleRecordRepository.findAll(
						ModuleRecordSpecifications.matching(moduleName, recordType, searchRequest),
						pageRequest.toPageable("name")),
				moduleRecordMapper::toResponse);
	}

	@Transactional(readOnly = true)
	public ModuleRecordResponse get(String moduleName, String recordType, UUID id) {
		return moduleRecordMapper.toResponse(findRecord(moduleName, recordType, id));
	}

	@Transactional
	public ModuleRecordResponse create(String moduleName, String recordType, ModuleRecordRequest request) {
		validateCodeAvailable(moduleName, recordType, request.code(), null);
		ModuleRecord record = moduleRecordRepository.save(new ModuleRecord(moduleName, recordType, request));
		auditLogService.record(new AuditLogEvent(moduleName, recordType, record.getId().toString(), AuditAction.CREATE, null,
				moduleRecordMapper.toResponse(record)));
		return moduleRecordMapper.toResponse(record);
	}

	@Transactional
	public ModuleRecordResponse update(String moduleName, String recordType, UUID id, ModuleRecordRequest request) {
		ModuleRecord record = findRecord(moduleName, recordType, id);
		ModuleRecordResponse oldValue = moduleRecordMapper.toResponse(record);
		validateCodeAvailable(moduleName, recordType, request.code(), id);
		record.update(request);
		ModuleRecord saved = moduleRecordRepository.save(record);
		auditLogService.record(new AuditLogEvent(moduleName, recordType, id.toString(), AuditAction.UPDATE, oldValue,
				moduleRecordMapper.toResponse(saved)));
		return moduleRecordMapper.toResponse(saved);
	}

	@Transactional
	public void delete(String moduleName, String recordType, UUID id) {
		ModuleRecord record = findRecord(moduleName, recordType, id);
		ModuleRecordResponse oldValue = moduleRecordMapper.toResponse(record);
		record.softDelete(currentActor());
		moduleRecordRepository.save(record);
		auditLogService.record(new AuditLogEvent(moduleName, recordType, id.toString(), AuditAction.DELETE, oldValue, null));
	}

	@Transactional(readOnly = true)
	public List<ModuleRecordCountResponse> counts(String moduleName) {
		String normalizedModule = ModuleRecord.normalizeModule(moduleName);
		return moduleRecordRepository.countByRecordType(normalizedModule).stream()
				.map(row -> {
					String type = String.valueOf(row[0]);
					return new ModuleRecordCountResponse(
							normalizedModule,
							type,
							(Long) row[1],
							moduleRecordRepository.countByModuleNameAndRecordTypeAndActiveTrueAndDeletedFalse(normalizedModule, type));
				})
				.toList();
	}

	@Transactional
	public ImportResultDto importExcel(String moduleName, String recordType, MultipartFile file) {
		return importRows(moduleName, recordType, excelImportService.readRows(file));
	}

	@Transactional
	public ImportResultDto importCsv(String moduleName, String recordType, MultipartFile file) {
		return importRows(moduleName, recordType, csvImportService.readRows(file));
	}

	@Transactional(readOnly = true)
	public ImportResultDto importErrors(UUID batchId) {
		return importErrorReportStore.find(batchId)
				.orElseThrow(() -> new ResourceNotFoundException("Import batch", batchId));
	}

	@Transactional(readOnly = true)
	public byte[] exportExcel(String moduleName, String recordType, ModuleRecordSearchRequest searchRequest) {
		List<Map<String, Object>> rows = exportRows(moduleName, recordType, searchRequest);
		auditExport(moduleName, recordType, "EXCEL", rows.size());
		return excelExportService.export(recordType, HEADERS, rows);
	}

	@Transactional(readOnly = true)
	public byte[] exportCsv(String moduleName, String recordType, ModuleRecordSearchRequest searchRequest) {
		List<Map<String, Object>> rows = exportRows(moduleName, recordType, searchRequest);
		auditExport(moduleName, recordType, "CSV", rows.size());
		return csvExportService.export(HEADERS, rows);
	}

	@Transactional(readOnly = true)
	public byte[] exportPdf(String moduleName, String recordType, ModuleRecordSearchRequest searchRequest) {
		List<Map<String, Object>> rows = exportRows(moduleName, recordType, searchRequest);
		auditExport(moduleName, recordType, "PDF", rows.size());
		return pdfExportService.exportTable(ModuleRecord.normalizeModule(moduleName) + " " + ModuleRecord.normalizeType(recordType), HEADERS, rows);
	}

	public byte[] excelTemplate(String recordType) {
		return templateGeneratorService.excelTemplate(recordType, HEADERS);
	}

	public byte[] csvTemplate() {
		return templateGeneratorService.csvTemplate(HEADERS);
	}

	private ImportResultDto importRows(String moduleName, String recordType, List<ImportRow> rows) {
		UUID batchId = UUID.randomUUID();
		List<ImportErrorDto> errors = new java.util.ArrayList<>();
		java.util.Set<String> seenCodes = new java.util.HashSet<>();
		int success = 0;

		for (ImportRow row : rows) {
			ModuleRecordRequest request = requestFromRow(row, errors);
			if (request == null) {
				continue;
			}
			String code = ModuleRecord.normalizeCode(request.code());
			if (!seenCodes.add(code)) {
				errors.add(new ImportErrorDto(row.rowNumber(), "code", "Duplicate code in import file."));
				continue;
			}
			if (moduleRecordRepository.existsByModuleNameAndRecordTypeAndCodeIgnoreCaseAndDeletedFalse(
					ModuleRecord.normalizeModule(moduleName),
					ModuleRecord.normalizeType(recordType),
					code)) {
				errors.add(new ImportErrorDto(row.rowNumber(), "code", "Code already exists."));
				continue;
			}
			moduleRecordRepository.save(new ModuleRecord(moduleName, recordType, request));
			success++;
		}

		ImportResultDto result = ImportResultDto.of(batchId, rows.size(), success, errors);
		importErrorReportStore.save(result);
		auditLogService.record(new AuditLogEvent(
				moduleName,
				recordType,
				batchId.toString(),
				AuditAction.IMPORT,
				null,
				Map.of("totalRows", rows.size(), "successRows", success, "failedRows", errors.size())));
		return result;
	}

	private ModuleRecordRequest requestFromRow(ImportRow row, List<ImportErrorDto> errors) {
		String code = value(row, "code");
		String name = value(row, "name");
		boolean valid = true;
		if (!StringUtils.hasText(code)) {
			errors.add(new ImportErrorDto(row.rowNumber(), "code", "Code is required."));
			valid = false;
		}
		if (!StringUtils.hasText(name)) {
			errors.add(new ImportErrorDto(row.rowNumber(), "name", "Name is required."));
			valid = false;
		}
		if (!valid) {
			return null;
		}

		return new ModuleRecordRequest(
				code,
				name,
				value(row, "description"),
				value(row, "status"),
				uuid(row, "parentId", errors),
				uuid(row, "ownerId", errors),
				date(row, "recordDate", errors),
				decimal(row, "amount", errors),
				value(row, "metadataJson"),
				bool(row, "active"));
	}

	private List<Map<String, Object>> exportRows(
			String moduleName,
			String recordType,
			ModuleRecordSearchRequest searchRequest) {
		return moduleRecordRepository.findAll(
						ModuleRecordSpecifications.matching(moduleName, recordType, searchRequest),
						Sort.by(Sort.Direction.ASC, "name"))
				.stream()
				.map(this::exportRow)
				.toList();
	}

	private Map<String, Object> exportRow(ModuleRecord record) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("code", record.getCode());
		row.put("name", record.getName());
		row.put("description", record.getDescription());
		row.put("status", record.getStatus());
		row.put("parentId", record.getParentId());
		row.put("ownerId", record.getOwnerId());
		row.put("recordDate", record.getRecordDate());
		row.put("amount", record.getAmount());
		row.put("metadataJson", record.getMetadataJson());
		row.put("active", record.isActive());
		return row;
	}

	private void validateCodeAvailable(String moduleName, String recordType, String code, UUID currentId) {
		String normalizedModule = ModuleRecord.normalizeModule(moduleName);
		String normalizedType = ModuleRecord.normalizeType(recordType);
		String normalizedCode = ModuleRecord.normalizeCode(code);
		boolean exists = currentId == null
				? moduleRecordRepository.existsByModuleNameAndRecordTypeAndCodeIgnoreCaseAndDeletedFalse(
						normalizedModule, normalizedType, normalizedCode)
				: moduleRecordRepository.existsByModuleNameAndRecordTypeAndCodeIgnoreCaseAndIdNotAndDeletedFalse(
						normalizedModule, normalizedType, normalizedCode, currentId);
		if (exists) {
			throw new BusinessException(ErrorCode.CONFLICT, "Code already exists for this module record type.");
		}
	}

	private ModuleRecord findRecord(String moduleName, String recordType, UUID id) {
		return moduleRecordRepository.findByIdAndModuleNameAndRecordTypeAndDeletedFalse(
						id,
						ModuleRecord.normalizeModule(moduleName),
						ModuleRecord.normalizeType(recordType))
				.orElseThrow(() -> new ResourceNotFoundException("Module record", id));
	}

	private void auditExport(String moduleName, String recordType, String format, int rowCount) {
		auditLogService.recordStandalone(new AuditLogEvent(
				moduleName,
				recordType,
				null,
				AuditAction.EXPORT,
				null,
				Map.of("format", format, "rowCount", rowCount)));
	}

	private String value(ImportRow row, String key) {
		String value = row.values().get(key);
		return StringUtils.hasText(value) ? value.trim() : null;
	}

	private UUID uuid(ImportRow row, String key, List<ImportErrorDto> errors) {
		String value = value(row, key);
		if (!StringUtils.hasText(value)) {
			return null;
		}
		try {
			return UUID.fromString(value);
		}
		catch (IllegalArgumentException ex) {
			errors.add(new ImportErrorDto(row.rowNumber(), key, "Must be a valid UUID."));
			return null;
		}
	}

	private LocalDate date(ImportRow row, String key, List<ImportErrorDto> errors) {
		String value = value(row, key);
		if (!StringUtils.hasText(value)) {
			return null;
		}
		try {
			return LocalDate.parse(value);
		}
		catch (RuntimeException ex) {
			errors.add(new ImportErrorDto(row.rowNumber(), key, "Must use yyyy-MM-dd format."));
			return null;
		}
	}

	private BigDecimal decimal(ImportRow row, String key, List<ImportErrorDto> errors) {
		String value = value(row, key);
		if (!StringUtils.hasText(value)) {
			return null;
		}
		try {
			return new BigDecimal(value);
		}
		catch (RuntimeException ex) {
			errors.add(new ImportErrorDto(row.rowNumber(), key, "Must be a valid decimal amount."));
			return null;
		}
	}

	private Boolean bool(ImportRow row, String key) {
		String value = value(row, key);
		return StringUtils.hasText(value) ? Boolean.valueOf(value) : null;
	}

	private String currentActor() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			return "system";
		}
		return authentication.getName();
	}
}
