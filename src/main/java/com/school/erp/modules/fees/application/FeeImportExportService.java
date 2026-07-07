package com.school.erp.modules.fees.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.school.erp.common.audit.application.AuditAction;
import com.school.erp.common.audit.application.AuditLogEvent;
import com.school.erp.common.audit.application.AuditLogService;
import com.school.erp.common.exception.BusinessException;
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
import com.school.erp.modules.fees.api.dto.FeeCategoryRequest;
import com.school.erp.modules.fees.api.dto.DefaulterSearchRequest;
import com.school.erp.modules.fees.api.dto.FeeDefaulterResponse;
import com.school.erp.modules.fees.api.dto.FeeReceiptResponse;
import com.school.erp.modules.fees.api.dto.FeeReportRequest;
import com.school.erp.modules.fees.api.dto.FeeReportSummaryResponse;
import com.school.erp.modules.fees.api.dto.FeeStructureInstallmentRequest;
import com.school.erp.modules.fees.api.dto.FeeStructureItemRequest;
import com.school.erp.modules.fees.api.dto.FeeStructureRequest;
import com.school.erp.modules.fees.api.dto.FeeStructureResponse;
import com.school.erp.modules.fees.api.dto.StudentFeeAssignmentRequest;
import com.school.erp.modules.fees.api.dto.StudentFeeAssignmentResponse;
import com.school.erp.modules.fees.domain.FeeCategory;
import com.school.erp.modules.fees.infrastructure.FeeCategoryRepository;
import com.school.erp.modules.fees.infrastructure.FeeReceiptRepository;
import com.school.erp.modules.fees.infrastructure.FeeStructureRepository;
import com.school.erp.modules.fees.infrastructure.StudentFeeAssignmentRepository;
import com.school.erp.modules.students.domain.Student;
import com.school.erp.modules.students.infrastructure.StudentRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FeeImportExportService {

	public static final List<String> FEE_STRUCTURE_TEMPLATE_COLUMNS = List.of(
			"academicYear",
			"className",
			"sectionName",
			"name",
			"description",
			"activate",
			"categoryCode",
			"categoryName",
			"itemAmount",
			"installmentSequence",
			"installmentTitle",
			"installmentDueDate",
			"installmentAmount");

	public static final List<String> FEE_STRUCTURE_EXPORT_COLUMNS = List.of(
			"id",
			"academicYear",
			"className",
			"sectionName",
			"name",
			"status",
			"totalAmount",
			"categories",
			"installments");

	public static final List<String> ASSIGNMENT_TEMPLATE_COLUMNS = List.of(
			"admissionNumber",
			"feeStructureId",
			"assignedDate",
			"notes");

	public static final List<String> ASSIGNMENT_EXPORT_COLUMNS = List.of(
			"id",
			"admissionNumber",
			"studentName",
			"feeStructureName",
			"academicYear",
			"className",
			"sectionName",
			"assignedDate",
			"grossAmount",
			"discountAmount",
			"lateFeeAmount",
			"paidAmount",
			"balanceAmount",
			"status");

	private final ExcelImportService excelImportService;
	private final CsvImportService csvImportService;
	private final ExcelExportService excelExportService;
	private final CsvExportService csvExportService;
	private final PdfExportService pdfExportService;
	private final TemplateGeneratorService templateGeneratorService;
	private final ImportErrorReportStore importErrorReportStore;
	private final FeeService feeService;
	private final FeeMapper feeMapper;
	private final FeeCategoryRepository feeCategoryRepository;
	private final FeeStructureRepository feeStructureRepository;
	private final StudentFeeAssignmentRepository assignmentRepository;
	private final FeeReceiptRepository feeReceiptRepository;
	private final StudentRepository studentRepository;
	private final AuditLogService auditLogService;

	public ImportResultDto importStructuresExcel(MultipartFile file) {
		return importStructures(excelImportService.readRows(file));
	}

	public ImportResultDto importStructuresCsv(MultipartFile file) {
		return importStructures(csvImportService.readRows(file));
	}

	public ImportResultDto importAssignmentsExcel(MultipartFile file) {
		return importAssignments(excelImportService.readRows(file));
	}

	public ImportResultDto importAssignmentsCsv(MultipartFile file) {
		return importAssignments(csvImportService.readRows(file));
	}

	public byte[] feeStructureTemplateExcel() {
		return templateGeneratorService.excelTemplate("fee-structures", FEE_STRUCTURE_TEMPLATE_COLUMNS);
	}

	public byte[] feeStructureTemplateCsv() {
		return templateGeneratorService.csvTemplate(FEE_STRUCTURE_TEMPLATE_COLUMNS);
	}

	public byte[] assignmentTemplateExcel() {
		return templateGeneratorService.excelTemplate("fee-assignments", ASSIGNMENT_TEMPLATE_COLUMNS);
	}

	public byte[] assignmentTemplateCsv() {
		return templateGeneratorService.csvTemplate(ASSIGNMENT_TEMPLATE_COLUMNS);
	}

	@Transactional(readOnly = true)
	public byte[] exportStructuresExcel() {
		byte[] content = excelExportService.export("fee-structures", FEE_STRUCTURE_EXPORT_COLUMNS, structureRows());
		auditLogService.recordStandalone(new AuditLogEvent("FEES", "FeeStructure", null, AuditAction.EXPORT, null, "Excel export"));
		return content;
	}

	@Transactional(readOnly = true)
	public byte[] exportStructuresCsv() {
		byte[] content = csvExportService.export(FEE_STRUCTURE_EXPORT_COLUMNS, structureRows());
		auditLogService.recordStandalone(new AuditLogEvent("FEES", "FeeStructure", null, AuditAction.EXPORT, null, "CSV export"));
		return content;
	}

	@Transactional(readOnly = true)
	public byte[] exportAssignmentsExcel() {
		byte[] content = excelExportService.export("fee-assignments", ASSIGNMENT_EXPORT_COLUMNS, assignmentRows());
		auditLogService.recordStandalone(new AuditLogEvent("FEES", "StudentFeeAssignment", null, AuditAction.EXPORT, null, "Excel export"));
		return content;
	}

	@Transactional(readOnly = true)
	public byte[] exportAssignmentsCsv() {
		byte[] content = csvExportService.export(ASSIGNMENT_EXPORT_COLUMNS, assignmentRows());
		auditLogService.recordStandalone(new AuditLogEvent("FEES", "StudentFeeAssignment", null, AuditAction.EXPORT, null, "CSV export"));
		return content;
	}

	public ImportResultDto importErrors(UUID batchId) {
		return importErrorReportStore.find(batchId)
				.orElseThrow(() -> new ResourceNotFoundException("Import batch", batchId));
	}

	@Transactional(readOnly = true)
	public byte[] receiptPdf(String receiptNumber) {
		FeeReceiptResponse receipt = feeMapper.toReceiptResponse(feeReceiptRepository.findByReceiptNumberAndDeletedFalse(receiptNumber)
				.orElseThrow(() -> new ResourceNotFoundException("Fee receipt", receiptNumber)));
		Map<String, Object> values = new LinkedHashMap<>();
		values.put("Receipt Number", receipt.receiptNumber());
		values.put("Receipt Date", receipt.receiptDate());
		values.put("Student", receipt.studentName());
		values.put("Admission Number", receipt.admissionNumber());
		values.put("Amount", receipt.totalAmount());
		values.put("Payer", receipt.payerName());
		values.put("Payment Mode", receipt.paymentMode());
		values.put("Status", receipt.status());
		auditLogService.recordStandalone(new AuditLogEvent("FEES", "FeeReceipt", receipt.id().toString(), AuditAction.EXPORT, null, "Receipt PDF"));
		return pdfExportService.exportProfile("Fee Receipt", values);
	}

	@Transactional(readOnly = true)
	public byte[] collectionReport(String format) {
		FeeReportSummaryResponse summary = feeService.summarizeFees(new FeeReportRequest(null, null, null, null));
		List<String> headers = List.of("assignments", "grossAmount", "discountAmount", "lateFeeAmount", "paidAmount", "balanceAmount");
		List<Map<String, Object>> rows = List.of(new LinkedHashMap<>(Map.of(
				"assignments", summary.assignments(),
				"grossAmount", summary.grossAmount(),
				"discountAmount", summary.discountAmount(),
				"lateFeeAmount", summary.lateFeeAmount(),
				"paidAmount", summary.paidAmount(),
				"balanceAmount", summary.balanceAmount())));
		auditLogService.recordStandalone(new AuditLogEvent("FEES", "FeeCollectionReport", null, AuditAction.EXPORT, null, format));
		return switch (normalizeFormat(format)) {
			case "pdf" -> pdfExportService.exportTable("Fee Collection Summary", headers, rows);
			case "csv" -> csvExportService.export(headers, rows);
			default -> excelExportService.export("collection-summary", headers, rows);
		};
	}

	@Transactional(readOnly = true)
	public byte[] defaulterReport(String format) {
		return defaulterReport(format, new DefaulterSearchRequest(null, null, null, null, null, null, null, null, null));
	}

	@Transactional(readOnly = true)
	public byte[] defaulterReport(String format, DefaulterSearchRequest request) {
		List<FeeDefaulterResponse> defaulters = feeService.findDefaulters(
				request == null ? new DefaulterSearchRequest(null, null, null, null, null, null, null, null, null) : request,
				new com.school.erp.common.api.PageRequestDto(0, 200, null, null)).content();
		List<String> headers = List.of("assignmentId", "admissionNumber", "studentName", "className", "sectionName", "balanceAmount", "oldestDueDate", "overdueInstallments");
		List<Map<String, Object>> rows = defaulters.stream().map(defaulter -> {
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("assignmentId", defaulter.assignmentId());
			row.put("admissionNumber", defaulter.admissionNumber());
			row.put("studentName", defaulter.studentName());
			row.put("className", defaulter.className());
			row.put("sectionName", defaulter.sectionName());
			row.put("balanceAmount", defaulter.balanceAmount());
			row.put("oldestDueDate", defaulter.oldestDueDate());
			row.put("overdueInstallments", defaulter.overdueInstallments());
			return row;
		}).toList();
		auditLogService.recordStandalone(new AuditLogEvent("FEES", "FeeDefaulterReport", null, AuditAction.EXPORT, null, format));
		return switch (normalizeFormat(format)) {
			case "pdf" -> pdfExportService.exportTable("Fee Defaulters", headers, rows);
			case "csv" -> csvExportService.export(headers, rows);
			default -> excelExportService.export("fee-defaulters", headers, rows);
		};
	}

	private ImportResultDto importStructures(List<ImportRow> rows) {
		UUID batchId = UUID.randomUUID();
		List<ImportErrorDto> errors = new ArrayList<>();
		Set<String> structureKeys = new LinkedHashSet<>();
		int successRows = 0;
		for (ImportRow row : rows) {
			List<ImportErrorDto> rowErrors = validateStructureRow(row, structureKeys);
			if (!rowErrors.isEmpty()) {
				errors.addAll(rowErrors);
				continue;
			}
			try {
				FeeCategory category = resolveCategory(row.values());
				feeService.createFeeStructure(toStructureRequest(row.values(), category.getId()));
				successRows++;
			}
			catch (BusinessException ex) {
				errors.add(new ImportErrorDto(row.rowNumber(), "row", ex.getMessage()));
			}
			catch (RuntimeException ex) {
				errors.add(new ImportErrorDto(row.rowNumber(), "row", "Unable to import row: " + ex.getMessage()));
			}
		}
		return saveImportResult("FeeStructure", batchId, rows.size(), successRows, errors);
	}

	private ImportResultDto importAssignments(List<ImportRow> rows) {
		UUID batchId = UUID.randomUUID();
		List<ImportErrorDto> errors = new ArrayList<>();
		int successRows = 0;
		for (ImportRow row : rows) {
			List<ImportErrorDto> rowErrors = validateAssignmentRow(row);
			if (!rowErrors.isEmpty()) {
				errors.addAll(rowErrors);
				continue;
			}
			try {
				Student student = studentRepository.findByAdmissionNumberIgnoreCaseAndDeletedFalse(value(row.values(), "admissionNumber"))
						.orElseThrow(() -> new ResourceNotFoundException("Student", value(row.values(), "admissionNumber")));
				feeService.assignFeeToStudent(new StudentFeeAssignmentRequest(
						student.getId(),
						UUID.fromString(value(row.values(), "feeStructureId")),
						LocalDate.parse(value(row.values(), "assignedDate")),
						blankToNull(value(row.values(), "notes"))));
				successRows++;
			}
			catch (BusinessException ex) {
				errors.add(new ImportErrorDto(row.rowNumber(), "row", ex.getMessage()));
			}
			catch (RuntimeException ex) {
				errors.add(new ImportErrorDto(row.rowNumber(), "row", "Unable to import row: " + ex.getMessage()));
			}
		}
		return saveImportResult("StudentFeeAssignment", batchId, rows.size(), successRows, errors);
	}

	private ImportResultDto saveImportResult(String entityName, UUID batchId, int totalRows, int successRows, List<ImportErrorDto> errors) {
		ImportResultDto result = ImportResultDto.of(batchId, totalRows, successRows, errors);
		importErrorReportStore.save(result);
		auditLogService.recordStandalone(new AuditLogEvent(
				"FEES",
				entityName,
				batchId.toString(),
				AuditAction.IMPORT,
				null,
				Map.of("batchId", batchId, "totalRows", totalRows, "successRows", successRows, "failedRows", errors.size())));
		return result;
	}

	private List<ImportErrorDto> validateStructureRow(ImportRow row, Set<String> structureKeys) {
		List<ImportErrorDto> errors = new ArrayList<>();
		Map<String, String> values = row.values();
		required(values, row.rowNumber(), "academicYear", errors);
		required(values, row.rowNumber(), "className", errors);
		required(values, row.rowNumber(), "name", errors);
		required(values, row.rowNumber(), "categoryCode", errors);
		required(values, row.rowNumber(), "categoryName", errors);
		required(values, row.rowNumber(), "itemAmount", errors);
		required(values, row.rowNumber(), "installmentSequence", errors);
		required(values, row.rowNumber(), "installmentTitle", errors);
		required(values, row.rowNumber(), "installmentDueDate", errors);
		required(values, row.rowNumber(), "installmentAmount", errors);
		validateDate(values, row.rowNumber(), "installmentDueDate", errors);
		validateDecimal(values, row.rowNumber(), "itemAmount", errors);
		validateDecimal(values, row.rowNumber(), "installmentAmount", errors);
		String key = (value(values, "academicYear") + "|" + value(values, "className") + "|" + value(values, "sectionName")).toLowerCase();
		if (StringUtils.hasText(value(values, "academicYear")) && StringUtils.hasText(value(values, "className")) && !structureKeys.add(key)) {
			errors.add(new ImportErrorDto(row.rowNumber(), "className", "Duplicate fee structure in import file."));
		}
		return errors;
	}

	private List<ImportErrorDto> validateAssignmentRow(ImportRow row) {
		List<ImportErrorDto> errors = new ArrayList<>();
		Map<String, String> values = row.values();
		required(values, row.rowNumber(), "admissionNumber", errors);
		required(values, row.rowNumber(), "feeStructureId", errors);
		required(values, row.rowNumber(), "assignedDate", errors);
		validateUuid(values, row.rowNumber(), "feeStructureId", errors);
		validateDate(values, row.rowNumber(), "assignedDate", errors);
		return errors;
	}

	private FeeCategory resolveCategory(Map<String, String> values) {
		String code = value(values, "categoryCode").toUpperCase();
		return feeCategoryRepository.findByCodeIgnoreCaseAndDeletedFalse(code)
				.orElseGet(() -> {
					feeService.createCategory(new FeeCategoryRequest(
							code,
							value(values, "categoryName"),
							"Imported fee category",
							true,
							0,
							true));
					return feeCategoryRepository.findByCodeIgnoreCaseAndDeletedFalse(code)
							.orElseThrow(() -> new ResourceNotFoundException("Fee category", code));
				});
	}

	private FeeStructureRequest toStructureRequest(Map<String, String> values, UUID categoryId) {
		BigDecimal amount = new BigDecimal(value(values, "itemAmount"));
		return new FeeStructureRequest(
				value(values, "academicYear"),
				value(values, "className"),
				blankToNull(value(values, "sectionName")),
				value(values, "name"),
				blankToNull(value(values, "description")),
				Boolean.parseBoolean(firstText(value(values, "activate"), "true")),
				List.of(new FeeStructureItemRequest(categoryId, amount, true, 1)),
				List.of(new FeeStructureInstallmentRequest(
						Integer.parseInt(value(values, "installmentSequence")),
						value(values, "installmentTitle"),
						LocalDate.parse(value(values, "installmentDueDate")),
						new BigDecimal(value(values, "installmentAmount")))));
	}

	private List<Map<String, Object>> structureRows() {
		return feeStructureRepository.findAllByDeletedFalse(Pageable.unpaged()).stream()
				.map(feeMapper::toStructureResponse)
				.map(this::toStructureRow)
				.toList();
	}

	private List<Map<String, Object>> assignmentRows() {
		return assignmentRepository.findAllByDeletedFalse(Pageable.unpaged()).stream()
				.map(feeMapper::toAssignmentResponse)
				.map(this::toAssignmentRow)
				.toList();
	}

	private Map<String, Object> toStructureRow(FeeStructureResponse structure) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("id", structure.id());
		row.put("academicYear", structure.academicYear());
		row.put("className", structure.className());
		row.put("sectionName", structure.sectionName());
		row.put("name", structure.name());
		row.put("status", structure.status());
		row.put("totalAmount", structure.totalAmount());
		row.put("categories", structure.items().stream()
				.map(item -> item.categoryCode() + ":" + item.amount())
				.collect(Collectors.joining("|")));
		row.put("installments", structure.installments().stream()
				.map(installment -> installment.sequenceNo() + ":" + installment.title() + ":" + installment.dueDate() + ":" + installment.amount())
				.collect(Collectors.joining("|")));
		return row;
	}

	private Map<String, Object> toAssignmentRow(StudentFeeAssignmentResponse assignment) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("id", assignment.id());
		row.put("admissionNumber", assignment.admissionNumber());
		row.put("studentName", assignment.studentName());
		row.put("feeStructureName", assignment.feeStructureName());
		row.put("academicYear", assignment.academicYear());
		row.put("className", assignment.className());
		row.put("sectionName", assignment.sectionName());
		row.put("assignedDate", assignment.assignedDate());
		row.put("grossAmount", assignment.grossAmount());
		row.put("discountAmount", assignment.discountAmount());
		row.put("lateFeeAmount", assignment.lateFeeAmount());
		row.put("paidAmount", assignment.paidAmount());
		row.put("balanceAmount", assignment.balanceAmount());
		row.put("status", assignment.status());
		return row;
	}

	private void required(Map<String, String> values, int rowNumber, String field, List<ImportErrorDto> errors) {
		if (!StringUtils.hasText(value(values, field))) {
			errors.add(new ImportErrorDto(rowNumber, field, field + " is required."));
		}
	}

	private void validateDate(Map<String, String> values, int rowNumber, String field, List<ImportErrorDto> errors) {
		if (!StringUtils.hasText(value(values, field))) {
			return;
		}
		try {
			LocalDate.parse(value(values, field));
		}
		catch (DateTimeParseException ex) {
			errors.add(new ImportErrorDto(rowNumber, field, field + " must be ISO date format yyyy-MM-dd."));
		}
	}

	private void validateUuid(Map<String, String> values, int rowNumber, String field, List<ImportErrorDto> errors) {
		if (!StringUtils.hasText(value(values, field))) {
			return;
		}
		try {
			UUID.fromString(value(values, field));
		}
		catch (IllegalArgumentException ex) {
			errors.add(new ImportErrorDto(rowNumber, field, field + " must be a valid UUID."));
		}
	}

	private void validateDecimal(Map<String, String> values, int rowNumber, String field, List<ImportErrorDto> errors) {
		if (!StringUtils.hasText(value(values, field))) {
			return;
		}
		try {
			new BigDecimal(value(values, field));
		}
		catch (NumberFormatException ex) {
			errors.add(new ImportErrorDto(rowNumber, field, field + " must be a valid amount."));
		}
	}

	private String normalizeFormat(String format) {
		return StringUtils.hasText(format) ? format.trim().toLowerCase() : "excel";
	}

	private String value(Map<String, String> values, String field) {
		return values.getOrDefault(field, "").trim();
	}

	private String blankToNull(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}

	private String firstText(String primary, String fallback) {
		return StringUtils.hasText(primary) ? primary.trim() : fallback;
	}
}
