package com.project.skillswap.logic.entity.Certification;

import com.project.skillswap.logic.entity.Credential.CredentialRepository;
import com.project.skillswap.logic.entity.Learner.Learner;
import com.project.skillswap.logic.entity.Skill.Skill;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * Servicio para gestión de certificaciones
 * Genera certificados PDF cuando un learner alcanza 10 credenciales de una habilidad
 */
@Service
public class CertificationService {

    //#region Constants
    private static final int CREDENTIALS_REQUIRED = 10;
    private static final String CERTIFICATES_PATH = "certificates/";
    private static final String LOGO_PATH = "static/img/Imagotipo.png";
    //#endregion

    //#region Dependencies
    private final CertificationRepository certificationRepository;
    private final CredentialRepository credentialRepository;
    private final JavaMailSender mailSender;
    //#endregion

    //#region Constructor
    @Autowired
    public CertificationService(
            CertificationRepository certificationRepository,
            CredentialRepository credentialRepository,
            JavaMailSender mailSender
    ) {
        this.certificationRepository = certificationRepository;
        this.credentialRepository = credentialRepository;
        this.mailSender = mailSender;
    }
    //#endregion

    //#region Public Methods
    /**
     * Verifica si un learner ha alcanzado 10 credenciales en una habilidad
     * y genera el certificado correspondiente
     *
     * @param learner el aprendiz
     * @param skill la habilidad
     * @throws Exception si hay error al generar el PDF o enviar el correo
     */
    @Transactional
    public void checkAndGenerateCertificate(Learner learner, Skill skill) throws Exception {
        long credentialCount = credentialRepository.countByLearnerAndSkill(learner, skill);

        if (credentialCount >= CREDENTIALS_REQUIRED) {
            boolean alreadyCertified = certificationRepository.existsByLearnerAndSkill(learner, skill);

            if (!alreadyCertified) {
                Certification certification = createCertification(learner, skill);
                String pdfPath = generateCertificatePDF(certification);
                certification.setPdfUrl(pdfPath);
                certificationRepository.save(certification);
                sendCertificateEmail(certification);
            }
        }
    }

    /**
     * Verifica si existe una certificación para un learner y skill
     *
     * @param learner el aprendiz
     * @param skill la habilidad
     * @return true si ya existe la certificación
     */
    public boolean existsByLearnerAndSkill(Learner learner, Skill skill) {
        return certificationRepository.existsByLearnerAndSkill(learner, skill);
    }
    //#endregion

    //#region Private Methods
    /**
     * Crea la entidad Certification
     */
    private Certification createCertification(Learner learner, Skill skill) {
        Certification certification = new Certification();
        certification.setLearner(learner);
        certification.setSkill(skill);
        certification.setName("Certificado de " + skill.getName());
        certification.setAccumulatedCredentials(CREDENTIALS_REQUIRED);
        certification.setVerificationCode(generateVerificationCode());
        return certification;
    }

    /**
     * Genera el PDF del certificado
     */
    private String generateCertificatePDF(Certification certification) throws Exception {
        Document document = new Document(PageSize.A4.rotate());
        String fileName = "certificate_" + certification.getVerificationCode() + ".pdf";
        String filePath = CERTIFICATES_PATH + fileName;

        File directory = new File(CERTIFICATES_PATH);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, baos);

        document.open();
        addCertificateContent(document, certification);
        document.close();

        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            fos.write(baos.toByteArray());
        }

        return filePath;
    }

    /**
     * Añade el contenido al certificado PDF
     */
    private void addCertificateContent(Document document, Certification certification) throws Exception {
        addTitle(document);
        addRecipientName(document, certification);
        addSkillName(document, certification);
        addDate(document, certification);
        addVerificationCode(document, certification);
        addSignature(document);
    }

    /**
     * Añade el título del certificado
     */
    private void addTitle(Document document) throws DocumentException {
        Font titleFont = new Font(Font.HELVETICA, 36, Font.BOLD, new java.awt.Color(170, 225, 107));
        Paragraph title = new Paragraph("CERTIFICADO DE EXCELENCIA", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingBefore(80);
        document.add(title);

        Font subtitleFont = new Font(Font.HELVETICA, 14, Font.NORMAL, new java.awt.Color(100, 100, 100));
        Paragraph subtitle = new Paragraph("SkillSwap certifica que", subtitleFont);
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingBefore(30);
        document.add(subtitle);
    }

    /**
     * Añade el nombre del recipiente
     */
    private void addRecipientName(Document document, Certification certification) throws DocumentException {
        Font nameFont = new Font(Font.HELVETICA, 28, Font.BOLD, new java.awt.Color(50, 50, 50));
        Paragraph name = new Paragraph(certification.getLearner().getPerson().getFullName(), nameFont);
        name.setAlignment(Element.ALIGN_CENTER);
        name.setSpacingBefore(20);
        document.add(name);
    }

    /**
     * Añade el nombre de la habilidad
     */
    private void addSkillName(Document document, Certification certification) throws DocumentException {
        Font textFont = new Font(Font.HELVETICA, 14, Font.NORMAL, new java.awt.Color(100, 100, 100));
        Paragraph text1 = new Paragraph("ha completado exitosamente el programa de certificación en", textFont);
        text1.setAlignment(Element.ALIGN_CENTER);
        text1.setSpacingBefore(15);
        document.add(text1);

        Font skillFont = new Font(Font.HELVETICA, 22, Font.BOLD, new java.awt.Color(123, 199, 77));
        Paragraph skillName = new Paragraph(certification.getSkill().getName(), skillFont);
        skillName.setAlignment(Element.ALIGN_CENTER);
        skillName.setSpacingBefore(10);
        document.add(skillName);

        Font credentialsFont = new Font(Font.HELVETICA, 12, Font.NORMAL, new java.awt.Color(100, 100, 100));
        Paragraph credentials = new Paragraph("Acumulando " + CREDENTIALS_REQUIRED + " credenciales verificadas", credentialsFont);
        credentials.setAlignment(Element.ALIGN_CENTER);
        credentials.setSpacingBefore(10);
        document.add(credentials);
    }

    /**
     * Añade la fecha de emisión
     */
    private void addDate(Document document, Certification certification) throws DocumentException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd 'de' MMMM 'de' yyyy");
        String formattedDate = dateFormat.format(certification.getIssueDate() != null ? certification.getIssueDate() : new Date());

        Font dateFont = new Font(Font.HELVETICA, 12, Font.NORMAL, new java.awt.Color(100, 100, 100));
        Paragraph date = new Paragraph("Emitido el " + formattedDate, dateFont);
        date.setAlignment(Element.ALIGN_CENTER);
        date.setSpacingBefore(30);
        document.add(date);
    }

    /**
     * Añade el código de verificación
     */
    private void addVerificationCode(Document document, Certification certification) throws DocumentException {
        Font codeFont = new Font(Font.HELVETICA, 10, Font.ITALIC, new java.awt.Color(150, 150, 150));
        Paragraph code = new Paragraph("Código de verificación: " + certification.getVerificationCode(), codeFont);
        code.setAlignment(Element.ALIGN_CENTER);
        code.setSpacingBefore(40);
        document.add(code);
    }

    /**
     * Añade la firma
     */
    private void addSignature(Document document) throws DocumentException {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(30);
        table.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.setSpacingBefore(50);

        PdfPCell lineCell = new PdfPCell();
        lineCell.setBorder(Rectangle.TOP);
        lineCell.setBorderWidthTop(1);
        lineCell.setBorderColorTop(new java.awt.Color(100, 100, 100));
        lineCell.setFixedHeight(1);
        table.addCell(lineCell);

        Font signatureFont = new Font(Font.HELVETICA, 11, Font.NORMAL, new java.awt.Color(100, 100, 100));
        PdfPCell textCell = new PdfPCell(new Phrase("SkillSwap", signatureFont));
        textCell.setBorder(Rectangle.NO_BORDER);
        textCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        textCell.setPaddingTop(5);
        table.addCell(textCell);

        Font roleFont = new Font(Font.HELVETICA, 9, Font.ITALIC, new java.awt.Color(150, 150, 150));
        PdfPCell roleCell = new PdfPCell(new Phrase("Plataforma de Intercambio de Habilidades", roleFont));
        roleCell.setBorder(Rectangle.NO_BORDER);
        roleCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(roleCell);

        document.add(table);
    }

    /**
     * Genera un código de verificación único
     */
    private String generateVerificationCode() {
        return "SS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Envía el certificado por correo electrónico
     */
    private void sendCertificateEmail(Certification certification) throws Exception {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(certification.getLearner().getPerson().getEmail());
            helper.setSubject("¡Felicitaciones! Has obtenido un certificado en " + certification.getSkill().getName());

            String emailBody = buildEmailBody(certification);
            helper.setText(emailBody, true);

            File certificateFile = new File(certification.getPdfUrl());
            if (certificateFile.exists()) {
                helper.addAttachment("certificado.pdf", certificateFile);
            }

            mailSender.send(message);
        } catch (Exception e) {
            System.out.println("Error al enviar el correo del certificado: " + e.getMessage());
        }
    }

    /**
     * Construye el cuerpo del correo electrónico
     */
    private String buildEmailBody(Certification certification) {
        return """
            <html>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f9f9f9; border-radius: 10px;">
                    <h1 style="color: #AAE16B; text-align: center;">¡Felicitaciones!</h1>
                    <p>Estimado/a <strong>%s</strong>,</p>
                    <p>Nos complace informarte que has completado exitosamente el programa de certificación en <strong>%s</strong>.</p>
                    <p>Has acumulado <strong>%d credenciales verificadas</strong>, demostrando tu dedicación y excelencia en esta habilidad.</p>
                    <p>Adjunto encontrarás tu certificado oficial en formato PDF.</p>
                    <p>Código de verificación: <strong>%s</strong></p>
                    <p style="margin-top: 30px;">¡Continúa aprendiendo y compartiendo tus conocimientos en SkillSwap!</p>
                    <p style="text-align: center; margin-top: 40px; color: #888; font-size: 12px;">
                        Este es un correo automático, por favor no respondas a este mensaje.
                    </p>
                </div>
            </body>
            </html>
            """.formatted(
                certification.getLearner().getPerson().getFullName(),
                certification.getSkill().getName(),
                certification.getAccumulatedCredentials(),
                certification.getVerificationCode()
        );
    }
    //#endregion
}