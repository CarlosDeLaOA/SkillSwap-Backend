
package com.project.skillswap.logic.entity.videocall;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.List;
import com.itextpdf.layout.element.ListItem;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.project.skillswap.logic.entity.LearningSession.LearningSession;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;

/**
 * Servicio encargado de generar un PDF con el resumen de una sesión educativa.
 * Este servicio NO genera transcripciones completas (eso lo maneja TranscriptionPdfService).
 */
@Service
public class SessionSummaryPdfService {

    //#region Public API

    /**
     * Genera el PDF de resumen de sesión.
     *
     * @param session Sesión a representar en PDF
     * @param summary Resumen generado por IA
     * @return PDF como arreglo de bytes listo para descarga
     */
    public byte[] generateSummaryPdf(LearningSession session, String summary) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            // Colores corporativos SkillSwap
            DeviceRgb skillswapPurple = new DeviceRgb(80, 74, 183);
            DeviceRgb skillswapGreen = new DeviceRgb(170, 225, 107);

            addHeader(document, skillswapPurple);
            addSessionTitle(document, session, skillswapGreen);
            addSessionInfo(document, session, skillswapPurple);
            addSummarySection(document, summary, skillswapGreen);
            addFooter(document);

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error al generar PDF de resumen: " + e.getMessage());
        }
    }

    //#endregion



    //#region Header & Footer Sections

    /**
     * Agrega el encabezado visual del documento.
     */
    private void addHeader(Document document, DeviceRgb primaryColor) {
        Paragraph header = new Paragraph("SkillSwap")
                .setFontSize(28)
                .setBold()
                .setFontColor(primaryColor)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(5);

        document.add(header);

        Paragraph subtitle = new Paragraph("Resumen de Sesión")
                .setFontSize(10)
                .setFontColor(ColorConstants.GRAY)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);

        document.add(subtitle);
    }

    /**
     * Agrega el footer informativo.
     */
    private void addFooter(Document document) {
        Paragraph footer = new Paragraph("© 2025 SkillSwap. Todos los derechos reservados.")
                .setFontSize(8)
                .setFontColor(ColorConstants.GRAY)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(30);

        document.add(footer);

        Paragraph techFooter = new Paragraph("Resumen generado con inteligencia artificial")
                .setFontSize(7)
                .setFontColor(ColorConstants.LIGHT_GRAY)
                .setTextAlignment(TextAlignment.CENTER);

        document.add(techFooter);
    }

    //#endregion



    //#region Session Info Section

    /**
     * Agrega el título de la sesión en estilo destacado.
     */
    private void addSessionTitle(Document document, LearningSession session, DeviceRgb accentColor) {
        Paragraph sessionTitle = new Paragraph(session.getTitle())
                .setFontSize(18)
                .setBold()
                .setFontColor(accentColor)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);

        document.add(sessionTitle);
    }

    /**
     * Sección que incluye la tabla resumen de la sesión (instructor, habilidad, fecha, etc.).
     */
    private void addSessionInfo(Document document, LearningSession session, DeviceRgb labelColor) {
        Table infoTable = new Table(UnitValue.createPercentArray(new float[]{25, 75}))
                .useAllAvailableWidth()
                .setMarginBottom(15);

        addInfoRow(infoTable, "Sesión ID:", "#" + session.getId(), labelColor);

        if (session.getInstructor() != null && session.getInstructor().getPerson() != null) {
            addInfoRow(infoTable, "Instructor:",
                    session.getInstructor().getPerson().getFullName(), labelColor);
        }

        if (session.getSkill() != null) {
            addInfoRow(infoTable, "Habilidad:", session.getSkill().getName(), labelColor);
        }

        int durationSeconds = session.getDurationSeconds() != null ? session.getDurationSeconds() : 0;
        int durationMinutes = durationSeconds / 60;

        addInfoRow(infoTable, "Duración:",
                durationMinutes + " minutos", labelColor);

        addInfoRow(infoTable, "Fecha de sesión:",
                session.getScheduledDatetime() != null
                        ? new SimpleDateFormat("dd/MM/yyyy").format(session.getScheduledDatetime())
                        : "N/A",
                labelColor);

        document.add(infoTable);
    }

    /**
     * Método auxiliar para insertar una fila en la tabla de datos.
     */
    private void addInfoRow(Table table, String label, String value, DeviceRgb labelColor) {
        table.addCell(new Paragraph(label)
                .setBold()
                .setFontColor(labelColor)
                .setFontSize(9));

        table.addCell(new Paragraph(value)
                .setFontSize(9));
    }

    //#endregion



    //#region Summary Content Section

    /**
     * Inserta el contenido del resumen en el PDF,
     * detectando encabezados y viñetas automáticamente.
     */
    private void addSummarySection(Document document, String summary, DeviceRgb accentColor) {

        Paragraph summaryTitle = new Paragraph("Resumen de la Sesión")
                .setFontSize(14)
                .setBold()
                .setFontColor(accentColor)
                .setMarginTop(15)
                .setMarginBottom(10);

        document.add(summaryTitle);

        parseSummaryContent(document, summary, accentColor);
    }

    /**
     * Procesa el texto del resumen para mostrar encabezados, listas y párrafos correctamente.
     */
    private void parseSummaryContent(Document document, String summary, DeviceRgb accentColor) {

        String[] lines = summary.split("\n");
        List currentList = null;

        for (String rawLine : lines) {

            String line = rawLine.trim();
            if (line.isEmpty()) {
                if (currentList != null) {
                    document.add(currentList);
                    currentList = null;
                }
                continue;
            }

            // Encabezados con formato **Título**
            if (line.startsWith("**") && line.endsWith("**")) {

                if (currentList != null) {
                    document.add(currentList);
                    currentList = null;
                }

                String headerText = line.replace("**", "").trim();

                Paragraph header = new Paragraph(headerText)
                        .setFontSize(12)
                        .setBold()
                        .setFontColor(accentColor)
                        .setMarginTop(10)
                        .setMarginBottom(5);

                document.add(header);
                continue;
            }

            // Viñetas
            if (line.startsWith("•") || line.startsWith("-") || line.startsWith("*")) {

                String bullet = line.substring(1).trim();

                if (currentList == null) {
                    currentList = new List().setSymbolIndent(10).setMarginLeft(20);
                }

                ListItem item = new ListItem(bullet);
                item.setFontSize(10);
                currentList.add(item);
                continue;
            }

            // Texto suelto
            if (currentList != null) {
                document.add(currentList);
                currentList = null;
            }

            Paragraph para = new Paragraph(line)
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.JUSTIFIED)
                    .setMarginBottom(5);

            document.add(para);
        }

        if (currentList != null) {
            document.add(currentList);
        }
    }

    //#endregion



    //#region Helpers

    /**
     * Formatea un tamaño de archivo en B, KB o MB.
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
    }

    //#endregion
}
