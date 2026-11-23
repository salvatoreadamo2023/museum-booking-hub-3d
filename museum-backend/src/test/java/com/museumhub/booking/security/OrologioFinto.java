package com.museumhub.booking.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Orologio che avanza solo quando lo dice il test: niente attese vere per provare le finestre di 15 minuti. */
final class OrologioFinto extends Clock {

    private Instant adesso = Instant.parse("2026-11-20T10:00:00Z");

    void avanza(Duration quanto) {
        adesso = adesso.plus(quanto);
    }

    @Override
    public Instant instant() {
        return adesso;
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zona) {
        return this;
    }
}
