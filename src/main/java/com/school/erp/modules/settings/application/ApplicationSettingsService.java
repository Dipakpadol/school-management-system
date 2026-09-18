package com.school.erp.modules.settings.application;

import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.school.erp.common.audit.application.AuditLogEvent;
import com.school.erp.common.audit.application.AuditLogService;
import com.school.erp.common.exception.BusinessException;
import com.school.erp.common.exception.ErrorCode;
import com.school.erp.modules.settings.api.dto.ApplicationSettingsRequest;
import com.school.erp.modules.settings.api.dto.ApplicationSettingsResponse;
import com.school.erp.modules.settings.domain.ApplicationSetting;
import com.school.erp.modules.settings.infrastructure.ApplicationSettingRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ApplicationSettingsService {

	private static final String MODULE_NAME = "SETTINGS";
	private static final String MASKED_SECRET = "********";
	private static final Map<String, Set<String>> SENSITIVE_SETTINGS = Map.of(
			"email", Set.of("smtpPassword"),
			"sms", Set.of("apiKey", "apiSecret"));
	private static final Map<String, Map<String, String>> DEFAULTS = Map.ofEntries(
			Map.entry("schoolProfile", ordered(Map.of(
					"schoolName", "",
					"schoolCode", "",
					"address", "",
					"contactNumber", "",
					"email", "",
					"logoUrl", "",
					"principalName", "",
					"affiliationBoard", ""))),
			Map.entry("academic", ordered(Map.of(
					"currentAcademicYearId", "",
					"defaultAttendanceTime", "09:00",
					"workingDays", "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY",
					"holidayConfiguration", ""))),
			Map.entry("exams", ordered(Map.of(
					"defaultPassingPercentage", "35",
					"gradeScale", "A+,A,B,C,D,F",
					"publishResultsAutomatically", "false",
					"allowMarksEditingAfterPublish", "false"))),
			Map.entry("grades", ordered(Map.of(
					"aPlusMinimum", "90",
					"aMinimum", "75",
					"bMinimum", "60",
					"cMinimum", "45",
					"dMinimum", "35"))),
			Map.entry("fees", ordered(Map.of(
					"receiptPrefix", "RCPT",
					"receiptNumberFormat", "{prefix}-{yyyyMMdd}-{sequence}",
					"lateFeeDefaultAmount", "0.00",
					"paymentModesEnabled", "CASH,UPI,BANK_TRANSFER,CHEQUE,ONLINE",
					"onlinePaymentEnabled", "false"))),
			Map.entry("notifications", ordered(Map.of(
					"emailEnabled", "false",
					"smsEnabled", "false",
					"whatsAppEnabled", "false",
					"reminderDaysBeforeDueDate", "3",
					"schedulerEnabled", "true"))),
			Map.entry("email", ordered(Map.of(
					"smtpHost", "",
					"smtpPort", "587",
					"smtpUsername", "",
					"smtpPassword", "",
					"fromAddress", ""))),
			Map.entry("sms", ordered(Map.of(
					"provider", "",
					"senderId", "",
					"apiKey", "",
					"apiSecret", "",
					"defaultCountryCode", "+91"))),
			Map.entry("application", ordered(Map.of(
					"timezone", "Asia/Kolkata",
					"dateFormat", "yyyy-MM-dd",
					"language", "en",
					"themePreference", "system",
					"maintenanceMode", "false"))),
			Map.entry("library", ordered(Map.of(
					"defaultLoanDays", "14",
					"maxActiveLoans", "3",
					"finePerDay", "0.00"))),
			Map.entry("security", ordered(Map.of(
					"passwordExpiryDays", "90",
					"maxFailedLoginAttempts", "5",
					"sessionTimeoutMinutes", "30",
					"requireTwoFactorAuth", "false"))),
			Map.entry("backup", ordered(Map.of(
					"backupEnabled", "false",
					"backupFrequency", "DAILY",
					"backupRetentionDays", "30",
					"backupLocation", ""))));

	private final ApplicationSettingRepository settingRepository;
	private final AuditLogService auditLogService;

	@Transactional(readOnly = true)
	public ApplicationSettingsResponse getSettings() {
		Map<String, Map<String, String>> settings = defaultsCopy();
		for (ApplicationSetting setting : settingRepository.findAllByDeletedFalseOrderByGroupNameAscSettingKeyAsc()) {
			settings
					.computeIfAbsent(setting.getGroupName(), ignored -> new LinkedHashMap<>())
					.put(setting.getSettingKey(), setting.getSettingValue() == null ? "" : setting.getSettingValue());
		}
		return new ApplicationSettingsResponse(maskedCopy(settings));
	}

	@Transactional(readOnly = true)
	public String rawSettingValue(String groupName, String settingKey) {
		return settingRepository.findByGroupNameIgnoreCaseAndSettingKeyIgnoreCaseAndDeletedFalse(groupName, settingKey)
				.map(ApplicationSetting::getSettingValue)
				.orElseGet(() -> DEFAULTS
						.getOrDefault(groupName, Map.of())
						.getOrDefault(settingKey, ""));
	}

	@Transactional
	public ApplicationSettingsResponse updateSettings(ApplicationSettingsRequest request) {
		Map<String, Map<String, String>> oldValue = getSettings().groups();
		Map<String, Map<String, String>> incoming = request.groups() == null ? Map.of() : request.groups();
		validate(incoming);
		incoming.forEach((groupName, values) -> values.forEach((settingKey, value) -> {
			if (isMaskedSecret(groupName, settingKey, value)) {
				return;
			}
			upsert(groupName, settingKey, value);
		}));
		ApplicationSettingsResponse response = getSettings();
		auditLogService.record(new AuditLogEvent(MODULE_NAME, "ApplicationSettings", "application", "UPDATE", oldValue, response));
		return response;
	}

	private void upsert(String groupName, String settingKey, String value) {
		ApplicationSetting setting = settingRepository
				.findByGroupNameIgnoreCaseAndSettingKeyIgnoreCaseAndDeletedFalse(groupName, settingKey)
				.orElseGet(() -> new ApplicationSetting(groupName, settingKey, value));
		setting.updateValue(value);
		settingRepository.save(setting);
	}

	private void validate(Map<String, Map<String, String>> incoming) {
		Set<String> groups = DEFAULTS.keySet();
		for (Map.Entry<String, Map<String, String>> groupEntry : incoming.entrySet()) {
			String groupName = groupEntry.getKey();
			if (!groups.contains(groupName)) {
				throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Unknown settings group: " + groupName);
			}
			if (groupEntry.getValue() == null) {
				throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Settings group cannot be empty: " + groupName);
			}
			Set<String> allowedKeys = DEFAULTS.get(groupName).keySet();
			for (Map.Entry<String, String> setting : groupEntry.getValue().entrySet()) {
				if (!allowedKeys.contains(setting.getKey())) {
					throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Unknown setting: " + groupName + "." + setting.getKey());
				}
				validateSetting(groupName, setting.getKey(), setting.getValue());
			}
		}
	}

	private void validateSetting(String groupName, String key, String value) {
		if ("schoolProfile".equals(groupName) && "email".equals(key) && StringUtils.hasText(value)
				&& !value.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "School email is invalid.");
		}
		if ("schoolProfile".equals(groupName) && "contactNumber".equals(key) && StringUtils.hasText(value)
				&& !value.matches("^\\+?[0-9]{10,15}$")) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Contact number must be 10 to 15 digits.");
		}
		if ("application".equals(groupName) && "timezone".equals(key) && StringUtils.hasText(value)) {
			try {
				ZoneId.of(value);
			}
			catch (RuntimeException ex) {
				throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Timezone is invalid.");
			}
		}
		if ("application".equals(groupName) && "dateFormat".equals(key) && StringUtils.hasText(value)
				&& !Set.of("yyyy-MM-dd", "dd-MM-yyyy", "MM/dd/yyyy").contains(value)) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Date format is invalid.");
		}
		if ("fees".equals(groupName) && "lateFeeDefaultAmount".equals(key) && StringUtils.hasText(value)
				&& !value.matches("^\\d{1,10}(\\.\\d{1,2})?$")) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Late fee amount is invalid.");
		}
		if ("notifications".equals(groupName) && "reminderDaysBeforeDueDate".equals(key) && StringUtils.hasText(value)
				&& !value.matches("^\\d{1,3}$")) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Reminder days must be numeric.");
		}
		if ("exams".equals(groupName) && "defaultPassingPercentage".equals(key)) {
			validatePercent(value, "Default passing percentage is invalid.");
		}
		if ("grades".equals(groupName)) {
			validatePercent(value, "Grade threshold is invalid.");
		}
		if ("email".equals(groupName) && "smtpPort".equals(key) && StringUtils.hasText(value)
				&& !value.matches("^\\d{1,5}$")) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "SMTP port must be numeric.");
		}
		if ("security".equals(groupName) && Set.of(
				"passwordExpiryDays",
				"maxFailedLoginAttempts",
				"sessionTimeoutMinutes").contains(key)
				&& StringUtils.hasText(value)
				&& !value.matches("^\\d{1,4}$")) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Security numeric setting is invalid.");
		}
		if ("backup".equals(groupName) && "backupRetentionDays".equals(key) && StringUtils.hasText(value)
				&& !value.matches("^\\d{1,4}$")) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Backup retention days must be numeric.");
		}
		if (isBooleanSetting(groupName, key) && StringUtils.hasText(value)
				&& !Set.of("true", "false").contains(value.toLowerCase())) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Boolean setting must be true or false.");
		}
	}

	private void validatePercent(String value, String message) {
		if (StringUtils.hasText(value) && !value.matches("^\\d{1,3}(\\.\\d{1,2})?$")) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, message);
		}
	}

	private boolean isBooleanSetting(String groupName, String key) {
		return Set.of(
				"fees.onlinePaymentEnabled",
				"notifications.emailEnabled",
				"notifications.smsEnabled",
				"notifications.whatsAppEnabled",
				"notifications.schedulerEnabled",
				"exams.publishResultsAutomatically",
				"exams.allowMarksEditingAfterPublish",
				"application.maintenanceMode",
				"security.requireTwoFactorAuth",
				"backup.backupEnabled")
				.contains(groupName + "." + key);
	}

	private Map<String, Map<String, String>> defaultsCopy() {
		Map<String, Map<String, String>> copy = new LinkedHashMap<>();
		DEFAULTS.forEach((group, values) -> copy.put(group, new LinkedHashMap<>(values)));
		return copy;
	}

	private Map<String, Map<String, String>> maskedCopy(Map<String, Map<String, String>> source) {
		Map<String, Map<String, String>> copy = new LinkedHashMap<>();
		source.forEach((group, values) -> {
			Map<String, String> groupValues = new LinkedHashMap<>();
			values.forEach((key, value) -> groupValues.put(
					key,
					isSensitive(group, key) && StringUtils.hasText(value) ? MASKED_SECRET : value));
			copy.put(group, groupValues);
		});
		return copy;
	}

	private boolean isMaskedSecret(String groupName, String settingKey, String value) {
		return isSensitive(groupName, settingKey) && MASKED_SECRET.equals(value);
	}

	private boolean isSensitive(String groupName, String settingKey) {
		return SENSITIVE_SETTINGS
				.getOrDefault(groupName, Set.of())
				.contains(settingKey);
	}

	private static Map<String, String> ordered(Map<String, String> values) {
		return new LinkedHashMap<>(values);
	}
}
