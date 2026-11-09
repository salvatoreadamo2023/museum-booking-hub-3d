package com.museumhub.booking.dto;

import com.museumhub.booking.entity.StatoPrenotazione;

import java.math.BigDecimal;
import java.util.List;

/**
 * "annullabile": per il visitatore e' vero fino a 1 ora prima della visita, per l'admin sempre (se confermata).
 * "passata": la visita e' gia' iniziata (serve a mostrare il pulsante del feedback).
 */
public record ReservationResponse(
        Long id,
        String codice,
        Long tourId,
        String tour,
        String data,
        String orario,
        List<SeatResponse> posti,
        BigDecimal totale,
        StatoPrenotazione stato,
        Long utenteId,
        String utenteNome,
        String utenteEmail,
        boolean annullabile,
        boolean passata) {
}
