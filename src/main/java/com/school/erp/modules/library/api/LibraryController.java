package com.school.erp.modules.library.api;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.school.erp.common.api.ApiResponse;
import com.school.erp.common.api.PageRequestDto;
import com.school.erp.common.api.PageResponse;
import com.school.erp.common.web.CorrelationIdFilter;
import com.school.erp.modules.library.api.dto.LibraryAuthorRequest;
import com.school.erp.modules.library.api.dto.LibraryAuthorResponse;
import com.school.erp.modules.library.api.dto.LibraryBookCopyRequest;
import com.school.erp.modules.library.api.dto.LibraryBookCopyResponse;
import com.school.erp.modules.library.api.dto.LibraryBookRequest;
import com.school.erp.modules.library.api.dto.LibraryBookResponse;
import com.school.erp.modules.library.api.dto.LibraryCategoryRequest;
import com.school.erp.modules.library.api.dto.LibraryCategoryResponse;
import com.school.erp.modules.library.api.dto.LibraryFinePaymentRequest;
import com.school.erp.modules.library.api.dto.LibraryFineResponse;
import com.school.erp.modules.library.api.dto.LibraryIssueRequest;
import com.school.erp.modules.library.api.dto.LibraryLoanResponse;
import com.school.erp.modules.library.api.dto.LibraryMembershipRequest;
import com.school.erp.modules.library.api.dto.LibraryMembershipResponse;
import com.school.erp.modules.library.api.dto.LibraryPublisherRequest;
import com.school.erp.modules.library.api.dto.LibraryPublisherResponse;
import com.school.erp.modules.library.api.dto.LibraryReturnRequest;
import com.school.erp.modules.library.api.dto.LibrarySummaryResponse;
import com.school.erp.modules.library.application.LibraryService;
import com.school.erp.modules.library.domain.LibraryBookCopyStatus;
import com.school.erp.modules.library.domain.LibraryFineStatus;
import com.school.erp.modules.library.domain.LibraryLoanStatus;
import com.school.erp.modules.library.domain.LibraryMemberType;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.slf4j.MDC;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequestMapping("/v1/library")
@RequiredArgsConstructor
@Tag(name = "Library", description = "Library catalog, inventory copies, memberships, circulation, fines, and summaries.")
public class LibraryController {

	private final LibraryService libraryService;

	@GetMapping("/summary")
	@PreAuthorize("hasAuthority('LIBRARY_READ')")
	public ResponseEntity<ApiResponse<LibrarySummaryResponse>> summary(HttpServletRequest request) {
		return ok(libraryService.summary(), "Library summary fetched successfully", request);
	}

	@GetMapping("/categories")
	@PreAuthorize("hasAuthority('LIBRARY_READ')")
	public ResponseEntity<ApiResponse<List<LibraryCategoryResponse>>> categories(HttpServletRequest request) {
		return ok(libraryService.categories(), "Library categories fetched successfully", request);
	}

	@PostMapping("/categories")
	@PreAuthorize("hasAuthority('LIBRARY_CREATE')")
	public ResponseEntity<ApiResponse<LibraryCategoryResponse>> createCategory(
			@Valid @RequestBody LibraryCategoryRequest body,
			HttpServletRequest request) {
		return created(libraryService.createCategory(body), "Library category created successfully", request);
	}

	@PutMapping("/categories/{categoryId}")
	@PreAuthorize("hasAuthority('LIBRARY_UPDATE')")
	public ResponseEntity<ApiResponse<LibraryCategoryResponse>> updateCategory(
			@PathVariable UUID categoryId,
			@Valid @RequestBody LibraryCategoryRequest body,
			HttpServletRequest request) {
		return ok(libraryService.updateCategory(categoryId, body), "Library category updated successfully", request);
	}

	@GetMapping("/authors")
	@PreAuthorize("hasAuthority('LIBRARY_READ')")
	public ResponseEntity<ApiResponse<List<LibraryAuthorResponse>>> authors(HttpServletRequest request) {
		return ok(libraryService.authors(), "Library authors fetched successfully", request);
	}

	@PostMapping("/authors")
	@PreAuthorize("hasAuthority('LIBRARY_CREATE')")
	public ResponseEntity<ApiResponse<LibraryAuthorResponse>> createAuthor(
			@Valid @RequestBody LibraryAuthorRequest body,
			HttpServletRequest request) {
		return created(libraryService.createAuthor(body), "Library author created successfully", request);
	}

	@PutMapping("/authors/{authorId}")
	@PreAuthorize("hasAuthority('LIBRARY_UPDATE')")
	public ResponseEntity<ApiResponse<LibraryAuthorResponse>> updateAuthor(
			@PathVariable UUID authorId,
			@Valid @RequestBody LibraryAuthorRequest body,
			HttpServletRequest request) {
		return ok(libraryService.updateAuthor(authorId, body), "Library author updated successfully", request);
	}

	@GetMapping("/publishers")
	@PreAuthorize("hasAuthority('LIBRARY_READ')")
	public ResponseEntity<ApiResponse<List<LibraryPublisherResponse>>> publishers(HttpServletRequest request) {
		return ok(libraryService.publishers(), "Library publishers fetched successfully", request);
	}

	@PostMapping("/publishers")
	@PreAuthorize("hasAuthority('LIBRARY_CREATE')")
	public ResponseEntity<ApiResponse<LibraryPublisherResponse>> createPublisher(
			@Valid @RequestBody LibraryPublisherRequest body,
			HttpServletRequest request) {
		return created(libraryService.createPublisher(body), "Library publisher created successfully", request);
	}

	@PutMapping("/publishers/{publisherId}")
	@PreAuthorize("hasAuthority('LIBRARY_UPDATE')")
	public ResponseEntity<ApiResponse<LibraryPublisherResponse>> updatePublisher(
			@PathVariable UUID publisherId,
			@Valid @RequestBody LibraryPublisherRequest body,
			HttpServletRequest request) {
		return ok(libraryService.updatePublisher(publisherId, body), "Library publisher updated successfully", request);
	}

	@GetMapping("/books")
	@PreAuthorize("hasAuthority('LIBRARY_READ')")
	public ResponseEntity<ApiResponse<PageResponse<LibraryBookResponse>>> books(
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) UUID categoryId,
			@RequestParam(required = false) UUID publisherId,
			@RequestParam(required = false) Boolean active,
			@Valid @ParameterObject PageRequestDto pageRequest,
			HttpServletRequest request) {
		return ok(
				libraryService.books(keyword, categoryId, publisherId, active, pageRequest),
				"Library books fetched successfully",
				request);
	}

	@PostMapping("/books")
	@PreAuthorize("hasAuthority('LIBRARY_CREATE')")
	public ResponseEntity<ApiResponse<LibraryBookResponse>> createBook(
			@Valid @RequestBody LibraryBookRequest body,
			HttpServletRequest request) {
		return created(libraryService.createBook(body), "Library book created successfully", request);
	}

	@GetMapping("/books/{bookId}")
	@PreAuthorize("hasAuthority('LIBRARY_READ')")
	public ResponseEntity<ApiResponse<LibraryBookResponse>> getBook(
			@PathVariable UUID bookId,
			HttpServletRequest request) {
		return ok(libraryService.getBook(bookId), "Library book fetched successfully", request);
	}

	@PutMapping("/books/{bookId}")
	@PreAuthorize("hasAuthority('LIBRARY_UPDATE')")
	public ResponseEntity<ApiResponse<LibraryBookResponse>> updateBook(
			@PathVariable UUID bookId,
			@Valid @RequestBody LibraryBookRequest body,
			HttpServletRequest request) {
		return ok(libraryService.updateBook(bookId, body), "Library book updated successfully", request);
	}

	@PatchMapping("/books/{bookId}/deactivate")
	@PreAuthorize("hasAuthority('LIBRARY_DELETE')")
	public ResponseEntity<ApiResponse<LibraryBookResponse>> deactivateBook(
			@PathVariable UUID bookId,
			HttpServletRequest request) {
		return ok(libraryService.deactivateBook(bookId), "Library book deactivated successfully", request);
	}

	@GetMapping("/copies")
	@PreAuthorize("hasAuthority('LIBRARY_READ')")
	public ResponseEntity<ApiResponse<PageResponse<LibraryBookCopyResponse>>> copies(
			@RequestParam(required = false) UUID bookId,
			@RequestParam(required = false) LibraryBookCopyStatus status,
			@RequestParam(required = false) String keyword,
			@Valid @ParameterObject PageRequestDto pageRequest,
			HttpServletRequest request) {
		return ok(libraryService.copies(bookId, status, keyword, pageRequest), "Library copies fetched successfully", request);
	}

	@PostMapping("/copies")
	@PreAuthorize("hasAuthority('LIBRARY_CREATE')")
	public ResponseEntity<ApiResponse<LibraryBookCopyResponse>> createCopy(
			@Valid @RequestBody LibraryBookCopyRequest body,
			HttpServletRequest request) {
		return created(libraryService.createCopy(body), "Library copy created successfully", request);
	}

	@PutMapping("/copies/{copyId}")
	@PreAuthorize("hasAuthority('LIBRARY_UPDATE')")
	public ResponseEntity<ApiResponse<LibraryBookCopyResponse>> updateCopy(
			@PathVariable UUID copyId,
			@Valid @RequestBody LibraryBookCopyRequest body,
			HttpServletRequest request) {
		return ok(libraryService.updateCopy(copyId, body), "Library copy updated successfully", request);
	}

	@PatchMapping("/copies/{copyId}/lost")
	@PreAuthorize("hasAuthority('LIBRARY_UPDATE')")
	public ResponseEntity<ApiResponse<LibraryBookCopyResponse>> markCopyLost(
			@PathVariable UUID copyId,
			@RequestBody(required = false) Map<String, String> body,
			HttpServletRequest request) {
		return ok(libraryService.markCopyLost(copyId, note(body)), "Library copy marked lost successfully", request);
	}

	@PatchMapping("/copies/{copyId}/damaged")
	@PreAuthorize("hasAuthority('LIBRARY_UPDATE')")
	public ResponseEntity<ApiResponse<LibraryBookCopyResponse>> markCopyDamaged(
			@PathVariable UUID copyId,
			@RequestBody(required = false) Map<String, String> body,
			HttpServletRequest request) {
		return ok(libraryService.markCopyDamaged(copyId, note(body)), "Library copy marked damaged successfully", request);
	}

	@PatchMapping("/copies/{copyId}/available")
	@PreAuthorize("hasAuthority('LIBRARY_UPDATE')")
	public ResponseEntity<ApiResponse<LibraryBookCopyResponse>> markCopyAvailable(
			@PathVariable UUID copyId,
			HttpServletRequest request) {
		return ok(libraryService.markCopyAvailable(copyId), "Library copy marked available successfully", request);
	}

	@GetMapping("/memberships")
	@PreAuthorize("hasAuthority('LIBRARY_READ')")
	public ResponseEntity<ApiResponse<PageResponse<LibraryMembershipResponse>>> memberships(
			@RequestParam(required = false) LibraryMemberType memberType,
			@RequestParam(required = false) Boolean active,
			@RequestParam(required = false) String keyword,
			@Valid @ParameterObject PageRequestDto pageRequest,
			HttpServletRequest request) {
		return ok(libraryService.memberships(memberType, active, keyword, pageRequest), "Library memberships fetched successfully", request);
	}

	@PostMapping("/memberships")
	@PreAuthorize("hasAuthority('LIBRARY_CREATE')")
	public ResponseEntity<ApiResponse<LibraryMembershipResponse>> createMembership(
			@Valid @RequestBody LibraryMembershipRequest body,
			HttpServletRequest request) {
		return created(libraryService.createMembership(body), "Library membership created successfully", request);
	}

	@PutMapping("/memberships/{membershipId}")
	@PreAuthorize("hasAuthority('LIBRARY_UPDATE')")
	public ResponseEntity<ApiResponse<LibraryMembershipResponse>> updateMembership(
			@PathVariable UUID membershipId,
			@Valid @RequestBody LibraryMembershipRequest body,
			HttpServletRequest request) {
		return ok(libraryService.updateMembership(membershipId, body), "Library membership updated successfully", request);
	}

	@PatchMapping("/memberships/{membershipId}/deactivate")
	@PreAuthorize("hasAuthority('LIBRARY_DELETE')")
	public ResponseEntity<ApiResponse<LibraryMembershipResponse>> deactivateMembership(
			@PathVariable UUID membershipId,
			HttpServletRequest request) {
		return ok(libraryService.deactivateMembership(membershipId), "Library membership deactivated successfully", request);
	}

	@GetMapping("/loans")
	@PreAuthorize("hasAuthority('LIBRARY_READ')")
	public ResponseEntity<ApiResponse<PageResponse<LibraryLoanResponse>>> loans(
			@RequestParam(required = false) LibraryLoanStatus status,
			@RequestParam(required = false) UUID membershipId,
			@RequestParam(required = false) UUID bookId,
			@RequestParam(required = false) UUID copyId,
			@RequestParam(required = false) LibraryMemberType memberType,
			@RequestParam(required = false) LocalDate fromDate,
			@RequestParam(required = false) LocalDate toDate,
			@RequestParam(defaultValue = "false") boolean overdueOnly,
			@Valid @ParameterObject PageRequestDto pageRequest,
			HttpServletRequest request) {
		return ok(
				libraryService.loans(status, membershipId, bookId, copyId, memberType, fromDate, toDate, overdueOnly, pageRequest),
				"Library loans fetched successfully",
				request);
	}

	@PostMapping("/loans/issue")
	@PreAuthorize("hasAuthority('LIBRARY_ISSUE')")
	public ResponseEntity<ApiResponse<LibraryLoanResponse>> issue(
			@Valid @RequestBody LibraryIssueRequest body,
			HttpServletRequest request) {
		return created(libraryService.issue(body), "Library book issued successfully", request);
	}

	@PatchMapping("/loans/{loanId}/return")
	@PreAuthorize("hasAuthority('LIBRARY_RETURN')")
	public ResponseEntity<ApiResponse<LibraryLoanResponse>> returnLoan(
			@PathVariable UUID loanId,
			@RequestBody(required = false) LibraryReturnRequest body,
			HttpServletRequest request) {
		return ok(libraryService.returnLoan(loanId, body), "Library book returned successfully", request);
	}

	@PatchMapping("/loans/{loanId}/lost")
	@PreAuthorize("hasAuthority('LIBRARY_RETURN')")
	public ResponseEntity<ApiResponse<LibraryLoanResponse>> markLoanLost(
			@PathVariable UUID loanId,
			@RequestBody(required = false) Map<String, String> body,
			HttpServletRequest request) {
		return ok(libraryService.markLoanLost(loanId, note(body)), "Library loan marked lost successfully", request);
	}

	@GetMapping("/fines")
	@PreAuthorize("hasAuthority('LIBRARY_READ')")
	public ResponseEntity<ApiResponse<PageResponse<LibraryFineResponse>>> fines(
			@RequestParam(required = false) LibraryFineStatus status,
			@RequestParam(required = false) UUID membershipId,
			@RequestParam(required = false) LocalDate fromDate,
			@RequestParam(required = false) LocalDate toDate,
			@Valid @ParameterObject PageRequestDto pageRequest,
			HttpServletRequest request) {
		return ok(libraryService.fines(status, membershipId, fromDate, toDate, pageRequest), "Library fines fetched successfully", request);
	}

	@PatchMapping("/fines/{fineId}/pay")
	@PreAuthorize("hasAuthority('LIBRARY_FINE')")
	public ResponseEntity<ApiResponse<LibraryFineResponse>> payFine(
			@PathVariable UUID fineId,
			@RequestBody(required = false) LibraryFinePaymentRequest body,
			HttpServletRequest request) {
		return ok(libraryService.payFine(fineId, body), "Library fine paid successfully", request);
	}

	@PatchMapping("/fines/{fineId}/waive")
	@PreAuthorize("hasAuthority('LIBRARY_FINE')")
	public ResponseEntity<ApiResponse<LibraryFineResponse>> waiveFine(
			@PathVariable UUID fineId,
			HttpServletRequest request) {
		return ok(libraryService.waiveFine(fineId), "Library fine waived successfully", request);
	}

	private String note(Map<String, String> body) {
		if (body == null) {
			return null;
		}
		return body.getOrDefault("note", body.get("remarks"));
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
