package com.museumhub.booking.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Calcolo del prezzo di un posto: prezzo dell'itinerario meno lo sconto percentuale della tariffa. */
public final class Prezzi {

    private Prezzi() {
    }

    public static BigDecimal calcola(BigDecimal prezzoItinerario, int scontoPercentuale) {
        return prezzoItinerario
                .multiply(BigDecimal.valueOf(100L - scontoPercentuale))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}
