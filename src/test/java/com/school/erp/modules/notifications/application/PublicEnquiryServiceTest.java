package com.school.erp.modules.notifications.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.school.erp.common.exception.BusinessException;
import com.school.erp.modules.notifications.api.dto.PublicEnquiryRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PublicEnquiryServiceTest {

	@Mock
	private NotificationProvider notificationProvider;

	private PublicEnquiryService publicEnquiryService;

	@BeforeEach
	void setUp() {
		publicEnquiryService = new PublicEnquiryService(
				notificationProvider,
				new PublicSiteProperties("Star International School", "info@starinternationalschool.com"));
	}

	@Test
	void validEnquirySendsSchoolEmailAndAcknowledgement() {
		when(notificationProvider.sendEmail(
				eq("info@starinternationalschool.com"),
				eq("New Admission Enquiry - Asha Student"),
				contains("Parent Name: Riya Parent")))
				.thenReturn(NotificationDeliveryResult.sent("smtp", "school-ref", "accepted"));
		when(notificationProvider.sendEmail(
				eq("parent@example.com"),
				eq("Admission Enquiry Received - Star International School"),
				contains("Student Name: Asha Student")))
				.thenReturn(NotificationDeliveryResult.sent("smtp", "ack-ref", "accepted"));

		publicEnquiryService.submit(validRequest());

		InOrder inOrder = inOrder(notificationProvider);
		inOrder.verify(notificationProvider).sendEmail(
				eq("info@starinternationalschool.com"),
				eq("New Admission Enquiry - Asha Student"),
				contains("Mobile Number: +91 98765 43210"));
		inOrder.verify(notificationProvider).sendEmail(
				eq("parent@example.com"),
				eq("Admission Enquiry Received - Star International School"),
				contains("Thank you for contacting Star International School."));
	}

	@Test
	void schoolEmailDeliveryFailureThrowsBusinessException() {
		when(notificationProvider.sendEmail(
				eq("info@starinternationalschool.com"),
				eq("New Admission Enquiry - Asha Student"),
				contains("New admission enquiry received.")))
				.thenReturn(NotificationDeliveryResult.failed("smtp", "SMTP rejected"));

		assertThatThrownBy(() -> publicEnquiryService.submit(validRequest()))
				.isInstanceOf(BusinessException.class)
				.hasMessage("Unable to submit the enquiry. Please try again.");

		verify(notificationProvider, never()).sendEmail(
				eq("parent@example.com"),
				eq("Admission Enquiry Received - Star International School"),
				contains("Thank you"));
	}

	@Test
	void acknowledgementFailureDoesNotLoseSubmittedEnquiry() {
		when(notificationProvider.sendEmail(
				eq("info@starinternationalschool.com"),
				eq("New Admission Enquiry - Asha Student"),
				contains("New admission enquiry received.")))
				.thenReturn(NotificationDeliveryResult.sent("smtp", "school-ref", "accepted"));
		when(notificationProvider.sendEmail(
				eq("parent@example.com"),
				eq("Admission Enquiry Received - Star International School"),
				contains("Student Name: Asha Student")))
				.thenReturn(NotificationDeliveryResult.failed("smtp", "Mailbox unavailable"));

		publicEnquiryService.submit(validRequest());

		verify(notificationProvider).sendEmail(
				eq("info@starinternationalschool.com"),
				eq("New Admission Enquiry - Asha Student"),
				contains("Class Interested: Primary"));
		verify(notificationProvider).sendEmail(
				eq("parent@example.com"),
				eq("Admission Enquiry Received - Star International School"),
				contains("Our admission office will contact you shortly."));
	}

	private PublicEnquiryRequest validRequest() {
		return new PublicEnquiryRequest(
				" Asha Student ",
				" Riya Parent ",
				" +91 98765 43210 ",
				" parent@example.com ",
				" Primary ",
				" Please call after 4 PM. ");
	}
}
