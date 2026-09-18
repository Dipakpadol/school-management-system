package com.school.erp.modules.library.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LibraryPublisherRequest(
		@NotBlank @Size(max = 160) String name,
		@Size(max = 500) String contactInfo,
		Boolean active) {
}
