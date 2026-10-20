package com.museumhub.booking.repository;

import com.museumhub.booking.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    Optional<Reservation> findByCodice(String codice);

    boolean existsByCodice(String codice);

    List<Reservation> findByUserIdOrderByDataVisitaDescOrarioDesc(Long userId);

    List<Reservation> findAllByOrderByDataVisitaDescOrarioDesc();
}
