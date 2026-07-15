package com.school.erp.modules.notifications.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.school.erp.common.exception.BusinessException;
import com.school.erp.common.exception.ErrorCode;
import com.school.erp.modules.notifications.api.dto.PublicEnquiryRequest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PublicEnquiryService {

    private static final Logger log = LoggerFactory.getLogger(PublicEnquiryService.class);

    private final NotificationProvider notificationProvider;
    private final PublicSiteProperties publicSiteProperties;

    public void submit(PublicEnquiryRequest request) {
        String subject = "New Admission Enquiry - " + normalized(request.studentName());

        String message = buildSchoolMessage(request);

        NotificationDeliveryResult schoolResult =
                notificationProvider.sendEmail(
                        publicSiteProperties.enquiryEmail(),
                        subject,
                        message
                );

        if (!schoolResult.success()) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION,
                    "Unable to submit the enquiry. Please try again."
            );
        }

        sendAcknowledgement(request);
    }

    private void sendAcknowledgement(
            PublicEnquiryRequest request
    ) {
        if (!StringUtils.hasText(request.email())) {
            return;
        }

        String subject =
                "Admission Enquiry Received - " + publicSiteProperties.name();

        String message = """
                Dear %s,

                Thank you for contacting %s.

                We have received the admission enquiry for:

                Student Name: %s
                Class Interested: %s

                Our admission office will contact you shortly.

                Regards,
                Admission Office
                %s
                """.formatted(
                normalized(request.parentName()),
                publicSiteProperties.name(),
                normalized(request.studentName()),
                normalized(request.classInterested()),
                publicSiteProperties.name()
        );

        NotificationDeliveryResult acknowledgementResult = notificationProvider.sendEmail(
                normalized(request.email()),
                subject,
                message
        );

        if (!acknowledgementResult.success()) {
            log.warn(
                    "Admission enquiry acknowledgement email failed for {} via {}: {}",
                    normalized(request.email()),
                    acknowledgementResult.provider(),
                    acknowledgementResult.errorMessage());
        }
    }

    private String buildSchoolMessage(
            PublicEnquiryRequest request
    ) {
        return """
                New admission enquiry received.

                Student Name: %s
                Parent Name: %s
                Mobile Number: %s
                Email: %s
                Class Interested: %s

                Message:
                %s

                Please contact the parent regarding the admission enquiry.
                """.formatted(
                normalized(request.studentName()),
                normalized(request.parentName()),
                normalized(request.mobileNumber()),
                normalized(request.email()),
                normalized(request.classInterested()),
                StringUtils.hasText(request.message())
                        ? request.message().trim()
                        : "No message provided"
        );
    }

    private String normalized(String value) {
        return value == null ? "" : value.trim();
    }
}
