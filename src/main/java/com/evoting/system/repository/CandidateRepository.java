package com.evoting.system.repository;

import com.evoting.system.model.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CandidateRepository extends JpaRepository<Candidate, Long> {
    
    // Find all active candidates
    List<Candidate> findByIsActiveTrue();
    
    // Find candidates by position
    List<Candidate> findByPosition(String position);
    
    // Find active candidates by position
    List<Candidate> findByPositionAndIsActiveTrue(String position);
    
    // Find candidates by party
    List<Candidate> findByParty(String party);
    
    // Find candidate by name (case insensitive)
    Optional<Candidate> findByNameIgnoreCase(String name);
    
    // Count all active candidates
    long countByIsActiveTrue();
    
    // Get vote statistics by position
    @Query("SELECT c.position, COUNT(c), SUM(c.voteCount) FROM Candidate c WHERE c.isActive = true GROUP BY c.position")
    List<Object[]> getVoteStatisticsByPosition();
    
    // Find top candidates by vote count
    List<Candidate> findTop5ByOrderByVoteCountDesc();
    
    // Find active candidates ordered by position and vote count (descending)
    @Query("SELECT c FROM Candidate c WHERE c.isActive = true ORDER BY c.position ASC, c.voteCount DESC")
    List<Candidate> findByIsActiveTrueOrderByPositionAscVoteCountDesc();
    
    // Find active candidates ordered by vote count (descending)
    List<Candidate> findByIsActiveTrueOrderByVoteCountDesc();
}