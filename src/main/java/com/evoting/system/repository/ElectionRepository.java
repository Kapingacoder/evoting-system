package com.evoting.system.repository;

import com.evoting.system.model.Election;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface ElectionRepository extends JpaRepository<Election, Long> {
    Optional<Election> findFirstByOrderByIdAsc();
    Optional<Election> findFirstByOrderByIdDesc();
    List<Election> findAllByOrderByStartTimeDesc();
    Optional<Election> findFirstByActiveTrue();
    List<Election> findByActiveTrue();
    List<Election> findByActiveFalseAndCompletedFalse();
}
