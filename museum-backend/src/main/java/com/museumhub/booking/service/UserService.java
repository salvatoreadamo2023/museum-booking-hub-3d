package com.museumhub.booking.service;

import com.museumhub.booking.dto.UserCreateRequest;
import com.museumhub.booking.dto.UserResponse;
import com.museumhub.booking.dto.UserUpdateRequest;
import com.museumhub.booking.entity.StatoAccount;
import com.museumhub.booking.entity.User;
import com.museumhub.booking.exception.ApiException;
import com.museumhub.booking.repository.FeedbackRepository;
import com.museumhub.booking.repository.ReservationRepository;
import com.museumhub.booking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/** Amministrazione degli utenti (solo admin). L'admin non puo' togliersi da solo accesso o permessi. */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;
    private final FeedbackRepository feedbackRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UserResponse> elenca() {
        return userRepository.findAll(Sort.by("id")).stream().map(this::toResponse).toList();
    }

    @Transactional
    public UserResponse crea(UserCreateRequest r) {
        String email = normalizza(r.email());
        if (userRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "Email già registrata");
        }
        User user = userRepository.save(User.builder()
                .nome(r.nome().trim())
                .email(email)
                .passwordHash(passwordEncoder.encode(r.password()))
                .ruolo(r.ruolo())
                .stato(r.stato() != null ? r.stato() : StatoAccount.ATTIVO)
                .build());
        return toResponse(user);
    }

    @Transactional
    public UserResponse aggiorna(Long attoreId, Long id, UserUpdateRequest r) {
        User user = trova(id);
        String email = normalizza(r.email());
        userRepository.findByEmail(email)
                .filter(altro -> !altro.getId().equals(id))
                .ifPresent(altro -> {
                    throw new ApiException(HttpStatus.CONFLICT, "Email già registrata");
                });
        if (attoreId.equals(id) && (r.ruolo() != user.getRuolo() || r.stato() != user.getStato())) {
            throw new ApiException(HttpStatus.CONFLICT, "Non puoi modificare il tuo ruolo o il tuo stato");
        }
        user.setNome(r.nome().trim());
        user.setEmail(email);
        user.setRuolo(r.ruolo());
        user.setStato(r.stato());
        if (r.password() != null && !r.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(r.password()));
        }
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public void elimina(Long attoreId, Long id) {
        trova(id);
        if (attoreId.equals(id)) {
            throw new ApiException(HttpStatus.CONFLICT, "Non puoi eliminare il tuo account");
        }
        if (reservationRepository.existsByUserId(id) || feedbackRepository.existsByUserId(id)) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "L'utente ha prenotazioni o feedback: sospendilo invece di eliminarlo");
        }
        userRepository.deleteById(id);
    }

    private User trova(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Utente non trovato"));
    }

    private String normalizza(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private UserResponse toResponse(User u) {
        String creatoIl = u.getCreatedAt() != null ? u.getCreatedAt().toLocalDate().toString() : null;
        return new UserResponse(u.getId(), u.getNome(), u.getEmail(), u.getRuolo(), u.getStato(), creatoIl);
    }
}
