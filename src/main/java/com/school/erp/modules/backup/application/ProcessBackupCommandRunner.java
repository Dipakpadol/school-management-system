package com.school.erp.modules.backup.application;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

@Component
public class ProcessBackupCommandRunner implements BackupCommandRunner {

	@Override
	public BackupCommandResult run(List<String> command, Map<String, String> environment, Duration timeout) {
		ProcessBuilder builder = new ProcessBuilder(command);
		builder.redirectErrorStream(true);
		if (environment != null && !environment.isEmpty()) {
			builder.environment().putAll(environment);
		}
		try {
			Process process = builder.start();
			CompletableFuture<String> output = CompletableFuture.supplyAsync(() -> readOutput(process));
			boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
			if (!finished) {
				process.destroyForcibly();
				return new BackupCommandResult(124, "Backup command timed out.");
			}
			return new BackupCommandResult(process.exitValue(), output.join());
		}
		catch (IOException ex) {
			return new BackupCommandResult(127, ex.getMessage());
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			return new BackupCommandResult(130, "Backup command was interrupted.");
		}
		catch (RuntimeException ex) {
			return new BackupCommandResult(1, ex.getMessage());
		}
	}

	private String readOutput(Process process) {
		try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			process.getInputStream().transferTo(output);
			return output.toString(StandardCharsets.UTF_8);
		}
		catch (IOException ex) {
			return ex.getMessage();
		}
	}
}
