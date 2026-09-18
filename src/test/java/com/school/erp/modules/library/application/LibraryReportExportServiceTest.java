package com.school.erp.modules.library.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.school.erp.common.exception.BusinessException;
import com.school.erp.common.exception.ErrorCode;
import com.school.erp.common.importexport.CsvExportService;
import com.school.erp.common.importexport.ExcelExportService;
import com.school.erp.common.importexport.PdfExportService;
import com.school.erp.modules.library.domain.LibraryFineStatus;
import com.school.erp.modules.library.domain.LibraryMemberType;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class LibraryReportExportServiceTest {

	@Mock
	private LibraryService libraryService;

	@Mock
	private ExcelExportService excelExportService;

	@Mock
	private CsvExportService csvExportService;

	@Mock
	private PdfExportService pdfExportService;

	private LibraryReportExportService reportExportService;

	@BeforeEach
	void setUp() {
		reportExportService = new LibraryReportExportService(
				libraryService,
				excelExportService,
				csvExportService,
				pdfExportService);
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void inventoryPdfUsesCanonicalRowsAndPdfExporter() {
		authenticate("LIBRARY_READ");
		UUID categoryId = UUID.randomUUID();
		List<Map<String, Object>> rows = List.of(Map.of(
				"Title", "Malgudi Days",
				"Copies", 3));
		when(libraryService.inventoryRows(categoryId, null, true)).thenReturn(rows);
		when(pdfExportService.exportTable(
				eq("Library Inventory Report"),
				anyMap(),
				eq(LibraryReportExportService.INVENTORY_HEADERS),
				eq(rows)))
				.thenReturn(bytes("inventory-pdf"));

		var content = reportExportService.inventoryReport("PDF", categoryId, null, true);

		assertThat(content).isEqualTo(bytes("inventory-pdf"));
	}

	@Test
	void issuedBooksCsvUsesLoanRows() {
		authenticate("LIBRARY_READ");
		UUID membershipId = UUID.randomUUID();
		LocalDate fromDate = LocalDate.of(2026, 9, 1);
		LocalDate toDate = LocalDate.of(2026, 9, 30);
		List<Map<String, Object>> rows = List.of(Map.of(
				"Membership Number", "LIB-M-001",
				"Status", "ACTIVE"));
		when(libraryService.loanRows(
				com.school.erp.modules.library.domain.LibraryLoanStatus.ACTIVE,
				membershipId,
				null,
				LibraryMemberType.STUDENT,
				fromDate,
				toDate,
				false))
				.thenReturn(rows);
		when(csvExportService.export(LibraryReportExportService.LOAN_HEADERS, rows)).thenReturn(bytes("issued-csv"));

		var content = reportExportService.issuedBooksReport(
				"CSV",
				membershipId,
				null,
				LibraryMemberType.STUDENT,
				fromDate,
				toDate);

		assertThat(content).isEqualTo(bytes("issued-csv"));
	}

	@Test
	void fineExcelDelegatesStatusFilters() {
		authenticate("LIBRARY_READ");
		UUID membershipId = UUID.randomUUID();
		List<Map<String, Object>> rows = List.of(Map.of(
				"Membership Number", "LIB-M-001",
				"Status", "PENDING"));
		when(libraryService.fineRows(LibraryFineStatus.PENDING, membershipId, null, null)).thenReturn(rows);
		when(excelExportService.export("library-fines", LibraryReportExportService.FINE_HEADERS, rows))
				.thenReturn(bytes("fines-xlsx"));

		var content = reportExportService.fineReport("EXCEL", LibraryFineStatus.PENDING, membershipId, null, null);

		assertThat(content).isEqualTo(bytes("fines-xlsx"));
	}

	@Test
	void memberLoanHistoryRequiresMembership() {
		authenticate("LIBRARY_READ");

		assertThatThrownBy(() -> reportExportService.memberLoanHistoryRows(null, null, null))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.VALIDATION_ERROR);
	}

	@Test
	void libraryReportsRequireLibraryReadAuthority() {
		authenticate("REPORTS_READ");

		assertThatThrownBy(() -> reportExportService.availableBooksReport("CSV", null, null))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.FORBIDDEN);
	}

	private byte[] bytes(String value) {
		return value.getBytes(StandardCharsets.UTF_8);
	}

	private void authenticate(String... authorities) {
		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
				"library-user",
				null,
				java.util.Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList()));
	}
}
