package com.project.skillswap.logic.entity.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

/**
 * Enum representing predefined SkillCoin packages available for purchase.
 * Each package contains a specific amount of coins and its corresponding USD price.
 */
public enum CoinPackageType {
    BASIC(new BigDecimal("10"), new BigDecimal("9.99")),
    MEDIUM(new BigDecimal("30"), new BigDecimal("27.99")),
    LARGE(new BigDecimal("50"), new BigDecimal("44.99")),
    PREMIUM(new BigDecimal("100"), new BigDecimal("84.99"));

    private final BigDecimal coins;
    private final BigDecimal priceUsd;

    CoinPackageType(BigDecimal coins, BigDecimal priceUsd) {
        this.coins = coins;
        this.priceUsd = priceUsd;
    }

    public BigDecimal getCoins() {
        return coins;
    }

    public BigDecimal getPriceUsd() {
        return priceUsd;
    }
}






