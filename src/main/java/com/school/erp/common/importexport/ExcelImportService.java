package com.school.erp.common.importexport;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ExcelImportService {

	public List<ImportRow> readRows(MultipartFile file) {
		try (InputStream inputStream = file.getInputStream();
				Workbook workbook = WorkbookFactory.create(inputStream)) {
			Sheet sheet = workbook.getNumberOfSheets() == 0 ? null : workbook.getSheetAt(0);
			if (sheet == null || sheet.getPhysicalNumberOfRows() == 0) {
				return List.of();
			}
			DataFormatter formatter = new DataFormatter();
			List<String> headers = headers(sheet.getRow(sheet.getFirstRowNum()), formatter);
			List<ImportRow> rows = new ArrayList<>();
			for (int rowIndex = sheet.getFirstRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
				Row row = sheet.getRow(rowIndex);
				if (row == null) {
					continue;
				}
				Map<String, String> values = new LinkedHashMap<>();
				boolean hasValue = false;
				for (int columnIndex = 0; columnIndex < headers.size(); columnIndex++) {
					String value = formatter.formatCellValue(row.getCell(columnIndex)).trim();
					values.put(headers.get(columnIndex), value);
					hasValue = hasValue || StringUtils.hasText(value);
				}
				if (hasValue) {
					rows.add(new ImportRow(rowIndex + 1, values));
				}
			}
			return rows;
		}
		catch (IOException | RuntimeException ex) {
			throw new ImportValidationException(ImportResultDto.of(
					java.util.UUID.randomUUID(),
					0,
					0,
					List.of(new ImportErrorDto(0, "file", "Unable to read Excel file: " + ex.getMessage()))));
		}
	}

	private List<String> headers(Row headerRow, DataFormatter formatter) {
		if (headerRow == null) {
			return List.of();
		}
		List<String> headers = new ArrayList<>();
		for (int columnIndex = 0; columnIndex < headerRow.getLastCellNum(); columnIndex++) {
			String header = formatter.formatCellValue(headerRow.getCell(columnIndex)).trim();
			if (StringUtils.hasText(header)) {
				headers.add(header);
			}
		}
		return headers;
	}
}
