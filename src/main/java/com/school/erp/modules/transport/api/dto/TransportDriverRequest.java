package com.school.erp.modules.transport.api.dto;

import java.time.LocalDate;

import com.school.erp.modules.transport.domain.TransportStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Transport driver create/update payload.")
public record TransportDriverRequest(
		@NotBlank @Size(max = 80) String firstName,
		@Size(max = 80) String middleName,
		@Size(max = 80) String lastName,
		@NotBlank @Size(max = 30) String mobileNumber,
		@NotBlank @Size(max = 80) String licenseNumber,
		@NotNull @Future LocalDate licenseExpiryDate,
		@Size(max = 500) String address,
		TransportStatus status) {
}
