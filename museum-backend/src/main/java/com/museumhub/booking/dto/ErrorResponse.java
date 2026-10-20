package com.museumhub.booking.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/** Corpo di tutte le risposte di errore. "campi" e' presente solo per gli errori di validazione. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(String messaggio, Map<String, String> campi) {

    public static ErrorResponse of(String messaggio) {
        return new ErrorResponse(messaggio, null);
    }
}
