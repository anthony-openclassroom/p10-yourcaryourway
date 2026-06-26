package com.ycyw.chat.dto;

import com.ycyw.chat.model.ChatMessage;
import jakarta.validation.constraints.NotBlank;

// Message reçu via WebSocket depuis le client
// sessionId passé dans l'URL STOMP (Simple Text Oriented Messaging Protocol), pas dans le body
public record InboundMessageDto(
        @NotBlank String content,
        ChatMessage.SenderRole senderRole 
) {}
