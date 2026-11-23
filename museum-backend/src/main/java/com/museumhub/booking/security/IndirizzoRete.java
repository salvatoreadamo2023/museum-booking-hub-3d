package com.museumhub.booking.security;

import java.net.InetAddress;
import java.util.HexFormat;

/**
 * Chiave da usare nei limiti "per IP". Un IPv4 resta com'e'. Un IPv6 viene ridotto alla sua rete /64,
 * perche' di solito un solo utente (o un solo provider) dispone di un intero /64: contare ogni indirizzo
 * a parte permetterebbe di aggirare i limiti cambiando indirizzo a ogni richiesta.
 */
final class IndirizzoRete {

    private IndirizzoRete() {
    }

    static String chiave(String ip) {
        if (ip == null || ip.isBlank()) {
            return "sconosciuto";
        }
        if (ip.indexOf(':') < 0) {
            return ip;
        }
        try {
            // con un indirizzo letterale getByName non fa nessuna richiesta DNS
            InetAddress indirizzo = InetAddress.getByName(ip);
            byte[] byteIp = indirizzo.getAddress();
            if (byteIp.length != 16) {
                return indirizzo.getHostAddress(); // IPv4 scritto in forma IPv6 (::ffff:1.2.3.4)
            }
            return HexFormat.of().formatHex(byteIp, 0, 8) + "::/64";
        } catch (Exception e) {
            return ip;
        }
    }
}
