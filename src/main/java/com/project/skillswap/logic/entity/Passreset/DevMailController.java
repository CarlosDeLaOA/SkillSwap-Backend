package com.project.skillswap.logic.entity.Passreset;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/dev/mail")
public class DevMailController {
    //#region deps
    private final MailService mail;
    //#endregion

    public DevMailController(MailService mail) {
        this.mail = mail;
    }

    //#region routes
    @PostMapping("/test")
    public ResponseEntity<?> test(@RequestBody Map<String, String> body) {
        String to = body.get("to");
        String code = body.getOrDefault("code", "123456");
        mail.sendResetCode(to, code);
        return ResponseEntity.ok(Map.of("status", "sent", "to", to));
    }
    //#endregion
}
