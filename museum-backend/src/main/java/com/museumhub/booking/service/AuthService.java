package com.museumhub.booking.service;

import com.museumhub.booking.dto.AuthResponse;
import com.museumhub.booking.dto.LoginRequest;
import com.museumhub.booking.dto.RegisterRequest;
import com.museumhub.booking.entity.Ruolo;
import com.museumhub.booking.entity.Sessione;
import com.museumhub.booking.entity.StatoAccount;
import com.museumhub.booking.entity.User;
import com.museumhub.booking.exception.ApiException;
import com.museumhub.booking.repository.SessioneRepository;
import com.museumhub.booking.repository.UserRepository;
import com.museumhub.booking.security.JwtService;
import com.museumhub.booking.security.LoginAttemptService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    /** Esito di un accesso: dati utente per il corpo della risposta e token per i cookie (refresh puo' mancare). */
    public record Esito(AuthResponse utente, String accesso, String refresh) {
    }

    private final UserRepository userRepository;
    private final SessioneRepository sessioneRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginAttemptService tentativi;

    /** Registrazione pubblica: crea sempre un VISITATORE attivo e lo fa entrare subito. */
    @Transactional
    public Esito register(RegisterRequest request) {
        String email = normalizza(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "Email già registrata");
        }
        User user = userRepository.save(User.builder()
                .nome(request.nome().trim())
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .ruolo(Ruolo.VISITATORE)
                .stato(StatoAccount.ATTIVO)
                .build());
        return nuovaSessione(user);
    }

    /**
     * Accesso con email e password. I tentativi errati sono limitati per coppia indirizzo IP + email: superata
     * la soglia, l'accesso e' bloccato per qualche minuto anche con la password giusta (HTTP 429).
     */
    @Transactional
    public Esito login(LoginRequest request, String indirizzoIp) {
        String email = normalizza(request.email());
        String chiave = indirizzoIp + "|" + email;
        tentativi.verifica(chiave);

        Optional<User> trovato = userRepository.findByEmail(email)
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()));
        if (trovato.isEmpty()) {
            tentativi.fallito(chiave);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Credenziali non valide");
        }
        tentativi.riuscito(chiave);

        User user = trovato.get();
        if (user.getStato() != StatoAccount.ATTIVO) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Account sospeso: contatta l'amministrazione del museo");
        }
        return nuovaSessione(user);
    }

    /** Emette un nuovo token di accesso a partire dal refresh token, se la sessione e' ancora attiva. */
    @Transactional(readOnly = true)
    public Esito refresh(String refreshToken) {
        if (refreshToken == null || !jwtService.isValid(refreshToken)
                || !JwtService.TIPO_REFRESH.equals(jwtService.extractTipo(refreshToken))) {
            throw nonAutorizzato();
        }
        String sid = jwtService.extractSid(refreshToken);
        Sessione sessione = sessioneRepository.findById(sid == null ? "" : sid)
                .filter(Sessione::isAttiva)
                .orElseThrow(this::nonAutorizzato);
        User user = userRepository.findById(sessione.getUserId())
                .filter(u -> u.getStato() == StatoAccount.ATTIVO)
                .filter(u -> u.getEmail().equals(jwtService.extractEmail(refreshToken)))
                .orElseThrow(this::nonAutorizzato);
        return new Esito(AuthResponse.of(user), jwtService.generateAccessToken(user, sessione.getId()), null);
    }

    /** Revoca la sessione a cui appartengono i token ricevuti (se validi). Non fallisce mai. */
    @Transactional
    public void logout(String accessToken, String refreshToken) {
        revoca(accessToken);
        revoca(refreshToken);
    }

    private void revoca(String token) {
        if (token == null || !jwtService.isValid(token)) {
            return;
        }
        String sid = jwtService.extractSid(token);
        if (sid != null) {
            sessioneRepository.findById(sid).ifPresent(s -> s.setRevocata(true));
        }
    }

    private Esito nuovaSessione(User user) {
        sessioneRepository.deleteByScadeIlBefore(Instant.now());
        Sessione sessione = sessioneRepository.save(Sessione.builder()
                .id(UUID.randomUUID().toString())
                .userId(user.getId())
                .scadeIl(Instant.now().plusMillis(jwtService.getRefreshMs()))
                .build());
        return new Esito(AuthResponse.of(user),
                jwtService.generateAccessToken(user, sessione.getId()),
                jwtService.generateRefreshToken(user, sessione.getId()));
    }

    private ApiException nonAutorizzato() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "Sessione scaduta: effettua di nuovo l'accesso");
    }

    private String normalizza(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
