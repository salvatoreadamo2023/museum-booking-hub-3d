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
}
