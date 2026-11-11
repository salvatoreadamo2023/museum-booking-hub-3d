package com.museumhub.booking.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

import java.time.Duration;

/**
 * Crea e legge i cookie di autenticazione. Sono sempre HttpOnly (JavaScript non puo' leggerli) e SameSite=Strict
 * (il browser non li invia nelle richieste che partono da altri siti). L'attributo Secure e' configurabile:
 * false in sviluppo su HTTP, true in produzione su HTTPS (app.cookie.secure).
 */
@Component
public class CookieService {

    public static final String ACCESSO = "mbh_access";
    public static final String REFRESH = "mbh_refresh";

    private static final String PATH_ACCESSO = "/api";
    private static final String PATH_REFRESH = "/api/auth";

    private final JwtService jwtService;
    private final boolean secure;
    private final String sameSite;

    public CookieService(JwtService jwtService,
                         @Value("${app.cookie.secure:false}") boolean secure,
                         @Value("${app.cookie.same-site:Strict}") String sameSite) {
        this.jwtService = jwtService;
        this.secure = secure;
        this.sameSite = sameSite;
    }

    public void impostaAccesso(HttpServletResponse response, String token) {
        aggiungi(response, ACCESSO, token, PATH_ACCESSO, Duration.ofMillis(jwtService.getAccessoMs()));
    }

    public void impostaRefresh(HttpServletResponse response, String token) {
        aggiungi(response, REFRESH, token, PATH_REFRESH, Duration.ofMillis(jwtService.getRefreshMs()));
    }

    /** Fa cancellare al browser entrambi i cookie (durata zero). */
    public void cancella(HttpServletResponse response) {
        aggiungi(response, ACCESSO, "", PATH_ACCESSO, Duration.ZERO);
        aggiungi(response, REFRESH, "", PATH_REFRESH, Duration.ZERO);
    }

    /** Valore del cookie, o null se assente o vuoto. */
    public String leggi(HttpServletRequest request, String nome) {
        Cookie cookie = WebUtils.getCookie(request, nome);
        if (cookie == null || cookie.getValue() == null || cookie.getValue().isBlank()) {
            return null;
        }
        return cookie.getValue();
    }

    private void aggiungi(HttpServletResponse response, String nome, String valore, String path, Duration durata) {
        ResponseCookie cookie = ResponseCookie.from(nome, valore)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(path)
                .maxAge(durata)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
