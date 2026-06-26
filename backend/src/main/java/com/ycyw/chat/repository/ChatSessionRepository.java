package com.ycyw.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ycyw.chat.model.ChatSession;
import java.util.UUID;

// Spring Data génère automatiquement les requêtes CRUD
public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {
}
