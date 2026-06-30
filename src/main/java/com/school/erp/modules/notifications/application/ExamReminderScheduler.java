package com.school.erp.modules.notifications.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ExamReminderScheduler {

	private static final Logger LOGGER = LoggerFactory.getLogger(ExamReminderScheduler.class);

	private final NotificationProperties properties;

	@Scheduled(cron = "${notifications.scheduler.exam-reminder-cron:0 0 18 * * *}")
	public void run() {
		if (!properties.getScheduler().isEnabled()) {
			return;
		}
		LOGGER.info("Exam reminder scheduler executed.");
	}
}
