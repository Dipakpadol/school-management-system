package com.school.erp.common.importexport;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
		return excelExportService.export(sheetName, headers, List.of(sampleRow(headers)));
	}

	public byte[] csvTemplate(List<String> headers) {
		return csvExportService.export(headers, List.of(sampleRow(headers)));
	}

	private Map<String, Object> sampleRow(List<String> headers) {
		Map<String, Object> row = new LinkedHashMap<>();
		for (String header : headers) {
			row.put(header, sampleValue(header));
		}
		return row;
	}

	private Object sampleValue(String header) {
		return switch (header) {
			case "admissionNumber" -> "ADM-2026-0001";
			case "firstName", "parentFirstName" -> "Aarav";
			case "middleName" -> "Kumar";
			case "lastName", "parentLastName" -> "Sharma";
			case "dateOfBirth" -> "2015-06-01";
			case "gender" -> "MALE";
			case "bloodGroup" -> "A+";
			case "email" -> "aarav.sharma@student.school.test";
			case "phoneNumber", "parentPhoneNumber" -> "+919876543210";
			case "admissionDate", "assignedDate", "effectiveFrom" -> "2026-04-01";
			case "status" -> "ACTIVE";
			case "previousSchool" -> "Green Valley Kindergarten";
			case "addressLine1" -> "12 MG Road";
			case "addressLine2" -> "Near City Library";
			case "city" -> "Bengaluru";
			case "state" -> "Karnataka";
			case "postalCode" -> "560001";
			case "country" -> "India";
			case "academicYear" -> "2026-2027";
			case "className" -> "Class 6";
			case "sectionName" -> "A";
			case "rollNumber" -> "23";
			case "parentRelation" -> "FATHER";
			case "parentEmail" -> "rajesh.sharma@example.com";
			case "parentOccupation" -> "Software Engineer";
			case "username" -> "teacher01";
			case "fullName" -> "Teacher One";
			case "role" -> "TEACHER";
			case "active", "activate" -> "true";
			case "academicYearId" -> "Use UUID from Academic Years";
			case "classId" -> "Use UUID from Classes";
			case "categoryCode" -> "TUITION";
			case "categoryName" -> "Tuition Fee";
			case "itemAmount", "installmentAmount", "amount" -> "12000.00";
			case "installmentSequence" -> "1";
			case "installmentTitle" -> "Term 1";
			case "installmentDueDate" -> "2026-06-10";
			case "feeStructureId" -> "Use UUID from fee structure export";
			case "notes", "description" -> "Sample row. Replace before import.";
			default -> "";
		};
	}
}
