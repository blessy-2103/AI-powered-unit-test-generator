package com.testgen.service;

import com.testgen.dto.AuthResponse;
import com.testgen.dto.LoginRequest;
import com.testgen.dto.RegisterRequest;
import com.testgen.model.PasswordResetToken;
import com.testgen.model.User;
import com.testgen.repository.PasswordResetTokenRepository;
import com.testgen.repository.UserRepository;
import com.testgen.security.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final EmailService emailService;

    private static final int RESET_TOKEN_VALID_MINUTES = 30;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                        AuthenticationManager authenticationManager, JwtUtil jwtUtil,
                        PasswordResetTokenRepository resetTokenRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.resetTokenRepository = resetTokenRepository;
        this.emailService = emailService;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            return AuthResponse.ofError("Username is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            return AuthResponse.ofError("An account with this email already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getUsername());
        return AuthResponse.ofSuccess(token, user.getUsername());
    }

    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        } catch (BadCredentialsException e) {
            return AuthResponse.ofError("Invalid username or password");
        }

        String token = jwtUtil.generateToken(request.getUsername());
        return AuthResponse.ofSuccess(token, request.getUsername());
    }

    /**
     * Always behaves the same way whether or not the email exists, so callers
     * can't use this endpoint to discover which emails are registered.
     */
    @Transactional
    public void requestPasswordReset(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            // Invalidate any previous outstanding tokens for this user
            resetTokenRepository.deleteByUsername(user.getUsername());

            String token = UUID.randomUUID().toString();
            PasswordResetToken resetToken = new PasswordResetToken(
                    null, token, user.getUsername(),
                    LocalDateTime.now().plusMinutes(RESET_TOKEN_VALID_MINUTES), false);
            resetTokenRepository.save(resetToken);

            emailService.sendPasswordResetEmail(user.getEmail(), token);
        });
    }

    public AuthResponse resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = resetTokenRepository.findByToken(token)
                .orElse(null);

        if (resetToken == null || resetToken.isUsed() || resetToken.isExpired()) {
            return AuthResponse.ofError("This reset link is invalid or has expired. Please request a new one.");
        }

        User user = userRepository.findByUsername(resetToken.getUsername())
                .orElse(null);
        if (user == null) {
            return AuthResponse.ofError("Account not found");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsed(true);
        resetTokenRepository.save(resetToken);

        return AuthResponse.ofSuccess(null, user.getUsername());
    }
}
