package com.museumhub.booking.controller;

import com.museumhub.booking.dto.StatsResponse;
import com.museumhub.booking.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Statistiche (admin)")
public class StatsController {

    private final StatsService statsService;

    @GetMapping
    @Operation(summary = "[Admin] Indicatori e grafici per periodo: oggi, 7giorni (default) o mese (ultimi 30 giorni)")
    public StatsResponse stats(@RequestParam(defaultValue = "7giorni") String periodo) {
        return statsService.calcola(periodo);
    }
}
