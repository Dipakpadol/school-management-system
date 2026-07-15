package com.school.erp.modules.fees.api;

import java.util.UUID;

import com.school.erp.common.api.ApiResponse;
import com.school.erp.common.api.PageRequestDto;
import com.school.erp.common.api.PageResponse;
import com.school.erp.common.importexport.ImportResultDto;
import com.school.erp.common.web.CorrelationIdFilter;
import com.school.erp.modules.fees.api.dto.AssessLateFeeRequest;
import com.school.erp.modules.fees.api.dto.ClassFeeAssignmentDetailResponse;
import com.school.erp.modules.fees.api.dto.ClassFeeAssignmentRequest;
import com.school.erp.modules.fees.api.dto.ClassFeeAssignmentResponse;
import com.school.erp.modules.fees.api.dto.ClassStudentFeeResponse;
import com.school.erp.modules.fees.api.dto.DefaulterSearchRequest;
import com.school.erp.modules.fees.api.dto.FeeAssignmentSearchRequest;
import com.school.erp.modules.fees.api.dto.FeeCategoryRequest;
import com.school.erp.modules.fees.api.dto.FeeCategoryResponse;
import com.school.erp.modules.fees.api.dto.FeeDefaulterResponse;
import com.school.erp.modules.fees.api.dto.FeeDiscountRequest;
import com.school.erp.modules.fees.api.dto.FeeReceiptResponse;
import com.school.erp.modules.fees.api.dto.FeeReportRequest;
import com.school.erp.modules.fees.api.dto.FeeReportSummaryResponse;
import com.school.erp.modules.fees.api.dto.FeeStructureRequest;
import com.school.erp.modules.fees.api.dto.FeeStructureResponse;
import com.school.erp.modules.fees.api.dto.LateFeeRuleRequest;
import com.school.erp.modules.fees.api.dto.LateFeeRuleResponse;
import com.school.erp.modules.fees.api.dto.PaymentActionRequest;
import com.school.erp.modules.fees.api.dto.PaymentCollectionRequest;
import com.school.erp.modules.fees.api.dto.StudentFeeAssignmentRequest;
import com.school.erp.modules.fees.api.dto.StudentFeeAssignmentResponse;
import com.school.erp.modules.fees.api.dto.StudentFeeSummaryResponse;
import com.school.erp.modules.fees.application.FeeImportExportService;
import com.school.erp.modules.fees.application.FeeService;
import com.school.erp.modules.fees.domain.FeeStructureStatus;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.slf4j.MDC;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequestMapping("/v1/fees")
@RequiredArgsConstructor
@Tag(name = "Fees Management", description = "Fee setup, assignments, discounts, payments, receipts, defaulters, and reports.")
public class FeeController {

	private final FeeService feeService;
	private final FeeImportExportService feeImportExportService;

	@PostMapping("/categories")
	@PreAuthorize("hasAuthority('FEES_MANAGE')")
	@Operation(summary = "Create fee category")
	public ResponseEntity<ApiResponse<FeeCategoryResponse>> createCategory(
			@Valid @RequestBody FeeCategoryRequest request,
			HttpServletRequest httpRequest) {
		return created(feeService.createCategory(request), "Fee category created successfully", httpRequest);
	}

	@GetMapping("/categories")
	@PreAuthorize("hasAuthority('FEES_READ')")
	@Operation(summary = "List fee categories")
	public ResponseEntity<ApiResponse<PageResponse<FeeCategoryResponse>>> listCategories(
			@Valid @ParameterObject PageRequestDto pageRequest,
			HttpServletRequest httpRequest) {
		return ok(feeService.listCategories(pageRequest), "Fee categories fetched successfully", httpRequest);
	}

	@GetMapping("/categories/{categoryId}")
	@PreAuthorize("hasAuthority('FEES_READ')")
	@Operation(summary = "Get fee category")
	public ResponseEntity<ApiResponse<FeeCategoryResponse>> getCategory(
			@PathVariable UUID categoryId,
			HttpServletRequest httpRequest) {
		return ok(feeService.getCategory(categoryId), "Fee category fetched successfully", httpRequest);
	}

	@PutMapping("/categories/{categoryId}")
	@PreAuthorize("hasAuthority('FEES_MANAGE')")
	@Operation(summary = "Update fee category")
	public ResponseEntity<ApiResponse<FeeCategoryResponse>> updateCategory(
			@PathVariable UUID categoryId,
			@Valid @RequestBody FeeCategoryRequest request,
			HttpServletRequest httpRequest) {
		return ok(feeService.updateCategory(categoryId, request), "Fee category updated successfully", httpRequest);
	}

	@DeleteMapping("/categories/{categoryId}")
	@PreAuthorize("hasAuthority('FEES_MANAGE')")
	@Operation(summary = "Soft delete fee category")
	public ResponseEntity<ApiResponse<FeeCategoryResponse>> deleteCategory(
			@PathVariable UUID categoryId,
			HttpServletRequest httpRequest) {
		return ok(feeService.deleteCategory(categoryId), "Fee category deleted successfully", httpRequest);
	}

	@PostMapping("/structures")
	@PreAuthorize("hasAuthority('FEES_MANAGE')")
	@Operation(summary = "Create fee structure")
	public ResponseEntity<ApiResponse<FeeStructureResponse>> createFeeStructure(
			@Valid @RequestBody FeeStructureRequest request,
			HttpServletRequest httpRequest) {
		return created(feeService.createFeeStructure(request), "Fee structure created successfully", httpRequest);
	}

	@GetMapping("/structures")
	@PreAuthorize("hasAuthority('FEES_READ')")
	@Operation(summary = "List fee structures")
	public ResponseEntity<ApiResponse<PageResponse<FeeStructureResponse>>> listFeeStructures(
			@RequestParam(required = false) UUID academicYearId,
			@RequestParam(required = false) UUID classId,
			@RequestParam(required = false) FeeStructureStatus status,
			@Valid @ParameterObject PageRequestDto pageRequest,
			HttpServletRequest httpRequest) {
		return ok(feeService.listFeeStructures(academicYearId, classId, status, pageRequest), "Fee structures fetched successfully", httpRequest);
	}

	@GetMapping("/structures/{structureId}")
	@PreAuthorize("hasAuthority('FEES_READ')")
	@Operation(summary = "Get fee structure")
	public ResponseEntity<ApiResponse<FeeStructureResponse>> getFeeStructure(
			@PathVariable UUID structureId,
			HttpServletRequest httpRequest) {
		return ok(feeService.getFeeStructure(structureId), "Fee structure fetched successfully", httpRequest);
	}

	@PostMapping(value = "/structures/import/excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("hasAuthority('FEES_MANAGE')")
	@Operation(summary = "Import fee structures from Excel")
	public ResponseEntity<ApiResponse<ImportResultDto>> importStructuresExcel(
			@RequestParam("file") MultipartFile file,
			HttpServletRequest httpRequest) {
		return ok(feeImportExportService.importStructuresExcel(file), "Fee structure Excel import completed", httpRequest);
	}

	@PostMapping(value = "/structures/import/csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("hasAuthority('FEES_MANAGE')")
	@Operation(summary = "Import fee structures from CSV")
	public ResponseEntity<ApiResponse<ImportResultDto>> importStructuresCsv(
			@RequestParam("file") MultipartFile file,
			HttpServletRequest httpRequest) {
		return ok(feeImportExportService.importStructuresCsv(file), "Fee structure CSV import completed", httpRequest);
	}

	@GetMapping("/structures/export/excel")
	@PreAuthorize("hasAuthority('FEES_READ')")
	@Operation(summary = "Export fee structures to Excel")
	public ResponseEntity<byte[]> exportStructuresExcel() {
		return file(
				feeImportExportService.exportStructuresExcel(),
				"fee-structures.xlsx",
				"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
	}

	@GetMapping("/structures/export/csv")
	@PreAuthorize("hasAuthority('FEES_READ')")
	@Operation(summary = "Export fee structures to CSV")
	public ResponseEntity<byte[]> exportStructuresCsv() {
		return file(feeImportExportService.exportStructuresCsv(), "fee-structures.csv", "text/csv");
	}

	@GetMapping("/structures/template")
	@PreAuthorize("hasAuthority('FEES_READ')")
	@Operation(summary = "Download fee structure Excel template")
	public ResponseEntity<byte[]> structureTemplate() {
		return file(
				feeImportExportService.feeStructureTemplateExcel(),
				"fee-structure-import-template.xlsx",
				"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
	}

	@GetMapping("/structures/template/csv")
	@PreAuthorize("hasAuthority('FEES_READ')")
	@Operation(summary = "Download fee structure CSV template")
	public ResponseEntity<byte[]> structureCsvTemplate() {
		return file(feeImportExportService.feeStructureTemplateCsv(), "fee-structure-import-template.csv", "text/csv");
	}

	@PutMapping("/structures/{structureId}")
	@PreAuthorize("hasAuthority('FEES_MANAGE')")
	@Operation(summary = "Update fee structure")
	public ResponseEntity<ApiResponse<FeeStructureResponse>> updateFeeStructure(
			@PathVariable UUID structureId,
			@Valid @RequestBody FeeStructureRequest request,
			HttpServletRequest httpRequest) {
		return ok(feeService.updateFeeStructure(structureId, request), "Fee structure updated successfully", httpRequest);
	}

	@DeleteMapping("/structures/{structureId}")
	@PreAuthorize("hasAuthority('FEES_MANAGE')")
	@Operation(summary = "Soft delete fee structure")
	public ResponseEntity<ApiResponse<FeeStructureResponse>> deleteFeeStructure(
			@PathVariable UUID structureId,
			HttpServletRequest httpRequest) {
		return ok(feeService.deleteFeeStructure(structureId), "Fee structure deleted successfully", httpRequest);
	}

	@PostMapping("/late-fee-rules")
	@PreAuthorize("hasAuthority('FEES_MANAGE')")
	@Operation(summary = "Create late fee rule")
	public ResponseEntity<ApiResponse<LateFeeRuleResponse>> createLateFeeRule(
			@Valid @RequestBody LateFeeRuleRequest request,
			HttpServletRequest httpRequest) {
		return created(feeService.createLateFeeRule(request), "Late fee rule created successfully", httpRequest);
	}

	@GetMapping("/late-fee-rules")
	@PreAuthorize("hasAuthority('FEES_READ')")
	@Operation(summary = "List late fee rules")
	public ResponseEntity<ApiResponse<PageResponse<LateFeeRuleResponse>>> listLateFeeRules(
			@Valid @ParameterObject PageRequestDto pageRequest,
			HttpServletRequest httpRequest) {
		return ok(feeService.listLateFeeRules(pageRequest), "Late fee rules fetched successfully", httpRequest);
	}

	@GetMapping("/late-fee-rules/{ruleId}")
	@PreAuthorize("hasAuthority('FEES_READ')")
	@Operation(summary = "Get late fee rule")
	public ResponseEntity<ApiResponse<LateFeeRuleResponse>> getLateFeeRule(
			@PathVariable UUID ruleId,
			HttpServletRequest httpRequest) {
		return ok(feeService.getLateFeeRule(ruleId), "Late fee rule fetched successfully", httpRequest);
	}

	@PutMapping("/late-fee-rules/{ruleId}")
	@PreAuthorize("hasAuthority('FEES_MANAGE')")
	@Operation(summary = "Update late fee rule")
	public ResponseEntity<ApiResponse<LateFeeRuleResponse>> updateLateFeeRule(
			@PathVariable UUID ruleId,
			@Valid @RequestBody LateFeeRuleRequest request,
			HttpServletRequest httpRequest) {
		return ok(feeService.updateLateFeeRule(ruleId, request), "Late fee rule updated successfully", httpRequest);
	}

	@DeleteMapping("/late-fee-rules/{ruleId}")
	@PreAuthorize("hasAuthority('FEES_MANAGE')")
	@Operation(summary = "Soft delete late fee rule")
	public ResponseEntity<ApiResponse<LateFeeRuleResponse>> deleteLateFeeRule(
			@PathVariable UUID ruleId,
			HttpServletRequest httpRequest) {
		return ok(feeService.deleteLateFeeRule(ruleId), "Late fee rule deleted successfully", httpRequest);
	}

	@PostMapping("/assignments")
	@PreAuthorize("hasAuthority('FEES_MANAGE')")
	@Operation(summary = "Assign fee structure to student")
	public ResponseEntity<ApiResponse<StudentFeeAssignmentResponse>> assignFeeToStudent(
			@Valid @RequestBody StudentFeeAssignmentRequest request,
			HttpServletRequest httpRequest) {
		return created(feeService.assignFeeToStudent(request), "Fee assigned to student successfully", httpRequest);
	}

	@GetMapping("/assignments")
	@PreAuthorize("hasAuthority('FEES_READ')")
	@Operation(summary = "Search student fee assignments")
	public ResponseEntity<ApiResponse<PageResponse<StudentFeeAssignmentResponse>>> searchAssignments(
			@Valid @ParameterObject FeeAssignmentSearchRequest searchRequest,
			@Valid @ParameterObject PageRequestDto pageRequest,
			HttpServletRequest httpRequest) {
		return ok(feeService.searchAssignments(searchRequest, pageRequest), "Fee assignments fetched successfully", httpRequest);
	}

	@GetMapping("/classes/{classId}/students")
	@PreAuthorize("hasAuthority('FEES_READ')")
	@Operation(summary = "Get class students with fee status")
	public ResponseEntity<ApiResponse<java.util.List<ClassStudentFeeResponse>>> classStudents(
			@PathVariable UUID classId,
			@RequestParam(required = false) UUID academicYearId,
			HttpServletRequest httpRequest) {
		return ok(feeService.studentsForClass(classId, academicYearId), "Class fee students fetched successfully", httpRequest);
	}

	@GetMapping("/classes/{classId}/assignments")
	@PreAuthorize("hasAuthority('FEES_READ')")
	@Operation(summary = "Get class fee assignments")
	public ResponseEntity<ApiResponse<java.util.List<ClassFeeAssignmentDetailResponse>>> classFeeAssignments(
			@PathVariable UUID classId,
			@RequestParam(required = false) UUID academicYearId,
			HttpServletRequest httpRequest) {
		return ok(feeService.classFeeAssignments(classId, academicYearId), "Class fee assignments fetched successfully", httpRequest);
	}

	@PostMapping("/classes/{classId}/assign")
	@PreAuthorize("hasAuthority('FEES_MANAGE')")
	@Operation(summary = "Assign class fee structure to all students in a class")
	public ResponseEntity<ApiResponse<ClassFeeAssignmentResponse>> assignFeeToClass(
			@PathVariable UUID classId,
			@Valid @RequestBody ClassFeeAssignmentRequest request,
			HttpServletRequest httpRequest) {
		return created(feeService.assignFeeToClass(classId, request), "Class fee assigned successfully", httpRequest);
	}

	@GetMapping("/students/{studentId}/summary")
	@PreAuthorize("hasAuthority('FEES_READ')")
	@Operation(summary = "Get student fee summary")
	public ResponseEntity<ApiResponse<StudentFeeSummaryResponse>> studentFeeSummary(
			@PathVariable UUID studentId,
			@RequestParam(required = false) UUID academicYearId,
			HttpServletRequest httpRequest) {
		return ok(feeService.studentFeeSummary(studentId, academicYearId), "Student fee summary fetched successfully", httpRequest);
	}

	@GetMapping("/students/{studentId}/payment-history")
	@PreAuthorize("hasAuthority('FEES_READ')")
	@Operation(summary = "Get student payment history")
	public ResponseEntity<ApiResponse<java.util.List<com.school.erp.modules.fees.api.dto.FeePaymentResponse>>> studentPaymentHistory(
			@PathVariable UUID studentId,
			HttpServletRequest httpRequest) {
		return ok(feeService.studentPaymentHistory(studentId), "Student payment history fetched successfully", httpRequest);
	}

	@PostMapping("/students/{studentId}/payments")
	@PreAuthorize("hasAuthority('FEES_MANAGE')")
	@Operation(summary = "Collect payment for the latest open student fee assignment")
	public ResponseEntity<ApiResponse<FeeReceiptResponse>> collectStudentPayment(
			@PathVariable UUID studentId,
			@Valid @RequestBody PaymentCollectionRequest request,
			HttpServletRequest httpRequest) {
		return created(feeService.collectStudentPayment(studentId, request), "Payment collected successfully", httpRequest);
	}

	@GetMapping("/assignments/{assignmentId}")
	@PreAuthorize("hasAuthority('FEES_READ')")
	@Operation(summary = "Get student fee assignment")
	public ResponseEntity<ApiResponse<StudentFeeAssignmentResponse>> getAssignment(
			@PathVariable UUID assignmentId,
			HttpServletRequest httpRequest) {
		return ok(feeService.getAssignment(assignmentId), "Fee assignment fetched successfully", httpRequest);
	}

	@PostMapping(value = "/assignments/import/excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("hasAuthority('FEES_MANAGE')")
	@Operation(summary = "Import student fee assignments from Excel")
	public ResponseEntity<ApiResponse<ImportResultDto>> importAssignmentsExcel(
			@RequestParam("file") MultipartFile file,
			HttpServletRequest httpRequest) {
		return ok(feeImportExportService.importAssignmentsExcel(file), "Fee assignment Excel import completed", httpRequest);
	}

	@PostMapping(value = "/assignments/import/csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("hasAuthority('FEES_MANAGE')")
	@Operation(summary = "Import student fee assignments from CSV")
	public ResponseEntity<ApiResponse<ImportResultDto>> importAssignmentsCsv(
			@RequestParam("file") MultipartFile file,
			HttpServletRequest httpRequest) {
		return ok(feeImportExportService.importAssignmentsCsv(file), "Fee assignment CSV import completed", httpRequest);
	}

	@GetMapping("/assignments/export/excel")
	@PreAuthorize("hasAuthority('FEES_READ')")
	@Operation(summary = "Export student fee assignments to Excel")
	public ResponseEntity<byte[]> exportAssignmentsExcel() {
		return file(
				feeImportExportService.exportAssignmentsExcel(),
				"student-fee-assignments.xlsx",
				"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
	}

	@GetMapping("/assignments/export/csv")
	@PreAuthorize("hasAuthority('FEES_READ')")
	@Operation(summary = "Export student fee assignments to CSV")
	public ResponseEntity<byte[]> exportAssignmentsCsv() {
		return file(feeImportExportService.exportAssignmentsCsv(), "student-fee-assignments.csv", "text/csv");
	}

	@GetMapping("/assignments/template")
	@PreAuthorize("hasAuthority('FEES_READ')")
	@Operation(summary = "Download fee assignment Excel template")
	public ResponseEntity<byte[]> assignmentTemplate() {
		return file(
				feeImportExportService.assignmentTemplateExcel(),
				"student-fee-assignment-import-template.xlsx",
				"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
	}

	@GetMapping("/assignments/template/csv")
	@PreAuthorize("hasAuthority('FEES_READ')")
	@Operation(summary = "Download fee assignment CSV template")
	public ResponseEntity<byte[]> assignmentCsvTemplate() {
		return file(feeImportExportService.assignmentTemplateCsv(), "student-fee-assignment-import-template.csv", "text/csv");
	}

	@PostMapping("/assignments/{assignmentId}/discounts")
	@PreAuthorize("hasAuthority('FEES_MANAGE')")
	@Operation(summary = "Apply discount")
	public ResponseEntity<ApiResponse<StudentFeeAssignmentResponse>> applyDiscount(
			@PathVariable UUID assignmentId,
			@Valid @RequestBody FeeDiscountRequest request,
			HttpServletRequest httpRequest) {
		return ok(feeService.applyDiscount(assignmentId, request), "Discount applied successfully", httpRequest);
	}

	@PostMapping("/assignments/{assignmentId}/late-fees/assess")
	@PreAuthorize("hasAuthority('FEES_MANAGE')")
	@Operation(summary = "Assess late fees")
	public ResponseEntity<ApiResponse<StudentFeeAssignmentResponse>> assessLateFees(
			@PathVariable UUID assignmentId,
			@RequestBody(required = false) AssessLateFeeRequest request,
			HttpServletRequest httpRequest) {
		AssessLateFeeRequest effectiveRequest = request == null ? new AssessLateFeeRequest(null) : request;
		return ok(feeService.assessLateFees(assignmentId, effectiveRequest), "Late fees assessed successfully", httpRequest);
	}

	@PostMapping("/assignments/{assignmentId}/payments")
	@PreAuthorize("hasAuthority('FEES_MANAGE')")
	@Operation(summary = "Collect payment and generate receipt")
	public ResponseEntity<ApiResponse<FeeReceiptResponse>> collectPayment(
			@PathVariable UUID assignmentId,
			@Valid @RequestBody PaymentCollectionRequest request,
			HttpServletRequest httpRequest) {
		return created(feeService.collectPayment(assignmentId, request), "Payment collected successfully", httpRequest);
	}

	@PatchMapping("/assignments/{assignmentId}/cancel")
	@PreAuthorize("hasAuthority('FEES_MANAGE')")
	@Operation(summary = "Cancel fee assignment")
	public ResponseEntity<ApiResponse<StudentFeeAssignmentResponse>> cancelAssignment(
			@PathVariable UUID assignmentId,
			HttpServletRequest httpRequest) {
		return ok(feeService.cancelAssignment(assignmentId), "Fee assignment cancelled successfully", httpRequest);
	}

	@DeleteMapping("/assignments/{assignmentId}")
	@PreAuthorize("hasAuthority('FEES_MANAGE')")
	@Operation(summary = "Soft delete fee assignment")
	public ResponseEntity<ApiResponse<StudentFeeAssignmentResponse>> deleteAssignment(
			@PathVariable UUID assignmentId,
			HttpServletRequest httpRequest) {
		return ok(feeService.deleteAssignment(assignmentId), "Fee assignment deleted successfully", httpRequest);
	}

	@GetMapping("/receipts/{receiptNumber}")
	@PreAuthorize("hasAuthority('FEES_READ')")
	@Operation(summary = "Get fee receipt")
	public ResponseEntity<ApiResponse<FeeReceiptResponse>> getReceipt(
			@PathVariable String receiptNumber,
			HttpServletRequest httpRequest) {
		return ok(feeService.getReceipt(receiptNumber), "Fee receipt fetched successfully", httpRequest);
	}

	@GetMapping("/receipts/{receiptNumber}/pdf")
	@PreAuthorize("hasAuthority('FEES_READ')")
	@Operation(summary = "Download fee receipt PDF")
	public ResponseEntity<byte[]> receiptPdf(@PathVariable String receiptNumber) {
		return file(feeImportExportService.receiptPdf(receiptNumber), "fee-receipt-" + receiptNumber + ".pdf", "application/pdf");
	}

	@GetMapping("/payments/{paymentId}/receipt")
	@PreAuthorize("hasAuthority('FEES_READ')")
	@Operation(summary = "Download receipt PDF by payment ID")
	public ResponseEntity<byte[]> paymentReceiptPdf(@PathVariable UUID paymentId) {
		FeeReceiptResponse receipt = feeService.getPaymentReceipt(paymentId);
		return file(feeImportExportService.receiptPdf(receipt.receiptNumber()), "fee-receipt-" + receipt.receiptNumber() + ".pdf", "application/pdf");
	}

	@GetMapping("/import-errors/{batchId}")
	@PreAuthorize("hasAuthority('FEES_READ')")
	@Operation(summary = "Get fees import validation errors")
	public ResponseEntity<ApiResponse<ImportResultDto>> importErrors(
			@PathVariable UUID batchId,
			HttpServletRequest httpRequest) {
		return ok(feeImportExportService.importErrors(batchId), "Fees import errors fetched successfully", httpRequest);
	}

	@PostMapping("/payments/{paymentId}/reverse")
	@PreAuthorize("hasAuthority('FEES_MANAGE')")
	@Operation(summary = "Reverse payment")
	public ResponseEntity<ApiResponse<StudentFeeAssignmentResponse>> reversePayment(
			@PathVariable UUID paymentId,
			@RequestBody(required = false) PaymentActionRequest request,
			HttpServletRequest httpRequest) {
		return ok(feeService.reversePayment(paymentId, request), "Payment reversed successfully", httpRequest);
	}

	@PostMapping("/payments/{paymentId}/void")
	@PreAuthorize("hasAuthority('FEES_MANAGE')")
	@Operation(summary = "Void payment")
	public ResponseEntity<ApiResponse<StudentFeeAssignmentResponse>> voidPayment(
			@PathVariable UUID paymentId,
			@RequestBody(required = false) PaymentActionRequest request,
			HttpServletRequest httpRequest) {
		return ok(feeService.voidPayment(paymentId, request), "Payment voided successfully", httpRequest);
	}

	@PostMapping("/payments/{paymentId}/refund")
	@PreAuthorize("hasAuthority('FEES_MANAGE')")
	@Operation(summary = "Refund payment")
	public ResponseEntity<ApiResponse<StudentFeeAssignmentResponse>> refundPayment(
			@PathVariable UUID paymentId,
			@RequestBody(required = false) PaymentActionRequest request,
			HttpServletRequest httpRequest) {
		return ok(feeService.refundPayment(paymentId, request), "Payment refunded successfully", httpRequest);
	}

	@GetMapping("/reports/defaulters")
	@PreAuthorize("hasAuthority('FEES_READ')")
	@Operation(summary = "Get fee defaulters")
	public ResponseEntity<ApiResponse<PageResponse<FeeDefaulterResponse>>> findDefaulters(
			@Valid @ParameterObject DefaulterSearchRequest searchRequest,
			@Valid @ParameterObject PageRequestDto pageRequest,
			HttpServletRequest httpRequest) {
		return ok(feeService.findDefaulters(searchRequest, pageRequest), "Fee defaulters fetched successfully", httpRequest);
	}

	@GetMapping("/defaulters")
	@PreAuthorize("hasAuthority('FEES_READ')")
	@Operation(summary = "Get fee defaulters")
	public ResponseEntity<ApiResponse<PageResponse<FeeDefaulterResponse>>> findDefaultersAlias(
			@Valid @ParameterObject DefaulterSearchRequest searchRequest,
			@Valid @ParameterObject PageRequestDto pageRequest,
			HttpServletRequest httpRequest) {
		return findDefaulters(searchRequest, pageRequest, httpRequest);
	}

	@GetMapping("/reports/defaulters/export/{format}")
	@PreAuthorize("hasAuthority('FEES_READ')")
	@Operation(summary = "Export fee defaulters")
	public ResponseEntity<byte[]> exportDefaulters(
			@PathVariable String format,
			@Valid @ParameterObject DefaulterSearchRequest searchRequest) {
		return file(
				feeImportExportService.defaulterReport(format, searchRequest),
				"fee-defaulters." + extension(format),
				contentType(format));
	}

	@GetMapping("/defaulters/export/{format}")
	@PreAuthorize("hasAuthority('FEES_READ')")
	@Operation(summary = "Export fee defaulters")
	public ResponseEntity<byte[]> exportDefaultersAlias(
			@PathVariable String format,
			@Valid @ParameterObject DefaulterSearchRequest searchRequest) {
		return exportDefaulters(format, searchRequest);
	}

	@GetMapping("/reports/summary")
	@PreAuthorize("hasAuthority('FEES_READ')")
	@Operation(summary = "Get fee collection summary")
	public ResponseEntity<ApiResponse<FeeReportSummaryResponse>> summarizeFees(
			@Valid @ParameterObject FeeReportRequest reportRequest,
			HttpServletRequest httpRequest) {
		return ok(feeService.summarizeFees(reportRequest), "Fee summary fetched successfully", httpRequest);
	}

	@GetMapping("/reports/collection/export/{format}")
	@PreAuthorize("hasAuthority('FEES_READ')")
	@Operation(summary = "Export fee collection summary")
	public ResponseEntity<byte[]> exportCollectionSummary(@PathVariable String format) {
		return file(
				feeImportExportService.collectionReport(format),
				"fee-collection-summary." + extension(format),
				contentType(format));
	}

	private <T> ResponseEntity<ApiResponse<T>> ok(T data, String message, HttpServletRequest request) {
		return ResponseEntity.ok(response(data, message, request));
	}

	private <T> ResponseEntity<ApiResponse<T>> created(T data, String message, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(response(data, message, request));
	}

	private <T> ApiResponse<T> response(T data, String message, HttpServletRequest request) {
		return ApiResponse.success(
				data,
				message,
				request.getRequestURI(),
				MDC.get(CorrelationIdFilter.CORRELATION_ID));
	}

	private ResponseEntity<byte[]> file(byte[] content, String filename, String contentType) {
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
				.contentType(MediaType.parseMediaType(contentType))
				.body(content);
	}

	private String extension(String format) {
		return "pdf".equalsIgnoreCase(format) ? "pdf" : "csv".equalsIgnoreCase(format) ? "csv" : "xlsx";
	}

	private String contentType(String format) {
		if ("pdf".equalsIgnoreCase(format)) {
			return "application/pdf";
		}
		if ("csv".equalsIgnoreCase(format)) {
			return "text/csv";
		}
		return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
	}
}
