package com.museumhub.booking.controller;

import com.museumhub.booking.dto.PaginaResponse;
import com.museumhub.booking.dto.UserCreateRequest;
import com.museumhub.booking.dto.UserResponse;
import com.museumhub.booking.dto.UserUpdateRequest;
import com.museumhub.booking.security.UserPrincipal;
import com.museumhub.booking.service.UserService;
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


@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Utenti (admin)")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "[Admin] Elenco degli utenti, paginato. Pagina da 0; dimensione da 1 a 100")
    public PaginaResponse<UserResponse> elenca(@RequestParam(defaultValue = "0") int pagina,
                                               @RequestParam(defaultValue = "20") int dimensione) {
        return userService.elenca(pagina, dimensione);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "[Admin] Crea un utente scegliendo ruolo (e stato, ATTIVO se omesso)")
    public UserResponse crea(@Valid @RequestBody UserCreateRequest request) {
        return userService.crea(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "[Admin] Modifica nome, email, password, ruolo e stato di un utente")
    public UserResponse aggiorna(@Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
                                 @PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        return userService.aggiorna(principal.getUser().getId(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "[Admin] Elimina un utente senza prenotazioni ne' feedback")
    public void elimina(@Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
                        @PathVariable Long id) {
        userService.elimina(principal.getUser().getId(), id);
    }
}
