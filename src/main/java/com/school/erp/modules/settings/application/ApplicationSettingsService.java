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
	private static final Map<String, Map<String, String>> DEFAULTS = Map.of(
			"schoolProfile", ordered(Map.of(
					"schoolName", "",
					"schoolCode", "",
					"address", "",
					"contactNumber", "",
					"email", "",
					"logoUrl", "",
					"principalName", "",
					"affiliationBoard", "")),
			"academic", ordered(Map.of(
					"currentAcademicYearId", "",
					"defaultAttendanceTime", "09:00",
					"workingDays", "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY",
					"holidayConfiguration", "")),
			"fees", ordered(Map.of(
					"receiptPrefix", "RCPT",
					"receiptNumberFormat", "{prefix}-{yyyyMMdd}-{sequence}",
					"lateFeeDefaultAmount", "0.00",
					"paymentModesEnabled", "CASH,UPI,BANK_TRANSFER,CHEQUE,ONLINE",
					"onlinePaymentEnabled", "false")),
			"notifications", ordered(Map.of(
					"emailEnabled", "false",
					"smsEnabled", "false",
					"whatsAppEnabled", "false",
					"reminderDaysBeforeDueDate", "3",
					"schedulerEnabled", "true")),
			"system", ordered(Map.of(
					"timezone", "Asia/Kolkata",
					"dateFormat", "yyyy-MM-dd",
					"language", "en",
					"themePreference", "system",
					"maintenanceMode", "false")));

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
		return new ApplicationSettingsResponse(settings);
	}

	@Transactional
	public ApplicationSettingsResponse updateSettings(ApplicationSettingsRequest request) {
		Map<String, Map<String, String>> oldValue = getSettings().groups();
		Map<String, Map<String, String>> incoming = request.groups() == null ? Map.of() : request.groups();
		validate(incoming);
		incoming.forEach((groupName, values) -> values.forEach((settingKey, value) -> upsert(groupName, settingKey, value)));
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
		if ("system".equals(groupName) && "timezone".equals(key) && StringUtils.hasText(value)) {
			try {
				ZoneId.of(value);
			}
			catch (RuntimeException ex) {
				throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Timezone is invalid.");
			}
		}
		if ("system".equals(groupName) && "dateFormat".equals(key) && StringUtils.hasText(value)
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
	}

	private Map<String, Map<String, String>> defaultsCopy() {
		Map<String, Map<String, String>> copy = new LinkedHashMap<>();
		DEFAULTS.forEach((group, values) -> copy.put(group, new LinkedHashMap<>(values)));
		return copy;
	}

	private static Map<String, String> ordered(Map<String, String> values) {
		return new LinkedHashMap<>(values);
	}
}
