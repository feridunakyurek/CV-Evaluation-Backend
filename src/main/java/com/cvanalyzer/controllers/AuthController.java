package com.cvanalyzer.controllers;

import com.cvanalyzer.dtos.AuthRequest;
import com.cvanalyzer.dtos.JwtResponse;
import com.cvanalyzer.entities.User;
import com.cvanalyzer.security.JwtUtil;
import com.cvanalyzer.services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000")
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserService userService, JwtUtil jwtUtil, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody User user) {
        boolean isRegistered = userService.registerUser(user);
        if (isRegistered) {
            return ResponseEntity.ok("User registered successfully");
        } else {
            return ResponseEntity.badRequest().body("User already exists");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest authRequest) {
        User user = userService.findByEmail(authRequest.getEmail())
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));

        String incomingPassword = authRequest.getPassword();
        if (incomingPassword == null || incomingPassword.isEmpty()) {
            throw new RuntimeException("Şifre boş olamaz");
        }

        String storedPassword = user.getPassword();

        boolean isAuthenticated = passwordEncoder.matches(incomingPassword, storedPassword);

        if (!isAuthenticated) {
            throw new RuntimeException("Geçersiz şifre");
        }

        String token = jwtUtil.generateToken(user.getEmail(), String.valueOf(user.getRole()));
        return ResponseEntity.ok(new JwtResponse(token));

    }
}
