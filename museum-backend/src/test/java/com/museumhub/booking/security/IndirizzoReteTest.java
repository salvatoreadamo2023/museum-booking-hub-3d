package com.museumhub.booking.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/** Normalizzazione degli indirizzi usati come chiave nei limiti per IP. */
class IndirizzoReteTest {

    @Test
    void unIpv4RestaComE() {
        assertEquals("203.0.113.7", IndirizzoRete.chiave("203.0.113.7"));
    }

    @Test
    void unIpv4ScrittoInFormaIpv6DiventaIlSuoIpv4() {
        // stesso client, che arrivi come IPv4 o come IPv4-mapped, deve avere lo stesso contatore
        assertEquals("203.0.113.7", IndirizzoRete.chiave("::ffff:203.0.113.7"));
        assertEquals("203.0.113.7", IndirizzoRete.chiave("0:0:0:0:0:ffff:cb00:7107"));
    }

    @Test
    void formeDiverseDelloStessoIpv6DannoLaStessaChiave() {
        String compressa = IndirizzoRete.chiave("2001:db8::1");
        assertEquals(compressa, IndirizzoRete.chiave("2001:0DB8:0000:0000:0000:0000:0000:0001"));
        assertEquals("20010db800000000::/64", compressa);
    }

    @Test
    void stessaRete64StessaChiaveReteDiversaChiaveDiversa() {
        assertEquals(IndirizzoRete.chiave("2001:db8:1:2::a"), IndirizzoRete.chiave("2001:db8:1:2:ffff:ffff:ffff:ffff"));
        assertNotEquals(IndirizzoRete.chiave("2001:db8:1:2::a"), IndirizzoRete.chiave("2001:db8:1:3::a"));
    }

    @Test
    void loopbackEZoneIdSonoGestiti() {
        assertEquals("00000000000000000::/64".substring(1), IndirizzoRete.chiave("::1"));
        assertEquals(IndirizzoRete.chiave("fe80::1"), IndirizzoRete.chiave("fe80::1%1"));
    }

    @Test
    void indirizzoMancanteNonRompeNulla() {
        assertEquals("sconosciuto", IndirizzoRete.chiave(null));
        assertEquals("sconosciuto", IndirizzoRete.chiave(" "));
    }
}
