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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Posto prenotato (un "omino" nella mappa 3D). Itinerario, data e orario sono ripetuti rispetto
 * alla prenotazione per poter imporre al database il vincolo di unicita' del posto: due
 * prenotazioni non possono occupare lo stesso posto nella stessa fascia.
 * Il prezzo applicato e' salvato qui, cosi' una modifica successiva del prezzo non cambia lo storico.
 */
@Entity
@Table(name = "reservation_seats",
        uniqueConstraints = @UniqueConstraint(name = "uk_seat_slot_posto",
                columnNames = {"tour_id", "data_visita", "orario", "numero_posto"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tour_id", nullable = false)
    private Tour tour;

    @Column(name = "data_visita", nullable = false)
    private LocalDate dataVisita;

    @Column(nullable = false)
    private LocalTime orario;

    @Column(name = "numero_posto", nullable = false)
    private Integer numeroPosto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tariffa_id", nullable = false)
    private Tariffa tariffa;

    @Column(name = "prezzo_applicato", nullable = false, precision = 8, scale = 2)
    private BigDecimal prezzoApplicato;
}
