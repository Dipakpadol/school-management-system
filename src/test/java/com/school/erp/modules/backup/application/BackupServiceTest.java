package com.school.erp.modules.backup.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.audit.application.AuditLogService;
import com.school.erp.common.exception.BusinessException;
import com.school.erp.common.exception.ErrorCode;
import com.school.erp.modules.backup.api.dto.CreateBackupRequest;
import com.school.erp.modules.backup.api.dto.RestoreBackupRequest;
import com.school.erp.modules.backup.domain.BackupRecord;
import com.school.erp.modules.backup.domain.BackupStatus;
import com.school.erp.modules.backup.domain.BackupType;
import com.school.erp.modules.backup.domain.RestoreHistory;
import com.school.erp.modules.backup.domain.RestoreStatus;
import com.school.erp.modules.backup.infrastructure.BackupRecordRepository;
import com.school.erp.modules.backup.infrastructure.RestoreHistoryRepository;
import com.school.erp.modules.settings.application.ApplicationSettingsService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.FileSystemUtils;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BackupServiceTest {

	@Mock
	private BackupRecordRepository backupRepository;

	@Mock
	private RestoreHistoryRepository restoreHistoryRepository;

	@Mock
	private BackupCommandRunner commandRunner;

	@Mock
	private ApplicationSettingsService settingsService;

	@Mock
	private JdbcTemplate jdbcTemplate;

	@Mock
	private AuditLogService auditLogService;

	private Path tempDir;

	private BackupService backupService;
	private DataSourceProperties dataSourceProperties;

	@BeforeEach
	void setUp() {
		tempDir = Paths.get("target", "test-data", "backup-service", UUID.randomUUID().toString())
				.toAbsolutePath()
				.normalize();
		try {
			Files.createDirectories(tempDir);
		}
		catch (IOException ex) {
			throw new IllegalStateException("Unable to create backup test directory.", ex);
		}
		dataSourceProperties = new DataSourceProperties();
		dataSourceProperties.setUrl("jdbc:postgresql://postgres:5432/school_erp");
		dataSourceProperties.setUsername("school_user");
		dataSourceProperties.setPassword("secret");
		backupService = new BackupService(
				backupRepository,
				restoreHistoryRepository,
				commandRunner,
				new BackupProperties(tempDir.toString(), "pg_dump", "pg_restore", Duration.ofSeconds(5), 30, 10, "0 0 * * * *"),
				dataSourceProperties,
				settingsService,
				jdbcTemplate,
				new BackupMapper(),
				auditLogService);

		lenient().when(settingsService.rawSettingValue("backup", "backupLocation")).thenReturn(tempDir.toString());
		lenient().when(settingsService.rawSettingValue("backup", "backupRetentionDays")).thenReturn("30");
		lenient().when(settingsService.rawSettingValue("backup", "backupRetentionCount")).thenReturn("10");
		lenient().when(jdbcTemplate.queryForObject(anyString(), eq(String.class))).thenReturn("32");
		lenient().when(backupRepository.findAllByStatusInAndDeletedFalseOrderByCompletedAtDesc(any())).thenReturn(List.of());
		lenient().when(backupRepository.save(any(BackupRecord.class))).thenAnswer(invocation -> entity(invocation.getArgument(0)));
	}

	@AfterEach
	void tearDown() throws IOException {
		if (tempDir != null && tempDir.startsWith(Paths.get("target").toAbsolutePath().normalize())) {
			FileSystemUtils.deleteRecursively(tempDir);
		}
	}

	@Test
	void createBackupRunsPgDumpWithSafeArgumentsAndStoresMetadata() {
		when(commandRunner.run(any(), any(), any())).thenAnswer(invocation -> {
			List<String> command = invocation.getArgument(0);
			if (command.contains("--version")) {
				return new BackupCommandResult(0, "pg_dump (PostgreSQL) 16.0");
			}
			int outputIndex = command.indexOf("--file") + 1;
			Files.writeString(Path.of(command.get(outputIndex)), "backup-bytes");
			return new BackupCommandResult(0, "");
		});

		var response = backupService.createBackup(new CreateBackupRequest("Manual smoke backup"));

		assertThat(response.status()).isEqualTo(BackupStatus.COMPLETED);
		assertThat(response.backupType()).isEqualTo(BackupType.DATABASE);
		assertThat(response.databaseVersion()).isEqualTo("32");
		assertThat(response.checksumSha256()).hasSize(64);
		assertThat(response.fileName()).startsWith("school-erp-db-").endsWith(".backup");
		ArgumentCaptor<List<String>> commandCaptor = ArgumentCaptor.forClass(List.class);
		ArgumentCaptor<Map<String, String>> envCaptor = ArgumentCaptor.forClass(Map.class);
		verify(commandRunner, atLeast(1)).run(commandCaptor.capture(), envCaptor.capture(), any());
		List<String> dumpCommand = commandCaptor.getAllValues().stream()
				.filter(command -> command.contains("-Fc"))
				.findFirst()
				.orElseThrow();
		assertThat(dumpCommand).contains("--host", "postgres", "--port", "5432", "--username", "school_user", "school_erp");
		assertThat(dumpCommand).doesNotContain("sh", "-c", "cmd", "/c");
		assertThat(envCaptor.getAllValues()).anySatisfy(env -> assertThat(env).containsEntry("PGPASSWORD", "secret"));
	}

	@Test
	void createBackupRecordsFailureWhenPgDumpFails() {
		when(commandRunner.run(any(), any(), any())).thenReturn(new BackupCommandResult(1, "permission denied"));

		assertThatThrownBy(() -> backupService.createBackup(new CreateBackupRequest(null)))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.BUSINESS_RULE_VIOLATION);

		ArgumentCaptor<BackupRecord> backupCaptor = ArgumentCaptor.forClass(BackupRecord.class);
		verify(backupRepository, atLeast(2)).save(backupCaptor.capture());
		assertThat(backupCaptor.getAllValues().getLast().getStatus()).isEqualTo(BackupStatus.FAILED);
		assertThat(backupCaptor.getAllValues().getLast().getErrorMessage()).contains("permission denied");
	}

	@Test
	void restoreValidatesArchiveCreatesSafetyBackupAndRunsPgRestore() throws Exception {
		Path existingFile = tempDir.resolve("existing.backup");
		Files.writeString(existingFile, "existing-backup");
		BackupRecord source = completedBackup(existingFile, "existing.backup");
		when(backupRepository.findByIdAndDeletedFalse(source.getId())).thenReturn(Optional.of(source));
		when(restoreHistoryRepository.save(any(RestoreHistory.class))).thenAnswer(invocation -> entity(invocation.getArgument(0)));
		when(commandRunner.run(any(), any(), any())).thenAnswer(invocation -> {
			List<String> command = invocation.getArgument(0);
			if (command.contains("--version") || command.contains("--list") || command.contains("--clean")) {
				return new BackupCommandResult(0, "");
			}
			if (command.contains("-Fc")) {
				int outputIndex = command.indexOf("--file") + 1;
				Files.writeString(Path.of(command.get(outputIndex)), "safety-backup");
				return new BackupCommandResult(0, "");
			}
			return new BackupCommandResult(0, "");
		});

		var response = backupService.restore(source.getId(), new RestoreBackupRequest("RESTORE", "Rollback test"));

		assertThat(response.status()).isEqualTo(RestoreStatus.COMPLETED);
		assertThat(response.safetyBackupId()).isNotNull();
		assertThat(source.getStatus()).isEqualTo(BackupStatus.RESTORED);
		ArgumentCaptor<List<String>> commandCaptor = ArgumentCaptor.forClass(List.class);
		verify(commandRunner, atLeast(1)).run(commandCaptor.capture(), any(), any());
		assertThat(commandCaptor.getAllValues()).anySatisfy(command -> assertThat(command).contains("--list", existingFile.toString()));
		assertThat(commandCaptor.getAllValues()).anySatisfy(command -> assertThat(command).contains("--clean", "--if-exists", "--exit-on-error"));
	}

	@Test
	void restoreRejectsChecksumMismatchBeforeRestoreCommand() throws Exception {
		Path existingFile = tempDir.resolve("existing.backup");
		Files.writeString(existingFile, "existing-backup");
		BackupRecord source = completedBackup(existingFile, "existing.backup");
		Files.writeString(existingFile, "tampered");
		when(backupRepository.findByIdAndDeletedFalse(source.getId())).thenReturn(Optional.of(source));

		assertThatThrownBy(() -> backupService.restore(source.getId(), new RestoreBackupRequest("RESTORE", null)))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.BUSINESS_RULE_VIOLATION);
	}

	@Test
	void downloadRejectsBackupPathOutsideConfiguredStorageRoot() {
		Path outside = tempDir.resolveSibling("outside.backup");
		BackupRecord backup = new BackupRecord(
				BackupType.DATABASE,
				"outside.backup",
				outside.toString(),
				"admin",
				"32",
				"test",
				"POSTGRES_CUSTOM",
				false,
				null,
				null);
		entity(backup);
		backup.markCompleted(12, null, null);
		when(backupRepository.findByIdAndDeletedFalse(backup.getId())).thenReturn(Optional.of(backup));

		assertThatThrownBy(() -> backupService.download(backup.getId()))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.VALIDATION_ERROR);
	}

	private BackupRecord completedBackup(Path path, String fileName) throws Exception {
		BackupRecord backup = new BackupRecord(
				BackupType.DATABASE,
				fileName,
				path.toString(),
				"admin",
				"32",
				"test",
				"POSTGRES_CUSTOM",
				false,
				null,
				null);
		entity(backup);
		backup.markCompleted(Files.size(path), sha256(path), "pg_dump 16");
		return backup;
	}

	private <T> T entity(T entity) {
		if (ReflectionTestUtils.getField(entity, "id") == null) {
			ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
		}
		return entity;
	}

	private String sha256(Path path) throws Exception {
		byte[] digest = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path));
		return HexFormat.of().formatHex(digest);
	}
}
