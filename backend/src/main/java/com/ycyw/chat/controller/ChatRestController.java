package com.ycyw.chat.controller;

import com.ycyw.chat.dto.ChatSessionResponse;
import com.ycyw.chat.dto.CreateSessionRequest;
import com.ycyw.chat.dto.OutboundMessageDto;
import com.ycyw.chat.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "Gestion des sessions de tchat support")
public class ChatRestController {

    private final ChatService chatService;

    @PostMapping("/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Ouvre une nouvelle session de tchat")
    public ChatSessionResponse createSession(@RequestBody @Valid CreateSessionRequest request) {
        return chatService.createSession(request);
    }

    @GetMapping("/sessions/{sessionId}/messages")
    @Operation(summary = "Récupère l'historique des messages d'une session")
    public List<OutboundMessageDto> getHistory(@PathVariable UUID sessionId) {
        return chatService.getHistory(sessionId);
    }

    // ResponseEntity utilisé ici (pas @ResponseStatus) pour contrôler explicitement le 204
    @PatchMapping("/sessions/{sessionId}/close")
    @Operation(summary = "Ferme une session de tchat")
    public ResponseEntity<Void> closeSession(@PathVariable UUID sessionId) {
        chatService.closeSession(sessionId);
        return ResponseEntity.noContent().build();
    }
}
