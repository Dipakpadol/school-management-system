package com.school.erp.common.importexport;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class TemplateGeneratorService {

	private final ExcelExportService excelExportService;
	private final CsvExportService csvExportService;

	public TemplateGeneratorService(ExcelExportService excelExportService, CsvExportService csvExportService) {
		this.excelExportService = excelExportService;
		this.csvExportService = csvExportService;
	}

	public byte[] excelTemplate(String sheetName, List<String> headers) {
		return excelExportService.export(sheetName, headers, List.of());
	}

	public byte[] csvTemplate(List<String> headers) {
		return csvExportService.export(headers, List.of());
	}
}
