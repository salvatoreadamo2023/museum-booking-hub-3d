package com.museumhub.booking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Tariffa uguale per tutti gli itinerari, espressa come sconto percentuale sul prezzo dell'itinerario
 * (es. "Intero" = 0, "Under 16" = 50).
 */
@Entity
@Table(name = "tariffe")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tariffa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String nome;

    @Column(name = "sconto_percentuale", nullable = false)
    private Integer scontoPercentuale;

    @Column(nullable = false)
    @Builder.Default
    private Boolean predefinita = false;
}
