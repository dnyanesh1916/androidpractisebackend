package com.attendease.dto;

public class AuthResponse {
    private Long userId;
    private String fullName;
    private String email;
    private String role;
    private String profileImage;
    private String message;

    public AuthResponse() {
    }

    public AuthResponse(Long userId, String fullName, String email, String role, String profileImage, String message) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.profileImage = profileImage;
        this.message = message;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
