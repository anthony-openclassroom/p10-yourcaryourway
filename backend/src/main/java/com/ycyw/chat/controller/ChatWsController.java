package com.ycyw.chat.controller;

import com.ycyw.chat.dto.InboundMessageDto;
import com.ycyw.chat.dto.OutboundMessageDto;
import com.ycyw.chat.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class ChatWsController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    // Reçoit sur /app/chat/{sessionId}, diffuse à tous les abonnés de /topic/chat/{sessionId}
    @MessageMapping("/chat/{sessionId}")
    public void handleMessage(
            @DestinationVariable UUID sessionId,
            @Valid @Payload InboundMessageDto payload) {

        OutboundMessageDto saved = chatService.saveMessage(sessionId, payload);
        messagingTemplate.convertAndSend("/topic/chat/" + sessionId, saved);
    }
}
