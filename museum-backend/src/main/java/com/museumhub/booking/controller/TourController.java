package com.museumhub.booking.controller;

import com.museumhub.booking.dto.PostiResponse;
import com.museumhub.booking.dto.TourRequest;
import com.museumhub.booking.dto.TourResponse;
import com.museumhub.booking.service.TourService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/tours")
@RequiredArgsConstructor
@Tag(name = "Itinerari")
public class TourController {

    private final TourService tourService;

    @GetMapping
    @Operation(summary = "Elenco itinerari con fasce orarie, posti liberi e prezzi per tariffa (data facoltativa: oggi)")
    public List<TourResponse> elenca(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return tourService.elenca(data);
    }

    @GetMapping("/{id}/seats")
    @Operation(summary = "Posti occupati di un itinerario in una data e fascia oraria (per la mappa 3D)")
    public PostiResponse posti(@PathVariable Long id,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
                               @RequestParam String orario) {
        return tourService.posti(id, data, orario);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "[Admin] Crea un itinerario con le sue fasce orarie")
    public TourResponse crea(@Valid @RequestBody TourRequest request) {
        return tourService.crea(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[Admin] Modifica un itinerario (dati, capienza, fasce orarie)")
    public TourResponse aggiorna(@PathVariable Long id, @Valid @RequestBody TourRequest request) {
        return tourService.aggiorna(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "[Admin] Elimina un itinerario senza prenotazioni")
    public void elimina(@PathVariable Long id) {
        tourService.elimina(id);
    }
}
