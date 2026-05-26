package com.school.erp.common.importexport;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import com.opencsv.CSVWriter;

import org.springframework.stereotype.Service;

@Service
public class CsvExportService {

	public byte[] export(List<String> headers, List<Map<String, Object>> rows) {
		StringWriter stringWriter = new StringWriter();
		try (CSVWriter writer = new CSVWriter(stringWriter)) {
			writer.writeNext(headers.toArray(String[]::new));
			for (Map<String, Object> row : rows) {
				String[] values = headers.stream()
						.map(header -> stringValue(row.get(header)))
						.toArray(String[]::new);
				writer.writeNext(values);
			}
		}
		catch (java.io.IOException ex) {
			throw new IllegalStateException("Unable to generate CSV export.", ex);
		}
		return stringWriter.toString().getBytes(StandardCharsets.UTF_8);
	}

	private String stringValue(Object value) {
		return value == null ? "" : String.valueOf(value);
	}
}
