package com.museumhub.booking.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Itinerario di visita (es. Salone Monumentale). Il prezzo e' quello intero. */
@Entity
@Table(name = "tours")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tour {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String titolo;

    @Column(length = 500)
    private String descrizione;

    @Column(name = "durata_min", nullable = false)
    private Integer durataMin;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal prezzo;

    @Column(nullable = false)
    private Integer capienza;

    @OneToMany(mappedBy = "tour", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orario ASC")
    @Builder.Default
    private List<TourSlot> slots = new ArrayList<>();
}
