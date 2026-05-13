package com.evoting.system.repository;

import com.evoting.system.model.CandidateTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CandidateTicketRepository extends JpaRepository<CandidateTicket, Long> {
    
    List<CandidateTicket> findAllByOrderByVoteCountDesc();
    
    List<CandidateTicket> findByIsActiveTrueOrderByVoteCountDesc();
    
    List<CandidateTicket> findByIsActiveTrue();
}

