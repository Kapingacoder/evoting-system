package com.evoting.system.controller;

import com.evoting.system.model.*;
import com.evoting.system.repository.*;
import com.evoting.system.service.CandidateTicketService;
import com.evoting.system.repository.CandidateTicketRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/voter")
public class VoterController {

    private final UserRepository userRepository;
    private final VoteRepository voteRepository;
    private final CandidateRepository candidateRepository;
    private final ElectionRepository electionRepository;
    private final CandidateTicketRepository candidateTicketRepository;
    private final PasswordEncoder passwordEncoder;

    public VoterController(UserRepository userRepository, 
                         VoteRepository voteRepository,
                         CandidateRepository candidateRepository,
                         ElectionRepository electionRepository,
                         CandidateTicketRepository candidateTicketRepository,
                         PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.voteRepository = voteRepository;
        this.candidateRepository = candidateRepository;
        this.electionRepository = electionRepository;
        this.candidateTicketRepository = candidateTicketRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/dashboard")
    public String voterDashboard(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        Optional<User> userOpt = userRepository.findByUsername(username);
        
        if (userOpt.isEmpty()) {
            return "redirect:/login";
        }

        User user = userOpt.get();
        boolean hasVoted = user.isVoted();
        
        // Add user's full name to the model
        model.addAttribute("fullName", user.getFullName() != null ? user.getFullName() : "Voter");
        
        // Get the most recent election (regardless of active status)
        Election mostRecentElection = electionRepository.findFirstByOrderByIdDesc().orElse(null);
        
        // Get all active elections
        List<Election> activeElections = electionRepository.findByActiveTrue();
        
        // Get all candidate tickets (new system)
        List<CandidateTicket> activeTickets = candidateTicketRepository.findByIsActiveTrueOrderByVoteCountDesc();
        
        // Get all candidates (old system for backward compatibility)
        List<Candidate> allCandidates = candidateRepository.findAll();
        
        // Group candidates by position
        Map<String, List<Candidate>> candidatesByPosition = allCandidates.stream()
            .collect(Collectors.groupingBy(Candidate::getPosition));
        
        // Check if election is active and voting is open
        boolean isVotingOpen = false;
        if (mostRecentElection != null) {
            LocalDateTime now = LocalDateTime.now();
            isVotingOpen = mostRecentElection.isActive() && !mostRecentElection.isCompleted() &&
                          mostRecentElection.getStartTime() != null && mostRecentElection.getEndTime() != null &&
                          now.isAfter(mostRecentElection.getStartTime()) && now.isBefore(mostRecentElection.getEndTime());
        }
        
        model.addAttribute("user", user);
        model.addAttribute("hasVoted", hasVoted);
        model.addAttribute("tickets", activeTickets);
        model.addAttribute("candidates", allCandidates);
        model.addAttribute("candidatesByPosition", candidatesByPosition);
        model.addAttribute("activeElections", activeElections);
        model.addAttribute("mostRecentElection", mostRecentElection);
        model.addAttribute("isVotingOpen", isVotingOpen);
        model.addAttribute("currentTime", LocalDateTime.now());
        
        return "voter-dashboard";
    }

    @PostMapping("/vote")
    public String castVote(@RequestParam(required = false) Map<String, String> allParams,
                          RedirectAttributes redirectAttributes) {
        System.out.println("=== VOTING DEBUG ===");
        System.out.println("Received parameters: " + allParams);
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        System.out.println("Username: " + username);
        
        Optional<User> userOpt = userRepository.findByUsername(username);
        
        if (userOpt.isEmpty()) {
            System.out.println("User not found!");
            return "redirect:/login";
        }

        User user = userOpt.get();
        System.out.println("User found: " + user.getUsername() + ", voted: " + user.isVoted());
        
        // Check if user has already voted
        if (user.isVoted()) {
            redirectAttributes.addFlashAttribute("error", "You have already voted in this election!");
            return "redirect:/voter/dashboard";
        }
        
        // Get the most recent election (regardless of active status)
        Election election = electionRepository.findFirstByOrderByIdDesc().orElse(null);
        System.out.println("Election found: " + (election != null ? election.getName() : "null"));
        
        if (election == null) {
            redirectAttributes.addFlashAttribute("error", "No election found!");
            return "redirect:/voter/dashboard";
        }
        
        // Check if voting window is valid (within start and end time)
        LocalDateTime now = LocalDateTime.now();
        System.out.println("Now: " + now);
        System.out.println("Election start: " + election.getStartTime());
        System.out.println("Election end: " + election.getEndTime());
        System.out.println("Active: " + election.isActive());
        System.out.println("Completed: " + election.isCompleted());
        
        if (election.getStartTime() == null || election.getEndTime() == null) {
            redirectAttributes.addFlashAttribute("error", "Election timing not set properly!");
            return "redirect:/voter/dashboard";
        }
        
        if (now.isBefore(election.getStartTime())) {
            redirectAttributes.addFlashAttribute("error", "Voting has not started yet!");
            return "redirect:/voter/dashboard";
        }
        
        if (now.isAfter(election.getEndTime())) {
            redirectAttributes.addFlashAttribute("error", "Voting has ended!");
            return "redirect:/voter/dashboard";
        }
        
        // Get all unique positions
        List<Candidate> allCandidates = candidateRepository.findAll();
        System.out.println("Total candidates: " + allCandidates.size());
        Set<String> positions = allCandidates.stream()
            .map(Candidate::getPosition)
            .collect(Collectors.toSet());
        System.out.println("Positions: " + positions);
        
        // Collect selected candidates from request parameters
        Map<String, Long> selectedCandidates = new HashMap<>();
        for (String position : positions) {
            // Try both with and without underscores (handle spaces in position names)
            String paramName = "candidate_" + position;
            String paramNameUnderscore = paramName.replace(" ", "_");
            String candidateIdStr = allParams.get(paramName);
            if (candidateIdStr == null) {
                candidateIdStr = allParams.get(paramNameUnderscore);
            }
            System.out.println("Position '" + position + "' -> param '" + paramName + "' = " + candidateIdStr);
            
            if (candidateIdStr != null && !candidateIdStr.isEmpty()) {
                try {
                    Long candidateId = Long.parseLong(candidateIdStr);
                    Optional<Candidate> candidateOpt = candidateRepository.findById(candidateId);
                    if (candidateOpt.isPresent() && 
                        candidateOpt.get().getPosition().equals(position)) {
                        selectedCandidates.put(position, candidateId);
                        System.out.println("  Selected: " + candidateOpt.get().getName());
                    }
                } catch (NumberFormatException e) {
                    System.out.println("  Invalid candidate ID format: " + candidateIdStr);
                }
            }
        }
        
        System.out.println("Selected positions: " + selectedCandidates.size() + " of " + positions.size());
        
        // Validate that all positions are selected
        if (selectedCandidates.size() != positions.size()) {
            redirectAttributes.addFlashAttribute("error", 
                "Please select a candidate for ALL positions before submitting your vote!");
            return "redirect:/voter/dashboard";
        }
        
        try {
            // Cast vote for each position
            int voteCount = 0;
            for (Map.Entry<String, Long> entry : selectedCandidates.entrySet()) {
                Long candidateId = entry.getValue();
                Optional<Candidate> candidateOpt = candidateRepository.findById(candidateId);
                
                if (candidateOpt.isPresent()) {
                    Candidate candidate = candidateOpt.get();
                    System.out.println("Recording vote for: " + candidate.getName() + " (" + candidate.getPosition() + ")");
                    
                    // Create and save vote
                    Vote vote = new Vote();
                    vote.setUser(user);
                    vote.setCandidate(candidate);
                    vote.setTimestamp(LocalDateTime.now());
                    voteRepository.save(vote);
                    System.out.println("Vote saved with ID: " + vote.getId());
                    
                    // Increment candidate's vote count
                    candidate.incrementVoteCount();
                    candidateRepository.save(candidate);
                    System.out.println("Candidate vote count updated to: " + candidate.getVoteCount());
                    
                    voteCount++;
                }
            }
            
            // Update user's voted status
            user.setVoted(true);
            userRepository.save(user);
            System.out.println("User marked as voted");
            
            redirectAttributes.addFlashAttribute("success", 
                String.format("Your votes for %d position(s) have been cast successfully!", voteCount));
            System.out.println("=== VOTING SUCCESSFUL ===");
        } catch (Exception e) {
            System.out.println("ERROR during voting: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", 
                "An error occurred while processing your votes: " + e.getMessage());
        }
        
        return "redirect:/voter/dashboard";
    }
    
    @GetMapping("/profile")
    public String viewProfile(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        Optional<User> userOpt = userRepository.findByUsername(username);
        
        if (userOpt.isEmpty()) {
            return "redirect:/login";
        }
        
        User user = userOpt.get();
        model.addAttribute("user", user);
        
        // Check if user has voted in the current election
        boolean hasVoted = user.isVoted();
        model.addAttribute("hasVoted", hasVoted);
        
        // Get the most recent election
        Election election = electionRepository.findFirstByOrderByIdDesc().orElse(null);
        model.addAttribute("election", election);
        
        return "voter-profile";
    }
    
    @GetMapping("/change-password")
    public String showChangePasswordForm() {
        return "change-password";
    }

    @PostMapping("/change-password")
    public String changePassword(@RequestParam("currentPassword") String currentPassword,
                               @RequestParam("newPassword") String newPassword,
                               @RequestParam("confirmPassword") String confirmPassword,
                               RedirectAttributes redirectAttributes) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isEmpty()) {
            return "redirect:/login";
        }

        User user = userOpt.get();

        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            redirectAttributes.addFlashAttribute("error", "Current password is incorrect");
            return "redirect:/voter/change-password";
        }

        // Check if new password and confirm password match
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "New passwords do not match");
            return "redirect:/voter/change-password";
        }

        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        redirectAttributes.addFlashAttribute("success", true);
        return "redirect:/voter/change-password";
    }
    
    @GetMapping("/results")
    public String viewResults(Model model) {
        // Get the most recent election
        Election election = electionRepository.findFirstByOrderByIdDesc().orElse(null);
        
        if (election == null) {
            model.addAttribute("message", "No election results available yet.");
            return "voter-results";
        }
        
        // Get all candidate tickets
        List<CandidateTicket> tickets = candidateTicketRepository.findByIsActiveTrueOrderByVoteCountDesc();
        
        // Get all candidates and group by position
        List<Candidate> allCandidates = candidateRepository.findAll();
        Map<String, List<Candidate>> candidatesByPosition = allCandidates.stream()
            .sorted(Comparator.comparing(Candidate::getVoteCount).reversed())
            .collect(Collectors.groupingBy(Candidate::getPosition, LinkedHashMap::new, Collectors.toList()));
        
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
        
        // Add attributes to model
        model.addAttribute("election", election);
        model.addAttribute("tickets", tickets);
        model.addAttribute("winningTicket", winningTicket);
        model.addAttribute("candidatesByPosition", candidatesByPosition);
        model.addAttribute("totalVotes", totalVotes);
        model.addAttribute("now", now);
        
        return "voter-results";
    }
    
    // Ticket-based voting endpoint
    @PostMapping("/vote/ticket")
    public String castTicketVote(@RequestParam Long ticketId,
                                 RedirectAttributes redirectAttributes) {
        System.out.println("=== TICKET VOTING DEBUG ===");
        System.out.println("Ticket ID: " + ticketId);
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        
        Optional<User> userOpt = userRepository.findByUsername(username);
        
        if (userOpt.isEmpty()) {
            return "redirect:/login";
        }

        User user = userOpt.get();
        
        // Check if user has already voted
        if (user.isVoted()) {
            redirectAttributes.addFlashAttribute("error", "You have already voted in this election!");
            return "redirect:/voter/dashboard";
        }
        
        // Get the most recent election
        Election election = electionRepository.findFirstByOrderByIdDesc().orElse(null);
        
        if (election == null) {
            redirectAttributes.addFlashAttribute("error", "No election found!");
            return "redirect:/voter/dashboard";
        }
        
        // Check if voting window is valid
        LocalDateTime now = LocalDateTime.now();
        
        if (election.getStartTime() == null || election.getEndTime() == null) {
            redirectAttributes.addFlashAttribute("error", "Election timing not set properly!");
            return "redirect:/voter/dashboard";
        }
        
        if (now.isBefore(election.getStartTime())) {
            redirectAttributes.addFlashAttribute("error", "Voting has not started yet!");
            return "redirect:/voter/dashboard";
        }
        
        if (now.isAfter(election.getEndTime())) {
            redirectAttributes.addFlashAttribute("error", "Voting has ended!");
            return "redirect:/voter/dashboard";
        }
        
        // Get the ticket
        Optional<CandidateTicket> ticketOpt = candidateTicketRepository.findById(ticketId);
        
        if (ticketOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Ticket not found!");
            return "redirect:/voter/dashboard";
        }
        
        try {
            CandidateTicket ticket = ticketOpt.get();
            
            // Increment ticket vote count
            ticket.incrementVoteCount();
            candidateTicketRepository.save(ticket);
            
            System.out.println("Ticket vote count updated to: " + ticket.getVoteCount());
            
            // Update user's voted status
            user.setVoted(true);
            userRepository.save(user);
            System.out.println("User marked as voted");
            
            redirectAttributes.addFlashAttribute("success", 
                "Your vote for " + ticket.getPresidentName() + " (President) and " + 
                ticket.getVicePresidentName() + " (Vice President) has been cast successfully!");
            System.out.println("=== TICKET VOTING SUCCESSFUL ===");
        } catch (Exception e) {
            System.out.println("ERROR during ticket voting: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", 
                "An error occurred while processing your vote: " + e.getMessage());
        }
        
        return "redirect:/voter/dashboard";
    }
}

