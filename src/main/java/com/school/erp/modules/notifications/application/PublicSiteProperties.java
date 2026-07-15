package com.school.erp.modules.notifications.application;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "school.public-site")
public record PublicSiteProperties(
		String name,
		String enquiryEmail) {

	public PublicSiteProperties {
		name = StringUtils.hasText(name) ? name.trim() : "Star International School";
		enquiryEmail = StringUtils.hasText(enquiryEmail) ? enquiryEmail.trim() : "info@starinternationalschool.com";
	}
}
