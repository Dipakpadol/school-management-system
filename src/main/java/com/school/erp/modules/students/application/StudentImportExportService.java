package com.school.erp.modules.students.application;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import com.school.erp.common.audit.application.AuditAction;
import com.school.erp.common.audit.application.AuditLogEvent;
import com.school.erp.common.audit.application.AuditLogService;
import com.school.erp.common.exception.BusinessException;
import com.school.erp.common.exception.ResourceNotFoundException;
import com.school.erp.common.importexport.CsvExportService;
import com.school.erp.common.importexport.CsvImportService;
import com.school.erp.common.importexport.ExcelExportService;
import com.school.erp.common.importexport.ExcelImportService;
import com.school.erp.common.importexport.ImportErrorDto;
import com.school.erp.common.importexport.ImportErrorReportStore;
import com.school.erp.common.importexport.ImportResultDto;
import com.school.erp.common.importexport.ImportRow;
import com.school.erp.common.importexport.PdfExportService;
import com.school.erp.common.importexport.TemplateGeneratorService;
import com.school.erp.modules.students.api.dto.ClassSectionAssignmentRequest;
import com.school.erp.modules.students.api.dto.ParentGuardianRequest;
import com.school.erp.modules.students.api.dto.ParentMappingRequest;
import com.school.erp.modules.students.api.dto.StudentAdmissionRequest;
import com.school.erp.modules.students.api.dto.StudentProfileRequest;
import com.school.erp.modules.students.api.dto.StudentResponse;
import com.school.erp.modules.students.domain.Gender;
import com.school.erp.modules.students.domain.ParentRelation;
import com.school.erp.modules.students.infrastructure.StudentRepository;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StudentImportExportService {

	public static final List<String> STUDENT_COLUMNS = List.of(
			"admissionNumber",
			"firstName",
			"middleName",
			"lastName",
			"dateOfBirth",
			"gender",
			"bloodGroup",
			"email",
			"phoneNumber",
			"admissionDate",
			"previousSchool",
			"addressLine1",
			"addressLine2",
			"city",
			"state",
			"postalCode",
			"country",
			"academicYear",
			"className",
			"sectionName",
			"rollNumber",
			"parentRelation",
			"parentFirstName",
			"parentLastName",
			"parentEmail",
			"parentPhoneNumber",
			"parentOccupation");

	private static final Pattern MOBILE_PATTERN = Pattern.compile("^\\+?[0-9]{10,15}$");

	private final ExcelImportService excelImportService;
	private final CsvImportService csvImportService;
	private final ExcelExportService excelExportService;
	private final CsvExportService csvExportService;
	private final PdfExportService pdfExportService;
	private final TemplateGeneratorService templateGeneratorService;
	private final ImportErrorReportStore importErrorReportStore;
	private final StudentService studentService;
	private final StudentRepository studentRepository;
	private final StudentMapper studentMapper;
	private final AuditLogService auditLogService;

	public ImportResultDto importExcel(MultipartFile file) {
		return importRows(excelImportService.readRows(file));
	}

	public ImportResultDto importCsv(MultipartFile file) {
		return importRows(csvImportService.readRows(file));
	}

	public byte[] excelTemplate() {
		return templateGeneratorService.excelTemplate("students", STUDENT_COLUMNS);
	}

	public byte[] csvTemplate() {
		return templateGeneratorService.csvTemplate(STUDENT_COLUMNS);
	}

	@Transactional(readOnly = true)
	public byte[] exportExcel() {
		byte[] content = excelExportService.export("students", STUDENT_COLUMNS, exportRows());
		auditLogService.recordStandalone(new AuditLogEvent("STUDENTS", "Student", null, AuditAction.EXPORT, null, "Excel export"));
		return content;
	}

	@Transactional(readOnly = true)
	public byte[] exportCsv() {
		byte[] content = csvExportService.export(STUDENT_COLUMNS, exportRows());
		auditLogService.recordStandalone(new AuditLogEvent("STUDENTS", "Student", null, AuditAction.EXPORT, null, "CSV export"));
		return content;
	}

	public ImportResultDto importErrors(UUID batchId) {
		return importErrorReportStore.find(batchId)
				.orElseThrow(() -> new ResourceNotFoundException("Import batch", batchId));
	}

	public byte[] profilePdf(UUID studentId) {
		StudentResponse student = studentService.getStudentProfile(studentId);
		Map<String, Object> profile = new LinkedHashMap<>();
		profile.put("Admission Number", student.admissionNumber());
		profile.put("Name", student.displayName());
		profile.put("Status", student.status());
		profile.put("Date of Birth", student.dateOfBirth());
		profile.put("Gender", student.gender());
		profile.put("Class", student.currentAssignment() == null ? null : student.currentAssignment().className());
		profile.put("Section", student.currentAssignment() == null ? null : student.currentAssignment().sectionName());
		profile.put("Roll Number", student.currentAssignment() == null ? null : student.currentAssignment().rollNumber());
		profile.put("Email", student.email());
		profile.put("Phone", student.phoneNumber());
		profile.put("City", student.city());
		profile.put("Parents", student.parents().stream()
				.map(parent -> parent.relationType() + ": " + parent.displayName() + " (" + parent.phoneNumber() + ")")
				.toList());
		auditLogService.recordStandalone(new AuditLogEvent(
				"STUDENTS",
				"Student",
				studentId.toString(),
				AuditAction.EXPORT,
				null,
				"Student profile PDF"));
		return pdfExportService.exportProfile("Student Profile", profile);
	}

	private ImportResultDto importRows(List<ImportRow> rows) {
		UUID batchId = UUID.randomUUID();
		List<ImportErrorDto> errors = new ArrayList<>();
		Set<String> admissionNumbers = new LinkedHashSet<>();
		Set<String> rollKeys = new LinkedHashSet<>();
		int successRows = 0;

		for (ImportRow row : rows) {
			List<ImportErrorDto> rowErrors = validateRow(row, admissionNumbers, rollKeys);
			if (!rowErrors.isEmpty()) {
				errors.addAll(rowErrors);
				continue;
			}
			try {
				studentService.admitStudent(toAdmissionRequest(row.values()));
				successRows++;
			}
			catch (BusinessException ex) {
				errors.add(new ImportErrorDto(row.rowNumber(), "row", ex.getMessage()));
			}
			catch (RuntimeException ex) {
				errors.add(new ImportErrorDto(row.rowNumber(), "row", "Unable to import row: " + ex.getMessage()));
			}
		}

		ImportResultDto result = ImportResultDto.of(batchId, rows.size(), successRows, errors);
		importErrorReportStore.save(result);
		auditLogService.recordStandalone(new AuditLogEvent(
				"STUDENTS",
				"Student",
				batchId.toString(),
				AuditAction.IMPORT,
				null,
				Map.of("batchId", batchId, "totalRows", rows.size(), "successRows", successRows, "failedRows", errors.size())));
		return result;
	}

	private List<ImportErrorDto> validateRow(ImportRow row, Set<String> admissionNumbers, Set<String> rollKeys) {
		List<ImportErrorDto> errors = new ArrayList<>();
		Map<String, String> values = row.values();
		required(values, row.rowNumber(), "admissionNumber", errors);
		required(values, row.rowNumber(), "firstName", errors);
		required(values, row.rowNumber(), "dateOfBirth", errors);
		required(values, row.rowNumber(), "gender", errors);
		required(values, row.rowNumber(), "admissionDate", errors);
		required(values, row.rowNumber(), "academicYear", errors);
		required(values, row.rowNumber(), "className", errors);
		required(values, row.rowNumber(), "sectionName", errors);
		required(values, row.rowNumber(), "parentRelation", errors);
		required(values, row.rowNumber(), "parentFirstName", errors);
		required(values, row.rowNumber(), "parentPhoneNumber", errors);

		String admissionNumber = value(values, "admissionNumber");
		if (StringUtils.hasText(admissionNumber)) {
			String key = admissionNumber.toLowerCase();
			if (!admissionNumbers.add(key)) {
				errors.add(new ImportErrorDto(row.rowNumber(), "admissionNumber", "Duplicate admission number in import file."));
			}
			if (studentRepository.existsByAdmissionNumberIgnoreCaseAndDeletedFalse(admissionNumber)) {
				errors.add(new ImportErrorDto(row.rowNumber(), "admissionNumber", "Admission number already exists."));
			}
		}

		String parentMobile = value(values, "parentPhoneNumber");
		if (StringUtils.hasText(parentMobile) && !MOBILE_PATTERN.matcher(parentMobile).matches()) {
			errors.add(new ImportErrorDto(row.rowNumber(), "parentPhoneNumber", "Parent mobile number must contain 10 to 15 digits."));
		}

		String rollNumber = value(values, "rollNumber");
		if (StringUtils.hasText(rollNumber)
				&& StringUtils.hasText(value(values, "academicYear"))
				&& StringUtils.hasText(value(values, "className"))
				&& StringUtils.hasText(value(values, "sectionName"))) {
			String rollKey = String.join("|",
					value(values, "academicYear").toLowerCase(),
					value(values, "className").toLowerCase(),
					value(values, "sectionName").toLowerCase(),
					rollNumber.toLowerCase());
			if (!rollKeys.add(rollKey)) {
				errors.add(new ImportErrorDto(row.rowNumber(), "rollNumber", "Duplicate roll number in class/section/year in import file."));
			}
			if (studentRepository.existsRollNumberInClassSectionYear(
					value(values, "academicYear"),
					value(values, "className"),
					value(values, "sectionName"),
					rollNumber)) {
				errors.add(new ImportErrorDto(row.rowNumber(), "rollNumber", "Roll number already exists in class/section/year."));
			}
		}

		validateDate(values, row.rowNumber(), "dateOfBirth", errors);
		validateDate(values, row.rowNumber(), "admissionDate", errors);
		validateEnum(values, row.rowNumber(), "gender", Gender.class, errors);
		validateEnum(values, row.rowNumber(), "parentRelation", ParentRelation.class, errors);
		return errors;
	}

	private StudentAdmissionRequest toAdmissionRequest(Map<String, String> values) {
		StudentProfileRequest profile = new StudentProfileRequest(
				value(values, "firstName"),
				blankToNull(value(values, "middleName")),
				blankToNull(value(values, "lastName")),
				LocalDate.parse(value(values, "dateOfBirth")),
				Gender.valueOf(value(values, "gender").toUpperCase()),
				blankToNull(value(values, "bloodGroup")),
				blankToNull(value(values, "email")),
				blankToNull(value(values, "phoneNumber")),
				LocalDate.parse(value(values, "admissionDate")),
				blankToNull(value(values, "previousSchool")),
				blankToNull(value(values, "addressLine1")),
				blankToNull(value(values, "addressLine2")),
				blankToNull(value(values, "city")),
				blankToNull(value(values, "state")),
				blankToNull(value(values, "postalCode")),
				firstText(value(values, "country"), "India"));
		ParentGuardianRequest parent = new ParentGuardianRequest(
				value(values, "parentFirstName"),
				blankToNull(value(values, "parentLastName")),
				blankToNull(value(values, "parentEmail")),
				value(values, "parentPhoneNumber"),
				null,
				blankToNull(value(values, "parentOccupation")),
				blankToNull(value(values, "addressLine1")),
				blankToNull(value(values, "addressLine2")),
				blankToNull(value(values, "city")),
				blankToNull(value(values, "state")),
				blankToNull(value(values, "postalCode")),
				firstText(value(values, "country"), "India"),
				null);
		ClassSectionAssignmentRequest assignment = new ClassSectionAssignmentRequest(
				value(values, "academicYear"),
				value(values, "className"),
				value(values, "sectionName"),
				blankToNull(value(values, "rollNumber")),
				LocalDate.parse(firstText(value(values, "effectiveFrom"), value(values, "admissionDate"))));
		return new StudentAdmissionRequest(
				value(values, "admissionNumber"),
				profile,
				List.of(new ParentMappingRequest(
						ParentRelation.valueOf(value(values, "parentRelation").toUpperCase()),
						true,
						true,
						true,
						parent)),
				assignment,
				List.of());
	}

	private List<Map<String, Object>> exportRows() {
		return studentRepository.findAllByDeletedFalse(Pageable.unpaged()).stream()
				.map(studentMapper::toProfileResponse)
				.map(this::toExportRow)
				.toList();
	}

	private Map<String, Object> toExportRow(StudentResponse student) {
		Map<String, Object> row = new LinkedHashMap<>();
		STUDENT_COLUMNS.forEach(column -> row.put(column, null));
		row.put("admissionNumber", student.admissionNumber());
		row.put("firstName", student.firstName());
		row.put("middleName", student.middleName());
		row.put("lastName", student.lastName());
		row.put("dateOfBirth", student.dateOfBirth());
		row.put("gender", student.gender());
		row.put("bloodGroup", student.bloodGroup());
		row.put("email", student.email());
		row.put("phoneNumber", student.phoneNumber());
		row.put("admissionDate", student.admissionDate());
		row.put("previousSchool", student.previousSchool());
		row.put("addressLine1", student.addressLine1());
		row.put("addressLine2", student.addressLine2());
		row.put("city", student.city());
		row.put("state", student.state());
		row.put("postalCode", student.postalCode());
		row.put("country", student.country());
		if (student.currentAssignment() != null) {
			row.put("academicYear", student.currentAssignment().academicYear());
			row.put("className", student.currentAssignment().className());
			row.put("sectionName", student.currentAssignment().sectionName());
			row.put("rollNumber", student.currentAssignment().rollNumber());
		}
		if (!student.parents().isEmpty()) {
			var parent = student.parents().getFirst();
			row.put("parentRelation", parent.relationType());
			row.put("parentFirstName", parent.firstName());
			row.put("parentLastName", parent.lastName());
			row.put("parentEmail", parent.email());
			row.put("parentPhoneNumber", parent.phoneNumber());
			row.put("parentOccupation", parent.occupation());
		}
		return row;
	}

	private void required(Map<String, String> values, int rowNumber, String field, List<ImportErrorDto> errors) {
		if (!StringUtils.hasText(value(values, field))) {
			errors.add(new ImportErrorDto(rowNumber, field, field + " is required."));
		}
	}

	private void validateDate(Map<String, String> values, int rowNumber, String field, List<ImportErrorDto> errors) {
		if (!StringUtils.hasText(value(values, field))) {
			return;
		}
		try {
			LocalDate.parse(value(values, field));
		}
		catch (DateTimeParseException ex) {
			errors.add(new ImportErrorDto(rowNumber, field, field + " must be ISO date format yyyy-MM-dd."));
		}
	}

	private <T extends Enum<T>> void validateEnum(
			Map<String, String> values,
			int rowNumber,
			String field,
			Class<T> enumType,
			List<ImportErrorDto> errors) {
		if (!StringUtils.hasText(value(values, field))) {
			return;
		}
		try {
			Enum.valueOf(enumType, value(values, field).toUpperCase());
		}
		catch (IllegalArgumentException ex) {
			errors.add(new ImportErrorDto(rowNumber, field, field + " is not supported."));
		}
	}

	private String value(Map<String, String> values, String field) {
		return values.getOrDefault(field, "").trim();
	}

	private String blankToNull(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}

	private String firstText(String primary, String fallback) {
		return StringUtils.hasText(primary) ? primary.trim() : fallback;
	}
}
