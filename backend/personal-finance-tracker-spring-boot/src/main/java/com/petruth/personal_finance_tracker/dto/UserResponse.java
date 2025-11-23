package com.petruth.personal_finance_tracker.dto;

public record UserResponse(long id, String username, String email, boolean emailVerified) {
}
