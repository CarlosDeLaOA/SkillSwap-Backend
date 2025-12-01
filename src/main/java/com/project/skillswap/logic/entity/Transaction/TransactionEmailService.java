package com.project.skillswap.logic.entity.Transaction;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
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
    private static final Logger logger = LoggerFactory.getLogger(TransactionEmailService.class);

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

            helper.setText(emailBody, true);

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
     * Construye el cuerpo HTML del email de confirmación de compra.
     *
     * @param userName nombre del usuario
     * @param transactionId ID de la transacción
     * @param packageType tipo de paquete
     * @param coinsAdded monedas agregadas
     * @param usdAmount monto en USD
     * @param newBalance nuevo balance
     * @param paypalReference referencia PayPal
     * @return cuerpo del email en formato HTML
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

        return "<!DOCTYPE html>" +
                "<html lang='es'>" +
                "<head>" +
                "    <meta charset='UTF-8'>" +
                "    <meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "    <title>Confirmación de Compra</title>" +
                "</head>" +
                "<body style='margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #39434b;'>" +
                "    <table width='100%' cellpadding='0' cellspacing='0' style='background-color: #39434b; padding: 40px 20px;'>" +
                "        <tr>" +
                "            <td align='center'>" +
                "                <table width='600' cellpadding='0' cellspacing='0' style='background-color: #141414; border-radius: 10px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.1);'>" +
                "                    <tr>" +
                "                        <td style='background: linear-gradient(135deg, #504ab7 0%, #aae16b 100%); padding: 40px 20px; text-align: center;'>" +
                "                            <h1 style='color: #ffffff; margin: 0; font-size: 28px; font-weight: bold;'>SkillSwap</h1>" +
                "                        </td>" +
                "                    </tr>" +
                "                    <tr>" +
                "                        <td style='padding: 40px 30px; color: #ffffff;'>" +
                "                            <h2 style='color: #aae16b; margin-top: 0; font-size: 24px;'>¡Hola, " + userName + "!</h2>" +
                "                            <p style='font-size: 16px; line-height: 1.6; color: #ffffff; margin: 20px 0;'>" +
                "                                ¡Muchas gracias por tu compra! Tus <strong style='color: #aae16b;'>SkillCoins</strong> han sido agregadas a tu cuenta exitosamente." +
                "                            </p>" +
                "                            <div style='background-color: #39434b; padding: 20px; border-radius: 5px; margin: 30px 0;'>" +
                "                                <h3 style='color: #aae16b; margin-top: 0; font-size: 18px; border-bottom: 2px solid #504ab7; padding-bottom: 10px;'>Detalles de la Compra</h3>" +
                "                                <table width='100%' cellpadding='8' cellspacing='0' style='font-size: 14px;'>" +
                "                                    <tr>" +
                "                                        <td style='color: #aae16b; font-weight: bold; width: 180px;'>Paquete:</td>" +
                "                                        <td style='color: #ffffff;'>" + packageType + "</td>" +
                "                                    </tr>" +
                "                                    <tr>" +
                "                                        <td style='color: #aae16b; font-weight: bold;'>SkillCoins Agregadas:</td>" +
                "                                        <td style='color: #ffffff;'>" + coinsAdded + " coins</td>" +
                "                                    </tr>" +
                "                                    <tr>" +
                "                                        <td style='color: #aae16b; font-weight: bold;'>Monto Pagado:</td>" +
                "                                        <td style='color: #ffffff;'>$" + usdAmount + " USD</td>" +
                "                                    </tr>" +
                "                                    <tr>" +
                "                                        <td style='color: #aae16b; font-weight: bold;'>Nuevo Balance:</td>" +
                "                                        <td style='color: #ffffff; font-size: 16px; font-weight: bold;'>" + newBalance + " coins</td>" +
                "                                    </tr>" +
                "                                    <tr>" +
                "                                        <td style='color: #aae16b; font-weight: bold;'>ID de Transacción:</td>" +
                "                                        <td style='color: #ffffff;'>#" + transactionId + "</td>" +
                "                                    </tr>" +
                "                                    <tr>" +
                "                                        <td style='color: #aae16b; font-weight: bold;'>Referencia PayPal:</td>" +
                "                                        <td style='color: #ffffff; font-size: 12px;'>" + paypalReference + "</td>" +
                "                                    </tr>" +
                "                                    <tr>" +
                "                                        <td style='color: #aae16b; font-weight: bold;'>Fecha y Hora:</td>" +
                "                                        <td style='color: #ffffff;'>" + timestamp + "</td>" +
                "                                    </tr>" +
                "                                </table>" +
                "                            </div>" +
                "                            <div style='background-color: #504ab7; padding: 15px; border-radius: 5px; margin: 20px 0; border-left: 4px solid #aae16b;'>" +
                "                                <p style='font-size: 14px; line-height: 1.6; color: #ffffff; margin: 0;'>" +
                "                                    <strong>📄 Comprobante Adjunto:</strong> Hemos adjuntado tu comprobante de compra en formato PDF para tus registros." +
                "                                </p>" +
                "                            </div>" +
                "                            <div style='background-color: #39434b; padding: 15px; border-radius: 5px; margin: 20px 0; text-align: center;'>" +
                "                                <p style='font-size: 14px; line-height: 1.6; color: #b0b0b0; margin: 0;'>" +
                "                                    Si tienes alguna pregunta sobre tu compra, por favor contacta a nuestro equipo de soporte." +
                "                                </p>" +
                "                            </div>" +
                "                            <p style='font-size: 14px; line-height: 1.6; color: #b0b0b0; margin: 30px 0 0 0;'>" +
                "                                Saludos cordiales,<br>" +
                "                                <strong style='color: #aae16b;'>El Equipo de " + appName + "</strong>" +
                "                            </p>" +
                "                        </td>" +
                "                    </tr>" +
                "                    <tr>" +
                "                        <td style='background-color: #39434b; padding: 20px 30px; text-align: center;'>" +
                "                            <p style='margin: 0; font-size: 12px; color: #b0b0b0;'>" +
                "                                Este es un mensaje automático. Por favor no respondas a este correo." +
                "                            </p>" +
                "                            <p style='margin: 10px 0 0 0; font-size: 12px; color: #b0b0b0;'>" +
                "                                © 2025 SkillSwap. Todos los derechos reservados." +
                "                            </p>" +
                "                        </td>" +
                "                    </tr>" +
                "                </table>" +
                "            </td>" +
                "        </tr>" +
                "    </table>" +
                "</body>" +
                "</html>";
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
            helper.setSubject("⚠️ Pago Fallido - " + appName);

            String emailBody = buildPurchaseFailedEmail(userName, packageType, errorMessage);

            helper.setText(emailBody, true);
            mailSender.send(message);

        } catch (Exception e) {
            throw new RuntimeException("Failed to send failure notification email: " + e.getMessage(), e);
        }
    }

    /**
     * Construye el cuerpo HTML del email de notificación de fallo.
     *
     * @param userName nombre del usuario
     * @param packageType tipo de paquete
     * @param errorMessage mensaje de error
     * @return cuerpo del email en formato HTML
     */
    private String buildPurchaseFailedEmail(
            String userName,
            String packageType,
            String errorMessage
    ) {
        return "<!DOCTYPE html>" +
                "<html lang='es'>" +
                "<head>" +
                "    <meta charset='UTF-8'>" +
                "    <meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "    <title>Pago Fallido</title>" +
                "</head>" +
                "<body style='margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #39434b;'>" +
                "    <table width='100%' cellpadding='0' cellspacing='0' style='background-color: #39434b; padding: 40px 20px;'>" +
                "        <tr>" +
                "            <td align='center'>" +
                "                <table width='600' cellpadding='0' cellspacing='0' style='background-color: #141414; border-radius: 10px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.1);'>" +
                "                    <tr>" +
                "                        <td style='background: linear-gradient(135deg, #504ab7 0%, #aae16b 100%); padding: 40px 20px; text-align: center;'>" +
                "                            <h1 style='color: #ffffff; margin: 0; font-size: 28px; font-weight: bold;'>SkillSwap</h1>" +
                "                        </td>" +
                "                    </tr>" +
                "                    <tr>" +
                "                        <td style='padding: 40px 30px; color: #ffffff;'>" +
                "                            <h2 style='color: #aae16b; margin-top: 0; font-size: 24px;'>Hola, " + userName + "</h2>" +
                "                            <p style='font-size: 16px; line-height: 1.6; color: #ffffff; margin: 20px 0;'>" +
                "                                Lo sentimos, pero tu intento de compra reciente no fue exitoso." +
                "                            </p>" +
                "                            <div style='background-color: #39434b; padding: 20px; border-radius: 5px; margin: 30px 0;'>" +
                "                                <h3 style='color: #aae16b; margin-top: 0; font-size: 18px; border-bottom: 2px solid #504ab7; padding-bottom: 10px;'>Detalles de la Transacción</h3>" +
                "                                <table width='100%' cellpadding='8' cellspacing='0' style='font-size: 14px;'>" +
                "                                    <tr>" +
                "                                        <td style='color: #aae16b; font-weight: bold; width: 120px;'>Paquete:</td>" +
                "                                        <td style='color: #ffffff;'>" + packageType + "</td>" +
                "                                    </tr>" +
                "                                    <tr>" +
                "                                        <td style='color: #aae16b; font-weight: bold;'>Estado:</td>" +
                "                                        <td style='color: #ff6b6b; font-weight: bold;'>❌ Fallido</td>" +
                "                                    </tr>" +
                "                                    <tr>" +
                "                                        <td style='color: #aae16b; font-weight: bold;'>Razón:</td>" +
                "                                        <td style='color: #ffffff;'>" + errorMessage + "</td>" +
                "                                    </tr>" +
                "                                </table>" +
                "                            </div>" +
                "                            <div style='background-color: #39434b; padding: 20px; border-radius: 5px; margin: 20px 0;'>" +
                "                                <h3 style='color: #aae16b; margin-top: 0; font-size: 18px;'>¿Qué hacer ahora?</h3>" +
                "                                <ul style='color: #ffffff; font-size: 14px; line-height: 1.8; padding-left: 20px; margin: 10px 0;'>" +
                "                                    <li>Por favor verifica que tu cuenta de <strong style='color: #aae16b;'>PayPal</strong> esté activa y tenga fondos suficientes</li>" +
                "                                    <li>Intenta realizar la compra nuevamente</li>" +
                "                                    <li>Si el problema persiste, contacta a nuestro equipo de soporte</li>" +
                "                                </ul>" +
                "                            </div>" +
                "                            <div style='background-color: #504ab7; padding: 15px; border-radius: 5px; margin: 20px 0; border-left: 4px solid #aae16b; text-align: center;'>" +
                "                                <p style='font-size: 14px; line-height: 1.6; color: #ffffff; margin: 0;'>" +
                "                                    <strong>💡 Consejo:</strong> Asegúrate de completar el proceso de pago en la ventana de PayPal sin cerrarla." +
                "                                </p>" +
                "                            </div>" +
                "                            <p style='font-size: 14px; line-height: 1.6; color: #b0b0b0; margin: 30px 0 0 0;'>" +
                "                                Saludos cordiales,<br>" +
                "                                <strong style='color: #aae16b;'>El Equipo de " + appName + "</strong>" +
                "                            </p>" +
                "                        </td>" +
                "                    </tr>" +
                "                    <tr>" +
                "                        <td style='background-color: #39434b; padding: 20px 30px; text-align: center;'>" +
                "                            <p style='margin: 0; font-size: 12px; color: #b0b0b0;'>" +
                "                                Este es un mensaje automático. Por favor no respondas a este correo." +
                "                            </p>" +
                "                            <p style='margin: 10px 0 0 0; font-size: 12px; color: #b0b0b0;'>" +
                "                                © 2025 SkillSwap. Todos los derechos reservados." +
                "                            </p>" +
                "                        </td>" +
                "                    </tr>" +
                "                </table>" +
                "            </td>" +
                "        </tr>" +
                "    </table>" +
                "</body>" +
                "</html>";
    }
}