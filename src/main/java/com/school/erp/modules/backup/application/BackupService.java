package com.school.erp.modules.backup.application;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

import com.school.erp.common.api.PageRequestDto;
import com.school.erp.common.api.PageResponse;
import com.school.erp.common.audit.application.AuditLogEvent;
import com.school.erp.common.audit.application.AuditLogService;
import com.school.erp.common.exception.BusinessException;
import com.school.erp.common.exception.ErrorCode;
import com.school.erp.common.exception.ResourceNotFoundException;
import com.school.erp.modules.backup.api.dto.BackupRecordResponse;
import com.school.erp.modules.backup.api.dto.BackupSummaryResponse;
import com.school.erp.modules.backup.api.dto.CreateBackupRequest;
import com.school.erp.modules.backup.api.dto.RestoreBackupRequest;
import com.school.erp.modules.backup.api.dto.RestoreHistoryResponse;
import com.school.erp.modules.backup.domain.BackupRecord;
import com.school.erp.modules.backup.domain.BackupStatus;
import com.school.erp.modules.backup.domain.BackupType;
import com.school.erp.modules.backup.domain.RestoreHistory;
import com.school.erp.modules.backup.domain.RestoreStatus;
import com.school.erp.modules.backup.infrastructure.BackupRecordRepository;
import com.school.erp.modules.backup.infrastructure.RestoreHistoryRepository;
import com.school.erp.modules.settings.application.ApplicationSettingsService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BackupService {

	private static final Logger log = LoggerFactory.getLogger(BackupService.class);
	private static final String MODULE_NAME = "BACKUP";
	private static final String RESTORE_CONFIRMATION = "RESTORE";
	private static final List<BackupStatus> DOWNLOADABLE_STATUSES = List.of(BackupStatus.COMPLETED, BackupStatus.RESTORED);
	private static final List<BackupStatus> STORAGE_STATUSES = List.of(BackupStatus.COMPLETED, BackupStatus.RESTORED);

	private final BackupRecordRepository backupRepository;
	private final RestoreHistoryRepository restoreHistoryRepository;
	private final BackupCommandRunner commandRunner;
	private final BackupProperties properties;
	private final DataSourceProperties dataSourceProperties;
	private final ApplicationSettingsService settingsService;
	private final JdbcTemplate jdbcTemplate;
	private final BackupMapper mapper;
	private final AuditLogService auditLogService;
	private final ReentrantLock operationLock = new ReentrantLock();

	@Transactional(readOnly = true)
	public BackupSummaryResponse summary() {
		BackupRecord lastSuccessful = backupRepository
				.findTopByStatusInAndDeletedFalseOrderByCompletedAtDesc(STORAGE_STATUSES)
				.orElse(null);
		BackupRecord lastFailed = backupRepository
				.findTopByStatusAndDeletedFalseOrderByCompletedAtDesc(BackupStatus.FAILED)
				.orElse(null);
		long completedCount = backupRepository.countByStatusAndDeletedFalse(BackupStatus.COMPLETED)
				+ backupRepository.countByStatusAndDeletedFalse(BackupStatus.RESTORED);
		long failedCount = backupRepository.countByStatusAndDeletedFalse(BackupStatus.FAILED);
		Long ageHours = lastSuccessful == null || lastSuccessful.getCompletedAt() == null
				? null
				: ChronoUnit.HOURS.between(lastSuccessful.getCompletedAt(), Instant.now());
		return new BackupSummaryResponse(
				mapper.toResponse(lastSuccessful),
				mapper.toResponse(lastFailed),
				completedCount,
				failedCount,
				backupRepository.totalSizeByStatusIn(STORAGE_STATUSES),
				ageHours,
				booleanSetting("backupEnabled", false),
				stringSetting("backupFrequency", "DAILY"),
				stringSetting("backupTime", "02:00"),
				intSetting("backupRetentionDays", properties.retentionDays()),
				intSetting("backupRetentionCount", properties.retainLast()),
				StringUtils.hasText(storagePathSetting()),
				toolAvailable(properties.pgDumpPath()),
				toolAvailable(properties.pgRestorePath()),
				true,
				false,
				latestDatabaseVersion());
	}

	@Transactional(readOnly = true)
	public PageResponse<BackupRecordResponse> backups(
			BackupStatus status,
			BackupType backupType,
			String keyword,
			PageRequestDto pageRequest) {
		return PageResponse.from(
				backupRepository.search(
						status,
						backupType,
						containsLowerOrNull(keyword),
						PageRequest.of(
								pageRequest.page(),
								pageRequest.size(),
								Sort.by(Sort.Direction.DESC, "startedAt"))),
				mapper::toResponse);
	}

	@Transactional(readOnly = true)
	public PageResponse<RestoreHistoryResponse> restoreHistory(PageRequestDto pageRequest) {
		return PageResponse.from(
				restoreHistoryRepository.findAllByDeletedFalse(PageRequest.of(
						pageRequest.page(),
						pageRequest.size(),
						Sort.by(Sort.Direction.DESC, "startedAt"))),
				mapper::toResponse);
	}

	public BackupRecordResponse createBackup(CreateBackupRequest request) {
		if (!operationLock.tryLock()) {
			throw new BusinessException(ErrorCode.CONFLICT, "Another backup or restore operation is already running.");
		}
		try {
			BackupRecord backup = runDatabaseBackup(false, null, request == null ? null : request.notes());
			cleanupRetention(backup.getId());
			return mapper.toResponse(backup);
		}
		finally {
			operationLock.unlock();
		}
	}

	public BackupDownload download(UUID backupId) {
		BackupRecord backup = loadBackup(backupId);
		if (!DOWNLOADABLE_STATUSES.contains(backup.getStatus())) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Only completed backups can be downloaded.");
		}
		Path path = validatedRecordPath(backup);
		if (!Files.isRegularFile(path)) {
			throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Backup file is no longer available.");
		}
		try {
			return new BackupDownload(
					backup.getFileName(),
					"application/octet-stream",
					Files.size(path),
					path);
		}
		catch (IOException ex) {
			throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Unable to prepare backup download.");
		}
	}

	public RestoreHistoryResponse restore(UUID backupId, RestoreBackupRequest request) {
		if (request == null || !RESTORE_CONFIRMATION.equals(request.confirmationText())) {
			throw new BusinessException(
					ErrorCode.VALIDATION_ERROR,
					"Type RESTORE to confirm this database restore operation.");
		}
		if (!operationLock.tryLock()) {
			throw new BusinessException(ErrorCode.CONFLICT, "Another backup or restore operation is already running.");
		}
		try {
			BackupRecord backup = loadBackup(backupId);
			if (!DOWNLOADABLE_STATUSES.contains(backup.getStatus())) {
				throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Only completed backups can be restored.");
			}
			Path backupFile = validatedRecordPath(backup);
			validateChecksum(backup, backupFile);
			RestoreHistory restore = restoreHistoryRepository.save(new RestoreHistory(
					backup,
					currentActor(),
					true,
					request.notes()));
			try {
				validateRestoreArchive(backupFile);
				BackupRecord safetyBackup = runDatabaseBackup(
						true,
						backup,
						"Safety backup before restoring " + backup.getFileName());
				restore.attachSafetyBackup(safetyBackup);
				restoreHistoryRepository.save(restore);
				runRestoreCommand(backupFile);
				backup.markRestored();
				backupRepository.save(backup);
				restore.markCompleted();
				restoreHistoryRepository.save(restore);
				audit("RestoreHistory", restore.getId(), "RESTORE_COMPLETED", null, mapper.toResponse(restore));
				return mapper.toResponse(restore);
			}
			catch (RuntimeException ex) {
				restore.markFailed(sanitize(ex.getMessage()));
				restoreHistoryRepository.save(restore);
				audit("RestoreHistory", restore.getId(), "RESTORE_FAILED", null, mapper.toResponse(restore));
				throw ex;
			}
		}
		finally {
			operationLock.unlock();
		}
	}

	public BackupRecordResponse deleteBackup(UUID backupId) {
		BackupRecord backup = loadBackup(backupId);
		if (backup.getStatus() == BackupStatus.RUNNING) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Running backups cannot be deleted.");
		}
		Path path = validatedRecordPath(backup);
		try {
			Files.deleteIfExists(path);
			BackupRecordResponse oldValue = mapper.toResponse(backup);
			backup.markDeleted(currentActor());
			BackupRecord saved = backupRepository.save(backup);
			audit("BackupRecord", backupId, "DELETE", oldValue, mapper.toResponse(saved));
			return mapper.toResponse(saved);
		}
		catch (IOException ex) {
			throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Unable to delete backup file.");
		}
	}

	@Scheduled(cron = "${app.backup.scheduler-cron:0 0 * * * *}")
	public void scheduledBackup() {
		if (!booleanSetting("backupEnabled", false) || !scheduledBackupDue()) {
			return;
		}
		if (!operationLock.tryLock()) {
			log.info("Skipping scheduled backup because another backup or restore operation is running.");
			return;
		}
		try {
			BackupRecord backup = runDatabaseBackup(false, null, "Scheduled database backup.");
			cleanupRetention(backup.getId());
		}
		catch (RuntimeException ex) {
			log.warn("Scheduled database backup failed: {}", sanitize(ex.getMessage()));
		}
		finally {
			operationLock.unlock();
		}
	}

	private BackupRecord runDatabaseBackup(boolean preRestoreSafety, BackupRecord sourceBackup, String notes) {
		Path storageRoot = storageRoot();
		String fileName = backupFileName(preRestoreSafety);
		Path target = safeResolve(storageRoot, fileName);
		DatabaseTarget database = databaseTarget();
		BackupRecord backup = backupRepository.save(new BackupRecord(
				BackupType.DATABASE,
				fileName,
				target.toString(),
				currentActor(),
				latestDatabaseVersion(),
				applicationVersion(),
				"POSTGRES_CUSTOM",
				preRestoreSafety,
				sourceBackup,
				notes));
		try {
			Files.createDirectories(storageRoot);
			BackupCommandResult result = commandRunner.run(
					List.of(
							properties.pgDumpPath(),
							"-Fc",
							"--file",
							target.toString(),
							"--host",
							database.host(),
							"--port",
							String.valueOf(database.port()),
							"--username",
							database.username(),
							database.databaseName()),
					database.environment(),
					properties.processTimeout());
			if (!result.successful()) {
				throw new BusinessException(
						ErrorCode.BUSINESS_RULE_VIOLATION,
						"Database backup failed: " + sanitize(result.output()));
			}
			if (!Files.isRegularFile(target) || Files.size(target) == 0) {
				throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Database backup file was not created.");
			}
			backup.markCompleted(Files.size(target), sha256(target), toolVersion(properties.pgDumpPath()));
			BackupRecord saved = backupRepository.save(backup);
			audit("BackupRecord", saved.getId(), preRestoreSafety ? "SAFETY_BACKUP_COMPLETED" : "BACKUP_COMPLETED", null, mapper.toResponse(saved));
			return saved;
		}
		catch (BusinessException ex) {
			backup.markFailed(sanitize(ex.getMessage()));
			BackupRecord saved = backupRepository.save(backup);
			audit("BackupRecord", saved.getId(), preRestoreSafety ? "SAFETY_BACKUP_FAILED" : "BACKUP_FAILED", null, mapper.toResponse(saved));
			throw ex;
		}
		catch (IOException ex) {
			backup.markFailed(sanitize(ex.getMessage()));
			BackupRecord saved = backupRepository.save(backup);
			audit("BackupRecord", saved.getId(), preRestoreSafety ? "SAFETY_BACKUP_FAILED" : "BACKUP_FAILED", null, mapper.toResponse(saved));
			throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Unable to write database backup file.");
		}
	}

	private void validateRestoreArchive(Path backupFile) {
		BackupCommandResult result = commandRunner.run(
				List.of(properties.pgRestorePath(), "--list", backupFile.toString()),
				Map.of(),
				shortTimeout());
		if (!result.successful()) {
			throw new BusinessException(
					ErrorCode.BUSINESS_RULE_VIOLATION,
					"Backup archive validation failed: " + sanitize(result.output()));
		}
	}

	private void runRestoreCommand(Path backupFile) {
		DatabaseTarget database = databaseTarget();
		BackupCommandResult result = commandRunner.run(
				List.of(
						properties.pgRestorePath(),
						"--clean",
						"--if-exists",
						"--no-owner",
						"--no-privileges",
						"--exit-on-error",
						"--host",
						database.host(),
						"--port",
						String.valueOf(database.port()),
						"--username",
						database.username(),
						"--dbname",
						database.databaseName(),
						backupFile.toString()),
				database.environment(),
				properties.processTimeout());
		if (!result.successful()) {
			throw new BusinessException(
					ErrorCode.BUSINESS_RULE_VIOLATION,
					"Database restore failed: " + sanitize(result.output()));
		}
	}

	private void cleanupRetention(UUID protectedBackupId) {
		int retainLast = intSetting("backupRetentionCount", properties.retainLast());
		int retentionDays = intSetting("backupRetentionDays", properties.retentionDays());
		Instant cutoff = Instant.now().minus(Math.max(retentionDays, 1), ChronoUnit.DAYS);
		List<BackupRecord> backups = backupRepository.findAllByStatusInAndDeletedFalseOrderByCompletedAtDesc(
				List.of(BackupStatus.COMPLETED, BackupStatus.RESTORED, BackupStatus.FAILED));
		int retained = 0;
		for (BackupRecord backup : backups) {
			if (backup.getId() != null && backup.getId().equals(protectedBackupId)) {
				retained++;
				continue;
			}
			if (retained < retainLast) {
				retained++;
				continue;
			}
			Instant completedAt = backup.getCompletedAt();
			if (completedAt == null || completedAt.isAfter(cutoff)) {
				continue;
			}
			deleteBackupFileForRetention(backup);
		}
	}

	private void deleteBackupFileForRetention(BackupRecord backup) {
		try {
			Files.deleteIfExists(validatedRecordPath(backup));
			BackupRecordResponse oldValue = mapper.toResponse(backup);
			backup.markDeleted("system");
			BackupRecord saved = backupRepository.save(backup);
			audit("BackupRecord", saved.getId(), "RETENTION_DELETE", oldValue, mapper.toResponse(saved));
		}
		catch (RuntimeException | IOException ex) {
			log.warn("Backup retention could not delete {}: {}", backup.getFileName(), sanitize(ex.getMessage()));
		}
	}

	private boolean scheduledBackupDue() {
		BackupRecord lastSuccessful = backupRepository
				.findTopByStatusInAndDeletedFalseOrderByCompletedAtDesc(STORAGE_STATUSES)
				.orElse(null);
		if (lastSuccessful == null || lastSuccessful.getCompletedAt() == null) {
			return true;
		}
		long ageHours = ChronoUnit.HOURS.between(lastSuccessful.getCompletedAt(), Instant.now());
		return switch (stringSetting("backupFrequency", "DAILY").toUpperCase()) {
			case "HOURLY" -> ageHours >= 1;
			case "WEEKLY" -> ageHours >= 24 * 7;
			case "MONTHLY" -> ageHours >= 24 * 28;
			default -> ageHours >= 24;
		};
	}

	private BackupRecord loadBackup(UUID backupId) {
		return backupRepository.findByIdAndDeletedFalse(backupId)
				.orElseThrow(() -> new ResourceNotFoundException("Backup", backupId));
	}

	private Path storageRoot() {
		return Path.of(storagePathSetting()).toAbsolutePath().normalize();
	}

	private String storagePathSetting() {
		String configured = stringSetting("backupLocation", "");
		return StringUtils.hasText(configured) ? configured : properties.storagePath();
	}

	private Path safeResolve(Path root, String fileName) {
		Path resolved = root.resolve(fileName).normalize();
		if (!resolved.startsWith(root) || !resolved.getFileName().toString().equals(fileName)) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Backup file path is invalid.");
		}
		return resolved;
	}

	private Path validatedRecordPath(BackupRecord backup) {
		Path root = storageRoot();
		Path path = Path.of(backup.getStoragePath()).toAbsolutePath().normalize();
		if (!path.startsWith(root) || !path.getFileName().toString().equals(backup.getFileName())) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Backup file metadata is invalid.");
		}
		return path;
	}

	private void validateChecksum(BackupRecord backup, Path path) {
		if (!StringUtils.hasText(backup.getChecksumSha256())) {
			return;
		}
		try {
			String actual = sha256(path);
			if (!backup.getChecksumSha256().equalsIgnoreCase(actual)) {
				throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Backup checksum validation failed.");
			}
		}
		catch (IOException ex) {
			throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Backup file is no longer available.");
		}
	}

	private String sha256(Path path) throws IOException {
		try (InputStream input = Files.newInputStream(path);
				DigestInputStream digestInput = new DigestInputStream(input, messageDigest())) {
			digestInput.transferTo(java.io.OutputStream.nullOutputStream());
			return HexFormat.of().formatHex(digestInput.getMessageDigest().digest());
		}
	}

	private MessageDigest messageDigest() {
		try {
			return MessageDigest.getInstance("SHA-256");
		}
		catch (java.security.NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 is not available.", ex);
		}
	}

	private DatabaseTarget databaseTarget() {
		String url = dataSourceProperties.getUrl();
		if (!StringUtils.hasText(url) || !url.startsWith("jdbc:postgresql:")) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Database backup requires a PostgreSQL datasource.");
		}
		String raw = url.substring("jdbc:postgresql:".length());
		String host = "localhost";
		int port = 5432;
		String databaseName;
		if (raw.startsWith("//")) {
			URI uri = URI.create(raw);
			host = uri.getHost() == null ? host : uri.getHost();
			port = uri.getPort() < 0 ? port : uri.getPort();
			String path = uri.getPath();
			databaseName = path == null || path.length() <= 1 ? "" : path.substring(1);
		}
		else {
			databaseName = raw;
		}
		int queryIndex = databaseName.indexOf('?');
		if (queryIndex >= 0) {
			databaseName = databaseName.substring(0, queryIndex);
		}
		if (!StringUtils.hasText(databaseName)) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "PostgreSQL database name could not be resolved.");
		}
		return new DatabaseTarget(
				host,
				port,
				databaseName,
				StringUtils.hasText(dataSourceProperties.getUsername()) ? dataSourceProperties.getUsername() : "",
				dataSourceProperties.getPassword());
	}

	private String latestDatabaseVersion() {
		try {
			return jdbcTemplate.queryForObject(
					"select version from flyway_schema_history where success = true order by installed_rank desc limit 1",
					String.class);
		}
		catch (RuntimeException ex) {
			return "unknown";
		}
	}

	private String applicationVersion() {
		String envVersion = System.getenv("APP_VERSION");
		if (StringUtils.hasText(envVersion)) {
			return envVersion.trim();
		}
		String packageVersion = BackupService.class.getPackage().getImplementationVersion();
		return StringUtils.hasText(packageVersion) ? packageVersion : "0.0.1-SNAPSHOT";
	}

	private boolean toolAvailable(String toolPath) {
		if (!StringUtils.hasText(toolPath)) {
			return false;
		}
		return commandRunner.run(List.of(toolPath, "--version"), Map.of(), shortTimeout()).successful();
	}

	private String toolVersion(String toolPath) {
		BackupCommandResult result = commandRunner.run(List.of(toolPath, "--version"), Map.of(), shortTimeout());
		return result.successful() ? sanitize(result.output()) : null;
	}

	private Duration shortTimeout() {
		return Duration.ofSeconds(Math.min(Math.max(properties.processTimeout().toSeconds(), 1), 10));
	}

	private String backupFileName(boolean safetyBackup) {
		String prefix = safetyBackup ? "school-erp-db-safety" : "school-erp-db";
		String timestamp = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
				.withZone(java.time.ZoneOffset.UTC)
				.format(Instant.now());
		return prefix + "-" + timestamp + "-" + UUID.randomUUID().toString().substring(0, 8) + ".backup";
	}

	private boolean booleanSetting(String key, boolean fallback) {
		return Boolean.parseBoolean(stringSetting(key, Boolean.toString(fallback)));
	}

	private int intSetting(String key, int fallback) {
		try {
			String value = stringSetting(key, String.valueOf(fallback));
			return StringUtils.hasText(value) ? Math.max(1, Integer.parseInt(value)) : fallback;
		}
		catch (RuntimeException ex) {
			return fallback;
		}
	}

	private String stringSetting(String key, String fallback) {
		try {
			String value = settingsService.rawSettingValue("backup", key);
			return StringUtils.hasText(value) ? value.trim() : fallback;
		}
		catch (RuntimeException ex) {
			return fallback;
		}
	}

	private String blankToNull(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}

	private String containsLowerOrNull(String value) {
		String normalized = blankToNull(value);
		return normalized == null ? null : "%" + normalized.toLowerCase(Locale.ROOT) + "%";
	}

	private String sanitize(String value) {
		if (!StringUtils.hasText(value)) {
			return "No command output was returned.";
		}
		String sanitized = value.replaceAll("[\\r\\n]+", " ").trim();
		return sanitized.length() <= 1000 ? sanitized : sanitized.substring(0, 1000);
	}

	private String currentActor() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null
				|| !authentication.isAuthenticated()
				|| authentication instanceof AnonymousAuthenticationToken) {
			return "system";
		}
		return authentication.getName();
	}

	private void audit(String entityName, UUID entityId, String action, Object oldValue, Object newValue) {
		auditLogService.recordStandalone(new AuditLogEvent(
				MODULE_NAME,
				entityName,
				entityId == null ? null : entityId.toString(),
				action,
				oldValue,
				newValue == null ? Map.of() : newValue));
	}

	private record DatabaseTarget(
			String host,
			int port,
			String databaseName,
			String username,
			String password) {

		Map<String, String> environment() {
			Map<String, String> environment = new HashMap<>();
			if (StringUtils.hasText(password)) {
				environment.put("PGPASSWORD", password);
			}
			return environment;
		}
	}
}
