package com.school.erp.common.importexport;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

@Service
public class ExcelExportService {

	public byte[] export(String sheetName, List<String> headers, List<Map<String, Object>> rows) {
		try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
			Sheet sheet = workbook.createSheet(safeSheetName(sheetName));
			CellStyle headerStyle = workbook.createCellStyle();
			Font font = workbook.createFont();
			font.setBold(true);
			headerStyle.setFont(font);

			Row headerRow = sheet.createRow(0);
			for (int index = 0; index < headers.size(); index++) {
				Cell cell = headerRow.createCell(index);
				cell.setCellValue(headers.get(index));
				cell.setCellStyle(headerStyle);
			}

			for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
				Row dataRow = sheet.createRow(rowIndex + 1);
				Map<String, Object> row = rows.get(rowIndex);
				for (int columnIndex = 0; columnIndex < headers.size(); columnIndex++) {
					dataRow.createCell(columnIndex).setCellValue(stringValue(row.get(headers.get(columnIndex))));
				}
			}

			for (int index = 0; index < headers.size(); index++) {
				sheet.autoSizeColumn(index);
			}
			workbook.write(outputStream);
			return outputStream.toByteArray();
		}
		catch (IOException ex) {
			throw new IllegalStateException("Unable to generate Excel export.", ex);
		}
	}

	private String safeSheetName(String sheetName) {
		if (sheetName == null || sheetName.isBlank()) {
			return "Export";
		}
		return sheetName.replaceAll("[\\\\/?*\\[\\]:]", "_");
	}

	private String stringValue(Object value) {
		return value == null ? "" : String.valueOf(value);
	}
}
