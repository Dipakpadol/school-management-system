package com.school.erp.common.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
			"/api/v1/fees/categories",
			"/api/v1/staff",
			"/api/v1/staff/attendance/daily?date=2026-09-01",
			"/api/v1/staff/leave-types",
			"/api/v1/staff/payroll",
			"/api/v1/communications",
			"/api/v1/library/books",
			"/api/v1/settings"
	})
	void protectedAuthStudentsAndFeesRoutesRejectAnonymousUsers(String path) throws Exception {
		mockMvc.perform(api(get(path)))
				.andExpect(status().isUnauthorized());
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"/api/v1/students",
			"/api/v1/fees/categories",
			"/api/v1/staff",
			"/api/v1/staff/attendance/daily?date=2026-09-01",
			"/api/v1/staff/leave-types",
			"/api/v1/staff/payroll",
			"/api/v1/communications",
			"/api/v1/library/books",
			"/api/v1/settings"
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
	void phase5StaffCreateRejectsAuthenticatedUserWithoutCreatePermission() throws Exception {
		mockMvc.perform(api(post("/api/v1/staff"))
				.with(user("limited-user"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "employeeCode": "EMP-SEC-001",
						  "firstName": "Security",
						  "lastName": "Check",
						  "email": "security.check@school.test",
						  "departmentId": "00000000-0000-0000-0000-000000000001",
						  "designationId": "00000000-0000-0000-0000-000000000002",
						  "joiningDate": "2026-09-01",
						  "staffType": "NON_TEACHING",
						  "status": "ACTIVE"
						}
						"""))
				.andExpect(status().isForbidden());
	}

	@Test
	void phase5LeaveApprovalRejectsAuthenticatedUserWithoutApprovePermission() throws Exception {
		mockMvc.perform(api(patch("/api/v1/staff/leaves/00000000-0000-0000-0000-000000000001/approve"))
				.with(user("limited-user"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "comment": "Approved"
						}
						"""))
				.andExpect(status().isForbidden());
	}

	@Test
	void phase5PayrollGenerationRejectsAuthenticatedUserWithoutProcessPermission() throws Exception {
		mockMvc.perform(api(post("/api/v1/staff/payroll/generate"))
				.with(user("limited-user"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "staffId": "00000000-0000-0000-0000-000000000001",
						  "payrollYear": 2026,
						  "payrollMonth": 9
						}
						"""))
				.andExpect(status().isForbidden());
	}

	@Test
	void phase5CommunicationCreateRejectsAuthenticatedUserWithoutCreatePermission() throws Exception {
		mockMvc.perform(api(post("/api/v1/communications"))
				.with(user("limited-user"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "type": "ANNOUNCEMENT",
						  "title": "Staff meeting",
						  "message": "Monthly meeting",
						  "audienceType": "STAFF",
						  "status": "DRAFT"
						}
						"""))
				.andExpect(status().isForbidden());
	}

	@Test
	void phase5SettingsUpdateRejectsAuthenticatedUserWithoutUpdatePermission() throws Exception {
		mockMvc.perform(api(put("/api/v1/settings"))
				.with(user("limited-user"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "groups": {
						    "application": {
						      "maintenanceMode": "false"
						    }
						  }
						}
						"""))
				.andExpect(status().isForbidden());
	}

	@Test
	void phase6LibraryBookCreateRejectsAuthenticatedUserWithoutCreatePermission() throws Exception {
		mockMvc.perform(api(post("/api/v1/library/books"))
				.with(user("limited-user"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "title": "Malgudi Days",
						  "isbn": "9788185986173",
						  "authorIds": [],
						  "publicationYear": 1943,
						  "language": "English",
						  "active": true
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
