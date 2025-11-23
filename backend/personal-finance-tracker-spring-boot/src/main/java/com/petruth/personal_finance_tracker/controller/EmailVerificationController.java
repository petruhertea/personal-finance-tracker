package com.petruth.personal_finance_tracker.controller;

import com.petruth.personal_finance_tracker.service.EmailVerificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/email")
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    public EmailVerificationController(EmailVerificationService emailVerificationService) {
        this.emailVerificationService = emailVerificationService;
    }

    /**
     * Verify email with token
     * GET /api/email/verify?token=...
     */
    @GetMapping("/verify")
    public ResponseEntity<String> verifyEmail(@RequestParam String token) {
        boolean verified = emailVerificationService.verifyEmail(token);

        if (verified) {
            return ResponseEntity.ok("Email verified successfully! You can now receive budget alerts.");
        } else {
            return ResponseEntity.badRequest()
                    .body("Invalid or expired verification token. Please request a new one.");
        }
    }

    /**
     * Resend verification email
     * POST /api/email/resend
     */
    @PostMapping("/resend")
    public ResponseEntity<String> resendVerificationEmail(@RequestBody ResendEmailRequest request) {
        try {
            emailVerificationService.resendVerificationEmail(request.getEmail());
            return ResponseEntity.ok("Verification email sent! Please check your inbox.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // DTO for resend request
    public static class ResendEmailRequest {
        private String email;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }
}