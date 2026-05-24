package com.school.erp.modules.students.api.dto;

import java.time.LocalDate;

import com.school.erp.modules.students.domain.Gender;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

@Schema(description = "Student demographic and contact profile.")
public record StudentProfileRequest(
		@NotBlank @Size(max = 80) @Schema(example = "Aarav") String firstName,
		@Size(max = 80) @Schema(example = "Kumar") String middleName,
		@Size(max = 80) @Schema(example = "Sharma") String lastName,
		@NotNull @Past @Schema(example = "2014-08-17") LocalDate dateOfBirth,
		@NotNull @Schema(example = "MALE") Gender gender,
		@Size(max = 10) @Schema(example = "B+") String bloodGroup,
		@Email @Size(max = 160) @Schema(example = "aarav.sharma@student.school.test") String email,
		@Size(max = 30) @Schema(example = "+919876543210") String phoneNumber,
		@NotNull @PastOrPresent @Schema(example = "2026-04-01") LocalDate admissionDate,
		@Size(max = 160) @Schema(example = "Green Valley Kindergarten") String previousSchool,
		@Size(max = 160) @Schema(example = "12 MG Road") String addressLine1,
		@Size(max = 160) @Schema(example = "Near City Library") String addressLine2,
		@Size(max = 80) @Schema(example = "Bengaluru") String city,
		@Size(max = 80) @Schema(example = "Karnataka") String state,
		@Size(max = 20) @Schema(example = "560001") String postalCode,
		@Size(max = 80) @Schema(example = "India") String country) {
}
