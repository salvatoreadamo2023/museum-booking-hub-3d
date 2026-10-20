package com.museumhub.booking.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/** Errore applicativo con il codice HTTP da restituire al client. */
@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}
