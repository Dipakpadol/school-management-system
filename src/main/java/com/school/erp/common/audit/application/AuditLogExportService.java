package com.school.erp.common.audit.application;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.school.erp.common.audit.api.dto.AuditLogSearchRequest;
import com.school.erp.common.audit.domain.AuditLog;
import com.school.erp.common.audit.infrastructure.AuditLogRepository;
import com.school.erp.common.importexport.CsvExportService;
import com.school.erp.common.importexport.ExcelExportService;
import com.school.erp.common.importexport.PdfExportService;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuditLogExportService {

	private static final List<String> HEADERS = List.of(
			"Module",
			"Entity",
			"Entity ID",
			"Action",
			"Performed By",
			"Performed At",
			"IP Address",
			"Old Value",
			"New Value");

	private final AuditLogRepository auditLogRepository;
	private final ExcelExportService excelExportService;
	private final CsvExportService csvExportService;
	private final PdfExportService pdfExportService;
	private final AuditLogService auditLogService;

	@Transactional(readOnly = true)
	public byte[] exportExcel(AuditLogSearchRequest request) {
		List<Map<String, Object>> rows = rows(request);
		recordExport("EXCEL", rows.size());
		return excelExportService.export("Audit Logs", HEADERS, rows);
	}

	@Transactional(readOnly = true)
	public byte[] exportCsv(AuditLogSearchRequest request) {
		List<Map<String, Object>> rows = rows(request);
		recordExport("CSV", rows.size());
		return csvExportService.export(HEADERS, rows);
	}

	@Transactional(readOnly = true)
	public byte[] exportPdf(AuditLogSearchRequest request) {
		List<Map<String, Object>> rows = rows(request);
		recordExport("PDF", rows.size());
		return pdfExportService.exportTable("Audit Logs", HEADERS, rows);
	}

	private List<Map<String, Object>> rows(AuditLogSearchRequest request) {
		return auditLogRepository
				.findAll(AuditLogSpecifications.matching(request), Sort.by(Sort.Direction.DESC, "performedAt"))
				.stream()
				.map(this::row)
				.toList();
	}

	private Map<String, Object> row(AuditLog auditLog) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("Module", auditLog.getModuleName());
		row.put("Entity", auditLog.getEntityName());
		row.put("Entity ID", auditLog.getEntityId());
		row.put("Action", auditLog.getAction());
		row.put("Performed By", auditLog.getPerformedBy());
		row.put("Performed At", auditLog.getPerformedAt());
		row.put("IP Address", auditLog.getIpAddress());
		row.put("Old Value", auditLog.getOldValue());
		row.put("New Value", auditLog.getNewValue());
		return row;
	}

	private void recordExport(String format, int rowCount) {
		auditLogService.recordStandalone(new AuditLogEvent(
				"Audit",
				"AuditLog",
				null,
				AuditAction.EXPORT,
				null,
				Map.of("format", format, "rowCount", rowCount)));
	}
}
