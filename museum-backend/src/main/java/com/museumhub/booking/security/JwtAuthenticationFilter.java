package com.museumhub.booking.security;

import com.museumhub.booking.repository.SessioneRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Autentica la richiesta a partire dal token di accesso, letto dal cookie HttpOnly (usato dal frontend) oppure
 * dall'header "Authorization: Bearer" (usato da Swagger, dagli script e dai test). La richiesta e' autenticata
 * solo se il token e' valido, e' di tipo "access", la sua sessione non e' revocata ne' scaduta e l'utente e'
 * attivo. Ruolo e stato vengono riletti dal database a ogni richiesta, quindi sospendere un account o
 * cambiarne il ruolo ha effetto subito.
 * Non e' un @Component: viene creato in SecurityConfig per non essere registrato due volte.
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final SessioneRepository sessioneRepository;
    private final CookieService cookieService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = estraiToken(request);

        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null
                && jwtService.isValid(token) && JwtService.TIPO_ACCESSO.equals(jwtService.extractTipo(token))) {
            try {
                UserPrincipal principal = userDetailsService.loadUserByUsername(jwtService.extractEmail(token));
                if (principal.isEnabled() && sessioneAttiva(jwtService.extractSid(token), principal)) {
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            principal, null, principal.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (UsernameNotFoundException ignored) {
                // utente eliminato: la richiesta resta non autenticata
            }
        }
        chain.doFilter(request, response);
    }

    private boolean sessioneAttiva(String sid, UserPrincipal principal) {
        if (sid == null) {
            return false;
        }
        return sessioneRepository.findById(sid)
                .filter(s -> s.isAttiva() && s.getUserId().equals(principal.getUser().getId()))
                .isPresent();
    }

    private String estraiToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return cookieService.leggi(request, CookieService.ACCESSO);
    }
}
