package com.ycyw.chat.model;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "chat_sessions") // nom de la table défini dans V1__create_chat_tables.sql
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ChatSession {

    // Valeurs en minuscules pour correspondre exactement au CHECK constraint de la migration SQL
    public enum Status { open, closed }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // user_id et agency_id sont gérés par le service d'auth externe — pas de @ManyToOne
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "agency_id", nullable = false)
    private UUID agencyId;

    // STRING pour stocker "open"/"closed" en clair, cohérent avec le CHECK constraint SQL
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Status status = Status.open;

    @Column(name = "created_at")
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "closed_at")
    private OffsetDateTime closedAt; // null tant que la session est ouverte
}
