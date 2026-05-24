package com.school.erp.modules.users.api.dto;

import java.util.Set;

import com.school.erp.modules.users.domain.RoleName;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(
		@NotBlank @Email @Size(max = 160) String email,
		@NotBlank @Size(max = 80) String username,
		@NotBlank @Size(max = 80) String firstName,
		@Size(max = 80) String lastName,
		@Size(max = 30) String phoneNumber,
		@NotBlank @Size(min = 8, max = 72) String password,
		@NotEmpty Set<RoleName> roles) {
}
