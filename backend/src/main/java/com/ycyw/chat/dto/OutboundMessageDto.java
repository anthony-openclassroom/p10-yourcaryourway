package com.ycyw.chat.dto;

import com.ycyw.chat.model.ChatMessage;
import java.time.OffsetDateTime;
import java.util.UUID;

// Message diffusé à tous les abonnés du topic STOMP (Simple Text Oriented Messaging Protocol) de la session
public record OutboundMessageDto(
        UUID id,
        UUID sessionId,
        ChatMessage.SenderRole senderRole,
        String content,
        OffsetDateTime sentAt
) {
    // Mapper statique pour garder la logique de conversion hors du service
    public static OutboundMessageDto from(ChatMessage message) {
        return new OutboundMessageDto(
                message.getId(), message.getSessionId(), message.getSenderRole(),
                message.getContent(), message.getSentAt()
        );
    }

    // Notification de fermeture de session — non persistée en base
    public static OutboundMessageDto sessionClosed(UUID sessionId, String closedBy) {
        return new OutboundMessageDto(
                UUID.randomUUID(), sessionId, ChatMessage.SenderRole.system,
                "La session a été fermée par " + closedBy + ".",
                OffsetDateTime.now()
        );
    }
}
