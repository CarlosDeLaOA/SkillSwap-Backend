package com.project.skillswap.logic.entity.Notification;

import com.project.skillswap.logic.entity.CommunityDocument.CommunityDocument;
import com.project.skillswap.logic.entity.LearningCommunity.LearningCommunity;
import com.project.skillswap.logic.entity.Person.Person;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@Service
public class NotificationEmailService {

    private final JavaMailSender mailSender;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public NotificationEmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Envía email cuando se agrega un documento
     */
    public void sendDocumentAddedEmail(Person recipient, LearningCommunity community,
                                       CommunityDocument document, Person uploader) throws MessagingException {
        String subject = "📄 Nuevo documento en " + community.getName();
        String htmlContent = buildDocumentAddedTemplate(recipient, community, document, uploader);
        sendHtmlEmail(recipient.getEmail(), subject, htmlContent);
    }

    /**
     * Envía email cuando alguien se une a la comunidad
     */
    public void sendMemberJoinedEmail(Person recipient, LearningCommunity community,
                                      Person newMember) throws MessagingException {
        String subject = "👋 Nuevo miembro en " + community.getName();
        String htmlContent = buildMemberJoinedTemplate(recipient, community, newMember);
        sendHtmlEmail(recipient.getEmail(), subject, htmlContent);
    }

    /**
     * Envía email al creador cuando alguien sale
     */
    public void sendMemberLeftEmail(Person creator, LearningCommunity community,
                                    Person leftMember) throws MessagingException {
        String subject = "Un miembro salió de " + community.getName();
        String htmlContent = buildMemberLeftTemplate(creator, community, leftMember);
        sendHtmlEmail(creator.getEmail(), subject, htmlContent);
    }

    /**
     * Envía email cuando alguien obtiene un logro
     */
    public void sendAchievementEarnedEmail(Person recipient, LearningCommunity community,
                                           Person achiever, String achievementName) throws MessagingException {
        String subject = "🏆" + achiever.getFullName() + " obtuvo certificación";
        String htmlContent = buildAchievementEarnedTemplate(recipient, community, achiever, achievementName);
        sendHtmlEmail(recipient.getEmail(), subject, htmlContent);
    }

    /**
     * Envía digest diario de notificaciones
     */
    public void sendDailyDigest(Person recipient, List<Notification> unreadNotifications) throws MessagingException {
        String subject = "Resumen diario de SkillSwap";
        String htmlContent = buildDailyDigestTemplate(recipient, unreadNotifications);
        sendHtmlEmail(recipient.getEmail(), subject, htmlContent);
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }

    private String buildDocumentAddedTemplate(Person recipient, LearningCommunity community,
                                              CommunityDocument document, Person uploader) {
        String communityLink = frontendUrl + "/app/communities/" + community.getId();

        return "<!DOCTYPE html>" +
                "<html lang='es'>" +
                "<head><meta charset='UTF-8'><title>Nuevo Documento</title></head>" +
                "<body style='margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #39434b;'>" +
                "    <table width='100%' cellpadding='0' cellspacing='0' style='background-color: #39434b; padding: 40px 20px;'>" +
                "        <tr><td align='center'>" +
                "            <table width='600' cellpadding='0' cellspacing='0' style='background-color: #141414; border-radius: 10px; overflow: hidden;'>" +
                "                <tr><td style='background: linear-gradient(135deg, #504ab7 0%, #aae16b 100%); padding: 40px 20px; text-align: center;'>" +
                "                    <h1 style='color: #ffffff; margin: 0; font-size: 28px;'>SkillSwap</h1>" +
                "                </td></tr>" +
                "                <tr><td style='padding: 40px 30px; color: #ffffff;'>" +
                "                    <h2 style='color: #aae16b; margin-top: 0;'>Nuevo Documento</h2>" +
                "                    <p style='font-size: 16px; line-height: 1.6;'>Hola <strong style='color: #aae16b;'>" + recipient.getFullName() + "</strong>,</p>" +
                "                    <p style='font-size: 16px; line-height: 1.6;'><strong style='color: #aae16b;'>" + uploader.getFullName() +
                "                    </strong> agregó un nuevo documento a tu comunidad <strong style='color: #504ab7;'>" + community.getName() + "</strong></p>" +
                "                    <div style='background-color: #39434b; padding: 20px; border-radius: 8px; margin: 25px 0; border-left: 4px solid #aae16b;'>" +
                "                        <h3 style='color: #aae16b; margin-top: 0;'>" + document.getTitle() + "</h3>" +
                "                        <p style='color: #b0b0b0; font-size: 14px;'>Subido el " + formatDate(document.getUploadDate()) + "</p>" +
                "                    </div>" +
                "                    <table width='100%' cellpadding='0' cellspacing='0' style='margin: 30px 0;'>" +
                "                        <tr><td align='center'>" +
                "                            <a href='" + communityLink + "' style='display: inline-block; background: linear-gradient(135deg, #504ab7 0%, #aae16b 100%); color: #ffffff; text-decoration: none; padding: 15px 40px; border-radius: 5px; font-size: 16px; font-weight: bold;'>Ver Comunidad</a>" +
                "                        </td></tr>" +
                "                    </table>" +
                "                </td></tr>" +
                "                <tr><td style='background-color: #39434b; padding: 20px 30px; text-align: center;'>" +
                "                    <p style='margin: 0; font-size: 12px; color: #b0b0b0;'>© 2025 SkillSwap. Todos los derechos reservados.</p>" +
                "                </td></tr>" +
                "            </table>" +
                "        </td></tr>" +
                "    </table>" +
                "</body></html>";
    }

    private String buildMemberJoinedTemplate(Person recipient, LearningCommunity community, Person newMember) {
        String communityLink = frontendUrl + "/app/communities/" + community.getId();

        return "<!DOCTYPE html>" +
                "<html lang='es'>" +
                "<head><meta charset='UTF-8'><title>Nuevo Miembro</title></head>" +
                "<body style='margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #39434b;'>" +
                "    <table width='100%' cellpadding='0' cellspacing='0' style='background-color: #39434b; padding: 40px 20px;'>" +
                "        <tr><td align='center'>" +
                "            <table width='600' cellpadding='0' cellspacing='0' style='background-color: #141414; border-radius: 10px; overflow: hidden;'>" +
                "                <tr><td style='background: linear-gradient(135deg, #504ab7 0%, #aae16b 100%); padding: 40px 20px; text-align: center;'>" +
                "                    <h1 style='color: #ffffff; margin: 0; font-size: 28px;'>SkillSwap</h1>" +
                "                </td></tr>" +
                "                <tr><td style='padding: 40px 30px; color: #ffffff;'>" +
                "                    <h2 style='color: #aae16b; margin-top: 0;'>Nuevo Miembro</h2>" +
                "                    <p style='font-size: 16px; line-height: 1.6;'>Hola <strong style='color: #aae16b;'>" + recipient.getFullName() + "</strong>,</p>" +
                "                    <p style='font-size: 16px; line-height: 1.6;'><strong style='color: #aae16b;'>" + newMember.getFullName() +
                "                    </strong> se unió a tu comunidad <strong style='color: #504ab7;'>" + community.getName() + "</strong></p>" +
                "                    <table width='100%' cellpadding='0' cellspacing='0' style='margin: 30px 0;'>" +
                "                        <tr><td align='center'>" +
                "                            <a href='" + communityLink + "' style='display: inline-block; background: linear-gradient(135deg, #504ab7 0%, #aae16b 100%); color: #ffffff; text-decoration: none; padding: 15px 40px; border-radius: 5px; font-size: 16px; font-weight: bold;'>Ver Comunidad</a>" +
                "                        </td></tr>" +
                "                    </table>" +
                "                </td></tr>" +
                "                <tr><td style='background-color: #39434b; padding: 20px 30px; text-align: center;'>" +
                "                    <p style='margin: 0; font-size: 12px; color: #b0b0b0;'>© 2025 SkillSwap. Todos los derechos reservados.</p>" +
                "                </td></tr>" +
                "            </table>" +
                "        </td></tr>" +
                "    </table>" +
                "</body></html>";
    }

    private String buildMemberLeftTemplate(Person creator, LearningCommunity community, Person leftMember) {
        String communityLink = frontendUrl + "/app/communities/" + community.getId();

        return "<!DOCTYPE html>" +
                "<html lang='es'>" +
                "<head><meta charset='UTF-8'><title>Miembro Salió</title></head>" +
                "<body style='margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #39434b;'>" +
                "    <table width='100%' cellpadding='0' cellspacing='0' style='background-color: #39434b; padding: 40px 20px;'>" +
                "        <tr><td align='center'>" +
                "            <table width='600' cellpadding='0' cellspacing='0' style='background-color: #141414; border-radius: 10px; overflow: hidden;'>" +
                "                <tr><td style='background: linear-gradient(135deg, #504ab7 0%, #aae16b 100%); padding: 40px 20px; text-align: center;'>" +
                "                    <h1 style='color: #ffffff; margin: 0; font-size: 28px;'>SkillSwap</h1>" +
                "                </td></tr>" +
                "                <tr><td style='padding: 40px 30px; color: #ffffff;'>" +
                "                    <h2 style='color: #ffa500; margin-top: 0;'>Un miembro salió</h2>" +
                "                    <p style='font-size: 16px; line-height: 1.6;'>Hola <strong style='color: #aae16b;'>" + creator.getFullName() + "</strong>,</p>" +
                "                    <p style='font-size: 16px; line-height: 1.6;'><strong style='color: #ffa500;'>" + leftMember.getFullName() +
                "                    </strong> salió de tu comunidad <strong style='color: #504ab7;'>" + community.getName() + "</strong></p>" +
                "                    <table width='100%' cellpadding='0' cellspacing='0' style='margin: 30px 0;'>" +
                "                        <tr><td align='center'>" +
                "                            <a href='" + communityLink + "' style='display: inline-block; background: linear-gradient(135deg, #504ab7 0%, #aae16b 100%); color: #ffffff; text-decoration: none; padding: 15px 40px; border-radius: 5px; font-size: 16px; font-weight: bold;'>Ver Comunidad</a>" +
                "                        </td></tr>" +
                "                    </table>" +
                "                </td></tr>" +
                "                <tr><td style='background-color: #39434b; padding: 20px 30px; text-align: center;'>" +
                "                    <p style='margin: 0; font-size: 12px; color: #b0b0b0;'>© 2025 SkillSwap. Todos los derechos reservados.</p>" +
                "                </td></tr>" +
                "            </table>" +
                "        </td></tr>" +
                "    </table>" +
                "</body></html>";
    }

    private String buildAchievementEarnedTemplate(Person recipient, LearningCommunity community,
                                                  Person achiever, String achievementName) {
        String communityLink = frontendUrl + "/app/communities/" + community.getId();

        return "<!DOCTYPE html>" +
                "<html lang='es'>" +
                "<head><meta charset='UTF-8'><title>Nuevo Logro</title></head>" +
                "<body style='margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #39434b;'>" +
                "    <table width='100%' cellpadding='0' cellspacing='0' style='background-color: #39434b; padding: 40px 20px;'>" +
                "        <tr><td align='center'>" +
                "            <table width='600' cellpadding='0' cellspacing='0' style='background-color: #141414; border-radius: 10px; overflow: hidden;'>" +
                "                <tr><td style='background: linear-gradient(135deg, #504ab7 0%, #aae16b 100%); padding: 40px 20px; text-align: center;'>" +
                "                    <h1 style='color: #ffffff; margin: 0; font-size: 28px;'>SkillSwap</h1>" +
                "                </td></tr>" +
                "                <tr><td style='padding: 40px 30px; color: #ffffff;'>" +
                "                    <h2 style='color: #aae16b; margin-top: 0;'>¡Nuevo Logro!</h2>" +
                "                    <p style='font-size: 16px; line-height: 1.6;'>Hola <strong style='color: #aae16b;'>" + recipient.getFullName() + "</strong>,</p>" +
                "                    <p style='font-size: 16px; line-height: 1.6;'><strong style='color: #aae16b;'>" + achiever.getFullName() +
                "                    </strong> obtuvo la certificación <strong style='color: #504ab7;'>" + achievementName + "</strong> en tu comunidad " + community.getName() + "</p>" +
                "                    <div style='background-color: #39434b; padding: 30px; border-radius: 8px; margin: 25px 0; text-align: center;'>" +
                "                        <div style='font-size: 60px; margin-bottom: 10px;'></div>" +
                "                        <h3 style='color: #aae16b; margin: 10px 0;'>" + achievementName + "</h3>" +
                "                    </div>" +
                "                    <table width='100%' cellpadding='0' cellspacing='0' style='margin: 30px 0;'>" +
                "                        <tr><td align='center'>" +
                "                            <a href='" + communityLink + "' style='display: inline-block; background: linear-gradient(135deg, #504ab7 0%, #aae16b 100%); color: #ffffff; text-decoration: none; padding: 15px 40px; border-radius: 5px; font-size: 16px; font-weight: bold;'>Ver Comunidad</a>" +
                "                        </td></tr>" +
                "                    </table>" +
                "                </td></tr>" +
                "                <tr><td style='background-color: #39434b; padding: 20px 30px; text-align: center;'>" +
                "                    <p style='margin: 0; font-size: 12px; color: #b0b0b0;'>© 2025 SkillSwap. Todos los derechos reservados.</p>" +
                "                </td></tr>" +
                "            </table>" +
                "        </td></tr>" +
                "    </table>" +
                "</body></html>";
    }

    private String buildDailyDigestTemplate(Person recipient, List<Notification> notifications) {
        String notificationsLink = frontendUrl + "/app/notifications";

        StringBuilder notificationsList = new StringBuilder();
        for (Notification notif : notifications) {
            notificationsList.append("<li style='margin: 10px 0; padding: 10px; background: #2a2a2a; border-radius: 5px;'>")
                    .append("<strong style='color: #aae16b;'>").append(notif.getTitle()).append("</strong><br>")
                    .append("<span style='color: #b0b0b0; font-size: 14px;'>").append(notif.getReadableContent()).append("</span>")
                    .append("</li>");
        }

        return "<!DOCTYPE html>" +
                "<html lang='es'>" +
                "<head><meta charset='UTF-8'><title>Resumen Diario</title></head>" +
                "<body style='margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #39434b;'>" +
                "    <table width='100%' cellpadding='0' cellspacing='0' style='background-color: #39434b; padding: 40px 20px;'>" +
                "        <tr><td align='center'>" +
                "            <table width='600' cellpadding='0' cellspacing='0' style='background-color: #141414; border-radius: 10px; overflow: hidden;'>" +
                "                <tr><td style='background: linear-gradient(135deg, #504ab7 0%, #aae16b 100%); padding: 40px 20px; text-align: center;'>" +
                "                    <h1 style='color: #ffffff; margin: 0; font-size: 28px;'>SkillSwap</h1>" +
                "                </td></tr>" +
                "                <tr><td style='padding: 40px 30px; color: #ffffff;'>" +
                "                    <h2 style='color: #aae16b; margin-top: 0;'>Resumen Diario</h2>" +
                "                    <p style='font-size: 16px; line-height: 1.6;'>Hola <strong style='color: #aae16b;'>" + recipient.getFullName() + "</strong>,</p>" +
                "                    <p style='font-size: 16px; line-height: 1.6;'>Tienes <strong style='color: #aae16b;'>" + notifications.size() +
                "                    " + (notifications.size() == 1 ? "notificación" : "notificaciones") + " sin leer:</strong></p>" +
                "                    <ul style='list-style: none; padding: 0; margin: 20px 0;'>" + notificationsList.toString() + "</ul>" +
                "                    <table width='100%' cellpadding='0' cellspacing='0' style='margin: 30px 0;'>" +
                "                        <tr><td align='center'>" +
                "                            <a href='" + notificationsLink + "' style='display: inline-block; background: linear-gradient(135deg, #504ab7 0%, #aae16b 100%); color: #ffffff; text-decoration: none; padding: 15px 40px; border-radius: 5px; font-size: 16px; font-weight: bold;'>Ver Todas las Notificaciones</a>" +
                "                        </td></tr>" +
                "                    </table>" +
                "                </td></tr>" +
                "                <tr><td style='background-color: #39434b; padding: 20px 30px; text-align: center;'>" +
                "                    <p style='margin: 0; font-size: 12px; color: #b0b0b0;'>© 2025 SkillSwap. Todos los derechos reservados.</p>" +
                "                </td></tr>" +
                "            </table>" +
                "        </td></tr>" +
                "    </table>" +
                "</body></html>";
    }

    private String formatDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("d 'de' MMMM 'de' yyyy", new Locale("es", "ES"));
        return sdf.format(date);
    }
}