package com.ycyw.chat.dto;

import com.ycyw.chat.model.ChatSession;
import java.time.OffsetDateTime;
import java.util.UUID;

// Réponse API
// N'expose pas closedAt car non pertinent à la création
public record ChatSessionResponse(
        UUID id,
        UUID userId,
        UUID agencyId,
        ChatSession.Status status, // enum sérialisé en String par Jackson ("open"/"closed")
        OffsetDateTime createdAt
) {
    // Mapper statique pour garder la logique de conversion hors du service
    public static ChatSessionResponse from(ChatSession session) {
        return new ChatSessionResponse(
                session.getId(), session.getUserId(), session.getAgencyId(),
                session.getStatus(), session.getCreatedAt()
        );
    }
}
