package com.school.erp.modules.auth.api.dto;

import com.school.erp.modules.users.domain.RoleName;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest(
		@NotBlank @Size(max = 80) String firstName,
		@Size(max = 80) String middleName,
		@NotBlank @Size(max = 80) String lastName,
		@NotBlank @Email @Size(max = 160) String email,
		@NotBlank @Pattern(regexp = "^[0-9+\\-\\s]{7,30}$", message = "Mobile number is invalid") String mobileNumber,
		@NotNull RoleName role,
		@NotBlank @Size(min = 8, max = 72) String password,
		@NotBlank @Size(min = 8, max = 72) String confirmPassword) {
}
