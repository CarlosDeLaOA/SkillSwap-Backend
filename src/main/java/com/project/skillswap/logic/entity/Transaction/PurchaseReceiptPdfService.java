
package com.project.skillswap.logic.entity.Transaction;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.project.skillswap.logic.entity.Person.Person;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Servicio encargado de generar comprobantes PDF para compras de SkillCoins.
 * Genera documentos profesionales con información completa de la transacción,
 * datos del cliente y detalles del pago procesado.
 *
 * @author Equipo de Desarrollo SkillSwap
 * @version 1.0
 */
@Service
public class PurchaseReceiptPdfService {
    private static final Logger logger = LoggerFactory.getLogger(PurchaseReceiptPdfService.class);

    private static final DeviceRgb SKILLSWAP_PURPLE = new DeviceRgb(80, 74, 183);
    private static final DeviceRgb SKILLSWAP_GREEN = new DeviceRgb(170, 225, 107);

    /**
     * Genera un PDF con el comprobante de compra de SkillCoins.
     * El documento incluye encabezado corporativo, información del cliente,
     * detalles de la transacción y pie de página informativo.
     *
     * @param transaction transacción completada
     * @param person persona que realizó la compra
     * @param packageType tipo de paquete comprado
     * @param newBalance nuevo balance después de la compra
     * @return PDF como arreglo de bytes listo para descarga
     * @throws RuntimeException si ocurre un error durante la generación del PDF
     */
    public byte[] generatePurchaseReceipt(
            Transaction transaction,
            Person person,
            CoinPackageType packageType,
            BigDecimal newBalance) {

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            addHeader(document);
            addReceiptTitle(document);
            addCustomerInfo(document, person);
            addTransactionDetails(document, transaction, packageType, newBalance);
            addPaymentInfo(document, transaction);
            addFooter(document);

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error al generar PDF de comprobante: " + e.getMessage(), e);
        }
    }

    /**
     * Agrega el encabezado corporativo del documento.
     *
     * @param document documento PDF en construcción
     */
    private void addHeader(Document document) {
        Paragraph header = new Paragraph("SkillSwap")
                .setFontSize(28)
                .setBold()
                .setFontColor(SKILLSWAP_PURPLE)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(5);
        document.add(header);

        Paragraph subtitle = new Paragraph("Plataforma de Intercambio de Habilidades")
                .setFontSize(10)
                .setFontColor(ColorConstants.GRAY)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(30);
        document.add(subtitle);
    }

    /**
     * Agrega el título del comprobante.
     *
     * @param document documento PDF en construcción
     */
    private void addReceiptTitle(Document document) {
        Paragraph title = new Paragraph("COMPROBANTE DE COMPRA")
                .setFontSize(18)
                .setBold()
                .setFontColor(SKILLSWAP_GREEN)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(title);

        Paragraph date = new Paragraph("Fecha de emisión: " +
                new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date()))
                .setFontSize(9)
                .setFontColor(ColorConstants.GRAY)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(date);
    }

    /**
     * Agrega la información del cliente.
     *
     * @param document documento PDF en construcción
     * @param person datos de la persona que realizó la compra
     */
    private void addCustomerInfo(Document document, Person person) {
        Paragraph sectionTitle = new Paragraph("INFORMACIÓN DEL CLIENTE")
                .setFontSize(12)
                .setBold()
                .setFontColor(SKILLSWAP_PURPLE)
                .setMarginBottom(10);
        document.add(sectionTitle);

        Table customerTable = new Table(UnitValue.createPercentArray(new float[]{30, 70}))
                .useAllAvailableWidth()
                .setMarginBottom(20);

        addInfoRow(customerTable, "Nombre:", person.getFullName());
        addInfoRow(customerTable, "Email:", person.getEmail());

        if (person.getRegistrationDate() != null) {
            addInfoRow(customerTable, "Cliente desde:",
                    new SimpleDateFormat("dd/MM/yyyy").format(person.getRegistrationDate()));
        }

        document.add(customerTable);
    }

    /**
     * Agrega los detalles de la transacción.
     *
     * @param document documento PDF en construcción
     * @param transaction datos de la transacción
     * @param packageType tipo de paquete comprado
     * @param newBalance balance actualizado después de la compra
     */
    private void addTransactionDetails(
            Document document,
            Transaction transaction,
            CoinPackageType packageType,
            BigDecimal newBalance) {

        Paragraph sectionTitle = new Paragraph("DETALLES DE LA COMPRA")
                .setFontSize(12)
                .setBold()
                .setFontColor(SKILLSWAP_PURPLE)
                .setMarginBottom(10);
        document.add(sectionTitle);

        Table detailsTable = new Table(UnitValue.createPercentArray(new float[]{30, 70}))
                .useAllAvailableWidth()
                .setMarginBottom(20);

        addInfoRow(detailsTable, "ID de Transacción:", "#" + transaction.getId());
        addInfoRow(detailsTable, "Paquete:", packageType.name());
        addInfoRow(detailsTable, "SkillCoins Adquiridas:", transaction.getSkillcoinsAmount().toString() + " coins");
        addInfoRow(detailsTable, "Monto Pagado:", "$" + transaction.getUsdAmount() + " USD");
        addInfoRow(detailsTable, "Estado:", transaction.getStatus().toString());

        if (transaction.getTransactionDate() != null) {
            addInfoRow(detailsTable, "Fecha de Compra:",
                    new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(transaction.getTransactionDate()));
        }

        document.add(detailsTable);

        // Balance actualizado
        Paragraph balanceInfo = new Paragraph("Balance actual: " + newBalance + " SkillCoins")
                .setFontSize(11)
                .setBold()
                .setFontColor(SKILLSWAP_GREEN)
                .setTextAlignment(TextAlignment.RIGHT)
                .setMarginBottom(20);
        document.add(balanceInfo);
    }

    /**
     * Agrega la información del pago procesado.
     *
     * @param document documento PDF en construcción
     * @param transaction datos de la transacción con información de PayPal
     */
    private void addPaymentInfo(Document document, Transaction transaction) {
        Paragraph sectionTitle = new Paragraph("INFORMACIÓN DE PAGO")
                .setFontSize(12)
                .setBold()
                .setFontColor(SKILLSWAP_PURPLE)
                .setMarginBottom(10);
        document.add(sectionTitle);

        Table paymentTable = new Table(UnitValue.createPercentArray(new float[]{30, 70}))
                .useAllAvailableWidth()
                .setMarginBottom(30);

        addInfoRow(paymentTable, "Método de Pago:", transaction.getPaymentMethod().toString());

        if (transaction.getPaypalReference() != null) {
            addInfoRow(paymentTable, "Referencia PayPal:", transaction.getPaypalReference());
        }

        document.add(paymentTable);
    }

    /**
     * Agrega el pie de página con información legal y de contacto.
     *
     * @param document documento PDF en construcción
     */
    private void addFooter(Document document) {
        Paragraph separator = new Paragraph("─────────────────────────────────────────")
                .setFontSize(8)
                .setFontColor(ColorConstants.LIGHT_GRAY)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(20)
                .setMarginBottom(10);
        document.add(separator);

        Paragraph footer = new Paragraph("Gracias por confiar en SkillSwap")
                .setFontSize(10)
                .setBold()
                .setFontColor(SKILLSWAP_PURPLE)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(5);
        document.add(footer);

        Paragraph contact = new Paragraph("Para soporte técnico, contacta a: soporte@skillswap.com")
                .setFontSize(8)
                .setFontColor(ColorConstants.GRAY)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(15);
        document.add(contact);

        Paragraph copyright = new Paragraph("© 2025 SkillSwap. Todos los derechos reservados.")
                .setFontSize(8)
                .setFontColor(ColorConstants.GRAY)
                .setTextAlignment(TextAlignment.CENTER);
        document.add(copyright);

        Paragraph legal = new Paragraph("Este documento es un comprobante de compra válido y puede ser usado para fines contables.")
                .setFontSize(7)
                .setFontColor(ColorConstants.LIGHT_GRAY)
                .setTextAlignment(TextAlignment.CENTER)
                .setItalic();
        document.add(legal);
    }

    /**
     * Método auxiliar para agregar una fila a una tabla de información.
     *
     * @param table tabla a la que se agregará la fila
     * @param label etiqueta descriptiva
     * @param value valor correspondiente
     */
    private void addInfoRow(Table table, String label, String value) {
        table.addCell(new Paragraph(label)
                .setBold()
                .setFontColor(SKILLSWAP_PURPLE)
                .setFontSize(9));

        table.addCell(new Paragraph(value != null ? value : "N/A")
                .setFontSize(9));
    }
}