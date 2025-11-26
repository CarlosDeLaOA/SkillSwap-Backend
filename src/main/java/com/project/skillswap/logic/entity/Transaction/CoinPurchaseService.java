package com.project.skillswap.logic.entity.Transaction;

import com.project.skillswap.logic.entity.Learner.Learner;
import com.project.skillswap.logic.entity.Learner.LearnerRepository;
import com.project.skillswap.logic.entity.Person.Person;
import com.project.skillswap.logic.entity.Person.PersonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class CoinPurchaseService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private LearnerRepository learnerRepository;

    @Autowired
    private PayPalService payPalService;

    @Autowired
    private TransactionEmailService emailService;

    // Maximum coins that can be purchased per day per user
    private static final BigDecimal MAX_COINS_PER_DAY = new BigDecimal("500");

    /**
     * Processes a SkillCoin purchase transaction
     * @param personId the ID of the person making the purchase
     * @param packageType the coin package being purchased
     * @param paypalOrderId the PayPal order ID (after user approves payment)
     * @return the completed transaction
     * @throws IllegalArgumentException if validations fail
     * @throws IllegalStateException if user is not a learner
     */
    @Transactional
    public Transaction purchaseCoins(Long personId, CoinPackageType packageType, String paypalOrderId) {

        // 1. Get and validate person
        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // 2. Verify user is a learner
        Learner learner = person.getLearner();
        if (learner == null) {
            throw new IllegalStateException("Only learners can purchase SkillCoins");
        }

        // 3. Check daily limit
        BigDecimal coinsToday = transactionRepository.sumCoinsPurchasedToday(personId);
        BigDecimal newTotal = coinsToday.add(packageType.getCoins());

        if (newTotal.compareTo(MAX_COINS_PER_DAY) > 0) {
            throw new IllegalArgumentException(
                    String.format("Daily purchase limit exceeded. You can only purchase %s more coins today.",
                            MAX_COINS_PER_DAY.subtract(coinsToday))
            );
        }

        // 4. Check for duplicate transaction (idempotency)
        Optional<Transaction> existingTransaction = transactionRepository.findByPaypalReference(paypalOrderId);
        if (existingTransaction.isPresent()) {
            return existingTransaction.get(); // Return existing transaction
        }

        // 5. Process payment with PayPal - Capture the order
        boolean paymentSuccessful = payPalService.executePayment(paypalOrderId, packageType.getPriceUsd());

        if (!paymentSuccessful) {
            // Create failed transaction record
            Transaction failedTransaction = createTransaction(
                    person, packageType, paypalOrderId, TransactionStatus.FAILED
            );
            failedTransaction = transactionRepository.save(failedTransaction);

            // Send failed purchase notification email
            try {
                emailService.sendPurchaseFailedNotification(
                        person.getEmail(),
                        person.getFullName(),
                        packageType.name(),
                        "Payment processing failed. Please verify your PayPal account and try again."
                );
            } catch (Exception e) {
                System.err.println("Warning: Failed to send failure notification email: " + e.getMessage());
            }

            return failedTransaction;
        }

        // 6. Create successful transaction
        Transaction transaction = createTransaction(
                person, packageType, paypalOrderId, TransactionStatus.COMPLETED
        );
        transaction = transactionRepository.save(transaction);

        // 7. Update learner balance
        BigDecimal newBalance = learner.getSkillcoinsBalance().add(packageType.getCoins());
        learner.setSkillcoinsBalance(newBalance);
        learnerRepository.save(learner);

        // 8. Send confirmation email
        try {
            emailService.sendPurchaseConfirmation(
                    person.getEmail(),
                    person.getFullName(),
                    transaction.getId(),
                    packageType.name(),
                    packageType.getCoins(),
                    packageType.getPriceUsd(),
                    newBalance,
                    paypalOrderId
            );
        } catch (Exception e) {
            // Log error but don't fail the transaction
            System.err.println("Warning: Failed to send purchase confirmation email: " + e.getMessage());
        }

        return transaction;
    }

    /**
     * Creates a PayPal order for coin purchase
     * @param packageType the package type to purchase
     * @return the PayPal order ID
     * @throws RuntimeException if order creation fails
     */
    public String createPayPalOrder(CoinPackageType packageType) {
        try {
            return payPalService.createOrder(packageType.getPriceUsd(), packageType.name());
        } catch (Exception e) {
            throw new RuntimeException("Failed to create PayPal order: " + e.getMessage(), e);
        }
    }

    /**
     * Gets the current balance of a learner
     * @param personId the person ID
     * @return the current SkillCoins balance
     * @throws IllegalStateException if user is not a learner
     */
    public BigDecimal getBalance(Long personId) {
        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Learner learner = person.getLearner();
        if (learner == null) {
            throw new IllegalStateException("User is not a learner");
        }

        return learner.getSkillcoinsBalance();
    }

    /**
     * Gets the purchase history for a user
     * @param personId the person ID
     * @return list of purchase transactions
     */
    public List<Transaction> getUserPurchases(Long personId) {
        return transactionRepository.findPurchasesByPersonId(personId);
    }

    /**
     * Gets all available coin packages
     * @return array of available packages
     */
    public CoinPackageType[] getAvailablePackages() {
        return CoinPackageType.values();
    }

    /**
     * Helper method to create a transaction entity
     */
    private Transaction createTransaction(
            Person person,
            CoinPackageType packageType,
            String paypalReference,
            TransactionStatus status) {

        Transaction transaction = new Transaction();
        transaction.setPerson(person);
        transaction.setType(TransactionType.PURCHASE);
        transaction.setSkillcoinsAmount(packageType.getCoins());
        transaction.setUsdAmount(packageType.getPriceUsd());
        transaction.setPaymentMethod(PaymentMethod.PAYPAL);
        transaction.setStatus(status);
        transaction.setPaypalReference(paypalReference);

        return transaction;
    }
}