package com.example.Backend_J2EE.dto.chat;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatWsSendRequest {

    private Integer conversationId;
    private String content;
}
