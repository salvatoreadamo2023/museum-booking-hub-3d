package com.museumhub.booking.controller;

import com.museumhub.booking.dto.PaginaResponse;
import com.museumhub.booking.dto.ReservationRequest;
import com.museumhub.booking.dto.ReservationResponse;
import com.museumhub.booking.dto.ReservationUpdateRequest;
import com.museumhub.booking.security.UserPrincipal;
import com.museumhub.booking.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
@Tag(name = "Prenotazioni")
public class ReservationController {

    private final ReservationService reservationService;

    @GetMapping
    @Operation(summary = "Le proprie prenotazioni")
    public List<ReservationResponse> elenca(@Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        return reservationService.elenca(principal.getUser());
    }

    @GetMapping("/registro")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Registro di tutte le prenotazioni, paginato (admin). Pagina da 0; dimensione da 1 a 100")
    public PaginaResponse<ReservationResponse> registro(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int dimensione) {
        return reservationService.registro(pagina, dimensione);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Prenota i posti scelti per un itinerario, una data e una fascia (409 se un posto e' occupato)")
    public ReservationResponse crea(@Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
                                    @Valid @RequestBody ReservationRequest request) {
        return reservationService.crea(principal.getUser(), request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[Admin] Modifica fascia oraria e numero di posti di una prenotazione")
    public ReservationResponse aggiorna(@PathVariable Long id, @Valid @RequestBody ReservationUpdateRequest request) {
        return reservationService.aggiorna(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Disdice una prenotazione: il visitatore fino a 1 ora prima, l'admin sempre")
    public ReservationResponse annulla(@Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
                                       @PathVariable Long id) {
        return reservationService.annulla(principal.getUser(), id);
    }
}
