package com.school.erp.modules.settings.api.dto;

import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Grouped application settings.")
public record ApplicationSettingsResponse(Map<String, Map<String, String>> groups) {
}
