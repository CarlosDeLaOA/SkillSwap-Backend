package com.project.skillswap.logic.entity.PassReset;

public interface MailService {
    void sendPasswordResetCode(String toEmail, String code, int ttlMinutes);
}
