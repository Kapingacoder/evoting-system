package com.evoting.system.service;

import com.evoting.system.model.CandidateTicket;
import com.evoting.system.repository.CandidateTicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CandidateTicketService {

    private final CandidateTicketRepository ticketRepository;

    public CandidateTicketService(CandidateTicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    public List<CandidateTicket> getAllTickets() {
        return ticketRepository.findAll();
    }

    public List<CandidateTicket> getActiveTickets() {
        return ticketRepository.findByIsActiveTrueOrderByVoteCountDesc();
    }

    public Optional<CandidateTicket> getTicketById(Long id) {
        return ticketRepository.findById(id);
    }

    @Transactional
    public CandidateTicket saveTicket(CandidateTicket ticket) {
        return ticketRepository.save(ticket);
    }

    @Transactional
    public void deleteTicket(Long id) {
        ticketRepository.deleteById(id);
    }

    @Transactional
    public CandidateTicket incrementVoteCount(Long id) {
        Optional<CandidateTicket> ticketOpt = ticketRepository.findById(id);
        if (ticketOpt.isPresent()) {
            CandidateTicket ticket = ticketOpt.get();
            ticket.incrementVoteCount();
            return ticketRepository.save(ticket);
        }
        return null;
    }

    @Transactional
    public CandidateTicket deactivateTicket(Long id) {
        Optional<CandidateTicket> ticketOpt = ticketRepository.findById(id);
        if (ticketOpt.isPresent()) {
            CandidateTicket ticket = ticketOpt.get();
            ticket.setActive(false);
            return ticketRepository.save(ticket);
        }
        return null;
    }

    @Transactional
    public CandidateTicket activateTicket(Long id) {
        Optional<CandidateTicket> ticketOpt = ticketRepository.findById(id);
        if (ticketOpt.isPresent()) {
            CandidateTicket ticket = ticketOpt.get();
            ticket.setActive(true);
            return ticketRepository.save(ticket);
        }
        return null;
    }

    @Transactional
    public void resetAllVotes() {
        List<CandidateTicket> tickets = ticketRepository.findAll();
        for (CandidateTicket ticket : tickets) {
            ticket.setVoteCount(0);
            ticketRepository.save(ticket);
        }
    }
}

