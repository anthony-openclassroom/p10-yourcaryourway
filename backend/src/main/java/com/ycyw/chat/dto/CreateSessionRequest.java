package com.ycyw.chat.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

// Le status initial "open" est appliqué par défaut dans l'entité
// pas besoin de le passer ici
public record CreateSessionRequest(
        @NotNull UUID userId,
        @NotNull UUID agencyId
) {}
