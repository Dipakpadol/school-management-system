package com.school.erp.modules.notifications.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class HostelFeeReminderScheduler {

	private static final Logger LOGGER = LoggerFactory.getLogger(HostelFeeReminderScheduler.class);

	private final NotificationProperties properties;

	@Scheduled(cron = "${notifications.scheduler.hostel-fee-cron:0 15 9 * * *}")
	public void run() {
		if (!properties.getScheduler().isEnabled()) {
			return;
		}
		LOGGER.info("Hostel fee reminder scheduler executed.");
	}
}
