package com.comply.auth.dto;

public class MfaVerifyRequest {

  private String challengeToken;
  private String tempToken;

  private String code;
  private String totpCode;

  public MfaVerifyRequest() {}

  public MfaVerifyRequest(String challengeToken, String code) {
    this.challengeToken = challengeToken;
    this.code = code;
  }

  public String getChallengeToken() {
    return challengeToken != null && !challengeToken.isBlank() ? challengeToken : tempToken;
  }

  public void setChallengeToken(String challengeToken) {
    this.challengeToken = challengeToken;
  }

  public String getTempToken() {
    return tempToken;
  }

  public void setTempToken(String tempToken) {
    this.tempToken = tempToken;
  }

  public String getCode() {
    return code != null && !code.isBlank() ? code : totpCode;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getTotpCode() {
    return totpCode;
  }

  public void setTotpCode(String totpCode) {
    this.totpCode = totpCode;
  }
}
