
package com.project.skillswap.logic.entity.CreditPackage;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Table(name = "credit_package", indexes = {
        @Index(name = "idx_credit_package_quantity", columnList = "package_quantity", unique = true)
})
@Entity
public class CreditPackage {

    //<editor-fold desc="Fields">
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100)
    private String name = "SkillCoin";

    @Column(name = "package_quantity", nullable = false)
    private Integer packageQuantity;

    @Column(name = "price_usd", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceUsd;

    @Column(name = "active")
    private Boolean active = true;
    //</editor-fold>

    //<editor-fold desc="Constructors">
    public CreditPackage() {}
    //</editor-fold>

    //<editor-fold desc="Getters and Setters">
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getPackageQuantity() {
        return packageQuantity;
    }

    public void setPackageQuantity(Integer packageQuantity) {
        this.packageQuantity = packageQuantity;
    }

    public BigDecimal getPriceUsd() {
        return priceUsd;
    }

    public void setPriceUsd(BigDecimal priceUsd) {
        this.priceUsd = priceUsd;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
    //</editor-fold>
}