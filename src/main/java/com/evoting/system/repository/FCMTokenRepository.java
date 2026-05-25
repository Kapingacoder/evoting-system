package com.evoting.system.repository;

import com.evoting.system.model.FCMToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FCMTokenRepository extends JpaRepository<FCMToken, Long> {
    List<FCMToken> findAll();
    List<FCMToken> findByRole(String role);
    Optional<FCMToken> findByUsername(String username);
    void deleteByUsername(String username);
}
