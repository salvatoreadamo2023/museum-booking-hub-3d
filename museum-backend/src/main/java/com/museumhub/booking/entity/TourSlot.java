package com.museumhub.booking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

/** Fascia oraria di un itinerario. */
@Entity
@Table(name = "tour_slots",
        uniqueConstraints = @UniqueConstraint(name = "uk_slot_tour_orario", columnNames = {"tour_id", "orario"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TourSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tour_id", nullable = false)
    private Tour tour;

    @Column(nullable = false)
    private LocalTime orario;

    @Column(nullable = false)
    @Builder.Default
    private Boolean attivo = true;
}
