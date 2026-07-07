package com.school.erp.modules.settings.api.dto;

import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Grouped application settings payload.")
public record ApplicationSettingsRequest(
		@NotNull Map<String, Map<String, String>> groups) {
}
