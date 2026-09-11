package com.school.erp.common.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiSecurityRegressionTest {

	@Autowired
	private MockMvc mockMvc;

	@ParameterizedTest
	@ValueSource(strings = {
			"/api/v1/auth/me",
			"/api/v1/students",
			"/api/v1/fees/categories"
	})
	void protectedAuthStudentsAndFeesRoutesRejectAnonymousUsers(String path) throws Exception {
		mockMvc.perform(api(get(path)))
				.andExpect(status().isUnauthorized());
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"/api/v1/students",
			"/api/v1/fees/categories"
	})
	void protectedReadRoutesRejectAuthenticatedUsersWithoutRequiredRolesOrPermissions(String path) throws Exception {
		mockMvc.perform(api(get(path)).with(user("limited-user")))
				.andExpect(status().isForbidden());
	}

	@Test
	void meAllowsAuthenticatedUsersWithoutModulePermissions() throws Exception {
		mockMvc.perform(api(get("/api/v1/auth/me")).with(user("limited-user")))
				.andExpect(status().isOk());
	}

	@Test
	void changePasswordRejectsAuthenticatedUserWithoutPasswordChangePermission() throws Exception {
		mockMvc.perform(api(post("/api/v1/auth/change-password"))
				.with(user("limited-user"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "currentPassword": "OldPassword123",
						  "newPassword": "NewPassword123",
						  "confirmPassword": "NewPassword123"
						}
						"""))
				.andExpect(status().isForbidden());
	}

	@Test
	void studentAdmissionRejectsAuthenticatedUserWithoutCreatePermission() throws Exception {
		mockMvc.perform(api(post("/api/v1/students/admissions"))
				.with(user("limited-user"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "admissionNumber": "ADM-2026-9001",
						  "profile": {
						    "firstName": "Aarav",
						    "lastName": "Sharma",
						    "dateOfBirth": "2014-08-17",
						    "gender": "MALE",
						    "admissionDate": "2026-04-01"
						  },
						  "parents": [
						    {
						      "relationType": "FATHER",
						      "primaryContact": true,
						      "emergencyContact": true,
						      "pickupAllowed": true,
						      "parent": {
						        "firstName": "Rajesh",
						        "lastName": "Sharma",
						        "phoneNumber": "+919812345678"
						      }
						    }
						  ],
						  "classAssignment": {
						    "academicYear": "2026-2027",
						    "className": "Class 6",
						    "sectionName": "A",
						    "effectiveFrom": "2026-04-01"
						  },
						  "documents": []
						}
						"""))
				.andExpect(status().isForbidden());
	}

	@Test
	void feeCategoryCreateRejectsAuthenticatedUserWithoutManagePermission() throws Exception {
		mockMvc.perform(api(post("/api/v1/fees/categories"))
				.with(user("limited-user"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "code": "TUITION",
						  "name": "Tuition Fee",
						  "description": "Core academic tuition.",
						  "active": true,
						  "sortOrder": 1
						}
						"""))
				.andExpect(status().isForbidden());
	}

	@Test
	void loginRejectsInvalidPayloadBeforeAuthenticationWork() throws Exception {
		mockMvc.perform(api(post("/api/v1/auth/login"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "email": "not-an-email",
						  "password": "short"
						}
						"""))
				.andExpect(status().isBadRequest());
	}

	private MockHttpServletRequestBuilder api(MockHttpServletRequestBuilder request) {
		return request.contextPath("/api");
	}
}
