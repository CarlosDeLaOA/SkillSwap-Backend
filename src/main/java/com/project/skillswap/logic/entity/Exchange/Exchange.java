package com.project.skillswap.logic.entity.Exchange;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.project.skillswap.logic.entity.LearningSession.LearningSession;
import com.project.skillswap.logic.entity.Transaction.Transaction;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Table(name = "exchange", indexes = {
        @Index(name = "idx_exchange_session", columnList = "learning_session_id", unique = true)
})
@Entity
public class Exchange {
    private static final Logger logger = LoggerFactory.getLogger(Exchange.class);

    //<editor-fold desc="Fields">
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learning_session_id", referencedColumnName = "id", nullable = false)
    private LearningSession learningSession;

    @Column(name = "credits_cost", precision = 10, scale = 2)
    private BigDecimal creditsCost = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 20)
    private ExchangeType type;

    @CreationTimestamp
    @Column(updatable = false, name = "creation_date")
    private Date creationDate;

    @OneToMany(mappedBy = "exchange", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Transaction> transactions;
    //</editor-fold>

    //<editor-fold desc="Constructors">
    public Exchange() {}
    //</editor-fold>

    //<editor-fold desc="Getters and Setters">
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LearningSession getLearningSession() {
        return learningSession;
    }

    public void setLearningSession(LearningSession learningSession) {
        this.learningSession = learningSession;
    }

    public BigDecimal getCreditsCost() {
        return creditsCost;
    }

    public void setCreditsCost(BigDecimal creditsCost) {
        this.creditsCost = creditsCost;
    }

    public ExchangeType getType() {
        return type;
    }

    public void setType(ExchangeType type) {
        this.type = type;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
    }
    //</editor-fold>
}