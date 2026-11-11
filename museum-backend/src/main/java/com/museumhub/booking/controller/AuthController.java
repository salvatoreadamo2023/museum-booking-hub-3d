package com.museumhub.booking.controller;

import com.museumhub.booking.dto.AuthResponse;
import com.museumhub.booking.dto.LoginRequest;
import com.museumhub.booking.dto.RegisterRequest;
import com.museumhub.booking.exception.ApiException;
import com.museumhub.booking.security.CookieService;
import com.museumhub.booking.security.UserPrincipal;
import com.museumhub.booking.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticazione")
@SecurityRequirements
public class AuthController {

    private final AuthService authService;
    private final CookieService cookies;

    @PostMapping("/login")
    @Operation(summary = "Accesso con email e password; imposta i cookie HttpOnly di sessione")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest http,
                              HttpServletResponse response) {
        AuthService.Esito esito = authService.login(request, http.getRemoteAddr());
        impostaCookie(response, esito);
        return esito.utente();
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrazione di un nuovo visitatore; imposta i cookie HttpOnly di sessione")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request, HttpServletResponse response) {
        AuthService.Esito esito = authService.register(request);
        impostaCookie(response, esito);
        return esito.utente();
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rinnova il token di accesso usando il cookie di refresh")
    public AuthResponse refresh(HttpServletRequest http, HttpServletResponse response) {
        try {
            AuthService.Esito esito = authService.refresh(cookies.leggi(http, CookieService.REFRESH));
            cookies.impostaAccesso(response, esito.accesso());
            return esito.utente();
        } catch (ApiException e) {
            cookies.cancella(response);
            throw e;
        }
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Esce: revoca la sessione e cancella i cookie")
    public void logout(HttpServletRequest http, HttpServletResponse response) {
        authService.logout(cookies.leggi(http, CookieService.ACCESSO), cookies.leggi(http, CookieService.REFRESH));
        cookies.cancella(response);
    }

    @GetMapping("/me")
    @Operation(summary = "Utente della sessione corrente (serve al frontend per ripristinare la sessione)")
    public AuthResponse me(@Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Autenticazione richiesta o token non valido");
        }
        return AuthResponse.of(principal.getUser());
    }

    private void impostaCookie(HttpServletResponse response, AuthService.Esito esito) {
        cookies.impostaAccesso(response, esito.accesso());
        cookies.impostaRefresh(response, esito.refresh());
    }
}
