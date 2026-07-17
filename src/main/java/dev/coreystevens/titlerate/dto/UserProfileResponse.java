package dev.coreystevens.titlerate.dto;

import java.time.Instant;

public record UserProfileResponse(String email, String role, Instant createdAt) {}
