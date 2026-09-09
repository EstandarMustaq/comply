package com.fincomp.auth.dto;

public class MfaEnrollResponse {

    private String secretKey;
    private String qrCodeBase64;
    private String otpauthUrl;

    public MfaEnrollResponse() {
    }

    public MfaEnrollResponse(String secretKey, String qrCodeBase64, String otpauthUrl) {
        this.secretKey = secretKey;
        this.qrCodeBase64 = qrCodeBase64;
        this.otpauthUrl = otpauthUrl;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getQrCodeBase64() {
        return qrCodeBase64;
    }

    public void setQrCodeBase64(String qrCodeBase64) {
        this.qrCodeBase64 = qrCodeBase64;
    }

    public String getOtpauthUrl() {
        return otpauthUrl;
    }

    public void setOtpauthUrl(String otpauthUrl) {
        this.otpauthUrl = otpauthUrl;
    }
}
