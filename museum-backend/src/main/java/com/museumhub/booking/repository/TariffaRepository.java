package com.museumhub.booking.repository;

import com.museumhub.booking.entity.Tariffa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TariffaRepository extends JpaRepository<Tariffa, Long> {

    Optional<Tariffa> findByPredefinitaTrue();
}
