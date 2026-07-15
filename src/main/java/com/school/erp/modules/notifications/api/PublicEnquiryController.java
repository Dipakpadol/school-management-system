package com.school.erp.modules.notifications.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.school.erp.modules.notifications.api.dto.PublicEnquiryRequest;
import com.school.erp.modules.notifications.application.PublicEnquiryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/public/enquiries")
@RequiredArgsConstructor
public class PublicEnquiryController {

    private final PublicEnquiryService publicEnquiryService;

    @PostMapping
    public ResponseEntity<Void> submit(@Valid @RequestBody PublicEnquiryRequest request) {
        publicEnquiryService.submit(request);

        return ResponseEntity.noContent().build();
    }
}
