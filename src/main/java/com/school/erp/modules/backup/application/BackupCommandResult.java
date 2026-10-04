package com.school.erp.modules.backup.application;

public record BackupCommandResult(
		int exitCode,
		String output) {

	public boolean successful() {
		return exitCode == 0;
	}
}
