package com.museumhub.booking.repository;

import com.museumhub.booking.entity.Sessione;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface SessioneRepository extends JpaRepository<Sessione, String> {

    /** Pulizia delle sessioni scadute. */
    void deleteByScadeIlBefore(Instant limite);

    /** Elimina tutte le sessioni di un utente (cambio password, eliminazione account). */
    void deleteByUserId(Long userId);
}
