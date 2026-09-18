package com.school.erp.modules.library.api.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LibraryBookRequest(
		@NotBlank @Size(max = 240) String title,
		@Size(max = 40) String isbn,
		UUID categoryId,
		UUID publisherId,
		List<UUID> authorIds,
		@Size(max = 80) String edition,
		@Min(1000) @Max(2200) Integer publicationYear,
		@Size(max = 80) String language,
		@Size(max = 1000) String description,
		@Size(max = 80) String shelfLocation,
		Boolean active) {
}
