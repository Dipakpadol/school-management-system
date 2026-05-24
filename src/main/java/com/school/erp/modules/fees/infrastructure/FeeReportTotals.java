package com.school.erp.modules.fees.infrastructure;

import java.math.BigDecimal;

public interface FeeReportTotals {

	long getAssignments();

	BigDecimal getGrossAmount();

	BigDecimal getDiscountAmount();

	BigDecimal getLateFeeAmount();

	BigDecimal getPaidAmount();

	BigDecimal getBalanceAmount();
}
