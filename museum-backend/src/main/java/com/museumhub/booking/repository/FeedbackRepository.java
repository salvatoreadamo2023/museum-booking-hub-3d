package com.museumhub.booking.repository;

import com.museumhub.booking.entity.Feedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    /** Tutti i feedback */
    @EntityGraph(attributePaths = {"user", "tour"})
    Page<Feedback> findAllBy(Pageable pageable);

    boolean existsByUserId(Long userId);

    long countByUserIdAndTourId(Long userId, Long tourId);
}
