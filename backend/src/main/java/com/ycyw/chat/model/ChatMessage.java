package com.ycyw.chat.model;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "chat_messages")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ChatMessage {

    // Valeurs en minuscules pour correspondre exactement au CHECK constraint de la migration SQL
    // "system" n'est pas persisté en base - uniquement diffusé via WebSocket comme notification
    public enum SenderRole { client, agent, system }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Clé étrangère vers ChatSession - UUID seul, pas de @ManyToOne pour éviter le chargement inutile
    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    // STRING pour stocker "client"/"agent" en clair, cohérent avec le CHECK constraint SQL
    @Enumerated(EnumType.STRING)
    @Column(name = "sender_role", nullable = false)
    private SenderRole senderRole;

    @Column(nullable = false)
    private String content;

    @Column(name = "sent_at")
    @Builder.Default
    private OffsetDateTime sentAt = OffsetDateTime.now();
}
