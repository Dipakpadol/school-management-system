package com.school.erp.modules.notifications.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class FeeOverdueReminderScheduler {

	private static final Logger LOGGER = LoggerFactory.getLogger(FeeOverdueReminderScheduler.class);

	private final NotificationProperties properties;

	@Scheduled(cron = "${notifications.scheduler.fee-overdue-cron:0 30 9 * * *}")
	public void run() {
		if (!properties.getScheduler().isEnabled()) {
			return;
		}
		LOGGER.info("Fee overdue reminder scheduler executed.");
	}
}
