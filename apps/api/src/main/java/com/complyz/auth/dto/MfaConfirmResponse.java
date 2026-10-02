package com.complyz.auth.dto;

import com.complyz.users.entity.Role;
import java.util.List;
import java.util.UUID;

public class MfaConfirmResponse {

    private String message;
    private List<String> backupCodes;
    private String accessToken;
    private String refreshToken;
    private UUID userId;
    private String fullName;
    private String email;
    private Role role;

    public MfaConfirmResponse() {
    }

    public MfaConfirmResponse(String message, List<String> backupCodes) {
        this.message = message;
        this.backupCodes = backupCodes;
    }

    public MfaConfirmResponse(String message, List<String> backupCodes, String accessToken, String refreshToken, UUID userId, String fullName, String email, Role role) {
        this.message = message;
        this.backupCodes = backupCodes;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<String> getBackupCodes() {
        return backupCodes;
    }

    public void setBackupCodes(List<String> backupCodes) {
        this.backupCodes = backupCodes;
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
}
