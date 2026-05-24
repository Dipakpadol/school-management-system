package com.school.erp.modules.students.api.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Parent or guardian details used for student mapping.")
public record ParentGuardianRequest(
		@NotBlank @Size(max = 80) @Schema(example = "Rajesh") String firstName,
		@Size(max = 80) @Schema(example = "Sharma") String lastName,
		@Email @Size(max = 160) @Schema(example = "rajesh.sharma@example.com") String email,
		@NotBlank @Size(max = 30) @Schema(example = "+919812345678") String phoneNumber,
		@Size(max = 30) @Schema(example = "+919812345679") String alternatePhoneNumber,
		@Size(max = 120) @Schema(example = "Software Engineer") String occupation,
		@Size(max = 160) @Schema(example = "12 MG Road") String addressLine1,
		@Size(max = 160) @Schema(example = "Near City Library") String addressLine2,
		@Size(max = 80) @Schema(example = "Bengaluru") String city,
		@Size(max = 80) @Schema(example = "Karnataka") String state,
		@Size(max = 20) @Schema(example = "560001") String postalCode,
		@Size(max = 80) @Schema(example = "India") String country,
		@Schema(description = "Optional future link to a user account.", example = "1c91e20b-25a4-4e35-97f2-d2eba14b1894")
		UUID userAccountId) {
}
