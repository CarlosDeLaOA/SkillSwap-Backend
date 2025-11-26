package com.project.skillswap.logic.entity.Transaction;

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

/**
 * Service for integrating with PayPal payment processing using PayPal REST API.
 */
@Service
public class PayPalService {

    @Value("${paypal.client.id}")
    private String clientId;

    @Value("${paypal.client.secret}")
    private String clientSecret;

    @Value("${paypal.mode:sandbox}")
    private String mode; // sandbox or live

    private PayPalHttpClient payPalClient;

    /**
     * Initializes the PayPal HTTP client based on the configured mode.
     * @return configured PayPal HTTP client
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
     * Creates a PayPal order for coin purchase
     * This method can be called from frontend to initiate payment
     * @param amount the amount in USD
     * @param packageType the package type for description
     * @return the created order ID
     */
    public String createOrder(BigDecimal amount, String packageType) throws IOException {
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.checkoutPaymentIntent("CAPTURE");

        // Application context
        ApplicationContext applicationContext = new ApplicationContext()
                .brandName("SkillSwap")
                .landingPage("BILLING")
                .shippingPreference("NO_SHIPPING");
        orderRequest.applicationContext(applicationContext);

        // Purchase unit
        List<PurchaseUnitRequest> purchaseUnits = new ArrayList<>();
        PurchaseUnitRequest purchaseUnit = new PurchaseUnitRequest()
                .description("SkillSwap SkillCoins - " + packageType)
                .amountWithBreakdown(new AmountWithBreakdown()
                        .currencyCode("USD")
                        .value(amount.toString()));
        purchaseUnits.add(purchaseUnit);
        orderRequest.purchaseUnits(purchaseUnits);

        // Create order request
        OrdersCreateRequest request = new OrdersCreateRequest();
        request.requestBody(orderRequest);

        try {
            HttpResponse<Order> response = getPayPalClient().execute(request);
            Order order = response.result();
            return order.id();
        } catch (IOException e) {
            System.err.println("Error creating PayPal order: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Captures/executes a PayPal payment after user approval
     * @param orderId the PayPal order ID (obtained after user approves payment)
     * @param expectedAmount the expected amount to verify
     * @return true if payment was successful and amount matches, false otherwise
     */
    public boolean executePayment(String orderId, BigDecimal expectedAmount) {
        // MODO TESTING: Acepta orderIds de prueba que empiecen con "TEST-"
        if (orderId != null && orderId.startsWith("TEST-")) {
            System.out.println("⚠️ TESTING MODE: Accepting test orderId: " + orderId);
            System.out.println("   Amount: $" + expectedAmount);
            System.out.println("   Mode: " + mode);
            System.out.println("   This would be a real PayPal payment in production");
            return true; // Simula éxito para testing
        }

        try {
            // MODO REAL: Captura orden real de PayPal
            OrdersCaptureRequest request = new OrdersCaptureRequest(orderId);
            HttpResponse<Order> response = getPayPalClient().execute(request);

            Order order = response.result();

            // Verify order status
            if (!"COMPLETED".equals(order.status())) {
                System.err.println("PayPal order not completed. Status: " + order.status());
                return false;
            }

            // Verify amount
            String capturedAmount = order.purchaseUnits().get(0).amountWithBreakdown().value();
            BigDecimal actualAmount = new BigDecimal(capturedAmount);

            if (actualAmount.compareTo(expectedAmount) != 0) {
                System.err.println("Amount mismatch. Expected: " + expectedAmount + ", Got: " + actualAmount);
                return false;
            }

            System.out.println("✅ PayPal payment successful:");
            System.out.println("   Order ID: " + orderId);
            System.out.println("   Amount: $" + actualAmount);
            System.out.println("   Status: " + order.status());

            return true;

        } catch (IOException e) {
            System.err.println("❌ Error executing PayPal payment: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Gets order details from PayPal
     * @param orderId the PayPal order ID
     * @return the order details or null if error
     */
    public Order getOrderDetails(String orderId) {
        try {
            OrdersGetRequest request = new OrdersGetRequest(orderId);
            HttpResponse<Order> response = getPayPalClient().execute(request);
            return response.result();
        } catch (IOException e) {
            System.err.println("Error getting PayPal order details: " + e.getMessage());
            return null;
        }
    }

    /**
     * Validates PayPal configuration
     * @return true if PayPal is properly configured
     */
    public boolean isConfigured() {
        return clientId != null && !clientId.isEmpty()
                && clientSecret != null && !clientSecret.isEmpty();
    }
}