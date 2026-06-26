package com.ycyw.chat.service;

import com.ycyw.chat.dto.*;
import com.ycyw.chat.model.ChatMessage;
import com.ycyw.chat.model.ChatSession;
import com.ycyw.chat.repository.ChatMessageRepository;
import com.ycyw.chat.repository.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;

    @Transactional
    public ChatSessionResponse createSession(CreateSessionRequest request) {
        ChatSession session = ChatSession.builder()
                .userId(request.userId())
                .agencyId(request.agencyId())
                .build();
        return ChatSessionResponse.from(sessionRepository.save(session));
    }

    @Transactional
    public OutboundMessageDto saveMessage(UUID sessionId, InboundMessageDto dto) {
        // Vérifie l'existence de la session sans la charger entièrement
        if (!sessionRepository.existsById(sessionId)) {
            throw new NoSuchElementException("Session not found: " + sessionId);
        }
        ChatMessage message = ChatMessage.builder()
                .sessionId(sessionId)
                .senderRole(dto.senderRole())
                .content(dto.content())
                .build();
        return OutboundMessageDto.from(messageRepository.save(message));
    }

    @Transactional(readOnly = true)
    public List<OutboundMessageDto> getHistory(UUID sessionId) {
        return messageRepository.findBySessionIdOrderBySentAtAsc(sessionId)
                .stream().map(OutboundMessageDto::from).toList();
    }

    @Transactional
    public void closeSession(UUID sessionId) {
        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NoSuchElementException("Session not found: " + sessionId));
        session.setStatus(ChatSession.Status.closed);
        session.setClosedAt(OffsetDateTime.now());
        // Pas besoin de save() explicite 
        // JPA persiste les changements sur l'entité gérée en fin de transaction
    }
}
