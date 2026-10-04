package com.school.erp.modules.backup.application;

import java.time.Duration;
import java.util.List;
import java.util.Map;

public interface BackupCommandRunner {

	BackupCommandResult run(List<String> command, Map<String, String> environment, Duration timeout);
}
