package com.project.skillswap.logic.entity.passreset;
public interface MailService {
    void sendPasswordResetCode(String toEmail, String code, int ttlMinutes);
}
