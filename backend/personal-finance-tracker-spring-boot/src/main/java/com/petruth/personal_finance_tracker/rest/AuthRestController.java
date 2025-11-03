package com.petruth.personal_finance_tracker.rest;

import com.petruth.personal_finance_tracker.dto.AuthRequest;
import com.petruth.personal_finance_tracker.dto.UserResponse;
import com.petruth.personal_finance_tracker.entity.RefreshToken;
import com.petruth.personal_finance_tracker.entity.User;
import com.petruth.personal_finance_tracker.jwt.JwtUtil;
import com.petruth.personal_finance_tracker.service.ProfileService;
import com.petruth.personal_finance_tracker.service.RefreshTokenService;
import com.petruth.personal_finance_tracker.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthRestController {
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final ProfileService profileService; // Add this

    public AuthRestController(UserService userService,
                              PasswordEncoder passwordEncoder,
                              JwtUtil jwtUtil,
                              RefreshTokenService refreshTokenService,
                              ProfileService profileService) { // Add this
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
        this.profileService = profileService; // Add this
    }

    @PostMapping("/register")
    public String register(@RequestBody AuthRequest request) {
        if (userService.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        if (userService.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userService.save(user);

        return "User registered successfully!";
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(
            @RequestBody AuthRequest request,
            HttpServletResponse response
    ) {
        User user = userService.findByUsername(request.getUsername());

        if (user != null && passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            // Update last login timestamp
            profileService.updateLastLogin(user.getId()); // Add this

            // Generate access token (15 minutes)
            String accessToken = jwtUtil.generateToken(user.getUsername(), user.getId());

            // Generate refresh token (7 days)
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

            // Set access token in HttpOnly cookie
            Cookie accessCookie = new Cookie("jwt_token", accessToken);
            accessCookie.setHttpOnly(true);
            accessCookie.setSecure(false); // Set to true in production with HTTPS
            accessCookie.setPath("/");
            accessCookie.setMaxAge(15 * 60); // 15 minutes
            accessCookie.setAttribute("SameSite", "Lax");
            response.addCookie(accessCookie);

            // Set refresh token in HttpOnly cookie
            Cookie refreshCookie = new Cookie("refresh_token", refreshToken.getToken());
            refreshCookie.setHttpOnly(true);
            refreshCookie.setSecure(false); // Set to true in production
            refreshCookie.setPath("/");
            refreshCookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
            refreshCookie.setAttribute("SameSite", "Lax");
            response.addCookie(refreshCookie);

            // Return user info
            UserResponse userResponse = new UserResponse(
                    user.getId(),
                    user.getUsername(),
                    user.getEmail()
            );

            return ResponseEntity.ok(userResponse);
        } else {
            throw new RuntimeException("Invalid username or password");
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        // Extract refresh token from cookie
        String refreshTokenValue = null;
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("refresh_token".equals(cookie.getName())) {
                    refreshTokenValue = cookie.getValue();
                    break;
                }
            }
        }

        if (refreshTokenValue == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Refresh token is missing");
        }

        // Verify refresh token
        Optional<RefreshToken> refreshTokenOpt = refreshTokenService.verifyRefreshToken(refreshTokenValue);

        if (refreshTokenOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid or expired refresh token");
        }

        RefreshToken refreshToken = refreshTokenOpt.get();
        User user = refreshToken.getUser();

        // Generate new access token
        String newAccessToken = jwtUtil.generateToken(user.getUsername(), user.getId());

        // Set new access token in cookie
        Cookie accessCookie = new Cookie("jwt_token", newAccessToken);
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(false); // Set to true in production
        accessCookie.setPath("/");
        accessCookie.setMaxAge(15 * 60); // 15 minutes
        accessCookie.setAttribute("SameSite", "Lax");
        response.addCookie(accessCookie);

        return ResponseEntity.ok("Token refreshed successfully");
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        // Extract and revoke refresh token
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("refresh_token".equals(cookie.getName())) {
                    refreshTokenService.revokeRefreshToken(cookie.getValue());
                    break;
                }
            }
        }

        // Clear both cookies
        Cookie accessCookie = new Cookie("jwt_token", null);
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(false);
        accessCookie.setPath("/");
        accessCookie.setMaxAge(0);
        response.addCookie(accessCookie);

        Cookie refreshCookie = new Cookie("refresh_token", null);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(false);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(0);
        response.addCookie(refreshCookie);

        return ResponseEntity.ok("Logged out successfully");
    }
}