package com.museumhub.booking.security;

import com.museumhub.booking.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Genera e verifica i token JWT. La chiave deve avere almeno 32 caratteri (app.jwt.secret).
 * Esistono due tipi di token, distinti dal claim "tipo": di accesso (breve, 15 minuti) e di refresh (lungo,
 * 7 giorni). Entrambi portano il claim "sid", cioe' l'identificativo della sessione nel database.
 */
@Service
public class JwtService {

    public static final String TIPO_ACCESSO = "access";
    public static final String TIPO_REFRESH = "refresh";

    private final SecretKey key;
    private final long accessoMs;
    private final long refreshMs;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.access-expiration-ms:900000}") long accessoMs,
                      @Value("${app.jwt.refresh-expiration-ms:604800000}") long refreshMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessoMs = accessoMs;
        this.refreshMs = refreshMs;
    }

    public String generateAccessToken(User user, String sid) {
        return costruisci(user, sid, TIPO_ACCESSO, accessoMs);
    }

    public String generateRefreshToken(User user, String sid) {
        return costruisci(user, sid, TIPO_REFRESH, refreshMs);
    }

    public long getAccessoMs() {
        return accessoMs;
    }

    public long getRefreshMs() {
        return refreshMs;
    }

    public String extractEmail(String token) {
        return parse(token).getSubject();
    }

    public String extractSid(String token) {
        return parse(token).get("sid", String.class);
    }

    public String extractTipo(String token) {
        return parse(token).get("tipo", String.class);
    }

    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private String costruisci(User user, String sid, String tipo, long durataMs) {
        Date now = new Date();
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("ruolo", user.getRuolo().name())
                .claim("uid", user.getId())
                .claim("sid", sid)
                .claim("tipo", tipo)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + durataMs))
                .signWith(key)
                .compact();
    }

    private Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
