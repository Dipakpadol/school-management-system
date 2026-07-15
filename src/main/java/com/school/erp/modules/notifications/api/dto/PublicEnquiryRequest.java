package com.school.erp.modules.notifications.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PublicEnquiryRequest(

        @NotBlank
        @Size(max = 150)
        String studentName,

        @NotBlank
        @Size(max = 150)
        String parentName,

        @NotBlank
        @Size(max = 20)
        String mobileNumber,

        @NotBlank
        @Email
        @Size(max = 180)
        String email,

        @NotBlank
        @Size(max = 100)
        String classInterested,

        @Size(max = 2000)
        String message
) {
}
