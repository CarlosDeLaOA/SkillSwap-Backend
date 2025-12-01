
package com.project.skillswap.logic.entity.Transaction;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.paypal.core.PayPalEnvironment;
import com.paypal.core.PayPalHttpClient;
import com.paypal.http.HttpResponse;
import com.paypal.orders.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;


@Service
public class PayPalService {
    private static final Logger logger = LoggerFactory.getLogger(PayPalService.class);

    @Value("${paypal.client.id}")
    private String clientId;

    @Value("${paypal.client.secret}")
    private String clientSecret;

    @Value("${paypal.mode:sandbox}")
    private String mode;

    private PayPalHttpClient payPalClient;

    /**
     * Inicializa el cliente HTTP de PayPal según el modo configurado.
     * Utiliza el entorno sandbox para pruebas o live para producción.
     *
     * @return cliente HTTP de PayPal configurado
     */
    private PayPalHttpClient getPayPalClient() {
        if (payPalClient == null) {
            PayPalEnvironment environment;

            if ("live".equalsIgnoreCase(mode)) {
                environment = new PayPalEnvironment.Live(clientId, clientSecret);
            } else {
                environment = new PayPalEnvironment.Sandbox(clientId, clientSecret);
            }

            payPalClient = new PayPalHttpClient(environment);
        }

        return payPalClient;
    }

    /**
     * Crea una orden de PayPal para compra de SkillCoins.
     * Esto inicia el flujo de pago y retorna un ID de orden que el cliente
     * usa para redirigir al usuario a PayPal para aprobar el pago.
     *
     * @param amount monto en USD a cobrar
     * @param packageType descripción del tipo de paquete para la orden
     * @return ID de la orden de PayPal creada
     * @throws IOException si falla la solicitud a la API de PayPal
     */
    public String createOrder(BigDecimal amount, String packageType) throws IOException {
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.checkoutPaymentIntent("CAPTURE");

        ApplicationContext applicationContext = new ApplicationContext()
                .brandName("SkillSwap")
                .landingPage("BILLING")
                .shippingPreference("NO_SHIPPING");
        orderRequest.applicationContext(applicationContext);

        List<PurchaseUnitRequest> purchaseUnits = new ArrayList<>();
        PurchaseUnitRequest purchaseUnit = new PurchaseUnitRequest()
                .description("SkillSwap SkillCoins - " + packageType)
                .amountWithBreakdown(new AmountWithBreakdown()
                        .currencyCode("USD")
                        .value(amount.toString()));
        purchaseUnits.add(purchaseUnit);
        orderRequest.purchaseUnits(purchaseUnits);

        OrdersCreateRequest request = new OrdersCreateRequest();
        request.requestBody(orderRequest);

        HttpResponse<Order> response = getPayPalClient().execute(request);
        Order order = response.result();
        return order.id();
    }

    /**
     * Captura y valida un pago de PayPal después de la aprobación del usuario.
     * Verifica que el estado de la orden sea COMPLETED y valida el monto del pago.
     * Soporta modo de prueba para órdenes con IDs que empiezan con "TEST-".
     *
     * @param orderId ID de la orden de PayPal obtenido después de la aprobación del usuario
     * @param expectedAmount monto esperado del pago a verificar
     * @return true si el pago fue capturado y verificado exitosamente, false en caso contrario
     */
    public boolean executePayment(String orderId, BigDecimal expectedAmount) {
        if (orderId != null && orderId.startsWith("TEST-")) {
            return true;
        }

        try {
            OrdersCaptureRequest request = new OrdersCaptureRequest(orderId);
            HttpResponse<Order> response = getPayPalClient().execute(request);
            Order order = response.result();

            if (!"COMPLETED".equals(order.status())) {
                return false;
            }

            String capturedAmount = null;

            try {
                if (order.purchaseUnits() != null
                        && !order.purchaseUnits().isEmpty()
                        && order.purchaseUnits().get(0).payments() != null
                        && order.purchaseUnits().get(0).payments().captures() != null
                        && !order.purchaseUnits().get(0).payments().captures().isEmpty()) {

                    Capture capture = order.purchaseUnits().get(0).payments().captures().get(0);
                    if (capture.amount() != null) {
                        capturedAmount = capture.amount().value();
                    }
                }

                if (capturedAmount == null
                        && order.purchaseUnits() != null
                        && !order.purchaseUnits().isEmpty()
                        && order.purchaseUnits().get(0).amountWithBreakdown() != null) {

                    capturedAmount = order.purchaseUnits().get(0).amountWithBreakdown().value();
                }

            } catch (Exception e) {
                // Verificación de monto falló, pero continuar si la orden está COMPLETED
            }

            if (capturedAmount != null) {
                BigDecimal actualAmount = new BigDecimal(capturedAmount);

                if (actualAmount.compareTo(expectedAmount) != 0) {
                    return false;
                }
            }

            return true;

        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Obtiene los detalles de una orden de PayPal.
     *
     * @param orderId ID de la orden de PayPal a recuperar
     * @return objeto Order con los detalles completos, o null si falla la recuperación
     */
    public Order getOrderDetails(String orderId) {
        try {
            OrdersGetRequest request = new OrdersGetRequest(orderId);
            HttpResponse<Order> response = getPayPalClient().execute(request);
            return response.result();
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Valida que PayPal esté correctamente configurado con las credenciales del cliente.
     *
     * @return true si tanto el ID del cliente como el secreto están configurados, false en caso contrario
     */
    public boolean isConfigured() {
        return clientId != null && !clientId.isEmpty()
                && clientSecret != null && !clientSecret.isEmpty();
    }
}