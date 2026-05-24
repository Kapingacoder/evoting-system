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

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminApiController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private CandidateTicketRepository candidateTicketRepository;

    @Autowired
    private ElectionRepository electionRepository;

    @Autowired
    private ElectionService electionService;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private SupportMessageRepository supportMessageRepository;

    private String getUsernameFromToken(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return jwtUtil.extractUsername(token);
    }

    // ═══════════════════════════════
    // WAPIGA KURA
    // ═══════════════════════════════

    // GET /api/admin/voters
    @GetMapping("/voters")
    public ResponseEntity<?> getVoters(
            @RequestHeader("Authorization") String authHeader) {
        try {
            List<User> voters = userRepository.findAllByRole(Role.VOTER);
            return ResponseEntity.ok(voters);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // POST /api/admin/voters/add
    @PostMapping("/voters/add")
    public ResponseEntity<?> addVoter(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> request) {
        try {
            userService.addVoter(
                request.get("fullName"),
                request.get("username"),
                request.get("admissionNumber"),
                request.get("email"),
                request.get("password")
            );
            return ResponseEntity.ok(Map.of("message", "Mpiga kura ameongezwa!"));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // DELETE /api/admin/voters/delete/{id}
    @DeleteMapping("/voters/delete/{id}")
    public ResponseEntity<?> deleteVoter(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        try {
            userRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "Mpiga kura amefutwa!"));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // POST /api/admin/voters/bulk-import
    @PostMapping("/voters/bulk-import")
    public ResponseEntity<?> bulkImportVoters(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody List<Map<String, String>> voters) {
        try {
            int count = 0;
            List<String> errors = new ArrayList<>();
            
            for (Map<String, String> voterData : voters) {
                try {
                    String fullName = voterData.get("fullName");
                    String admissionNumber = voterData.get("admissionNumber");
                    String email = voterData.getOrDefault("email", "");
                    
                    if (fullName == null || fullName.isEmpty() ||
                        admissionNumber == null || admissionNumber.isEmpty()) {
                        errors.add("Skipped: " + voterData);
                        continue;
                    }
                    
                    // Check kama tayari yupo
                    if (userRepository.existsByUsername(admissionNumber)) {
                        errors.add("Already exists: " + admissionNumber);
                        continue;
                    }
                    
                    userService.addVoter(fullName, admissionNumber, 
                                        admissionNumber, email, null);
                    count++;
                } catch (Exception e) {
                    errors.add("Error: " + e.getMessage());
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("imported", count);
            response.put("errors", errors);
            response.put("message", "Wapiga kura " + count + " wameingizwa!");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // ═══════════════════════════════
    // WAGOMBEA
    // ═══════════════════════════════

    // GET /api/admin/candidates
    @GetMapping("/candidates")
    public ResponseEntity<?> getCandidates(
            @RequestHeader("Authorization") String authHeader) {
        try {
            List<Candidate> candidates = candidateRepository.findAll();
            return ResponseEntity.ok(candidates);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // ═══════════════════════════════
    // TICKETS
    // ═══════════════════════════════

    // GET /api/admin/tickets
    @GetMapping("/tickets")
    public ResponseEntity<?> getTickets(
            @RequestHeader("Authorization") String authHeader) {
        try {
            List<CandidateTicket> tickets = candidateTicketRepository.findAll();
            return ResponseEntity.ok(tickets);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // POST /api/admin/tickets/add
    @PostMapping("/tickets/add")
    public ResponseEntity<?> addTicket(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> request) {
        try {
            CandidateTicket ticket = new CandidateTicket();
            ticket.setName((String) request.get("name"));
            ticket.setDescription((String) request.get("description"));
            ticket.setPresidentName((String) request.get("presidentName"));
            ticket.setPresidentParty((String) request.get("presidentParty"));
            ticket.setPresidentPhotoUrl((String) request.get("presidentPhotoUrl"));
            ticket.setVicePresidentName((String) request.get("vicePresidentName"));
            ticket.setVicePresidentParty((String) request.get("vicePresidentParty"));
            ticket.setVicePresidentPhotoUrl((String) request.get("vicePresidentPhotoUrl"));
            ticket.setVoteCount(0);
            ticket.setActive(request.get("isActive") != null ? (Boolean) request.get("isActive") : true);
            
            candidateTicketRepository.save(ticket);
            return ResponseEntity.ok(Map.of("message", "Ticket imeongezwa!"));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // PUT /api/admin/tickets/{id}
    @PutMapping("/tickets/{id}")
    public ResponseEntity<?> updateTicket(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        try {
            Optional<CandidateTicket> ticketOpt = candidateTicketRepository.findById(id);
            if (ticketOpt.isEmpty()) {
                return ResponseEntity.status(404)
                    .body(Map.of("error", "Ticket haipatikani"));
            }

            CandidateTicket ticket = ticketOpt.get();
            if (request.containsKey("name")) {
                ticket.setName((String) request.get("name"));
            }
            if (request.containsKey("description")) {
                ticket.setDescription((String) request.get("description"));
            }
            if (request.containsKey("presidentName")) {
                ticket.setPresidentName((String) request.get("presidentName"));
            }
            if (request.containsKey("presidentParty")) {
                ticket.setPresidentParty((String) request.get("presidentParty"));
            }
            if (request.containsKey("presidentPhotoUrl")) {
                ticket.setPresidentPhotoUrl((String) request.get("presidentPhotoUrl"));
            }
            if (request.containsKey("vicePresidentName")) {
                ticket.setVicePresidentName((String) request.get("vicePresidentName"));
            }
            if (request.containsKey("vicePresidentParty")) {
                ticket.setVicePresidentParty((String) request.get("vicePresidentParty"));
            }
            if (request.containsKey("vicePresidentPhotoUrl")) {
                ticket.setVicePresidentPhotoUrl((String) request.get("vicePresidentPhotoUrl"));
            }
            if (request.containsKey("isActive")) {
                ticket.setActive((Boolean) request.get("isActive"));
            }
            
            candidateTicketRepository.save(ticket);
            return ResponseEntity.ok(Map.of("message", "Ticket imesasishwa!"));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // DELETE /api/admin/tickets/{id}
    @DeleteMapping("/tickets/{id}")
    public ResponseEntity<?> deleteTicket(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        try {
            candidateTicketRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "Ticket imefutwa!"));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // ═══════════════════════════════
    // UCHAGUZI
    // ═══════════════════════════════

    // GET /api/admin/election
    @GetMapping("/election")
    public ResponseEntity<?> getElection(
            @RequestHeader("Authorization") String authHeader) {
        try {
            Election election = electionService.getElection();
            return ResponseEntity.ok(election);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // POST /api/admin/election/start
    @PostMapping("/election/start")
    public ResponseEntity<?> startElection(
            @RequestHeader("Authorization") String authHeader) {
        try {
            electionService.startElection();
            return ResponseEntity.ok(Map.of("message", "Uchaguzi umeanza!"));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // POST /api/admin/election/stop
    @PostMapping("/election/stop")
    public ResponseEntity<?> stopElection(
            @RequestHeader("Authorization") String authHeader) {
        try {
            electionService.stopElection();
            return ResponseEntity.ok(Map.of("message", "Uchaguzi umesimamishwa!"));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // PUT /api/admin/election/update
    @PutMapping("/election/update")
    public ResponseEntity<?> updateElection(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> request) {
        try {
            Election election = electionService.getElection();
            
            if (request.containsKey("name")) {
                election.setName((String) request.get("name"));
            }
            if (request.containsKey("description")) {
                election.setDescription((String) request.get("description"));
            }
            if (request.containsKey("startTime")) {
                String startTimeStr = (String) request.get("startTime");
                election.setStartTime(LocalDateTime.parse(startTimeStr));
            }
            if (request.containsKey("endTime")) {
                String endTimeStr = (String) request.get("endTime");
                election.setEndTime(LocalDateTime.parse(endTimeStr));
            }
            
            electionRepository.save(election);
            return ResponseEntity.ok(Map.of("message", "Uchaguzi imesasishwa!"));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // GET /api/admin/results
    @GetMapping("/results")
    public ResponseEntity<?> getResults(
            @RequestHeader("Authorization") String authHeader) {
        try {
            Map<String, Object> results = new HashMap<>();
            
            // Get all candidates with vote counts
            List<Candidate> candidates = candidateRepository.findAll();
            results.put("candidates", candidates);
            
            // Get all candidate tickets with vote counts
            List<CandidateTicket> tickets = candidateTicketRepository.findByIsActiveTrueOrderByVoteCountDesc();
            results.put("tickets", tickets);
            
            // Get election info
            Election election = electionService.getElection();
            results.put("election", election);
            
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // GET /api/admin/dashboard
    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard(
            @RequestHeader("Authorization") String authHeader) {
        try {
            Election election = electionService.getElection();
            List<User> voters = userRepository.findAllByRole(Role.VOTER);
            List<Candidate> candidates = candidateRepository.findAll();
            List<CandidateTicket> tickets = candidateTicketRepository.findByIsActiveTrueOrderByVoteCountDesc();

            Map<String, Object> response = new HashMap<>();
            response.put("totalVoters", voters.size());
            response.put("totalCandidates", candidates.size());
            response.put("totalTickets", tickets.size());
            response.put("electionActive", election != null && election.isActive());
            response.put("electionName", election != null ? election.getName() : "Hakuna");
            response.put("electionCompleted", election != null && election.isCompleted());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // GET /api/admin/profile
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(
            @RequestHeader("Authorization") String authHeader) {
        try {
            String username = getUsernameFromToken(authHeader);
            User user = userService.getUserByUsername(username);

            Map<String, Object> response = new HashMap<>();
            response.put("id", user.getId());
            response.put("username", user.getUsername());
            response.put("fullName", user.getFullName());
            response.put("email", user.getEmail());
            response.put("role", user.getRole());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // POST /api/admin/change-username
    @PostMapping("/change-username")
    public ResponseEntity<?> changeUsername(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> request) {
        try {
            String username = getUsernameFromToken(authHeader);
            String newUsername = request.get("newUsername");

            if (newUsername == null || newUsername.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "New username is required"));
            }

            userService.changeUsername(username, newUsername);
            return ResponseEntity.ok(Map.of("message", "Username changed successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }

    // POST /api/admin/change-password
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

    // ═══════════════════════════════
    // SUPPORT MESSAGES
    // ═══════════════════════════════

    // GET /api/admin/support-messages
    @GetMapping("/support-messages")
    public ResponseEntity<?> getSupportMessages(
            @RequestHeader("Authorization") String authHeader) {
        try {
            List<SupportMessage> messages = supportMessageRepository.findAllByOrderByCreatedAtDesc();
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // PUT /api/admin/support-messages/{id}/read
    @PutMapping("/support-messages/{id}/read")
    public ResponseEntity<?> markMessageAsRead(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        try {
            SupportMessage message = supportMessageRepository.findById(id).orElse(null);
            if (message == null) {
                return ResponseEntity.status(404)
                    .body(Map.of("error", "Ujumbe haupatikani"));
            }

            message.setRead(true);
            supportMessageRepository.save(message);
            return ResponseEntity.ok(Map.of("message", "Imesomwa!"));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // GET /api/admin/support-messages/unread-count
    @GetMapping("/support-messages/unread-count")
    public ResponseEntity<?> getUnreadMessageCount(
            @RequestHeader("Authorization") String authHeader) {
        try {
            long count = supportMessageRepository.countByIsReadFalse();
            return ResponseEntity.ok(Map.of("count", count));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(Map.of("error", e.getMessage()));
        }
    }
}
