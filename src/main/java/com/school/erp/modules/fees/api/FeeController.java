package com.school.erp.modules.fees.api;

import java.util.UUID;

import com.school.erp.common.api.ApiResponse;
import com.school.erp.common.api.PageRequestDto;
import com.school.erp.common.api.PageResponse;
import com.school.erp.common.web.CorrelationIdFilter;
import com.school.erp.modules.fees.api.dto.AssessLateFeeRequest;
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
import com.school.erp.modules.fees.api.dto.PaymentCollectionRequest;
import com.school.erp.modules.fees.api.dto.StudentFeeAssignmentRequest;
import com.school.erp.modules.fees.api.dto.StudentFeeAssignmentResponse;
import com.school.erp.modules.fees.application.FeeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.slf4j.MDC;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequestMapping("/v1/fees")
@RequiredArgsConstructor
@Tag(name = "Fees Management", description = "Fee setup, assignments, discounts, payments, receipts, defaulters, and reports.")
public class FeeController {

	private final FeeService feeService;

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
			@Valid @ParameterObject PageRequestDto pageRequest,
			HttpServletRequest httpRequest) {
		return ok(feeService.listFeeStructures(pageRequest), "Fee structures fetched successfully", httpRequest);
	}

	@GetMapping("/structures/{structureId}")
	@PreAuthorize("hasAuthority('FEES_READ')")
	@Operation(summary = "Get fee structure")
	public ResponseEntity<ApiResponse<FeeStructureResponse>> getFeeStructure(
			@PathVariable UUID structureId,
			HttpServletRequest httpRequest) {
		return ok(feeService.getFeeStructure(structureId), "Fee structure fetched successfully", httpRequest);
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

	@GetMapping("/assignments/{assignmentId}")
	@PreAuthorize("hasAuthority('FEES_READ')")
	@Operation(summary = "Get student fee assignment")
	public ResponseEntity<ApiResponse<StudentFeeAssignmentResponse>> getAssignment(
			@PathVariable UUID assignmentId,
			HttpServletRequest httpRequest) {
		return ok(feeService.getAssignment(assignmentId), "Fee assignment fetched successfully", httpRequest);
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

	@GetMapping("/reports/defaulters")
	@PreAuthorize("hasAuthority('FEES_READ')")
	@Operation(summary = "Get fee defaulters")
	public ResponseEntity<ApiResponse<PageResponse<FeeDefaulterResponse>>> findDefaulters(
			@Valid @ParameterObject DefaulterSearchRequest searchRequest,
			@Valid @ParameterObject PageRequestDto pageRequest,
			HttpServletRequest httpRequest) {
		return ok(feeService.findDefaulters(searchRequest, pageRequest), "Fee defaulters fetched successfully", httpRequest);
	}

	@GetMapping("/reports/summary")
	@PreAuthorize("hasAuthority('FEES_READ')")
	@Operation(summary = "Get fee collection summary")
	public ResponseEntity<ApiResponse<FeeReportSummaryResponse>> summarizeFees(
			@Valid @ParameterObject FeeReportRequest reportRequest,
			HttpServletRequest httpRequest) {
		return ok(feeService.summarizeFees(reportRequest), "Fee summary fetched successfully", httpRequest);
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
}
