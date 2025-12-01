package com.project.skillswap.logic.entity.Transaction;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Servicio para enviar emails relacionados con transacciones de SkillCoins.
 * Incluye notificaciones de compra exitosa con comprobante PDF adjunto
 * y notificaciones de fallos en el procesamiento de pagos.
 *
 * @author Equipo de Desarrollo SkillSwap
 * @version 1.0
 */
@Service
public class TransactionEmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private PurchaseReceiptPdfService purchaseReceiptPdfService;

    @Value("${spring.mail.username:noreply@skillswap.com}")
    private String fromEmail;

    @Value("${app.name:SkillSwap}")
    private String appName;

    /**
     * Envía email de confirmación de compra con comprobante PDF adjunto.
     * El email incluye detalles de la transacción y el PDF se genera automáticamente.
     *
     * @param userEmail email del destinatario
     * @param userName nombre del destinatario
     * @param transaction transacción completada
     * @param packageType tipo de paquete comprado
     * @param coinsAdded monedas agregadas a la cuenta
     * @param usdAmount monto pagado en USD
     * @param newBalance nuevo balance total
     * @param paypalReference referencia de PayPal
     */
    public void sendPurchaseConfirmation(
            String userEmail,
            String userName,
            Transaction transaction,
            CoinPackageType packageType,
            BigDecimal coinsAdded,
            BigDecimal usdAmount,
            BigDecimal newBalance,
            String paypalReference
    ) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(userEmail);
            helper.setSubject("Confirmación de Compra - " + appName);

            String emailBody = buildPurchaseConfirmationEmail(
                    userName, transaction.getId(), packageType.name(), coinsAdded,
                    usdAmount, newBalance, paypalReference
            );

            helper.setText(emailBody);

            byte[] pdfBytes = purchaseReceiptPdfService.generatePurchaseReceipt(
                    transaction,
                    transaction.getPerson(),
                    packageType,
                    newBalance
            );

            helper.addAttachment(
                    "Comprobante_SkillSwap_" + transaction.getId() + ".pdf",
                    new ByteArrayResource(pdfBytes)
            );

            mailSender.send(message);

        } catch (Exception e) {
            throw new RuntimeException("Failed to send purchase confirmation email: " + e.getMessage(), e);
        }
    }

    /**
     * Construye el cuerpo del email de confirmación de compra.
     *
     * @param userName nombre del usuario
     * @param transactionId ID de la transacción
     * @param packageType tipo de paquete
     * @param coinsAdded monedas agregadas
     * @param usdAmount monto en USD
     * @param newBalance nuevo balance
     * @param paypalReference referencia PayPal
     * @return cuerpo del email formateado
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
            
            COMPROBANTE ADJUNTO
            Hemos adjuntado tu comprobante de compra en formato PDF para tus registros.
            
            Si tienes alguna pregunta sobre tu compra, por favor contacta a nuestro equipo de soporte.
            
            Saludos cordiales,
            El Equipo de SkillSwap
            
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
     * Envía email de notificación de fallo en la transacción.
     *
     * @param userEmail email del destinatario
     * @param userName nombre del destinatario
     * @param packageType tipo de paquete que se intentó comprar
     * @param errorMessage descripción del error
     */
    public void sendPurchaseFailedNotification(
            String userEmail,
            String userName,
            String packageType,
            String errorMessage
    ) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(userEmail);
            helper.setSubject("️ Pago Fallido - " + appName);

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

            helper.setText(emailBody);
            mailSender.send(message);

        } catch (Exception e) {
            throw new RuntimeException("Failed to send failure notification email: " + e.getMessage(), e);
        }
    }
}