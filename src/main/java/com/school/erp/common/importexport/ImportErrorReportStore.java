package com.school.erp.common.importexport;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Component;

@Component
public class ImportErrorReportStore {

	private final ConcurrentMap<UUID, ImportResultDto> reports = new ConcurrentHashMap<>();

	public ImportResultDto save(ImportResultDto result) {
		reports.put(result.batchId(), result);
		return result;
	}

	public Optional<ImportResultDto> find(UUID batchId) {
		return Optional.ofNullable(reports.get(batchId));
	}
}
