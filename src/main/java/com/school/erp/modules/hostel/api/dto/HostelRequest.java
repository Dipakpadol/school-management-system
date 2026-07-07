package com.school.erp.modules.hostel.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record HostelRequest(
		@NotBlank @Size(max = 60) @Schema(example = "GIRLS-A") String code,
		@NotBlank @Size(max = 160) @Schema(example = "Girls Hostel A") String name,
		@Size(max = 500) @Schema(example = "North campus block") String address,
		Boolean active) {

	public boolean activeFlag() {
		return active == null || active;
	}
}
