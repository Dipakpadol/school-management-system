package com.school.erp.modules.library.application;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import com.school.erp.common.exception.BusinessException;
import com.school.erp.common.exception.ErrorCode;
import com.school.erp.common.importexport.CsvExportService;
import com.school.erp.common.importexport.ExcelExportService;
import com.school.erp.common.importexport.PdfExportService;
import com.school.erp.modules.library.domain.LibraryBookCopyStatus;
import com.school.erp.modules.library.domain.LibraryFineStatus;
import com.school.erp.modules.library.domain.LibraryLoanStatus;
import com.school.erp.modules.library.domain.LibraryMemberType;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LibraryReportExportService {

	public static final List<String> INVENTORY_HEADERS = List.of(
			"Title",
			"ISBN",
			"Category",
			"Publisher",
			"Authors",
			"Publication Year",
			"Shelf Location",
			"Active",
			"Copies");
	public static final List<String> COPY_HEADERS = List.of(
			"Accession Number",
			"Title",
			"ISBN",
			"Status",
			"Shelf Location",
			"Acquired On",
			"Price",
			"Condition Note");
	public static final List<String> LOAN_HEADERS = List.of(
			"Loan ID",
			"Accession Number",
			"Title",
			"Membership Number",
			"Member Type",
			"Member Name",
			"Issue Date",
			"Due Date",
			"Return Date",
			"Status",
			"Overdue");
	public static final List<String> FINE_HEADERS = List.of(
			"Accession Number",
			"Title",
			"Membership Number",
			"Member Name",
			"Fine Date",
			"Amount",
			"Paid Amount",
			"Reason",
			"Status");

	private final LibraryService libraryService;
	private final ExcelExportService excelExportService;
	private final CsvExportService csvExportService;
	private final PdfExportService pdfExportService;

	@Transactional(readOnly = true)
	public byte[] inventoryReport(String format, UUID categoryId, UUID publisherId, Boolean active) {
		requireAuthority();
		return export(
				format,
				"Library Inventory Report",
				"library-inventory",
				INVENTORY_HEADERS,
				inventoryRows(categoryId, publisherId, active),
				filters(categoryId, publisherId, null, null, null, active, null, null, null));
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> inventoryRows(UUID categoryId, UUID publisherId, Boolean active) {
		requireAuthority();
		return libraryService.inventoryRows(categoryId, publisherId, active);
	}

	@Transactional(readOnly = true)
	public byte[] availableBooksReport(String format, UUID bookId, String keyword) {
		requireAuthority();
		return export(
				format,
				"Library Available Books Report",
				"library-available-books",
				COPY_HEADERS,
				availableRows(bookId, keyword),
				filters(null, null, bookId, null, null, true, null, null, null));
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> availableRows(UUID bookId, String keyword) {
		requireAuthority();
		return libraryService.copyRows(bookId, LibraryBookCopyStatus.AVAILABLE, keyword);
	}

	@Transactional(readOnly = true)
	public byte[] issuedBooksReport(
			String format,
			UUID membershipId,
			UUID bookId,
			LibraryMemberType memberType,
			LocalDate fromDate,
			LocalDate toDate) {
		requireAuthority();
		return export(
				format,
				"Library Issued Books Report",
				"library-issued-books",
				LOAN_HEADERS,
				issuedRows(membershipId, bookId, memberType, fromDate, toDate),
				filters(null, null, bookId, membershipId, memberType, null, fromDate, toDate, LibraryLoanStatus.ACTIVE));
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> issuedRows(
			UUID membershipId,
			UUID bookId,
			LibraryMemberType memberType,
			LocalDate fromDate,
			LocalDate toDate) {
		requireAuthority();
		return libraryService.loanRows(LibraryLoanStatus.ACTIVE, membershipId, bookId, memberType, fromDate, toDate, false);
	}

	@Transactional(readOnly = true)
	public byte[] overdueBooksReport(
			String format,
			UUID membershipId,
			UUID bookId,
			LibraryMemberType memberType) {
		requireAuthority();
		return export(
				format,
				"Library Overdue Books Report",
				"library-overdue-books",
				LOAN_HEADERS,
				overdueRows(membershipId, bookId, memberType),
				filters(null, null, bookId, membershipId, memberType, null, null, null, LibraryLoanStatus.ACTIVE));
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> overdueRows(UUID membershipId, UUID bookId, LibraryMemberType memberType) {
		requireAuthority();
		return libraryService.loanRows(LibraryLoanStatus.ACTIVE, membershipId, bookId, memberType, null, null, true);
	}

	@Transactional(readOnly = true)
	public byte[] memberLoanHistoryReport(String format, UUID membershipId, LocalDate fromDate, LocalDate toDate) {
		requireAuthority();
		return export(
				format,
				"Library Member Loan History",
				"library-member-history",
				LOAN_HEADERS,
				memberLoanHistoryRows(membershipId, fromDate, toDate),
				filters(null, null, null, membershipId, null, null, fromDate, toDate, null));
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> memberLoanHistoryRows(UUID membershipId, LocalDate fromDate, LocalDate toDate) {
		requireAuthority();
		if (membershipId == null) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Membership is required for member loan history.");
		}
		return libraryService.loanRows(null, membershipId, null, null, fromDate, toDate, false);
	}

	@Transactional(readOnly = true)
	public byte[] fineReport(
			String format,
			LibraryFineStatus status,
			UUID membershipId,
			LocalDate fromDate,
			LocalDate toDate) {
		requireAuthority();
		return export(
				format,
				"Library Fine Report",
				"library-fines",
				FINE_HEADERS,
				fineRows(status, membershipId, fromDate, toDate),
				filters(null, null, null, membershipId, null, null, fromDate, toDate, status));
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> fineRows(
			LibraryFineStatus status,
			UUID membershipId,
			LocalDate fromDate,
			LocalDate toDate) {
		requireAuthority();
		return libraryService.fineRows(status, membershipId, fromDate, toDate);
	}

	@Transactional(readOnly = true)
	public byte[] lostDamagedReport(String format, UUID bookId) {
		requireAuthority();
		List<Map<String, Object>> rows = lostDamagedRows(bookId);
		return export(
				format,
				"Library Lost/Damaged Report",
				"library-lost-damaged",
				COPY_HEADERS,
				rows,
				filters(null, null, bookId, null, null, null, null, null, null));
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> lostDamagedRows(UUID bookId) {
		requireAuthority();
		List<Map<String, Object>> rows = new java.util.ArrayList<>();
		rows.addAll(libraryService.copyRows(bookId, LibraryBookCopyStatus.LOST, null));
		rows.addAll(libraryService.copyRows(bookId, LibraryBookCopyStatus.DAMAGED, null));
		return rows;
	}

	private byte[] export(
			String format,
			String title,
			String sheetName,
			List<String> headers,
			List<Map<String, Object>> rows,
			Map<String, Object> filters) {
		return switch (normalizeFormat(format)) {
			case "CSV" -> csvExportService.export(headers, rows);
			case "PDF" -> pdfExportService.exportTable(title, filters, headers, rows);
			case "EXCEL" -> excelExportService.export(sheetName, headers, rows);
			default -> throw new BusinessException(ErrorCode.VALIDATION_ERROR, title + " does not support " + format + " export.");
		};
	}

	private Map<String, Object> filters(
			UUID categoryId,
			UUID publisherId,
			UUID bookId,
			UUID membershipId,
			LibraryMemberType memberType,
			Boolean active,
			LocalDate fromDate,
			LocalDate toDate,
			Object status) {
		Map<String, Object> filters = new LinkedHashMap<>();
		filters.put("Category ID", categoryId);
		filters.put("Publisher ID", publisherId);
		filters.put("Book ID", bookId);
		filters.put("Membership ID", membershipId);
		filters.put("Member Type", memberType);
		filters.put("Active", active);
		filters.put("From Date", fromDate);
		filters.put("To Date", toDate);
		filters.put("Status", status);
		return filters;
	}

	private String normalizeFormat(String format) {
		return format == null ? "EXCEL" : format.trim().toUpperCase(Locale.ROOT);
	}

	private void requireAuthority() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null
				|| !authentication.isAuthenticated()
				|| authentication instanceof AnonymousAuthenticationToken) {
			return;
		}
		boolean allowed = authentication.getAuthorities().stream()
				.anyMatch(granted -> granted.getAuthority().equals("LIBRARY_READ"));
		if (!allowed) {
			throw new BusinessException(ErrorCode.FORBIDDEN, "LIBRARY_READ permission is required.");
		}
	}
}
