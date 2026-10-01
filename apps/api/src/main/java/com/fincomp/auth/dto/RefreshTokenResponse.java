package com.fincomp.auth.dto;

public record RefreshTokenResponse(String accessToken, String refreshToken) {
}
