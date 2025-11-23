package com.petruth.personal_finance_tracker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petruth.personal_finance_tracker.dto.*;
import com.petruth.personal_finance_tracker.security.SecurityUtil;
import com.petruth.personal_finance_tracker.service.ProfileService;
import com.petruth.personal_finance_tracker.service.RefreshTokenService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;
    private final RefreshTokenService refreshTokenService;
    private final SecurityUtil securityUtil;
    private final ObjectMapper objectMapper;

    public ProfileController(ProfileService profileService,
                             RefreshTokenService refreshTokenService,
                             SecurityUtil securityUtil,
                             ObjectMapper objectMapper) {
        this.profileService = profileService;
        this.refreshTokenService = refreshTokenService;
        this.securityUtil = securityUtil;
        this.objectMapper = objectMapper;
    }

    /**
     * Get account statistics
     */
    @GetMapping("/stats")
    public AccountStatsDTO getAccountStats() {
        Long userId = securityUtil.getCurrentUserId();
        return profileService.getAccountStats(userId);
    }

    /**
     * Update email address
     */
    @PutMapping("/email")
    public ResponseEntity<?> updateEmail(@RequestBody EmailUpdateRequest request) {
        try {
            Long userId = securityUtil.getCurrentUserId();

            UserResponse updatedUser = profileService.updateEmail(
                    userId,
                    request.getEmail(),
                    request.getPassword()
            );

            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        }
    }

    /**
     * Change password
     */
    @PutMapping("/password")
    public ResponseEntity<?> changePassword(@RequestBody PasswordChangeRequest request) {
        try {
            Long userId = securityUtil.getCurrentUserId();

            profileService.changePassword(
                    userId,
                    request.getCurrentPassword(),
                    request.getNewPassword()
            );

            return ResponseEntity.ok("Password changed successfully");
        } catch (RuntimeException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        }
    }

    /**
     * Logout from all devices (revoke all refresh tokens)
     */
    @PostMapping("/logout-all")
    public ResponseEntity<String> logoutAllDevices() {
        try {
            Long userId = securityUtil.getCurrentUserId();
            refreshTokenService.revokeAllUserTokens(userId);
            return ResponseEntity.ok("Successfully logged out from all devices");
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to logout from all devices");
        }
    }

    /**
     * Export user data (GDPR compliance)
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportUserData() {
        try {
            Long userId = securityUtil.getCurrentUserId();
            UserDataExportDTO exportData = profileService.exportUserData(userId);

            // Convert to JSON
            String jsonData = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(exportData);
            byte[] jsonBytes = jsonData.getBytes();

            // Set headers for file download
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setContentDispositionFormData("attachment",
                    "user-data-export-" + System.currentTimeMillis() + ".json");
            headers.setContentLength(jsonBytes.length);

            return new ResponseEntity<>(jsonBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    /**
     * Delete account permanently
     */
    @DeleteMapping("/account")
    public ResponseEntity<String> deleteAccount(@RequestBody DeleteAccountRequest request) {
        try {
            Long userId = securityUtil.getCurrentUserId();
            profileService.deleteAccount(userId, request.getPassword());
            return ResponseEntity.ok("Account deleted successfully");
        } catch (RuntimeException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        }
    }
}
