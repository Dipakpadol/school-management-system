package com.school.erp.modules.backup.api.dto;

import jakarta.validation.constraints.Size;

public record CreateBackupRequest(
		@Size(max = 1000) String notes) {
}
