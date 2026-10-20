package com.museumhub.booking.service;

import com.museumhub.booking.dto.AuthResponse;
import com.museumhub.booking.dto.LoginRequest;
import com.museumhub.booking.dto.RegisterRequest;
import com.museumhub.booking.entity.Ruolo;
import com.museumhub.booking.entity.StatoAccount;
import com.museumhub.booking.entity.User;
import com.museumhub.booking.exception.ApiException;
import com.museumhub.booking.repository.UserRepository;
import com.museumhub.booking.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /** Registrazione pubblica: crea sempre un VISITATORE attivo e lo fa entrare subito. */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
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
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(normalizza(request.email()))
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Credenziali non valide"));
        if (user.getStato() != StatoAccount.ATTIVO) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Account sospeso: contatta l'amministrazione del museo");
        }
        return toResponse(user);
    }

    private AuthResponse toResponse(User user) {
        return new AuthResponse(jwtService.generateToken(user), user.getId(), user.getNome(),
                user.getEmail(), user.getRuolo());
    }

    private String normalizza(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
