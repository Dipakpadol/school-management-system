package com.school.erp.modules.auth.application;

import java.time.Instant;

public record GeneratedRefreshToken(String value, String hash, Instant expiresAt) {
}
