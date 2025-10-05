package com.cvanalyzer.controllers;

import com.cvanalyzer.entities.User;
import com.cvanalyzer.security.JwtUtil;
import com.cvanalyzer.services.UserService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000")
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authManager;

    public AuthController(UserService userService, JwtUtil jwtUtil, AuthenticationManager authManager) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.authManager = authManager;
    }

    @PostMapping("/register")
    public User register(@RequestBody User user) {
        return userService.registerUser(user);
    }

    @PostMapping("/login")
    public Map<String, String> login(@RequestBody User user) {
        Authentication authentication = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getEmail(), user.getPassword())
        );
        if (authentication.isAuthenticated()) {
            String token = jwtUtil.generateToken(user.getEmail(), "USER");
            return Map.of("token", token);
        } else {
            throw new RuntimeException("Invalid credentials");
        }
    }
}
