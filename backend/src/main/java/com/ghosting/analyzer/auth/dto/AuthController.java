package com.ghosting.analyzer.auth.dto;

import com.ghosting.analyzer.auth.dto.*;
import com.ghosting.analyzer.security.JwtService;
import com.ghosting.analyzer.user.User;
import com.ghosting.analyzer.user.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        if (userRepository.existsByEmailIgnoreCase(req.email())) {
            return ResponseEntity.badRequest().build();
        }

        User u = User.builder()
                .email(req.email().trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(req.password()))
                .createdAt(Instant.now())
                .build();

        u = userRepository.save(u);

        String token = jwtService.generateAccessToken(u.getId().toString(), u.getEmail());
        return ResponseEntity.ok(new AuthResponse(token));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        var u = userRepository.findByEmailIgnoreCase(req.email().trim())
                .orElse(null);

        if (u == null || !passwordEncoder.matches(req.password(), u.getPasswordHash())) {
            return ResponseEntity.status(401).build();
        }

        String token = jwtService.generateAccessToken(u.getId().toString(), u.getEmail());
        return ResponseEntity.ok(new AuthResponse(token));
    }
}
