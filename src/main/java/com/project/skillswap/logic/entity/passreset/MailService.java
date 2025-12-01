package com.project.skillswap.logic.entity.passreset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface MailService {
    void sendPasswordResetCode(String toEmail, String code, int ttlMinutes);
}
