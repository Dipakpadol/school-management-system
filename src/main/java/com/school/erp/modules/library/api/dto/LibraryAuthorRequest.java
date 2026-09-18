package com.school.erp.modules.library.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LibraryAuthorRequest(
		@NotBlank @Size(max = 160) String name,
		@Size(max = 1000) String biography,
		Boolean active) {
}
