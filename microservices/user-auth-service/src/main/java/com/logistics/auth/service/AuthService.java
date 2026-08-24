package com.logistics.auth.service;

import com.logistics.auth.dto.AuthDTOs.*;
import com.logistics.auth.exception.AccountInactiveException;
import com.logistics.auth.exception.DuplicateUserException;
import com.logistics.auth.exception.InvalidCredentialsException;
import com.logistics.auth.exception.ResourceNotFoundException;
import com.logistics.auth.model.User;
import com.logistics.auth.repository.UserRepository;
import com.logistics.auth.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        log.info("Processing login attempt for user: {}", request.getUsernameOrEmail());

        User user = userRepository.findByUsername(request.getUsernameOrEmail())
                .or(() -> userRepository.findByEmail(request.getUsernameOrEmail()))
                .orElseThrow(() -> new InvalidCredentialsException("Tên đăng nhập hoặc mật khẩu không chính xác"));

        if (!user.getActive()) {
            throw new AccountInactiveException("Tài khoản người dùng đã bị khóa hoặc chưa được kích hoạt");
        }

        // Validate password against hashed credentials
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            // Also allow initial plain text fallback for backward compatible seed data
            if (!request.getPassword().equals(user.getPasswordHash())) {
                throw new InvalidCredentialsException("Tên đăng nhập hoặc mật khẩu không chính xác");
            }
        }

        // Issue JWT tokens
        String accessToken = jwtProvider.generateToken(
                user.getId(),
                user.getUsername(),
                user.getRole().name(),
                user.getEmail()
        );
        String refreshToken = jwtProvider.generateRefreshToken(user.getId(), user.getUsername());

        log.info("User {} successfully authenticated with role {}", user.getUsername(), user.getRole());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .fullName(user.getFullName())
                .build();
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user: {}", request.getUsername());

        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new DuplicateUserException("Tên đăng nhập đã tồn tại: " + request.getUsername());
        }

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new DuplicateUserException("Email đã được sử dụng: " + request.getEmail());
        }

        User.UserRole role = request.getRole() != null ? request.getRole() : User.UserRole.ROLE_CUSTOMER;

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .role(role)
                .active(true)
                .build();

        User savedUser = userRepository.save(user);

        String accessToken = jwtProvider.generateToken(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getRole().name(),
                savedUser.getEmail()
        );
        String refreshToken = jwtProvider.generateRefreshToken(savedUser.getId(), savedUser.getUsername());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .userId(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .role(savedUser.getRole().name())
                .fullName(savedUser.getFullName())
                .build();
    }

    @Transactional(readOnly = true)
    public TokenValidationResponse validateToken(String token) {
        if (!jwtProvider.validateToken(token)) {
            return TokenValidationResponse.builder().valid(false).build();
        }

        String username = jwtProvider.getUsernameFromToken(token);
        String role = jwtProvider.getRoleFromToken(token);
        UUID userId = jwtProvider.getUserIdFromToken(token);

        return TokenValidationResponse.builder()
                .valid(true)
                .username(username)
                .role(role)
                .userId(userId)
                .build();
    }

    @Transactional(readOnly = true)
    public User getUserProfile(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }
}
