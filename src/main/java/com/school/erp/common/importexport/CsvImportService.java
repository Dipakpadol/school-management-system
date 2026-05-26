package com.school.erp.common.importexport;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CsvImportService {

	public List<ImportRow> readRows(MultipartFile file) {
		try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
			String[] headers = reader.readNext();
			if (headers == null) {
				return List.of();
			}
			List<String> normalizedHeaders = normalizeHeaders(headers);
			List<ImportRow> rows = new ArrayList<>();
			String[] values;
			int rowNumber = 1;
			while ((values = reader.readNext()) != null) {
				rowNumber++;
				Map<String, String> row = new LinkedHashMap<>();
				boolean hasValue = false;
				for (int index = 0; index < normalizedHeaders.size(); index++) {
					String value = index < values.length && values[index] != null ? values[index].trim() : "";
					row.put(normalizedHeaders.get(index), value);
					hasValue = hasValue || StringUtils.hasText(value);
				}
				if (hasValue) {
					rows.add(new ImportRow(rowNumber, row));
				}
			}
			return rows;
		}
		catch (IOException | CsvValidationException ex) {
			throw new ImportValidationException(ImportResultDto.of(
					java.util.UUID.randomUUID(),
					0,
					0,
					List.of(new ImportErrorDto(0, "file", "Unable to read CSV file: " + ex.getMessage()))));
		}
	}

	private List<String> normalizeHeaders(String[] headers) {
		List<String> normalizedHeaders = new ArrayList<>();
		for (String header : headers) {
			if (StringUtils.hasText(header)) {
				normalizedHeaders.add(header.trim());
			}
		}
		return normalizedHeaders;
	}
}
