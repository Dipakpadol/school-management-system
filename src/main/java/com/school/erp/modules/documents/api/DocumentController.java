package com.school.erp.modules.documents.api;

import java.util.List;
import java.util.UUID;

import com.school.erp.common.api.ApiResponse;
import com.school.erp.common.web.CorrelationIdFilter;
import com.school.erp.modules.documents.api.dto.CertificateGenerationRequest;
import com.school.erp.modules.documents.api.dto.GeneratedDocumentResponse;
import com.school.erp.modules.documents.application.GeneratedDocumentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@Tag(name = "Documents", description = "Generated certificates and document history.")
public class DocumentController {

	private final GeneratedDocumentService generatedDocumentService;

	@PostMapping("/documents/certificates/preview")
	@PreAuthorize("hasAuthority('STUDENTS_READ')")
	@Operation(summary = "Preview generated certificate PDF")
	public ResponseEntity<byte[]> previewCertificate(@Valid @RequestBody CertificateGenerationRequest request) {
		var document = generatedDocumentService.preview(request);
		return file(document.content(), "preview-" + document.filename(), document.contentType());
	}

	@PostMapping("/documents/certificates/generate")
	@PreAuthorize("hasAuthority('STUDENTS_UPDATE')")
	@Operation(summary = "Generate certificate from student data")
	public ResponseEntity<ApiResponse<GeneratedDocumentResponse>> generateCertificate(
			@Valid @RequestBody CertificateGenerationRequest request,
			HttpServletRequest httpRequest) {
		return created(generatedDocumentService.generate(request), "Certificate generated successfully", httpRequest);
	}

	@GetMapping("/students/{studentId}/documents/generated")
	@PreAuthorize("hasAuthority('STUDENTS_READ')")
	@Operation(summary = "Get generated document history for student")
	public ResponseEntity<ApiResponse<List<GeneratedDocumentResponse>>> generatedHistory(
			@PathVariable UUID studentId,
			HttpServletRequest httpRequest) {
		return ok(generatedDocumentService.history(studentId), "Generated documents fetched successfully", httpRequest);
	}

	@GetMapping("/documents/{documentId}")
	@PreAuthorize("hasAuthority('STUDENTS_READ')")
	@Operation(summary = "Get generated document metadata")
	public ResponseEntity<ApiResponse<GeneratedDocumentResponse>> getDocument(
			@PathVariable UUID documentId,
			HttpServletRequest httpRequest) {
		return ok(generatedDocumentService.get(documentId), "Generated document fetched successfully", httpRequest);
	}

	@GetMapping("/documents/{documentId}/download")
	@PreAuthorize("hasAuthority('STUDENTS_READ')")
	@Operation(summary = "Download generated document PDF")
	public ResponseEntity<byte[]> download(@PathVariable UUID documentId) {
		var document = generatedDocumentService.download(documentId);
		return file(document.content(), document.filename(), document.contentType());
	}

	@PostMapping("/documents/{documentId}/reprint")
	@PreAuthorize("hasAuthority('STUDENTS_UPDATE')")
	@Operation(summary = "Create reprint history entry")
	public ResponseEntity<ApiResponse<GeneratedDocumentResponse>> reprint(
			@PathVariable UUID documentId,
			HttpServletRequest httpRequest) {
		return created(generatedDocumentService.reprint(documentId), "Certificate reprint recorded successfully", httpRequest);
	}

	private <T> ResponseEntity<ApiResponse<T>> ok(T data, String message, HttpServletRequest request) {
		return ResponseEntity.ok(ApiResponse.success(data, message, request.getRequestURI(), correlationId()));
	}

	private <T> ResponseEntity<ApiResponse<T>> created(T data, String message, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success(data, message, request.getRequestURI(), correlationId()));
	}

	private String correlationId() {
		return MDC.get(CorrelationIdFilter.CORRELATION_ID);
	}

	private ResponseEntity<byte[]> file(byte[] bytes, String filename, String contentType) {
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
				.header(HttpHeaders.CONTENT_TYPE, contentType)
				.body(bytes);
	}
}
