package com.bierliste.backend.dto;

public class AuthTokenResponseDto {

    private String accessToken;
    private String refreshToken;
    private String userEmail;

    public AuthTokenResponseDto() {
    }

    public AuthTokenResponseDto(String accessToken, String refreshToken, String userEmail) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.userEmail = userEmail;
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

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }
}
