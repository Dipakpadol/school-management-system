package com.school.erp.modules.auth.application;

import java.time.Instant;

public record GeneratedAccessToken(String value, Instant expiresAt) {
}
