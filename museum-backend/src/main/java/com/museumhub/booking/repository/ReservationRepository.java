package com.museumhub.booking.repository;

import com.museumhub.booking.entity.Reservation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    Optional<Reservation> findByCodice(String codice);

    boolean existsByCodice(String codice);

    List<Reservation> findByUserIdOrderByDataVisitaDescOrarioDesc(Long userId);

    /** Registro completo, una pagina alla volta. */
    @EntityGraph(attributePaths = {"user", "tour"})
    Page<Reservation> findAllBy(Pageable pageable);

    boolean existsByTourId(Long tourId);

    boolean existsByUserId(Long userId);

    /** Visite confermate gia' iniziate di un utente per un itinerario, inoltre si può a lasciare un feedback. */
    @Query("select count(r) from Reservation r where r.user.id = :userId and r.tour.id = :tourId "
            + "and r.stato = com.museumhub.booking.entity.StatoPrenotazione.CONFERMATA "
            + "and (r.dataVisita < :oggi or (r.dataVisita = :oggi and r.orario < :ora))")
    long contaVisiteConcluse(@Param("userId") Long userId, @Param("tourId") Long tourId,
                             @Param("oggi") LocalDate oggi, @Param("ora") LocalTime ora);
}
