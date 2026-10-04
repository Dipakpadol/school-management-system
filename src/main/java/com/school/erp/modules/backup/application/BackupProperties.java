package com.school.erp.modules.backup.application;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "app.backup")
public record BackupProperties(
		String storagePath,
		String pgDumpPath,
		String pgRestorePath,
		Duration processTimeout,
		int retentionDays,
		int retainLast,
		String schedulerCron) {

	public BackupProperties {
		storagePath = StringUtils.hasText(storagePath) ? storagePath.trim() : "./var/backups";
		pgDumpPath = StringUtils.hasText(pgDumpPath) ? pgDumpPath.trim() : "pg_dump";
		pgRestorePath = StringUtils.hasText(pgRestorePath) ? pgRestorePath.trim() : "pg_restore";
		processTimeout = processTimeout == null ? Duration.ofMinutes(10) : processTimeout;
		retentionDays = retentionDays <= 0 ? 30 : retentionDays;
		retainLast = retainLast <= 0 ? 10 : retainLast;
		schedulerCron = StringUtils.hasText(schedulerCron) ? schedulerCron.trim() : "0 0 * * * *";
	}
}
