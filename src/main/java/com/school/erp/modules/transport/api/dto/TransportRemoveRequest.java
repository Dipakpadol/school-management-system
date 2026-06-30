package com.school.erp.modules.transport.api.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Transport assignment removal payload.")
public record TransportRemoveRequest(LocalDate endDate) {
}
