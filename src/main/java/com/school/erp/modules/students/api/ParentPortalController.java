package com.school.erp.modules.students.api;

import java.util.List;

import com.school.erp.common.api.ApiResponse;
import com.school.erp.common.web.CorrelationIdFilter;
import com.school.erp.modules.students.api.dto.StudentSummaryResponse;
import com.school.erp.modules.students.application.StudentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequestMapping("/v1/parents")
@RequiredArgsConstructor
@Tag(name = "Parent Portal", description = "Scoped parent access to linked student records.")
public class ParentPortalController {

	private final StudentService studentService;

	@GetMapping("/me/children")
	@PreAuthorize("hasAuthority('STUDENTS_READ')")
	@Operation(summary = "Get current parent's linked children")
	public ResponseEntity<ApiResponse<List<StudentSummaryResponse>>> myChildren(HttpServletRequest request) {
		return ResponseEntity.ok(ApiResponse.success(
				studentService.getMyChildren(),
				"Parent children fetched successfully",
				request.getRequestURI(),
				MDC.get(CorrelationIdFilter.CORRELATION_ID)));
	}
}
