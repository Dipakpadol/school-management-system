package com.school.erp.common.importexport;

import java.util.Map;

public record ImportRow(
		int rowNumber,
		Map<String, String> values) {
}
