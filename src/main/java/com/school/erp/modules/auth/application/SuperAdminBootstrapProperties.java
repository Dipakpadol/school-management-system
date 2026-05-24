package com.school.erp.modules.auth.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.bootstrap-super-admin")
public record SuperAdminBootstrapProperties(
		boolean enabled,
		String email,
		String password,
		String firstName,
		String lastName) {

	public SuperAdminBootstrapProperties {
		firstName = firstName == null || firstName.isBlank() ? "Super" : firstName;
		lastName = lastName == null || lastName.isBlank() ? "Admin" : lastName;
	}
}
