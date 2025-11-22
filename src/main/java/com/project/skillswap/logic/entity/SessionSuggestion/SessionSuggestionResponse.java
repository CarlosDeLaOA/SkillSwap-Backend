package com.project.skillswap.logic.entity.SessionSuggestion;

import java.util.List;

/**
 * DTO para la respuesta de sugerencias personalizadas
 */
public class SessionSuggestionResponse {

    //#region Fields
    private boolean success;
    private String message;
    private List<SessionSuggestion> suggestions;
    //#endregion

    //#region Constructors
    public SessionSuggestionResponse() {}

    public SessionSuggestionResponse(boolean success, String message, List<SessionSuggestion> suggestions) {
        this.success = success;
        this.message = message;
        this.suggestions = suggestions;
    }
    //#endregion

    //#region Getters & Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<SessionSuggestion> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<SessionSuggestion> suggestions) {
        this.suggestions = suggestions;
    }
    //#endregion
}