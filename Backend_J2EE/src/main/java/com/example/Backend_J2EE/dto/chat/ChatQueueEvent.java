package com.example.Backend_J2EE.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatQueueEvent {

    private String eventType;
    private ConversationSummaryResponse conversation;
}
