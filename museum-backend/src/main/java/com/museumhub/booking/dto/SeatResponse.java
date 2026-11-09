package com.museumhub.booking.dto;

import java.math.BigDecimal;

public record SeatResponse(Integer numero, Long tariffaId, String tariffa, BigDecimal prezzo) {
}
