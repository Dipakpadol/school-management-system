package com.school.erp.modules.settings.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.school.erp.common.audit.application.AuditLogService;
import com.school.erp.common.exception.BusinessException;
import com.school.erp.common.exception.ErrorCode;
import com.school.erp.modules.settings.api.dto.ApplicationSettingsRequest;
import com.school.erp.modules.settings.domain.ApplicationSetting;
import com.school.erp.modules.settings.infrastructure.ApplicationSettingRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApplicationSettingsServiceTest {

	@Mock
	private ApplicationSettingRepository settingRepository;

	@Mock
	private AuditLogService auditLogService;

	private ApplicationSettingsService settingsService;

	@BeforeEach
	void setUp() {
		settingsService = new ApplicationSettingsService(settingRepository, auditLogService);
	}

	@Test
	void getSettingsIncludesPhase5GroupsAndMasksSensitiveValues() {
		when(settingRepository.findAllByDeletedFalseOrderByGroupNameAscSettingKeyAsc()).thenReturn(List.of(
				new ApplicationSetting("schoolProfile", "schoolName", "Star International School"),
				new ApplicationSetting("email", "smtpPassword", "smtp-secret"),
				new ApplicationSetting("sms", "apiKey", "sms-key"),
				new ApplicationSetting("sms", "apiSecret", "sms-secret")));

		var response = settingsService.getSettings();

		assertThat(response.groups().keySet()).contains(
				"schoolProfile",
				"academic",
				"exams",
				"fees",
				"notifications",
				"email",
				"sms",
				"application",
				"security",
				"backup");
		assertThat(response.groups().get("schoolProfile")).containsEntry("schoolName", "Star International School");
		assertThat(response.groups().get("email")).containsEntry("smtpPassword", "********");
		assertThat(response.groups().get("sms")).containsEntry("apiKey", "********");
		assertThat(response.groups().get("sms")).containsEntry("apiSecret", "********");
	}

	@Test
	void updateSettingsSkipsMaskedSensitiveValuesAndPersistsVisibleFields() {
		when(settingRepository.findAllByDeletedFalseOrderByGroupNameAscSettingKeyAsc()).thenReturn(List.of(
				new ApplicationSetting("email", "smtpPassword", "smtp-secret")));
		when(settingRepository.findByGroupNameIgnoreCaseAndSettingKeyIgnoreCaseAndDeletedFalse("email", "smtpHost"))
				.thenReturn(Optional.empty());
		when(settingRepository.save(any(ApplicationSetting.class))).thenAnswer(invocation -> invocation.getArgument(0));

		settingsService.updateSettings(new ApplicationSettingsRequest(Map.of(
				"email",
				Map.of(
						"smtpPassword", "********",
						"smtpHost", "smtp.school.test"))));

		verify(settingRepository).save(argThat(setting ->
				"email".equals(setting.getGroupName())
						&& "smtpHost".equals(setting.getSettingKey())
						&& "smtp.school.test".equals(setting.getSettingValue())));
		verify(settingRepository, never()).save(argThat(setting -> "smtpPassword".equals(setting.getSettingKey())));
	}

	@Test
	void updateSettingsRejectsUnknownGroupsAndInvalidValues() {
		when(settingRepository.findAllByDeletedFalseOrderByGroupNameAscSettingKeyAsc()).thenReturn(List.of());

		assertThatThrownBy(() -> settingsService.updateSettings(new ApplicationSettingsRequest(Map.of(
				"unknown",
				Map.of("enabled", "true")))))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.VALIDATION_ERROR);

		assertThatThrownBy(() -> settingsService.updateSettings(new ApplicationSettingsRequest(Map.of(
				"notifications",
				Map.of("smsEnabled", "sometimes")))))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.VALIDATION_ERROR);

		assertThatThrownBy(() -> settingsService.updateSettings(new ApplicationSettingsRequest(Map.of(
				"application",
				Map.of("timezone", "Not/A_Timezone")))))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.VALIDATION_ERROR);
	}
}
