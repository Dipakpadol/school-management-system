package com.school.erp.modules.users.api.dto;

import java.util.Set;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
		@NotBlank @Email @Size(max = 160) String email,
		@NotBlank @Size(max = 80) String username,
		@NotBlank @Size(max = 80) String firstName,
		@Size(max = 80) String middleName,
		@Size(max = 80) String lastName,
		@Size(max = 30) String phoneNumber,
		@NotEmpty Set<@NotBlank @Size(max = 60) String> roles) {
}
