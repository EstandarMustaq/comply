package com.fincomp.auth.controller;

import com.fincomp.audit.entity.AuditAction;
import com.fincomp.audit.service.AuditService;
import com.fincomp.auth.dto.*;
import com.fincomp.auth.service.EncryptionService;
import com.fincomp.auth.service.JwtService;
import com.fincomp.auth.service.MfaEnrollmentService;
import com.fincomp.auth.service.MfaVerificationService;
import com.fincomp.auth.service.TokenBlacklistService;
import com.fincomp.users.entity.Role;
import com.fincomp.users.entity.User;
import com.fincomp.users.repository.UserRepository;
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
@RequestMapping("/api/auth")
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
            return ResponseEntity.badRequest().body(Map.of("message", "O email é obrigatório"));
        }
        String password = request.getPassword();
        if (password == null || password.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "A password é obrigatória"));
        }

        if (tokenBlacklistService.isRateLimited(request.getEmail(), clientIp)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("message", "Conta temporariamente bloqueada por excesso de tentativas falhadas. Tente em 15 minutos."));
        }

        User user = userRepository.findByEmail(request.getEmail()).orElse(null);
        if (user == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            tokenBlacklistService.registerFailedLogin(request.getEmail(), clientIp);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Credenciais inválidas"));
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
            return ResponseEntity.badRequest().body(Map.of("message", "O token de desafio é obrigatório"));
        }
        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "O código de verificação é obrigatório"));
        }

        UUID userId = mfaVerificationService.getUserIdFromChallenge(token);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Desafio MFA expirado ou inválido. Reinicie o login."));
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Utilizador não encontrado"));
        }

        boolean valid = mfaVerificationService.verifyTotpCode(user, code);
        if (!valid) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Código TOTP inválido ou já utilizado"));
        }

        mfaVerificationService.consumeChallenge(token);
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
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Token de enrollment inválido ou expirado."));
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
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Token de enrollment inválido ou expirado."));
        }

        String pendingSecret = mfaEnrollmentService.getPendingEnrollmentSecret(user);
        if (pendingSecret == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Sessão de enrollment expirada. Inicie novamente."));
        }

        boolean valid = mfaVerificationService.verifyTotpWithSecret(user.getId(), pendingSecret, request.getCode());
        if (!valid) {
            return ResponseEntity.badRequest().body(Map.of("message", "Código TOTP incorreto. Verifique a hora do seu dispositivo."));
        }

        // Cifra o segredo com AES-256-GCM antes de persistir
        String encryptedSecret = encryptionService.encrypt(pendingSecret);
        user.setMfaSecretEncrypted(encryptedSecret);
        user.setMfaEnabled(true);
        userRepository.save(user);

        mfaEnrollmentService.clearPendingEnrollment(user);
        if (request.getEnrollmentToken() != null) {
            mfaEnrollmentService.consumeEnrollmentToken(request.getEnrollmentToken());
        }

        // Gera e retorna 8 códigos de backup
        List<String> backupCodes = mfaVerificationService.generateBackupCodes(user);

        auditService.record(user, AuditAction.MFA_ENROLLED, "users", user.getId(), "{\"mfa_enabled\": true}");

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return ResponseEntity.ok(new MfaConfirmResponse(
                "MFA ativado com sucesso. Guarde os códigos de recuperação em local seguro.",
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
        return ResponseEntity.ok(Map.of("message", "Sessão encerrada com sucesso"));
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
            // Recarrega do banco para ter o estado mais recente
            return userRepository.findById(user.getId()).orElse(user);
        }
        String email = authentication != null ? authentication.getName() : null;
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilizador não encontrado no contexto de segurança"));
    }
}
