package com.museumhub.booking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Sessione di accesso di un utente. Ogni login ne crea una; gli identificativi (sid) finiscono nei token
 * JWT. Revocare la sessione (logout) rende inutilizzabili subito sia il token di accesso sia il refresh token.
 */
@Entity
@Table(name = "sessioni")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sessione {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "scade_il", nullable = false)
    private Instant scadeIl;

    @Column(nullable = false)
    private boolean revocata;

    @CreationTimestamp
    @Column(name = "creata_il", nullable = false, updatable = false)
    private LocalDateTime creataIl;

    public boolean isAttiva() {
        return !revocata && scadeIl.isAfter(Instant.now());
    }
}
