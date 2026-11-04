package com.museumhub.booking.repository;

import com.museumhub.booking.entity.ReservationSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface ReservationSeatRepository extends JpaRepository<ReservationSeat, Long> {

    /** Numeri dei posti gia' occupati per un itinerario, una data e una fascia oraria. */
    @Query("select s.numeroPosto from ReservationSeat s "
            + "where s.tour.id = :tourId and s.dataVisita = :data and s.orario = :orario "
            + "order by s.numeroPosto")
    List<Integer> findPostiOccupati(@Param("tourId") Long tourId,
                                    @Param("data") LocalDate data,
                                    @Param("orario") LocalTime orario);

    /** Posti occupati in una data,  per fascia oraria */
    @Query("select s.orario, count(s) from ReservationSeat s "
            + "where s.tour.id = :tourId and s.dataVisita = :data group by s.orario")
    List<Object[]> contaPostiPerOrario(@Param("tourId") Long tourId, @Param("data") LocalDate data);

    /** Esistono posti prenotati in una fascia oraria da una certa data in poi? */
    boolean existsByTourIdAndOrarioAndDataVisitaGreaterThanEqual(Long tourId, LocalTime orario, LocalDate data);

    /** Numero del posto piu' alto prenotato da una certa data in poi . */
    @Query("select max(s.numeroPosto) from ReservationSeat s "
            + "where s.tour.id = :tourId and s.dataVisita >= :da")
    Integer maxPostoPrenotato(@Param("tourId") Long tourId, @Param("da") LocalDate da);

    /** Righe per le statistiche di ogni posto. */
    @Query("select s.dataVisita, s.orario, s.tour.id, s.prezzoApplicato from ReservationSeat s "
            + "where s.dataVisita between :da and :a")
    List<Object[]> righeStatistiche(@Param("da") LocalDate da, @Param("a") LocalDate a);
}
