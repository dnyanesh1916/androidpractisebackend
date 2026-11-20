package com.attendease.dto;

public class RecoveryCodeResponse {

    private final String recoveryCode;

    public RecoveryCodeResponse(String recoveryCode) {
        this.recoveryCode = recoveryCode;
    }

    public String getRecoveryCode() {
        return recoveryCode;
    }
}
