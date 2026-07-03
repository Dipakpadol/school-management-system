package com.school.erp.modules.users.application;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
import com.school.erp.common.importexport.TemplateGeneratorService;
import com.school.erp.modules.users.api.dto.UserCreateRequest;
import com.school.erp.modules.users.api.dto.UserResponse;
import com.school.erp.modules.users.domain.RoleName;
import com.school.erp.modules.users.infrastructure.UserAccountRepository;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserImportExportService {

	private static final Set<RoleName> DOMAIN_MANAGED_ROLES = Set.of(RoleName.STUDENT, RoleName.TEACHER);

	public static final List<String> USER_COLUMNS = List.of(
			"email",
			"username",
			"firstName",
			"lastName",
			"phoneNumber",
			"password",
			"roles",
			"status");

	private static final Pattern MOBILE_PATTERN = Pattern.compile("^\\+?[0-9]{10,15}$");

	private final ExcelImportService excelImportService;
	private final CsvImportService csvImportService;
	private final ExcelExportService excelExportService;
	private final CsvExportService csvExportService;
	private final TemplateGeneratorService templateGeneratorService;
	private final ImportErrorReportStore importErrorReportStore;
	private final UserService userService;
	private final UserMapper userMapper;
	private final UserAccountRepository userAccountRepository;
	private final AuditLogService auditLogService;

	public ImportResultDto importExcel(MultipartFile file) {
		return importRows(excelImportService.readRows(file));
	}

	public ImportResultDto importCsv(MultipartFile file) {
		return importRows(csvImportService.readRows(file));
	}

	public byte[] excelTemplate() {
		return templateGeneratorService.excelTemplate("users", USER_COLUMNS);
	}

	public byte[] csvTemplate() {
		return templateGeneratorService.csvTemplate(USER_COLUMNS);
	}

	@Transactional(readOnly = true)
	public byte[] exportExcel() {
		byte[] content = excelExportService.export("users", USER_COLUMNS, exportRows());
		auditLogService.recordStandalone(new AuditLogEvent("USERS", "UserAccount", null, AuditAction.EXPORT, null, "Excel export"));
		return content;
	}

	@Transactional(readOnly = true)
	public byte[] exportCsv() {
		byte[] content = csvExportService.export(USER_COLUMNS, exportRows());
		auditLogService.recordStandalone(new AuditLogEvent("USERS", "UserAccount", null, AuditAction.EXPORT, null, "CSV export"));
		return content;
	}

	public ImportResultDto importErrors(UUID batchId) {
		return importErrorReportStore.find(batchId)
				.orElseThrow(() -> new ResourceNotFoundException("Import batch", batchId));
	}

	private ImportResultDto importRows(List<ImportRow> rows) {
		UUID batchId = UUID.randomUUID();
		List<ImportErrorDto> errors = new ArrayList<>();
		Set<String> emails = new LinkedHashSet<>();
		Set<String> mobiles = new LinkedHashSet<>();
		int successRows = 0;

		for (ImportRow row : rows) {
			List<ImportErrorDto> rowErrors = validateRow(row, emails, mobiles);
			if (!rowErrors.isEmpty()) {
				errors.addAll(rowErrors);
				continue;
			}
			try {
				userService.create(toCreateRequest(row.values()));
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
				"USERS",
				"UserAccount",
				batchId.toString(),
				AuditAction.IMPORT,
				null,
				Map.of("batchId", batchId, "totalRows", rows.size(), "successRows", successRows, "failedRows", errors.size())));
		return result;
	}

	private List<ImportErrorDto> validateRow(ImportRow row, Set<String> emails, Set<String> mobiles) {
		List<ImportErrorDto> errors = new ArrayList<>();
		Map<String, String> values = row.values();
		required(values, row.rowNumber(), "email", errors);
		required(values, row.rowNumber(), "username", errors);
		required(values, row.rowNumber(), "firstName", errors);
		required(values, row.rowNumber(), "password", errors);
		required(values, row.rowNumber(), "roles", errors);

		String email = value(values, "email");
		if (StringUtils.hasText(email)) {
			String key = email.toLowerCase();
			if (!emails.add(key)) {
				errors.add(new ImportErrorDto(row.rowNumber(), "email", "Duplicate email in import file."));
			}
			if (userAccountRepository.existsByEmailIgnoreCaseAndDeletedFalse(email)) {
				errors.add(new ImportErrorDto(row.rowNumber(), "email", "Email already exists."));
			}
		}

		String mobile = value(values, "phoneNumber");
		if (StringUtils.hasText(mobile)) {
			if (!MOBILE_PATTERN.matcher(mobile).matches()) {
				errors.add(new ImportErrorDto(row.rowNumber(), "phoneNumber", "Mobile number must contain 10 to 15 digits."));
			}
			if (!mobiles.add(mobile)) {
				errors.add(new ImportErrorDto(row.rowNumber(), "phoneNumber", "Duplicate mobile number in import file."));
			}
			if (userAccountRepository.existsByPhoneNumberAndDeletedFalse(mobile)) {
				errors.add(new ImportErrorDto(row.rowNumber(), "phoneNumber", "Mobile number already exists."));
			}
		}

		parseRoles(values, row.rowNumber(), errors);
		return errors;
	}

	private UserCreateRequest toCreateRequest(Map<String, String> values) {
		return new UserCreateRequest(
				value(values, "email"),
				value(values, "username"),
				value(values, "firstName"),
				blankToNull(value(values, "middleName")),
				blankToNull(value(values, "lastName")),
				blankToNull(value(values, "phoneNumber")),
				value(values, "password"),
				parseRoles(values, 0, new ArrayList<>()));
	}

	private Set<RoleName> parseRoles(Map<String, String> values, int rowNumber, List<ImportErrorDto> errors) {
		String raw = value(values, "roles");
		if (!StringUtils.hasText(raw)) {
			return Set.of();
		}
		Set<RoleName> roles = new LinkedHashSet<>();
		for (String token : raw.split("[|,]")) {
			if (!StringUtils.hasText(token)) {
				continue;
			}
			try {
				RoleName roleName = RoleName.valueOf(token.trim().toUpperCase());
				if (DOMAIN_MANAGED_ROLES.contains(roleName)) {
					errors.add(new ImportErrorDto(
							rowNumber,
							"roles",
							"Student and Teacher users must be created from their domain management modules."));
				}
				roles.add(roleName);
			}
			catch (IllegalArgumentException ex) {
				errors.add(new ImportErrorDto(rowNumber, "roles", "Unsupported role: " + token.trim()));
			}
		}
		if (roles.isEmpty()) {
			errors.add(new ImportErrorDto(rowNumber, "roles", "At least one role is required."));
		}
		return roles;
	}

	private List<Map<String, Object>> exportRows() {
		return userAccountRepository.findAllByDeletedFalse(Pageable.unpaged()).stream()
				.map(userMapper::toResponse)
				.map(this::toExportRow)
				.toList();
	}

	private Map<String, Object> toExportRow(UserResponse user) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("email", user.email());
		row.put("username", user.username());
		row.put("firstName", user.firstName());
		row.put("lastName", user.lastName());
		row.put("phoneNumber", user.phoneNumber());
		row.put("password", "");
		row.put("roles", user.roles().stream().map(role -> role.name().name()).collect(Collectors.joining("|")));
		row.put("status", user.status());
		return row;
	}

	private void required(Map<String, String> values, int rowNumber, String field, List<ImportErrorDto> errors) {
		if (!StringUtils.hasText(value(values, field))) {
			errors.add(new ImportErrorDto(rowNumber, field, field + " is required."));
		}
	}

	private String value(Map<String, String> values, String field) {
		return values.getOrDefault(field, "").trim();
	}

	private String blankToNull(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}
}
