package com.museumhub.booking.security;

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
 * Autentica la richiesta a partire dal token letto dall'header "Authorization: Bearer". La richiesta e'
 * autenticata solo se il token e' valido e l'utente e' attivo. Ruolo e stato vengono riletti dal database a ogni
 * richiesta, quindi sospendere un account o cambiarne il ruolo ha effetto subito.
 * Non e' un @Component: viene creato in SecurityConfig per non essere registrato due volte.
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = estraiToken(request);

        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null
                && jwtService.isValid(token)) {
            try {
                UserPrincipal principal = userDetailsService.loadUserByUsername(jwtService.extractEmail(token));
                if (principal.isEnabled()) {
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

    private String estraiToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
