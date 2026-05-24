package com.evoting.system.controller;

import com.evoting.system.model.*;
import com.evoting.system.repository.*;
import com.evoting.system.service.ElectionService;
import com.evoting.system.service.UserService;
import com.evoting.system.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/voter")
@CrossOrigin(origins = "*")
public class VoterApiController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private CandidateTicketRepository candidateTicketRepository;

    @Autowired
    private VoteRepository voteRepository;

    @Autowired
    private ElectionRepository electionRepository;

    @Autowired
    private ElectionService electionService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserService userService;

    // Helper kupata username kutoka token
    private String getUsernameFromToken(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return jwtUtil.extractUsername(token);
    }

    // GET /api/voter/dashboard
    @GetMapping("/dashboard")
    public ResponseEntity<?> dashboard(@RequestHeader("Authorization") String authHeader) {
        try {
            String username = getUsernameFromToken(authHeader);
            Optional<User> userOpt = userRepository.findByUsername(username);

            if (userOpt.isEmpty()) {
                return ResponseEntity.status(401).body(Map.of("error", "User not found"));
            }

            User user = userOpt.get();
            boolean hasVoted = user.isVoted();

            // Get the most recent election
            Election mostRecentElection = electionRepository.findFirstByOrderByIdDesc().orElse(null);

            // Get all candidate tickets (new system)
            List<CandidateTicket> activeTickets = candidateTicketRepository.findByIsActiveTrueOrderByVoteCountDesc();

            // Get all candidates (old system for backward compatibility)
            List<Candidate> allCandidates = candidateRepository.findAll();

            // Group candidates by position
            Map<String, List<Map<String, Object>>> candidatesByPosition = new HashMap<>();
            if (allCandidates != null && !allCandidates.isEmpty()) {
                candidatesByPosition = allCandidates.stream()
                    .collect(Collectors.groupingBy(
                        Candidate::getPosition,
                        LinkedHashMap::new,
                        Collectors.mapping(
                            c -> {
                                Map<String, Object> candidateMap = new HashMap<>();
                                candidateMap.put("id", c.getId());
                                candidateMap.put("name", c.getName());
                                candidateMap.put("position", c.getPosition());
                                candidateMap.put("voteCount", c.getVoteCount());
                                return candidateMap;
                            },
                            Collectors.toList()
                        )
                    ));
            }

            // Check if election is active and voting is open
            boolean isVotingOpen = false;
            if (mostRecentElection != null) {
                LocalDateTime now = LocalDateTime.now();
                isVotingOpen = mostRecentElection.isActive() && !mostRecentElection.isCompleted() &&
                              mostRecentElection.getStartTime() != null && mostRecentElection.getEndTime() != null &&
                              now.isAfter(mostRecentElection.getStartTime()) && now.isBefore(mostRecentElection.getEndTime());
            }

            // Prepare tickets data
            List<Map<String, Object>> ticketsData = new ArrayList<>();
            if (activeTickets != null && !activeTickets.isEmpty()) {
                ticketsData = activeTickets.stream()
                    .map(ticket -> {
                        Map<String, Object> ticketMap = new HashMap<>();
                        ticketMap.put("id", ticket.getId());
                        ticketMap.put("name", ticket.getName());
                        ticketMap.put("description", ticket.getDescription());
                        ticketMap.put("presidentName", ticket.getPresidentName());
                        ticketMap.put("presidentParty", ticket.getPresidentParty());
                        ticketMap.put("presidentPhotoUrl", ticket.getPresidentPhotoUrl());
                        ticketMap.put("vicePresidentName", ticket.getVicePresidentName());
                        ticketMap.put("vicePresidentParty", ticket.getVicePresidentParty());
                        ticketMap.put("vicePresidentPhotoUrl", ticket.getVicePresidentPhotoUrl());
                        ticketMap.put("voteCount", ticket.getVoteCount());
                        ticketMap.put("isActive", ticket.isActive());
                        return ticketMap;
                    })
                    .collect(Collectors.toList());
            }

            Map<String, Object> response = new HashMap<>();
            
            Map<String, Object> userData = new HashMap<>();
            userData.put("fullName", user.getFullName() != null ? user.getFullName() : "Voter");
            userData.put("username", user.getUsername());
            userData.put("admissionNumber", user.getAdmissionNumber());
            userData.put("email", user.getEmail());
            
            response.put("user", userData);
            response.put("hasVoted", hasVoted);
            response.put("tickets", ticketsData);
            response.put("candidatesByPosition", candidatesByPosition);
            response.put("mostRecentElection", mostRecentElection);
            response.put("isVotingOpen", isVotingOpen);
            response.put("currentTime", LocalDateTime.now());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                "error", "Internal server error",
                "exceptionType", e.getClass().getSimpleName(),
                "message", e.getMessage() != null ? e.getMessage() : "No error message available"
            ));
        }
    }

    // GET /api/voter/results
    @GetMapping("/results")
    public ResponseEntity<?> results(@RequestHeader("Authorization") String authHeader) {
        // Get the most recent election
        Election election = electionRepository.findFirstByOrderByIdDesc().orElse(null);

        if (election == null) {
            return ResponseEntity.ok(Map.of("message", "No election results available yet."));
        }

        // Get all candidate tickets
        List<CandidateTicket> tickets = candidateTicketRepository.findByIsActiveTrueOrderByVoteCountDesc();

        // Get all candidates and group by position
        List<Candidate> allCandidates = candidateRepository.findAll();
        Map<String, List<Map<String, Object>>> candidatesByPosition = allCandidates.stream()
            .sorted(Comparator.comparing(Candidate::getVoteCount).reversed())
            .collect(Collectors.groupingBy(
                Candidate::getPosition,
                LinkedHashMap::new,
                Collectors.mapping(
                    c -> {
                        Map<String, Object> candidateMap = new HashMap<>();
                        candidateMap.put("id", c.getId());
                        candidateMap.put("name", c.getName());
                        candidateMap.put("position", c.getPosition());
                        candidateMap.put("voteCount", c.getVoteCount());
                        return candidateMap;
                    },
                    Collectors.toList()
                )
            ));

        // Calculate total votes
        int totalTicketVotes = tickets.stream().mapToInt(CandidateTicket::getVoteCount).sum();
        int totalCandidateVotes = allCandidates.stream().mapToInt(Candidate::getVoteCount).sum();
        int totalVotes = totalTicketVotes + totalCandidateVotes;

        // Get current time for checking if election is active
        LocalDateTime now = LocalDateTime.now();

        // Determine winning ticket
        CandidateTicket winningTicket = null;
        if (!tickets.isEmpty()) {
            winningTicket = tickets.get(0);
        }

        // Prepare tickets data
        List<Map<String, Object>> ticketsData = tickets.stream()
            .map(ticket -> {
                Map<String, Object> ticketMap = new HashMap<>();
                ticketMap.put("id", ticket.getId());
                ticketMap.put("name", ticket.getName());
                ticketMap.put("presidentName", ticket.getPresidentName());
                ticketMap.put("vicePresidentName", ticket.getVicePresidentName());
                ticketMap.put("voteCount", ticket.getVoteCount());
                ticketMap.put("percentage", totalTicketVotes > 0 ? 
                    Math.round(ticket.getVoteCount() * 100.0 / totalTicketVotes) : 0);
                return ticketMap;
            })
            .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("election", election);
        response.put("tickets", ticketsData);
        response.put("winningTicket", winningTicket);
        response.put("candidatesByPosition", candidatesByPosition);
        response.put("totalVotes", totalVotes);
        response.put("now", now);

        return ResponseEntity.ok(response);
    }

    // POST /api/voter/vote/ticket
    @PostMapping("/vote/ticket")
    public ResponseEntity<?> voteTicket(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Long> request) {
        String username = getUsernameFromToken(authHeader);

        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "User not found"));
        }

        User user = userOpt.get();

        // Check if user has already voted
        if (user.isVoted()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Umeshapiga kura tayari"));
        }

        // Get the most recent election
        Election election = electionRepository.findFirstByOrderByIdDesc().orElse(null);

        if (election == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "No election found"));
        }

        // Check if voting window is valid
        LocalDateTime now = LocalDateTime.now();

        if (election.getStartTime() == null || election.getEndTime() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Election timing not set properly"));
        }

        if (now.isBefore(election.getStartTime())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Voting has not started yet"));
        }

        if (now.isAfter(election.getEndTime())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Voting has ended"));
        }

        // Get the ticket
        Long ticketId = request.get("ticketId");
        Optional<CandidateTicket> ticketOpt = candidateTicketRepository.findById(ticketId);

        if (ticketOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Ticket not found"));
        }

        try {
            CandidateTicket ticket = ticketOpt.get();

            // Increment ticket vote count
            ticket.incrementVoteCount();
            candidateTicketRepository.save(ticket);

            // Update user's voted status
            user.setVoted(true);
            userRepository.save(user);

            return ResponseEntity.ok(Map.of("message", 
                "Your vote for " + ticket.getPresidentName() + " (President) and " + 
                ticket.getVicePresidentName() + " (Vice President) has been cast successfully!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "An error occurred while processing your vote: " + e.getMessage()));
        }
    }

    // GET /api/voter/profile
    @GetMapping("/profile")
    public ResponseEntity<?> profile(@RequestHeader("Authorization") String authHeader) {
        String username = getUsernameFromToken(authHeader);
        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "User not found"));
        }

        User user = userOpt.get();

        // Get the most recent election
        Election election = electionRepository.findFirstByOrderByIdDesc().orElse(null);

        Map<String, Object> response = new HashMap<>();
        response.put("fullName", user.getFullName());
        response.put("username", user.getUsername());
        response.put("admissionNumber", user.getAdmissionNumber());
        response.put("email", user.getEmail());
        response.put("profileImage", user.getProfileImage());
        response.put("hasVoted", user.isVoted());
        response.put("election", election);

        return ResponseEntity.ok(response);
    }

    // POST /api/voter/change-password
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> request) {
        try {
            String username = getUsernameFromToken(authHeader);
            String currentPassword = request.get("currentPassword");
            String newPassword = request.get("newPassword");

            if (currentPassword == null || newPassword == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Current password and new password are required"));
            }

            userService.changePassword(username, currentPassword, newPassword);
            return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }
}
