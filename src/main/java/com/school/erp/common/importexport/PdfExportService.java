package com.school.erp.common.importexport;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.school.erp.modules.settings.application.ApplicationSettingsService;

import org.springframework.stereotype.Service;

@Service
public class PdfExportService {

	private static final String SCHOOL_NAME = "Star International School";
	private static final ZoneId SCHOOL_ZONE = ZoneId.of("Asia/Kolkata");
	private static final DateTimeFormatter GENERATED_AT_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

	private final ApplicationSettingsService settingsService;

	public PdfExportService(ApplicationSettingsService settingsService) {
		this.settingsService = settingsService;
	}

	public byte[] exportTable(String title, List<String> headers, List<Map<String, Object>> rows) {
		return exportTable(title, Map.of(), headers, rows);
	}

	public byte[] exportTable(
			String title,
			Map<String, Object> appliedFilters,
			List<String> headers,
			List<Map<String, Object>> rows) {
		try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
			Document document = new Document(PageSize.A4.rotate(), 24, 24, 24, 24);
			PdfWriter.getInstance(document, outputStream);
			document.open();
			Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
			Paragraph heading = new Paragraph(title == null ? "Export" : title, titleFont);
			heading.setSpacingAfter(8);
			document.add(heading);
			addMetadata(document, title, appliedFilters);

			PdfPTable table = new PdfPTable(headers.size());
			table.setWidthPercentage(100);
			headers.forEach(header -> table.addCell(headerCell(header)));
			for (Map<String, Object> row : rows) {
				headers.forEach(header -> table.addCell(bodyCell(stringValue(row.get(header)))));
			}
			document.add(table);
			document.close();
			return outputStream.toByteArray();
		}
		catch (java.io.IOException | RuntimeException ex) {
			throw new IllegalStateException("Unable to generate PDF export.", ex);
		}
	}

	private void addMetadata(Document document, String title, Map<String, Object> appliedFilters)
			throws java.io.IOException {
		Map<String, Object> metadata = new LinkedHashMap<>();
		metadata.put("School Name", schoolName());
		metadata.put("Report Title", title == null ? "Export" : title);
		metadata.put("Generated At", GENERATED_AT_FORMATTER.format(Instant.now().atZone(SCHOOL_ZONE)));
		if (appliedFilters != null) {
			appliedFilters.forEach((key, value) -> {
				if (value != null && !String.valueOf(value).isBlank()) {
					metadata.put(key, value);
				}
			});
		}
		PdfPTable metadataTable = new PdfPTable(2);
		metadataTable.setWidthPercentage(100);
		metadata.forEach((key, value) -> {
			metadataTable.addCell(headerCell(key));
			metadataTable.addCell(bodyCell(stringValue(value)));
		});
		document.add(metadataTable);
		Paragraph spacer = new Paragraph(" ");
		spacer.setSpacingAfter(8);
		document.add(spacer);
	}

	public byte[] exportProfile(String title, Map<String, Object> values) {
		try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
			Document document = new Document(PageSize.A4, 36, 36, 36, 36);
			PdfWriter.getInstance(document, outputStream);
			document.open();
			Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
			Paragraph heading = new Paragraph(title == null ? "Profile" : title, titleFont);
			heading.setSpacingAfter(16);
			document.add(heading);

			PdfPTable table = new PdfPTable(2);
			table.setWidthPercentage(100);
			values.forEach((key, value) -> {
				table.addCell(headerCell(key));
				table.addCell(bodyCell(stringValue(value)));
			});
			document.add(table);
			document.close();
			return outputStream.toByteArray();
		}
		catch (java.io.IOException | RuntimeException ex) {
			throw new IllegalStateException("Unable to generate PDF profile.", ex);
		}
	}

	private PdfPCell headerCell(String value) {
		PdfPCell cell = new PdfPCell(new Phrase(value, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9)));
		cell.setHorizontalAlignment(Element.ALIGN_LEFT);
		cell.setBackgroundColor(new Color(238, 242, 247));
		cell.setPadding(6);
		return cell;
	}

	private PdfPCell bodyCell(String value) {
		PdfPCell cell = new PdfPCell(new Phrase(value, FontFactory.getFont(FontFactory.HELVETICA, 9)));
		cell.setPadding(6);
		return cell;
	}

	private String stringValue(Object value) {
		return value == null ? "" : String.valueOf(value);
	}

	private String schoolName() {
		String configured = settingsService.rawSettingValue("schoolProfile", "schoolName");
		return configured == null || configured.isBlank() ? SCHOOL_NAME : configured;
	}
}
