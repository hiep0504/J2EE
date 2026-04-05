package com.example.Backend_J2EE.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConversationDetailsResponse {

    private ConversationSummaryResponse conversation;
    private List<ChatMessageResponse> messages;
}
