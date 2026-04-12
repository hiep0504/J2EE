package com.example.Backend_J2EE.dto.rag;

import jakarta.validation.constraints.NotBlank;

public class RagChatRequest {

    @NotBlank(message = "question is required")
    private String question;

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }
}
