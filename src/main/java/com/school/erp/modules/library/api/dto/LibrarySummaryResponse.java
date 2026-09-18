package com.school.erp.modules.library.api.dto;

import java.math.BigDecimal;

public record LibrarySummaryResponse(
		long totalBooks,
		long totalCopies,
		long availableCopies,
		long issuedCopies,
		long overdueLoans,
		long activeMembers,
		long lostCopies,
		long damagedCopies,
		BigDecimal pendingFineAmount) {
}
