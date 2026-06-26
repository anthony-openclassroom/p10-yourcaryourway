package com.ycyw.chat.repository;

import com.ycyw.chat.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

// Spring Data génère automatiquement les requêtes CRUD
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
    // Récupère les messages d'une session triés par date c'est utilisé pour afficher l'historique du chat
    List<ChatMessage> findBySessionIdOrderBySentAtAsc(UUID sessionId);
}