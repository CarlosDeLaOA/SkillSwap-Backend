package com.project.skillswap.logic.entity.Person;


public class LoginResponse {

    //#region Fields
    private String token;
    private Person authPerson;
    private long expiresIn;
    //#endregion

    //#region Getters and Setters
    /**
     * Gets the JWT authentication token.
     *
     * @return the JWT token
     */
    public String getToken() {
        return token;
    }

    /**
     * Sets the JWT authentication token.
     *
     * @param token the JWT token to set
     */
    public void setToken(String token) {
        this.token = token;
    }

    /**
     * Gets the token expiration time in milliseconds.
     *
     * @return the expiration time
     */
    public long getExpiresIn() {
        return expiresIn;
    }

    /**
     * Sets the token expiration time.
     *
     * @param expiresIn the expiration time in milliseconds
     */
    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }

    /**
     * Gets the authenticated person information.
     *
     * @return the authenticated person
     */
    public Person getAuthPerson() {
        return authPerson;
    }

    /**
     * Sets the authenticated person information.
     *
     * @param authPerson the authenticated person to set
     */
    public void setAuthPerson(Person authPerson) {
        this.authPerson = authPerson;
    }
    //#endregion
}