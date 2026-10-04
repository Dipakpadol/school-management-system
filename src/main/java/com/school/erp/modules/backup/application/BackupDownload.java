package com.school.erp.modules.backup.application;

import java.nio.file.Path;

public record BackupDownload(
		String fileName,
		String contentType,
		long sizeBytes,
		Path path) {
}
