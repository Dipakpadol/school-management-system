package com.school.erp.modules.reports.api.dto;

import java.util.List;

public record ReportOptionsResponse(List<ReportTypeOption> reportTypes) {

	public ReportOptionsResponse {
		reportTypes = reportTypes == null ? List.of() : List.copyOf(reportTypes);
	}

	public record ReportTypeOption(
			String code,
			String name,
			List<String> formats,
			List<ReportFilterOption> filters) {

		public ReportTypeOption {
			formats = formats == null ? List.of() : List.copyOf(formats);
			filters = filters == null ? List.of() : List.copyOf(filters);
		}
	}

	public record ReportFilterOption(
			String code,
			String label,
			String type,
			boolean required) {
	}
}
