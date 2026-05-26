package com.school.erp.common.importexport;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
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

import org.springframework.stereotype.Service;

@Service
public class PdfExportService {

	public byte[] exportTable(String title, List<String> headers, List<Map<String, Object>> rows) {
		try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
			Document document = new Document(PageSize.A4.rotate(), 24, 24, 24, 24);
			PdfWriter.getInstance(document, outputStream);
			document.open();
			Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
			Paragraph heading = new Paragraph(title == null ? "Export" : title, titleFont);
			heading.setSpacingAfter(16);
			document.add(heading);

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
}
