package com.project.skillswap.logic.entity.videocall;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.project.skillswap.logic.entity.LearningSession.LearningSession;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;

@Service
public class TranscriptionPdfService {

    /**
     * Genera PDF de transcripción
     *
     * @param session Sesión con transcripción
     * @return PDF como array de bytes
     */
    public byte[] generateTranscriptionPdf(LearningSession session) {
        try {
            System.out.println("========================================");
            System.out.println(" GENERANDO PDF DE TRANSCRIPCIÓN");
            System.out.println("   Session ID: " + session.getId());
            System.out.println("========================================");


            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);


            DeviceRgb skillswapPurple = new DeviceRgb(80, 74, 183); // #504AB7
            DeviceRgb skillswapGreen = new DeviceRgb(170, 225, 107); // #AAE16B



            Paragraph header = new Paragraph("SkillSwap")
                    .setFontSize(32)
                    .setBold()
                    .setFontColor(skillswapPurple)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(5);
            document.add(header);

            Paragraph subtitle = new Paragraph("Plataforma de Intercambio de Conocimiento")
                    .setFontSize(10)
                    .setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20);
            document.add(subtitle);


            Paragraph title = new Paragraph("Transcripción de Sesión")
                    .setFontSize(24)
                    .setBold()
                    .setFontColor(skillswapGreen)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(30);
            document.add(title);


            Table infoTable = new Table(UnitValue.createPercentArray(new float[]{30, 70}))
                    .useAllAvailableWidth()
                    .setMarginBottom(20);

            // Calcular estadísticas
            String fullText = session.getFullText();
            int wordCount = fullText != null ? fullText.split("\\s+").length : 0;
            int durationSeconds = session.getDurationSeconds() != null ? session.getDurationSeconds() : 0;
            int durationMinutes = durationSeconds / 60;
            int remainingSeconds = durationSeconds % 60;

            // Datos de la sesión
            addInfoRow(infoTable, "Sesión ID:", "#" + session.getId(), skillswapPurple);
            addInfoRow(infoTable, "Título:", session.getTitle(), skillswapPurple);

            if (session.getInstructor() != null && session.getInstructor().getPerson() != null) {
                addInfoRow(infoTable, "Instructor:",
                        session.getInstructor().getPerson().getFullName(), skillswapPurple);
            }

            if (session.getSkill() != null) {
                addInfoRow(infoTable, "Habilidad:", session.getSkill().getName(), skillswapPurple);

                if (session.getSkill().getKnowledgeArea() != null) {
                    addInfoRow(infoTable, "Área de conocimiento:",
                            session.getSkill().getKnowledgeArea().getName(), skillswapPurple);
                }
            }

            addInfoRow(infoTable, "Palabras:", String.valueOf(wordCount), skillswapPurple);
            addInfoRow(infoTable, "Duración:",
                    durationMinutes + " minutos " + remainingSeconds + " segundos", skillswapPurple);
            addInfoRow(infoTable, "Fecha de generación:",
                    new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new java.util.Date()), skillswapPurple);

            document.add(infoTable);


            Paragraph transcriptionTitle = new Paragraph("Contenido de la Transcripción")
                    .setFontSize(16)
                    .setBold()
                    .setFontColor(skillswapPurple)
                    .setMarginTop(20)
                    .setMarginBottom(10);
            document.add(transcriptionTitle);


            Table transcriptionTable = new Table(UnitValue.createPercentArray(new float[]{100}))
                    .useAllAvailableWidth();


            Cell transcriptionCell = new Cell()
                    .add(new Paragraph(fullText)
                            .setFontSize(11)
                            .setTextAlignment(TextAlignment.JUSTIFIED))
                    .setPadding(8);

            transcriptionTable.addCell(transcriptionCell);
            document.add(transcriptionTable);

            // ========================================
            // FOOTER
            // ========================================
            Paragraph footer = new Paragraph("© 2025 SkillSwap. Todos los derechos reservados.")
                    .setFontSize(8)
                    .setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(20);
            document.add(footer);


            document.close();

            byte[] pdfBytes = baos.toByteArray();

            System.out.println("========================================");
            System.out.println(" PDF GENERADO EXITOSAMENTE");
            System.out.println("   Tamaño: " + formatFileSize(pdfBytes.length));
            System.out.println("========================================");

            return pdfBytes;

        } catch (Exception e) {
            System.err.println("========================================");
            System.err.println(" ERROR GENERANDO PDF");
            System.err.println("   Error: " + e.getMessage());
            System.err.println("========================================");
            e.printStackTrace();
            throw new RuntimeException("Error al generar PDF: " + e.getMessage());
        }
    }


    private void addInfoRow(Table table, String label, String value, DeviceRgb labelColor) {
        table.addCell(new Paragraph(label)
                .setBold()
                .setFontColor(labelColor)
                .setFontSize(10));

        table.addCell(new Paragraph(value)
                .setFontSize(10));
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
    }
}