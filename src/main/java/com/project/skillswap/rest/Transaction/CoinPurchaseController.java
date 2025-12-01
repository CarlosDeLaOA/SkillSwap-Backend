
package com.project.skillswap.rest.Transaction;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
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

/**
 * Controlador REST para gestionar compras de SkillCoins.
 * Expone endpoints para comprar monedas, consultar balance, obtener paquetes disponibles
 * y revisar el historial de transacciones.
 *
 * @author Equipo de Desarrollo SkillSwap
 * @version 1.0
 */
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
     * Compra SkillCoins usando PayPal.
     *
     * @param authHeader header de autorización con el token JWT
     * @param request cuerpo de la solicitud con packageType y paypalOrderId
     * @return ResponseEntity con los detalles de la transacción o mensaje de error
     */
    @PostMapping("/purchase")
    public ResponseEntity<?> purchaseCoins(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> request) {

        try {
            Long userId = getAuthenticatedUserId(authHeader);

            String packageTypeStr = request.get("packageType");
            String paypalOrderId = request.get("paypalOrderId");

            if (packageTypeStr == null || paypalOrderId == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Missing required fields: packageType and paypalOrderId"));
            }

            CoinPackageType packageType;
            try {
                packageType = CoinPackageType.valueOf(packageTypeStr);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid package type: " + packageTypeStr));
            }

            Transaction transaction = coinPurchaseService.purchaseCoins(userId, packageType, paypalOrderId);

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
                    .body(Map.of(
                            "error", "An error occurred processing your purchase",
                            "details", e.getMessage() != null ? e.getMessage() : "Unknown error"
                    ));
        }
    }

    /**
     * Obtiene el balance actual de SkillCoins del usuario.
     *
     * @param authHeader header de autorización con el token JWT
     * @return ResponseEntity con el balance actual o mensaje de error
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
     * Obtiene todos los paquetes de monedas disponibles para compra.
     *
     * @return ResponseEntity con la lista de paquetes disponibles
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
     * Crea una orden de PayPal para compra de monedas.
     * Este endpoint se llama antes de que el usuario apruebe el pago en PayPal.
     *
     * @param authHeader header de autorización con el token JWT
     * @param request cuerpo de la solicitud con packageType
     * @return ResponseEntity con el orderId de PayPal o mensaje de error
     */
    @PostMapping("/create-order")
    public ResponseEntity<?> createPayPalOrder(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> request) {
        try {
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
     * Obtiene el historial de compras del usuario actual.
     *
     * @param authHeader header de autorización con el token JWT
     * @return ResponseEntity con la lista de transacciones o mensaje de error
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
     * Método auxiliar para obtener el ID del usuario autenticado desde el token JWT.
     * Extrae el email del token, busca la persona en la base de datos y retorna su ID.
     *
     * @param authHeader header de autorización que contiene "Bearer {token}"
     * @return ID numérico del usuario
     * @throws IllegalStateException si el header es inválido o el usuario no se encuentra
     */
    private Long getAuthenticatedUserId(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalStateException("Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);
        String email = jwtService.extractUsername(token);

        if (email == null || email.isEmpty()) {
            throw new IllegalStateException("Invalid token: email not found");
        }

        Person person = personRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found: " + email));

        return person.getId();
    }
}