package com.ycyw.chat.service;

import com.ycyw.chat.dto.CreateSessionRequest;
import com.ycyw.chat.dto.InboundMessageDto;
import com.ycyw.chat.dto.OutboundMessageDto;
import com.ycyw.chat.dto.ChatSessionResponse;
import com.ycyw.chat.model.ChatMessage;
import com.ycyw.chat.model.ChatSession;
import com.ycyw.chat.repository.ChatMessageRepository;
import com.ycyw.chat.repository.ChatSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock private ChatSessionRepository sessionRepository;
    @Mock private ChatMessageRepository messageRepository;
    @InjectMocks private ChatService chatService;

    private UUID sessionId, userId, agencyId;

    @BeforeEach
    void setUp() {
        sessionId = UUID.randomUUID();
        userId    = UUID.randomUUID();
        agencyId  = UUID.randomUUID();
    }

    @Test
    void createSession_returnsSessionWithOpenStatus() {
        ChatSession saved = ChatSession.builder()
                .id(sessionId).userId(userId).agencyId(agencyId).build(); // status "open" par défaut via @Builder.Default
        when(sessionRepository.save(any())).thenReturn(saved);

        ChatSessionResponse response = chatService.createSession(new CreateSessionRequest(userId, agencyId));

        assertThat(response.id()).isEqualTo(sessionId);
        assertThat(response.status()).isEqualTo(ChatSession.Status.open);
    }

    @Test
    void saveMessage_persistsAndReturnsMappedDto() {
        when(sessionRepository.existsById(sessionId)).thenReturn(true);
        ChatMessage saved = ChatMessage.builder()
                .id(UUID.randomUUID()).sessionId(sessionId)
                .senderRole(ChatMessage.SenderRole.client).content("Bonjour").build();
        when(messageRepository.save(any())).thenReturn(saved);

        OutboundMessageDto result = chatService.saveMessage(
                sessionId, new InboundMessageDto("Bonjour", ChatMessage.SenderRole.client));

        assertThat(result.senderRole()).isEqualTo(ChatMessage.SenderRole.client);
        assertThat(result.content()).isEqualTo("Bonjour");
    }
}
