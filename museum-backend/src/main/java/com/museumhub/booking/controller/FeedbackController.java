package com.museumhub.booking.controller;

import com.museumhub.booking.dto.FeedbackRequest;
import com.museumhub.booking.dto.FeedbackResponse;
import com.museumhub.booking.security.UserPrincipal;
import com.museumhub.booking.service.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
@Tag(name = "Feedback")
public class FeedbackController {

    private final FeedbackService feedbackService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[Admin] Feedback ricevuti, dal piu' recente")
    public List<FeedbackResponse> elenca() {
        return feedbackService.elenca();
    }

    @PostMapping
    @PreAuthorize("hasRole('VISITATORE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "[Visitatore] Lascia un feedback dopo una visita completata")
    public FeedbackResponse crea(@Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
                                 @Valid @RequestBody FeedbackRequest request) {
        return feedbackService.crea(principal.getUser(), request);
    }
}
