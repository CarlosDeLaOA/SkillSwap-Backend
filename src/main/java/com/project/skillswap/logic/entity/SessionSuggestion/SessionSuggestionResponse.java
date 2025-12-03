package com.project.skillswap.logic.entity.SessionSuggestion;

import java.util.List;

/*
 DTO para encapsular la respuesta de sugerencias al controlador. ***
*/
public class SessionSuggestionResponse {
    private boolean success;
    private String message;
    private List<SessionSuggestion> suggestions;

    public SessionSuggestionResponse() {}

    public SessionSuggestionResponse(boolean success, String message, List<SessionSuggestion> suggestions) {
        this.success = success;
        this.message = message;
        this.suggestions = suggestions;
    }

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
}