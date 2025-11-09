package com.project.skillswap.logic.entity.dashboard;

/**
 * Response containing account balance information
 */
public class AccountBalanceResponse {

    //#region Fields
    private Integer skillCoins;
    //#endregion

    //#region Constructors
    public AccountBalanceResponse() {}

    public AccountBalanceResponse(Integer skillCoins) {
        this.skillCoins = skillCoins;
    }
    //#endregion

    //#region Getters and Setters
    public Integer getSkillCoins() {
        return skillCoins;
    }

    public void setSkillCoins(Integer skillCoins) {
        this.skillCoins = skillCoins;
    }
    //#endregion
}