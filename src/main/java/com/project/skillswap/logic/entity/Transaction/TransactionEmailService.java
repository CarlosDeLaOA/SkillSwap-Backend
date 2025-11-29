package com.project.skillswap.logic.entity.Transaction;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Service for sending transaction-related emails
 */
@Service
public class TransactionEmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@skillswap.com}")
    private String fromEmail;

    @Value("${app.name:SkillSwap}")
    private String appName;

    /**
     * Sends a purchase confirmation email to the user
     * @param userEmail recipient email
     * @param userName recipient name
     * @param transactionId transaction ID
     * @param packageType package purchased (BASIC, MEDIUM, LARGE, PREMIUM)
     * @param coinsAdded coins added to account
     * @param usdAmount amount paid in USD
     * @param newBalance new total balance
     * @param paypalReference PayPal order ID
     */
    public void sendPurchaseConfirmation(
            String userEmail,
            String userName,
            Long transactionId,
            String packageType,
            BigDecimal coinsAdded,
            BigDecimal usdAmount,
            BigDecimal newBalance,
            String paypalReference
    ) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(userEmail);
            message.setSubject("Confirmación de Compra - " + appName);

            String emailBody = buildPurchaseConfirmationEmail(
                    userName, transactionId, packageType, coinsAdded,
                    usdAmount, newBalance, paypalReference
            );

            message.setText(emailBody);

            mailSender.send(message);

            System.out.println("✅ Purchase confirmation email sent to: " + userEmail);

        } catch (Exception e) {
            // Log error but don't fail the transaction
            System.err.println("⚠️ Failed to send purchase confirmation email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Builds the email body for purchase confirmation
     */
    private String buildPurchaseConfirmationEmail(
            String userName,
            Long transactionId,
            String packageType,
            BigDecimal coinsAdded,
            BigDecimal usdAmount,
            BigDecimal newBalance,
            String paypalReference
    ) {
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy 'a las' hh:mm a", new Locale("es", "ES")));

        return String.format("""
            Hola %s,
            
            ¡Muchas gracias por tu compra! Tus SkillCoins han sido agregadas a tu cuenta exitosamente.
            
            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            DETALLES DE LA COMPRA
            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            
            Paquete:              %s
            SkillCoins Agregadas: %s coins
            Monto Pagado:         $%s USD
            ID de Transacción:    #%d
            Referencia PayPal:    %s
            Fecha y Hora:         %s
            
            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            BALANCE DE LA CUENTA
            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            
            Nuevo Balance:        %s coins
            
            ¡Tus SkillCoins ya están disponibles para reservar sesiones premium con nuestros instructores expertos!
            
            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            ¿NECESITAS AYUDA?
            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            
            Si tienes alguna pregunta sobre tu compra, por favor contacta a nuestro equipo de soporte.
            
            Saludos cordiales,
            El Equipo de SkillSwap
            
            ---
            Este es un mensaje automático. Por favor no respondas a este correo.
            """,
                userName,
                packageType,
                coinsAdded,
                usdAmount,
                transactionId,
                paypalReference,
                timestamp,
                newBalance
        );
    }

    /**
     * Sends a failed transaction notification email
     * @param userEmail recipient email
     * @param userName recipient name
     * @param packageType package attempted to purchase
     * @param errorMessage error description
     */
    public void sendPurchaseFailedNotification(
            String userEmail,
            String userName,
            String packageType,
            String errorMessage
    ) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(userEmail);
            message.setSubject(" Pago Fallido - " + appName);

            String emailBody = String.format("""
                Hola %s,
                
                Lo sentimos, pero tu intento de compra reciente no fue exitoso.
                
                ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                DETALLES DE LA TRANSACCIÓN
                ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                
                Paquete:    %s
                Estado:     Fallido
                Razón:      %s
                
                ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                ¿QUÉ HACER AHORA?
                ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                
                1. Por favor verifica que tu cuenta de PayPal esté activa y tenga fondos suficientes
                2. Intenta realizar la compra nuevamente
                3. Si el problema persiste, contacta a nuestro equipo de soporte
                
                Saludos cordiales,
                El Equipo de SkillSwap
                
                ---
                Este es un mensaje automático. Por favor no respondas a este correo.
                """,
                    userName,
                    packageType,
                    errorMessage
            );

            message.setText(emailBody);
            mailSender.send(message);

            System.out.println("✅ Failed purchase notification sent to: " + userEmail);

        } catch (Exception e) {
            System.err.println("⚠️ Failed to send failure notification email: " + e.getMessage());
            e.printStackTrace();
        }
    }
}