package com.fincomp.auth.dto;

import com.fincomp.users.entity.Role;
import java.util.UUID;

public class LoginResponse {

    private String status; // "SUCCESS", "MFA_REQUIRED", ou "MFA_ENROLLMENT_REQUIRED"
    private String challengeToken; // Presente quando status == "MFA_REQUIRED"
    private String enrollmentToken; // Presente quando status == "MFA_ENROLLMENT_REQUIRED"
    private String accessToken;
    private String refreshToken;
    private UUID userId;
    private String fullName;
    private String email;
    private Role role;
    private boolean mfaEnabled;

    public LoginResponse() {
    }

    public static LoginResponse mfaEnrollmentRequired(String enrollmentToken) {
        LoginResponse resp = new LoginResponse();
        resp.setStatus("MFA_ENROLLMENT_REQUIRED");
        resp.setEnrollmentToken(enrollmentToken);
        return resp;
    }

    public static LoginResponse mfaRequired(String challengeToken) {
        LoginResponse resp = new LoginResponse();
        resp.setStatus("MFA_REQUIRED");
        resp.setChallengeToken(challengeToken);
        return resp;
    }

    public static LoginResponse success(String accessToken, String refreshToken, UUID userId, String fullName, String email, Role role, boolean mfaEnabled) {
        LoginResponse resp = new LoginResponse();
        resp.setStatus("SUCCESS");
        resp.setAccessToken(accessToken);
        resp.setRefreshToken(refreshToken);
        resp.setUserId(userId);
        resp.setFullName(fullName);
        resp.setEmail(email);
        resp.setRole(role);
        resp.setMfaEnabled(mfaEnabled);
        return resp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getChallengeToken() {
        return challengeToken;
    }

    public void setChallengeToken(String challengeToken) {
        this.challengeToken = challengeToken;
    }

    public String getEnrollmentToken() {
        return enrollmentToken;
    }

    public void setEnrollmentToken(String enrollmentToken) {
        this.enrollmentToken = enrollmentToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isMfaEnabled() {
        return mfaEnabled;
    }

    public void setMfaEnabled(boolean mfaEnabled) {
        this.mfaEnabled = mfaEnabled;
    }
}
