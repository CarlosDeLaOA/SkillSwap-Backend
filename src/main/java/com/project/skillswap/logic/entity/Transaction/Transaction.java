
package com.project.skillswap.logic.entity.Transaction;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.project.skillswap.logic.entity.Person.Person;
import com.project.skillswap.logic.entity.Exchange.Exchange;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.util.Date;

@Table(name = "transaction", indexes = {
        @Index(name = "idx_transaction_person", columnList = "person_id"),
        @Index(name = "idx_transaction_type", columnList = "type"),
        @Index(name = "idx_transaction_status", columnList = "status"),
        @Index(name = "idx_transaction_date", columnList = "transaction_date")
})
@Entity
public class Transaction {
    private static final Logger logger = LoggerFactory.getLogger(Transaction.class);

    //<editor-fold desc="Fields">
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id", referencedColumnName = "id", nullable = false)
    private Person person;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exchange_id", referencedColumnName = "id")
    private Exchange exchange;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private TransactionType type;

    @Column(name = "skillcoins_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal skillcoinsAmount;

    @Column(name = "usd_amount", precision = 10, scale = 2)
    private BigDecimal usdAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 20)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private TransactionStatus status = TransactionStatus.PENDING;

    @Column(name = "paypal_reference", length = 255)
    private String paypalReference;

    @CreationTimestamp
    @Column(updatable = false, name = "transaction_date")
    private Date transactionDate;
    //</editor-fold>

    //<editor-fold desc="Constructors">
    public Transaction() {}
    //</editor-fold>

    //<editor-fold desc="Getters and Setters">
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public Exchange getExchange() {
        return exchange;
    }

    public void setExchange(Exchange exchange) {
        this.exchange = exchange;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public BigDecimal getSkillcoinsAmount() {
        return skillcoinsAmount;
    }

    public void setSkillcoinsAmount(BigDecimal skillcoinsAmount) {
        this.skillcoinsAmount = skillcoinsAmount;
    }

    public BigDecimal getUsdAmount() {
        return usdAmount;
    }

    public void setUsdAmount(BigDecimal usdAmount) {
        this.usdAmount = usdAmount;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public String getPaypalReference() {
        return paypalReference;
    }

    public void setPaypalReference(String paypalReference) {
        this.paypalReference = paypalReference;
    }

    public Date getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(Date transactionDate) {
        this.transactionDate = transactionDate;
    }
    //</editor-fold>
}