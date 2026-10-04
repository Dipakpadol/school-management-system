package com.school.erp.modules.backup.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RestoreBackupRequest(
		@NotBlank @Size(max = 40) String confirmationText,
		@Size(max = 1000) String notes) {
}
