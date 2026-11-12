package com.museumhub.booking.controller;

import com.museumhub.booking.dto.SistemaResponse;
import com.museumhub.booking.service.SistemaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/sistema")
@RequiredArgsConstructor
@Tag(name = "Sistema")
public class SistemaController {

    private final SistemaService sistemaService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Stato del backend: CPU, memoria, thread e tempi di risposta (admin)")
    public SistemaResponse rileva() {
        return sistemaService.rileva();
    }
}
