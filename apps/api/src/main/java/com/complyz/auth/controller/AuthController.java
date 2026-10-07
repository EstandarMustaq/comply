package com.complyz.auth.controller;

import com.complyz.audit.entity.AuditAction;
import com.complyz.audit.service.AuditService;
import com.complyz.auth.dto.*;
import com.complyz.auth.service.EncryptionService;
import com.complyz.auth.service.JwtService;
import com.complyz.auth.service.MfaEnrollmentService;
import com.complyz.auth.service.MfaVerificationService;
import com.complyz.auth.service.TokenBlacklistService;
import com.complyz.users.entity.Role;
import com.complyz.users.entity.User;
import com.complyz.users.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;
    private final MfaEnrollmentService mfaEnrollmentService;
    private final MfaVerificationService mfaVerificationService;
    private final EncryptionService encryptionService;
    private final AuditService auditService;

    public AuthController(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            TokenBlacklistService tokenBlacklistService,
            MfaEnrollmentService mfaEnrollmentService,
            MfaVerificationService mfaVerificationService,
            EncryptionService encryptionService,
            AuditService auditService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.tokenBlacklistService = tokenBlacklistService;
        this.mfaEnrollmentService = mfaEnrollmentService;
        this.mfaVerificationService = mfaVerificationService;
        this.encryptionService = encryptionService;
        this.auditService = auditService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String clientIp = httpRequest.getRemoteAddr();

        if (request.getEmail() == null || request.getEmail().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email is required"));
        }
        String password = request.getPassword();
        if (password == null || password.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Password is required"));
        }

        if (tokenBlacklistService.isRateLimited(request.getEmail(), clientIp)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("message", "Too many failed attempts. Try again in 15 minutes."));
        }

        User user = userRepository.findByEmail(request.getEmail()).orElse(null);
        if (user == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            tokenBlacklistService.registerFailedLogin(request.getEmail(), clientIp);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Email or password is incorrect"));
        }

        if (!user.isMfaEnabled()) {
            String enrollmentToken = mfaEnrollmentService.createEnrollmentToken(user.getId());
            return ResponseEntity.ok(LoginResponse.mfaEnrollmentRequired(enrollmentToken));
        } else {
            String challengeToken = mfaVerificationService.createChallengeToken(user.getId());
            return ResponseEntity.ok(LoginResponse.mfaRequired(challengeToken));
        }
    }

    @PostMapping("/mfa/verify")
    public ResponseEntity<?> verifyMfa(@RequestBody MfaVerifyRequest request, HttpServletRequest httpRequest) {
        String token = request.getChallengeToken();
        String code = request.getCode();
        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Challenge token is required"));
        }
        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Verification code is required"));
        }

        UUID userId = mfaVerificationService.getUserIdFromChallenge(token);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "This sign-in challenge expired or is invalid. Sign in again."));
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Account not found."));
        }

        if (mfaVerificationService.isAttemptRateLimited(token, userId, httpRequest.getRemoteAddr())) {
            mfaVerificationService.consumeChallenge(token);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("message", "Too many failed verification attempts. Sign in again."));
        }

        boolean valid = mfaVerificationService.verifyTotpCode(user, code);
        if (!valid) {
            mfaVerificationService.registerFailedAttempt(token, userId, httpRequest.getRemoteAddr());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "The verification code is invalid or has already been used."));
        }

        mfaVerificationService.consumeChallenge(token);
        mfaVerificationService.resetAttempts(token, userId, httpRequest.getRemoteAddr());
        tokenBlacklistService.resetFailedLogins(user.getEmail(), httpRequest.getRemoteAddr());

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        auditService.record(user, AuditAction.MFA_VERIFIED, "users", user.getId(), "{\"method\": \"TOTP\"}");

        return ResponseEntity.ok(LoginResponse.success(
                accessToken,
                refreshToken,
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.isMfaEnabled()
        ));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        String refreshToken = request.refreshToken();
        if (!jwtService.isRefreshTokenValid(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "The refresh token is invalid, expired or already used."));
        }

        UUID userId;
        String jti;
        try {
            userId = jwtService.extractUserId(refreshToken);
            jti = jwtService.extractJti(refreshToken);
        } catch (RuntimeException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "The refresh token is invalid."));
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Account not found."));
        }

        tokenBlacklistService.blacklistToken(jti, jwtService.getRemainingMillis(refreshToken));
        return ResponseEntity.ok(new RefreshTokenResponse(
                jwtService.generateAccessToken(user),
                jwtService.generateRefreshToken(user)
        ));
    }

    @PostMapping("/mfa/enroll")
    public ResponseEntity<?> enrollMfa(@RequestBody(required = false) Map<String, String> body, Authentication authentication) {
        User user = null;
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            user = getCurrentUser(authentication);
        } else if (body != null && body.containsKey("enrollmentToken")) {
            UUID userId = mfaEnrollmentService.getUserIdFromEnrollmentToken(body.get("enrollmentToken"));
            if (userId != null) {
                user = userRepository.findById(userId).orElse(null);
            }
        }

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "The enrollment token is invalid or expired."));
        }

        MfaEnrollResponse response = mfaEnrollmentService.startEnrollment(user);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/mfa/confirm")
    public ResponseEntity<?> confirmMfa(@Valid @RequestBody MfaConfirmRequest request, Authentication authentication) {
        User user = null;
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            user = getCurrentUser(authentication);
        } else if (request.getEnrollmentToken() != null && !request.getEnrollmentToken().isBlank()) {
            UUID userId = mfaEnrollmentService.getUserIdFromEnrollmentToken(request.getEnrollmentToken());
            if (userId != null) {
                user = userRepository.findById(userId).orElse(null);
            }
        }

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "The enrollment token is invalid or expired."));
        }

        String pendingSecret = mfaEnrollmentService.getPendingEnrollmentSecret(user);
        if (pendingSecret == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "The enrollment session expired. Start again."));
        }

        boolean valid = mfaVerificationService.verifyTotpWithSecret(user.getId(), pendingSecret, request.getCode());
        if (!valid) {
            return ResponseEntity.badRequest().body(Map.of("message", "The verification code is incorrect. Check your device clock."));
        }

        // Encrypt the secret with AES-256-GCM before persistence.
        String encryptedSecret = encryptionService.encrypt(pendingSecret);
        user.setMfaSecretEncrypted(encryptedSecret);
        user.setMfaEnabled(true);
        userRepository.save(user);

        mfaEnrollmentService.clearPendingEnrollment(user);
        if (request.getEnrollmentToken() != null) {
            mfaEnrollmentService.consumeEnrollmentToken(request.getEnrollmentToken());
        }

        // Generate and return eight recovery codes.
        List<String> backupCodes = mfaVerificationService.generateBackupCodes(user);

        auditService.record(user, AuditAction.MFA_ENROLLED, "users", user.getId(), "{\"mfa_enabled\": true}");

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return ResponseEntity.ok(new MfaConfirmResponse(
                "MFA is on. Store your recovery codes somewhere safe.",
                backupCodes,
                accessToken,
                refreshToken,
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole()
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, Authentication authentication) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                String jti = jwtService.extractJti(token);
                long remainingMillis = jwtService.getRemainingMillis(token);
                tokenBlacklistService.blacklistToken(jti, remainingMillis);
            } catch (Exception ignored) {
            }
        }
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            auditService.record(user, AuditAction.LOGOUT, "users", user.getId(), "{}");
        }
        return ResponseEntity.ok(Map.of("message", "You are signed out."));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUserInfo(Authentication authentication) {
        User user = getCurrentUser(authentication);
        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "fullName", user.getFullName(),
                "email", user.getEmail(),
                "role", user.getRole(),
                "mfaEnabled", user.isMfaEnabled()
        ));
    }

    private User getCurrentUser(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            // Reload the user to obtain the latest persisted state.
            return userRepository.findById(user.getId()).orElse(user);
        }
        String email = authentication != null ? authentication.getName() : null;
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Account not found in the security context."));
    }
}
