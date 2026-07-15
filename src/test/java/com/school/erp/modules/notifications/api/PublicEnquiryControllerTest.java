package com.school.erp.modules.notifications.api;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.school.erp.modules.notifications.application.NotificationDeliveryResult;
import com.school.erp.modules.notifications.application.NotificationProvider;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PublicEnquiryControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private NotificationProvider notificationProvider;

	@Test
	void validEnquirySubmissionIsPublicAndSendsBothEmails() throws Exception {
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

		mockMvc.perform(api(post("/api/v1/public/enquiries"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(validJson()))
				.andExpect(status().isNoContent());

		verify(notificationProvider).sendEmail(
				eq("info@starinternationalschool.com"),
				eq("New Admission Enquiry - Asha Student"),
				contains("Mobile Number: +91 98765 43210"));
		verify(notificationProvider).sendEmail(
				eq("parent@example.com"),
				eq("Admission Enquiry Received - Star International School"),
				contains("Thank you for contacting Star International School."));
	}

	@Test
	void invalidEmailReturnsBadRequest() throws Exception {
		mockMvc.perform(api(post("/api/v1/public/enquiries"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "studentName": "Asha Student",
						  "parentName": "Riya Parent",
						  "mobileNumber": "+91 98765 43210",
						  "email": "not-an-email",
						  "classInterested": "Primary",
						  "message": "Please call after 4 PM."
						}
						"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void missingRequiredFieldsReturnBadRequest() throws Exception {
		mockMvc.perform(api(post("/api/v1/public/enquiries"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "message": "Please call after 4 PM."
						}
						"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void schoolEmailDeliveryFailureReturnsUnprocessableEntity() throws Exception {
		when(notificationProvider.sendEmail(
				eq("info@starinternationalschool.com"),
				eq("New Admission Enquiry - Asha Student"),
				contains("New admission enquiry received.")))
				.thenReturn(NotificationDeliveryResult.failed("smtp", "SMTP rejected"));

		mockMvc.perform(api(post("/api/v1/public/enquiries"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(validJson()))
				.andExpect(status().isUnprocessableEntity());
	}

	@Test
	void notificationTestEmailEndpointRemainsProtected() throws Exception {
		mockMvc.perform(api(post("/api/v1/notifications/test/email"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "to": "test@example.com",
						  "subject": "Test",
						  "message": "Hello"
						}
						"""))
				.andExpect(status().isUnauthorized());
	}

	private String validJson() {
		return """
				{
				  "studentName": "Asha Student",
				  "parentName": "Riya Parent",
				  "mobileNumber": "+91 98765 43210",
				  "email": "parent@example.com",
				  "classInterested": "Primary",
				  "message": "Please call after 4 PM."
				}
				""";
	}

	private MockHttpServletRequestBuilder api(MockHttpServletRequestBuilder request) {
		return request.contextPath("/api");
	}
}
