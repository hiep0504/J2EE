package com.example.Backend_J2EE.dto.rag;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public class RagChatRequest {

    @NotBlank(message = "question is required")
    private String question;
    private List<RagChatTurn> history;
    private String focusProductName;

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public List<RagChatTurn> getHistory() {
        return history;
    }

    public void setHistory(List<RagChatTurn> history) {
        this.history = history;
    }

    public String getFocusProductName() {
        return focusProductName;
    }

    public void setFocusProductName(String focusProductName) {
        this.focusProductName = focusProductName;
    }
}
