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
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Valutazione (1-5 stelle) e commento lasciati da un visitatore dopo la visita.
 * Ogni feedback e' legato alla visita (prenotazione) a cui si riferisce, con un vincolo UNIQUE: anche con due
 * richieste nello stesso istante il database accetta un solo feedback per visita.
 * I feedback salvati prima di questo campo hanno la prenotazione vuota (NULL), che il vincolo ammette.
 */
@Entity
@Table(name = "feedback",
        uniqueConstraints = @UniqueConstraint(name = Feedback.VINCOLO_PRENOTAZIONE, columnNames = "reservation_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Feedback {

    public static final String VINCOLO_PRENOTAZIONE = "uk_feedback_prenotazione";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tour_id", nullable = false)
    private Tour tour;

    // ManyToOne e non OneToOne: con OneToOne Hibernate aggiungerebbe un secondo vincolo UNIQUE con un nome casuale,
    // mentre qui serve un solo vincolo con un nome noto (VINCOLO_PRENOTAZIONE) da riconoscere negli errori
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id")
    private Reservation reservation;

    @Column(nullable = false)
    private Integer voto;

    @Column(nullable = false, length = 1000)
    private String commento;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
