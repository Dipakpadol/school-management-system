package com.school.erp.modules.notifications.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AttendanceAbsenceNotificationScheduler {

	private static final Logger LOGGER = LoggerFactory.getLogger(AttendanceAbsenceNotificationScheduler.class);

	private final NotificationProperties properties;

	@Scheduled(cron = "${notifications.scheduler.absence-cron:0 0 17 * * *}")
	public void run() {
		if (!properties.getScheduler().isEnabled()) {
			return;
		}
		LOGGER.info("Attendance absence notification scheduler executed.");
	}
}
