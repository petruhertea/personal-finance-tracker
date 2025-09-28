package com.petruth.personal_finance_tracker.rest;

import com.petruth.personal_finance_tracker.dto.AuthRequest;
import com.petruth.personal_finance_tracker.dto.AuthResponse;
import com.petruth.personal_finance_tracker.entity.User;
import com.petruth.personal_finance_tracker.jwt.JwtUtil;
import com.petruth.personal_finance_tracker.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthRestController {
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;


    public AuthRestController(UserService userService, PasswordEncoder passwordEncoder,
                              JwtUtil jwtUtil){
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
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
    public AuthResponse login(@RequestBody AuthRequest request) {
        User user = userService.findByUsername(request.getUsername());
        System.out.println("Username: "+request.getUsername());
        if (user != null && passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            String token = jwtUtil.generateToken(user.getUsername(), user.getId());
            return new AuthResponse(token);
        } else {
            throw new RuntimeException("Invalid username or password");
        }
    }
}
