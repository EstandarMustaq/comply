package com.fincomp.auth.dto;

public class MfaConfirmRequest {

    private String code;
    private String enrollmentToken;

    public MfaConfirmRequest() {
    }

    public MfaConfirmRequest(String code) {
        this.code = code;
    }

    public MfaConfirmRequest(String code, String enrollmentToken) {
        this.code = code;
        this.enrollmentToken = enrollmentToken;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getEnrollmentToken() {
        return enrollmentToken;
    }

    public void setEnrollmentToken(String enrollmentToken) {
        this.enrollmentToken = enrollmentToken;
    }
}
