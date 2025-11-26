package com.project.skillswap.rest.Transaction;

import com.project.skillswap.logic.entity.Transaction.CoinPurchaseService;
import com.project.skillswap.logic.entity.Transaction.CoinPackageType;
import com.project.skillswap.logic.entity.Transaction.Transaction;
import com.project.skillswap.logic.entity.Person.Person;
import com.project.skillswap.logic.entity.Person.PersonRepository;
import com.project.skillswap.logic.entity.auth.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/coins")
@CrossOrigin(origins = "*")
public class CoinPurchaseController {

    @Autowired
    private CoinPurchaseService coinPurchaseService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PersonRepository personRepository;

    /**
     * Purchase SkillCoins using PayPal
     * POST /api/coins/purchase
     * Header: Authorization: Bearer <token>
     * Body: { "packageType": "BASIC", "paypalOrderId": "PAYPAL-ORDER-123" }
     */
    @PostMapping("/purchase")
    public ResponseEntity<?> purchaseCoins(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> request) {
        try {
            // Get authenticated user ID from JWT token
            Long userId = getAuthenticatedUserId(authHeader);

            // Parse request
            String packageTypeStr = request.get("packageType");
            String paypalOrderId = request.get("paypalOrderId");

            if (packageTypeStr == null || paypalOrderId == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Missing required fields: packageType and paypalOrderId"));
            }

            CoinPackageType packageType = CoinPackageType.valueOf(packageTypeStr);

            // Process purchase
            Transaction transaction = coinPurchaseService.purchaseCoins(userId, packageType, paypalOrderId);

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("success", transaction.getStatus().toString().equals("COMPLETED"));
            response.put("transactionId", transaction.getId());
            response.put("paypalReference", transaction.getPaypalReference());
            response.put("coinsAdded", transaction.getSkillcoinsAmount());
            response.put("newBalance", coinPurchaseService.getBalance(userId));
            response.put("status", transaction.getStatus().toString());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An error occurred processing your purchase"));
        }
    }

    /**
     * Get current SkillCoin balance
     * GET /api/coins/balance
     * Header: Authorization: Bearer <token>
     */
    @GetMapping("/balance")
    public ResponseEntity<?> getBalance(@RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = getAuthenticatedUserId(authHeader);
            BigDecimal balance = coinPurchaseService.getBalance(userId);

            return ResponseEntity.ok(Map.of("balance", balance));

        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An error occurred retrieving balance"));
        }
    }

    /**
     * Get all available coin packages
     * GET /api/coins/packages
     */
    @GetMapping("/packages")
    public ResponseEntity<?> getAvailablePackages() {
        try {
            CoinPackageType[] packages = coinPurchaseService.getAvailablePackages();

            List<Map<String, Object>> packageList = new ArrayList<>();
            for (CoinPackageType pkg : packages) {
                Map<String, Object> packageInfo = new HashMap<>();
                packageInfo.put("type", pkg.name());
                packageInfo.put("coins", pkg.getCoins());
                packageInfo.put("priceUsd", pkg.getPriceUsd());
                packageList.add(packageInfo);
            }

            return ResponseEntity.ok(packageList);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An error occurred retrieving packages"));
        }
    }

    /**
     * Create a PayPal order for coin purchase (optional endpoint)
     * POST /api/coins/create-order
     * Header: Authorization: Bearer <token>
     * Body: { "packageType": "BASIC" }
     * Returns: { "orderId": "PAYPAL-ORDER-123" }
     */
    @PostMapping("/create-order")
    public ResponseEntity<?> createPayPalOrder(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> request) {
        try {
            // Validate user is authenticated
            getAuthenticatedUserId(authHeader);

            String packageTypeStr = request.get("packageType");
            if (packageTypeStr == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Missing required field: packageType"));
            }

            CoinPackageType packageType = CoinPackageType.valueOf(packageTypeStr);
            String orderId = coinPurchaseService.createPayPalOrder(packageType);

            return ResponseEntity.ok(Map.of("orderId", orderId));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An error occurred creating PayPal order"));
        }
    }

    /**
     * Get purchase history for current user
     * GET /api/coins/transactions
     * Header: Authorization: Bearer <token>
     */
    @GetMapping("/transactions")
    public ResponseEntity<?> getTransactions(@RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = getAuthenticatedUserId(authHeader);
            List<Transaction> transactions = coinPurchaseService.getUserPurchases(userId);

            return ResponseEntity.ok(transactions);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An error occurred retrieving transactions"));
        }
    }

    /**
     * Helper method to get authenticated user ID from JWT token
     * @param authHeader the Authorization header containing "Bearer {token}"
     * @return the user's numeric ID
     */
    private Long getAuthenticatedUserId(String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new IllegalStateException("Missing or invalid Authorization header");
            }

            // Extract token from "Bearer {token}"
            String token = authHeader.substring(7);

            // Extract username (email) from token using extractUsername()
            // This uses Claims.getSubject() which is always present in JWT
            String email = jwtService.extractUsername(token);

            if (email == null || email.isEmpty()) {
                throw new IllegalStateException("Invalid token: email not found");
            }

            // Find person by email and get their numeric ID
            Person person = personRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalStateException("User not found: " + email));

            return person.getId();

        } catch (Exception e) {
            System.err.println("Error extracting user ID from token: " + e.getMessage());
            e.printStackTrace();
            throw new IllegalStateException("Authentication error: " + e.getMessage());
        }
    }
}