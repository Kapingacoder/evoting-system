package com.evoting.system.controller;

import com.evoting.system.dto.VoterImportResult;
import com.evoting.system.model.Role;
import com.evoting.system.model.User;
import com.evoting.system.model.Candidate;
import com.evoting.system.model.CandidateTicket;
import com.evoting.system.model.Election;
import com.evoting.system.repository.CandidateRepository;
import com.evoting.system.repository.CandidateTicketRepository;
import com.evoting.system.repository.ElectionRepository;
import com.evoting.system.repository.UserRepository;
import com.evoting.system.repository.VoteRepository;
import com.evoting.system.service.BulkImportService;
import com.evoting.system.service.CandidateTicketService;
import com.evoting.system.service.ElectionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.csrf.CsrfToken;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final CandidateRepository candidateRepository;
    private final VoteRepository voteRepository;
    private final ElectionRepository electionRepository;
    private final ElectionService electionService;
    private final PasswordEncoder passwordEncoder;
    private final BulkImportService bulkImportService;
    private final CandidateTicketService candidateTicketService;
    private final CandidateTicketRepository candidateTicketRepository;

    // Admin Profile Management
    @GetMapping("/profile")
    public String showAdminProfile(Model model, HttpServletRequest request) {
        // Get the currently authenticated admin
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User admin = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("Admin not found"));
            
        // Add CSRF token to the model
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            model.addAttribute("_csrf", csrfToken);
        }
        
        model.addAttribute("admin", admin);
        return "admin-profile";
    }

    @Value("${upload.path}")
    private String uploadPath;
    
    @PostMapping("/profile/update")
    public String updateAdminProfile(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String fullName,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String currentPassword,
            @RequestParam(required = false) String newPassword,
            @RequestParam(value = "profileImage", required = false) MultipartFile profileImage,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes,
            Model model) {
                
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        User admin = userRepository.findByUsername(currentUsername)
            .orElseThrow(() -> new UsernameNotFoundException("Admin not found"));
            
        // Add CSRF token to the model for the form
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            model.addAttribute("_csrf", csrfToken);
        }
            
        // Update username if provided and changed
        if (username != null && !username.trim().isEmpty() && !username.equals(admin.getUsername())) {
            // Check if new username is already taken
            if (userRepository.existsByUsername(username.trim())) {
                redirectAttributes.addFlashAttribute("error", "Username is already taken");
                return "redirect:/admin/profile";
            }
            admin.setUsername(username.trim());
        }
            
        // Update basic info if provided
        if (fullName != null && !fullName.trim().isEmpty()) {
            admin.setFullName(fullName.trim());
        }
        
        if (email != null && !email.trim().isEmpty()) {
            admin.setEmail(email.trim());
        }
        
        // Handle profile image upload
        if (profileImage != null && !profileImage.isEmpty()) {
            try {
                // Create upload directory if it doesn't exist
                Path uploadDir = Paths.get(uploadPath);
                if (!Files.exists(uploadDir)) {
                    Files.createDirectories(uploadDir);
                }
                
                // Generate a unique filename
                String originalFilename = profileImage.getOriginalFilename();
                String fileExtension = "";
                if (originalFilename != null && originalFilename.contains(".")) {
                    fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
                }
                String newFilename = "profile_" + UUID.randomUUID().toString() + fileExtension;
                
                // Save the file
                Path filePath = uploadDir.resolve(newFilename);
                Files.copy(profileImage.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                
                // Delete old profile image if exists
                if (admin.getProfileImage() != null && !admin.getProfileImage().isEmpty()) {
                    try {
                        Path oldFilePath = Paths.get(uploadPath).resolve(admin.getProfileImage());
                        Files.deleteIfExists(oldFilePath);
                    } catch (IOException e) {
                        // Log the error but don't fail the request
                        e.printStackTrace();
                    }
                }
                
                // Update user's profile image path
                admin.setProfileImage(newFilename);
                
            } catch (IOException e) {
                redirectAttributes.addFlashAttribute("error", "Failed to upload profile image: " + e.getMessage());
                return "redirect:/admin/profile";
            }
        }
        
        // Update password if all required fields are provided
        if (currentPassword != null && !currentPassword.isEmpty() && 
            newPassword != null && !newPassword.isEmpty()) {
                
            if (!passwordEncoder.matches(currentPassword, admin.getPassword())) {
                redirectAttributes.addFlashAttribute("error", "Current password is incorrect");
                return "redirect:/admin/profile";
            }
            
            if (newPassword.length() < 8) {
                redirectAttributes.addFlashAttribute("error", "New password must be at least 8 characters long");
                return "redirect:/admin/profile";
            }
            
            // Update the password
            admin.setPassword(passwordEncoder.encode(newPassword));
        }
        
        try {
            // Save the updated admin
            userRepository.save(admin);
            
            // Update the authentication object if username or password was changed
            if ((username != null && !username.equals(currentUsername)) || 
                (newPassword != null && !newPassword.isEmpty())) {
                
                // Create new authentication token with updated details
                Authentication newAuth = new UsernamePasswordAuthenticationToken(
                    admin.getUsername(), 
                    authentication.getCredentials(),
                    authentication.getAuthorities()
                );
                
                // Set the new authentication in the security context
                SecurityContext context = SecurityContextHolder.getContext();
                context.setAuthentication(newAuth);
            }
            
            redirectAttributes.addFlashAttribute("success", "Profile updated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating profile: " + e.getMessage());
        }
        
        return "redirect:/admin/profile";
    }

    public AdminController(UserRepository userRepository, 
                          CandidateRepository candidateRepository, 
                          VoteRepository voteRepository, 
                          ElectionRepository electionRepository,
                          ElectionService electionService,
                          PasswordEncoder passwordEncoder,
                          BulkImportService bulkImportService,
                          CandidateTicketService candidateTicketService,
                          CandidateTicketRepository candidateTicketRepository) {
        this.userRepository = userRepository;
        this.candidateRepository = candidateRepository;
        this.voteRepository = voteRepository;
        this.electionRepository = electionRepository;
        this.electionService = electionService;
        this.passwordEncoder = passwordEncoder;
        this.bulkImportService = bulkImportService;
        this.candidateTicketService = candidateTicketService;
        this.candidateTicketRepository = candidateTicketRepository;
    }

    // Election Management Endpoints
    @GetMapping("/elections")
    public String showElectionManagement(Model model) {
        List<Election> allElections = electionRepository.findAllByOrderByStartTimeDesc();
        Optional<Election> activeElectionOpt = electionRepository.findFirstByActiveTrue();
        
        LocalDateTime now = LocalDateTime.now();
        boolean updated = false;
        
        for (Election election : allElections) {
            if (election.isActive() && election.getEndTime() != null && election.getEndTime().isBefore(now)) {
                election.setActive(false);
                election.setCompleted(true);
                electionRepository.save(election);
                updated = true;
            } else if (!election.isCompleted() && election.getEndTime() != null && election.getEndTime().isBefore(now)) {
                election.setActive(false);
                election.setCompleted(true);
                electionRepository.save(election);
                updated = true;
            }
        }
        
        if (updated) {
            allElections = electionRepository.findAllByOrderByStartTimeDesc();
            activeElectionOpt = electionRepository.findFirstByActiveTrue();
        }
        
        model.addAttribute("elections", allElections);
        activeElectionOpt.ifPresent(election -> model.addAttribute("activeElection", election));
        model.addAttribute("now", now);
        return "election-management";
    }

    @PostMapping("/elections/schedule")
    public String scheduleElection(
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime startTime,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime endTime,
            RedirectAttributes redirectAttributes) {
        
        if (name == null || name.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Election name is required");
            return "redirect:/admin/elections";
        }
        
        if (startTime == null || endTime == null) {
            redirectAttributes.addFlashAttribute("error", "Please set start and end time for the election");
            return "redirect:/admin/elections";
        }
        
        LocalDateTime now = LocalDateTime.now();
        if (startTime.isBefore(now)) {
            redirectAttributes.addFlashAttribute("error", "Start time cannot be in the past");
            return "redirect:/admin/elections";
        }
        
        if (endTime.isBefore(startTime)) {
            redirectAttributes.addFlashAttribute("error", "End time cannot be before start time");
            return "redirect:/admin/elections";
        }
        
        // Check for overlapping elections
        List<Election> allElections = electionRepository.findAll();
        boolean hasOverlap = allElections.stream()
            .filter(e -> !e.isCompleted())
            .anyMatch(e -> 
                (startTime.isAfter(e.getStartTime()) && startTime.isBefore(e.getEndTime())) ||
                (endTime.isAfter(e.getStartTime()) && endTime.isBefore(e.getEndTime())) ||
                (startTime.isBefore(e.getStartTime()) && endTime.isAfter(e.getEndTime())) ||
                (startTime.equals(e.getStartTime()) || endTime.equals(e.getEndTime()))
            );
            
        if (hasOverlap) {
            redirectAttributes.addFlashAttribute("error", "There is another election scheduled in this time period");
            return "redirect:/admin/elections";
        }
        
        try {
            Election election = new Election();
            election.setName(name.trim());
            election.setDescription(description != null ? description.trim() : null);
            election.setStartTime(startTime);
            election.setEndTime(endTime);
            election.setActive(false);
            election.setCompleted(false);
            
            electionRepository.save(election);
            redirectAttributes.addFlashAttribute("success", "Election '" + name + "' scheduled successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to schedule election: " + e.getMessage());
        }
        return "redirect:/admin/elections";
    }
    
    @PostMapping("/elections/start/{id}")
    public String startElection(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Election election = electionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Election not found"));
                
            // End any currently active elections
            List<Election> activeElections = electionRepository.findByActiveTrue();
            for (Election active : activeElections) {
                if (!active.getId().equals(id)) {
                    active.setActive(false);
                    electionRepository.save(active);
                }
            }
            
            election.setActive(true);
            election.setCompleted(false);
            electionRepository.save(election);
            
            redirectAttributes.addFlashAttribute("success", "Election started successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to start election: " + e.getMessage());
        }
        return "redirect:/admin/elections";
    }

    @PostMapping("/elections/end/{id}")
    public String endElection(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Election election = electionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Election not found"));
            election.setActive(false);
            election.setCompleted(true);
            electionRepository.save(election);
            redirectAttributes.addFlashAttribute("success", "Election ended successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to end election: " + e.getMessage());
        }
        return "redirect:/admin/elections";
    }

    @GetMapping("/dashboard")
    public String showAdminDashboard(Model model) {
        // Get basic counts
        long totalVoters = userRepository.countByRole(Role.VOTER);
        long totalCandidates = candidateRepository.count();
        long totalTickets = candidateTicketRepository.count();
        
        // Count voters who have actually voted (both ticket and candidate voting)
        long votesCast = userRepository.countByRoleAndVotedTrue(Role.VOTER);
        long votersNotVoted = totalVoters - votesCast;
        
        // Get active election
        Optional<Election> activeElection = electionRepository.findFirstByActiveTrue();
        
        // Get candidate ticket statistics (new ticket-based system)
        List<CandidateTicket> topTickets = candidateTicketRepository.findByIsActiveTrueOrderByVoteCountDesc()
            .stream()
            .limit(5)
            .collect(Collectors.toList());
        
        // Get all tickets for detailed breakdown
        List<CandidateTicket> allTickets = candidateTicketRepository.findByIsActiveTrueOrderByVoteCountDesc();
        
        // Calculate ticket voting percentages
        Map<String, Object> ticketBreakdown = new HashMap<>();
        int totalTicketVotes = allTickets.stream().mapToInt(CandidateTicket::getVoteCount).sum();
        
        for (CandidateTicket ticket : allTickets) {
            Map<String, Object> ticketInfo = new HashMap<>();
            ticketInfo.put("name", ticket.getName());
            ticketInfo.put("votes", ticket.getVoteCount());
            ticketInfo.put("percentage", totalTicketVotes > 0 ? 
                Math.round(ticket.getVoteCount() * 100.0 / totalTicketVotes) : 0);
            ticketInfo.put("president", ticket.getPresidentName());
            ticketInfo.put("vicePresident", ticket.getVicePresidentName());
            ticketBreakdown.put(ticket.getName(), ticketInfo);
        }
            
        // Prepare data for charts - use ticket names if available, otherwise use candidate names
        List<String> candidateNames;
        List<Integer> voteCounts;
        
        if (!topTickets.isEmpty()) {
            // Use ticket names and vote counts
            candidateNames = topTickets.stream()
                .map(CandidateTicket::getName)
                .collect(Collectors.toList());
                
            voteCounts = topTickets.stream()
                .map(CandidateTicket::getVoteCount)
                .collect(Collectors.toList());
        } else {
            // Fall back to old candidate system
            List<Candidate> topCandidates = candidateRepository.findAll()
                .stream()
                .sorted((c1, c2) -> Integer.compare(c2.getVoteCount(), c1.getVoteCount()))
                .limit(5)
                .collect(Collectors.toList());
                
            candidateNames = topCandidates.stream()
                .map(Candidate::getName)
                .collect(Collectors.toList());
                
            voteCounts = topCandidates.stream()
                .map(Candidate::getVoteCount)
                .collect(Collectors.toList());
        }
        
        // Get recent activities
        List<Map<String, Object>> recentActivities = new ArrayList<>();
        
        // Collect recent votes
        List<Map<String, Object>> voteActivities = voteRepository
            .findAllByOrderByTimestampDesc(PageRequest.of(0, 5, Sort.by("timestamp").descending()))
            .stream()
            .map(vote -> {
                Map<String, Object> activity = new HashMap<>();
                activity.put("type", "vote");
                activity.put("voter", vote.getUser().getUsername());
                activity.put("time", vote.getTimestamp() != null ? vote.getTimestamp() : LocalDateTime.now());
                activity.put("icon", "vote-yea");
                activity.put("color", "green");
                return activity;
            })
            .collect(Collectors.toList());
        
        // Collect new registrations
        List<Map<String, Object>> registrationActivities = userRepository
            .findByRoleOrderByIdDesc(Role.VOTER, PageRequest.of(0, 5, Sort.by("id").descending()))
            .stream()
            .map(user -> {
                Map<String, Object> activity = new HashMap<>();
                activity.put("type", "registration");
                activity.put("username", user.getUsername());
                activity.put("time", LocalDateTime.now());
                activity.put("icon", "user-plus");
                activity.put("color", "blue");
                return activity;
            })
            .collect(Collectors.toList());
        
        // Combine and sort activities by time, then limit to 5 most recent
        recentActivities.addAll(voteActivities);
        recentActivities.addAll(registrationActivities);
        recentActivities.sort((a, b) -> ((LocalDateTime)b.get("time")).compareTo((LocalDateTime)a.get("time")));
        if (recentActivities.size() > 5) {
            recentActivities = recentActivities.subList(0, 5);
        }

        // Add attributes to model
        model.addAttribute("totalVoters", totalVoters);
        model.addAttribute("totalCandidates", totalCandidates + totalTickets);
        model.addAttribute("votesCast", votesCast);
        model.addAttribute("votersNotVoted", Math.max(0, votersNotVoted)); // Ensure non-negative
        model.addAttribute("candidateNames", candidateNames);
        model.addAttribute("voteCounts", voteCounts);
        model.addAttribute("ticketBreakdown", ticketBreakdown);
        model.addAttribute("allTickets", allTickets);
        model.addAttribute("recentActivities", recentActivities);
        model.addAttribute("activeElection", activeElection.orElse(null));
        model.addAttribute("now", LocalDateTime.now());
        model.addAttribute("isElectionInProgress", activeElection.isPresent() && activeElection.get().isActive());
        model.addAttribute("now", LocalDateTime.now());

        return "admin-dashboard";
    }

    @GetMapping("/voters")
    public String showVoterManagement(Model model, HttpServletRequest request) {
        // Add CSRF token to the model
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            model.addAttribute("_csrf", csrfToken);
        }
        
        List<User> voters = userRepository.findAllByRole(Role.VOTER);
        model.addAttribute("voters", voters);
        model.addAttribute("user", new User());
        return "voter-management";
    }
    
    @PostMapping("/election/schedule")
    public String scheduleSingleElection(
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam("startTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam("endTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            RedirectAttributes redirectAttributes) {
        
        if (endTime.isBefore(startTime)) {
            redirectAttributes.addFlashAttribute("error", "End time must be after start time");
            return "redirect:/admin/election";
        }
        
        try {
            Election election = new Election();
            election.setName(name);
            election.setDescription(description);
            election.setStartTime(startTime);
            election.setEndTime(endTime);
            election.setActive(false);
            election.setCompleted(false);
            
            electionRepository.save(election);
            redirectAttributes.addFlashAttribute("success", "Election '" + name + "' scheduled successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to schedule election: " + e.getMessage());
        }
        
        return "redirect:/admin/election";
    }

    @PostMapping("/voters/add")
    public String addVoter(
            @RequestParam String admissionNumber,
            @RequestParam String fullName,
            @RequestParam(required = false) String email,
            RedirectAttributes redirectAttributes) {
        
        try {
            // Trim and validate input
            String trimmedAdmissionNumber = admissionNumber.trim();
            String trimmedFullName = fullName.trim();
            String trimmedEmail = (email != null) ? email.trim() : null;
            
            // Validate required fields
            if (trimmedAdmissionNumber.isEmpty() || trimmedFullName.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Admission number and full name are required");
                return "redirect:/admin/voters";
            }
            
            // Check if admission number already exists
            if (userRepository.existsByAdmissionNumber(trimmedAdmissionNumber)) {
                redirectAttributes.addFlashAttribute("error", "A voter with admission number " + trimmedAdmissionNumber + " already exists");
                return "redirect:/admin/voters";
            }
            
            // Check if username already exists
            if (userRepository.existsByUsername(trimmedAdmissionNumber)) {
                redirectAttributes.addFlashAttribute("error", "A user with username " + trimmedAdmissionNumber + " already exists");
                return "redirect:/admin/voters";
            }
            
            // Create new user
            User user = new User();
            user.setAdmissionNumber(trimmedAdmissionNumber);
            user.setUsername(trimmedAdmissionNumber); // Use admission number as username
            user.setFullName(trimmedFullName);
            
            // Set email if provided and not empty
            if (trimmedEmail != null && !trimmedEmail.isEmpty()) {
                user.setEmail(trimmedEmail);
            }
            
            // Set default password for new voters (they can change it later)
            user.setPassword(passwordEncoder.encode("password123"));
            user.setRole(Role.VOTER);
            user.setVoted(false);
            
            userRepository.save(user);
            redirectAttributes.addFlashAttribute("success", 
                "Voter added successfully! The voter can now register with admission number: " + trimmedAdmissionNumber);
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to add voter: " + 
                (e.getMessage() != null ? e.getMessage() : "Unknown error occurred"));
            // Log the full error for debugging
            e.printStackTrace();
        }
        
        return "redirect:/admin/voters";
    }
 
    @GetMapping("/voters/edit/{id}")
    public String showEditVoterForm(@PathVariable Long id, Model model) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid voter Id:" + id));
        model.addAttribute("user", user);
        return "edit-voter";
    }
 
    @PostMapping("/voters/update/{id}")
    public String updateVoter(@PathVariable Long id, @ModelAttribute User user) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid voter Id:" + id));
        existingUser.setUsername(user.getUsername());
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            existingUser.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        userRepository.save(existingUser);
        return "redirect:/admin/voters";
    }
 
    @Transactional
    @GetMapping("/voters/delete/{id}")
    public String deleteVoter(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            // First delete all votes by this user
            voteRepository.deleteByUserId(id);
            // Then delete the user
            userRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "Voter deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to delete voter: " + e.getMessage());
        }
        return "redirect:/admin/voters";
    }
    
    @Transactional
    @PostMapping("/voters/delete-all")
    public String deleteAllVoters(RedirectAttributes redirectAttributes) {
        try {
            List<User> voters = userRepository.findAllByRole(Role.VOTER);
            int count = voters.size();
            
            // First delete all votes by all voters
            for (User voter : voters) {
                voteRepository.deleteByUserId(voter.getId());
            }
            
            // Then delete all voters
            userRepository.deleteAll(voters);
            redirectAttributes.addFlashAttribute("success", count + " voters deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to delete voters: " + e.getMessage());
        }
        return "redirect:/admin/voters";
    }

    private boolean electionStarted = false;
    
    @GetMapping("/candidates")
    public String showCandidateManagement(Model model) {
        List<Candidate> candidates = candidateRepository.findAll();
        List<CandidateTicket> tickets = candidateTicketRepository.findAll();
        model.addAttribute("candidates", candidates);
        model.addAttribute("tickets", tickets);
        model.addAttribute("candidate", new Candidate());
        model.addAttribute("candidateTicket", new CandidateTicket());
        model.addAttribute("electionStarted", electionStarted);
        return "candidate-management";
    }
    
    // ==================== CANDIDATE TICKET MANAGEMENT ====================
    
    @PostMapping("/candidates/ticket/add")
    public String addCandidateTicket(
            @RequestParam String ticketName,
            @RequestParam(required = false) String description,
            @RequestParam String presidentName,
            @RequestParam String presidentParty,
            @RequestParam(required = false) MultipartFile presidentPhoto,
            @RequestParam String vicePresidentName,
            @RequestParam String vicePresidentParty,
            @RequestParam(required = false) MultipartFile vicePresidentPhoto,
            RedirectAttributes redirectAttributes,
            Model model) {
        
        if (electionStarted) {
            redirectAttributes.addFlashAttribute("error", "Cannot add candidates after election has started");
            return "redirect:/admin/candidates";
        }
        
        try {
            CandidateTicket ticket = new CandidateTicket();
            ticket.setName(ticketName);
            ticket.setDescription(description);
            ticket.setPresidentName(presidentName);
            ticket.setPresidentParty(presidentParty);
            ticket.setVicePresidentName(vicePresidentName);
            ticket.setVicePresidentParty(vicePresidentParty);
            ticket.setActive(true);
            
            // Handle President photo upload
            if (presidentPhoto != null && !presidentPhoto.isEmpty()) {
                String presidentPhotoUrl = savePhoto(presidentPhoto, "president");
                ticket.setPresidentPhotoUrl(presidentPhotoUrl);
            }
            
            // Handle Vice President photo upload
            if (vicePresidentPhoto != null && !vicePresidentPhoto.isEmpty()) {
                String vicePresidentPhotoUrl = savePhoto(vicePresidentPhoto, "vicepresident");
                ticket.setVicePresidentPhotoUrl(vicePresidentPhotoUrl);
            }
            
            candidateTicketRepository.save(ticket);
            redirectAttributes.addFlashAttribute("success", "Candidate ticket added successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error adding candidate: " + e.getMessage());
            e.printStackTrace();
        }
        
        return "redirect:/admin/candidates";
    }
    
    private String savePhoto(MultipartFile photo, String prefix) throws IOException {
        // Create upload directory if it doesn't exist
        Path uploadDir = Paths.get(uploadPath);
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }
        
        // Generate unique filename
        String originalFilename = photo.getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String newFilename = prefix + "_" + UUID.randomUUID().toString() + fileExtension;
        
        // Save the file
        Path filePath = uploadDir.resolve(newFilename);
        Files.copy(photo.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        return newFilename;
    }
    
    @PostMapping("/candidates/ticket/delete/{id}")
    @Transactional
    public String deleteCandidateTicket(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        if (electionStarted) {
            redirectAttributes.addFlashAttribute("error", "Cannot delete candidates after election has started");
            return "redirect:/admin/candidates";
        }
        
        try {
            candidateTicketRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "Candidate ticket deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting candidate: " + e.getMessage());
        }
        
        return "redirect:/admin/candidates";
    }
    
    @GetMapping("/candidates/ticket/deactivate/{id}")
    public String deactivateCandidateTicket(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        if (electionStarted) {
            redirectAttributes.addFlashAttribute("error", "Cannot modify candidates after election has started");
            return "redirect:/admin/candidates";
        }
        
        candidateTicketService.deactivateTicket(id);
        redirectAttributes.addFlashAttribute("success", "Candidate deactivated successfully!");
        return "redirect:/admin/candidates";
    }
    
    @GetMapping("/candidates/ticket/activate/{id}")
    public String activateCandidateTicket(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        if (electionStarted) {
            redirectAttributes.addFlashAttribute("error", "Cannot modify candidates after election has started");
            return "redirect:/admin/candidates";
        }
        
        candidateTicketService.activateTicket(id);
        redirectAttributes.addFlashAttribute("success", "Candidate activated successfully!");
        return "redirect:/admin/candidates";
    }
    
    @GetMapping("/candidates/votes")
    @ResponseBody
    public List<Map<String, Object>> getCandidateVotes() {
        return candidateRepository.findAll().stream()
            .map(candidate -> {
                Map<String, Object> data = new HashMap<>();
                data.put("id", candidate.getId());
                data.put("voteCount", candidate.getVoteCount());
                return data;
            })
            .collect(Collectors.toList());
    }
    
    @GetMapping("/candidates/ticket/votes")
    @ResponseBody
    public List<Map<String, Object>> getTicketVotes() {
        return candidateTicketRepository.findAll().stream()
            .map(ticket -> {
                Map<String, Object> data = new HashMap<>();
                data.put("id", ticket.getId());
                data.put("name", ticket.getName());
                data.put("presidentName", ticket.getPresidentName());
                data.put("vicePresidentName", ticket.getVicePresidentName());
                data.put("voteCount", ticket.getVoteCount());
                return data;
            })
            .collect(Collectors.toList());
    }
 
    @PostMapping("/candidates/add")
    public String addCandidate(@ModelAttribute Candidate candidate, Model model) {
        if (electionStarted) {
            model.addAttribute("error", "Cannot add candidates after election has started");
            return "redirect:/admin/candidates";
        }
        
        candidate.setActive(true);
        candidateRepository.save(candidate);
        return "redirect:/admin/candidates";
    }
    
    @PostMapping("/candidates/update/{id}")
    public String updateCandidate(@PathVariable Long id, @ModelAttribute Candidate candidateDetails, Model model) {
        if (electionStarted) {
            model.addAttribute("error", "Cannot update candidates after election has started");
            return "redirect:/admin/candidates";
        }
        
        return candidateRepository.findById(id).map(candidate -> {
            candidate.setName(candidateDetails.getName());
            candidate.setParty(candidateDetails.getParty());
            candidate.setPosition(candidateDetails.getPosition());
            candidateRepository.save(candidate);
            return "redirect:/admin/candidates";
        }).orElseThrow(() -> new IllegalArgumentException("Invalid candidate Id:" + id));
    }
 
    @PostMapping("/candidates/delete/{id}")
    @Transactional
    public String deleteCandidate(@PathVariable Long id, Model model) {
        if (electionStarted) {
            model.addAttribute("error", "Cannot delete candidates after election has started");
            return "redirect:/admin/candidates";
        }
        
        try {
            // First delete all votes associated with this candidate
            voteRepository.deleteByCandidateId(id);
            
            // Then delete the candidate
            candidateRepository.deleteById(id);
            
            return "redirect:/admin/candidates";
        } catch (Exception e) {
            model.addAttribute("error", "Error deleting candidate: " + e.getMessage());
            return "redirect:/admin/candidates";
        }
    }
    
    @GetMapping("/candidates/deactivate/{id}")
    public String deactivateCandidate(@PathVariable Long id, Model model) {
        if (electionStarted) {
            model.addAttribute("error", "Cannot modify candidates after election has started");
            return "redirect:/admin/candidates";
        }
        
        return candidateRepository.findById(id).map(candidate -> {
            candidate.setActive(false);
            candidateRepository.save(candidate);
            return "redirect:/admin/candidates";
        }).orElseThrow(() -> new IllegalArgumentException("Invalid candidate Id:" + id));
    }
    
    @GetMapping("/candidates/activate/{id}")
    public String activateCandidate(@PathVariable Long id, Model model) {
        if (electionStarted) {
            model.addAttribute("error", "Cannot modify candidates after election has started");
            return "redirect:/admin/candidates";
        }
        
        return candidateRepository.findById(id).map(candidate -> {
            candidate.setActive(true);
            candidateRepository.save(candidate);
            return "redirect:/admin/candidates";
        }).orElseThrow(() -> new IllegalArgumentException("Invalid candidate Id:" + id));
    }
    
    @PostMapping("/election/start")
    public String startElectionManual(RedirectAttributes redirectAttributes) {
        try {
            Election election = electionService.getMostRecentElection();
            if (election == null) {
                redirectAttributes.addFlashAttribute("error", "No election found. Please schedule an election first.");
                return "redirect:/admin/election";
            }
            
            // End any currently active elections
            List<Election> activeElections = electionRepository.findByActiveTrue();
            for (Election active : activeElections) {
                if (!active.getId().equals(election.getId())) {
                    active.setActive(false);
                    electionRepository.save(active);
                }
            }
            
            election.setActive(true);
            election.setCompleted(false);
            electionRepository.save(election);
            
            redirectAttributes.addFlashAttribute("success", "Election started successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to start election: " + e.getMessage());
        }
        return "redirect:/admin/election";
    }
    
    @PostMapping("/election/end")
    public String endElectionManual(RedirectAttributes redirectAttributes) {
        try {
            Election election = electionService.getMostRecentElection();
            if (election == null) {
                redirectAttributes.addFlashAttribute("error", "No election found. Please schedule an election first.");
                return "redirect:/admin/election";
            }
            
            election.setActive(false);
            election.setCompleted(true);
            electionRepository.save(election);
            
            redirectAttributes.addFlashAttribute("success", "Election ended successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to end election: " + e.getMessage());
        }
        return "redirect:/admin/election";
    }
    
    @GetMapping("/election")
    public String showElectionControl(Model model) {
        Election activeElection = electionService.getMostRecentElection();
        if (activeElection == null) {
            activeElection = new Election();
            activeElection.setActive(false);
            activeElection.setCompleted(false);
        }
        
        model.addAttribute("election", activeElection);
        model.addAttribute("now", LocalDateTime.now());
        
        // Calculate voting status
        LocalDateTime now = LocalDateTime.now();
        boolean isVotingOpen = activeElection.getStartTime() != null && 
                               activeElection.getEndTime() != null &&
                               now.isAfter(activeElection.getStartTime()) && 
                               now.isBefore(activeElection.getEndTime()) &&
                               activeElection.isActive() && 
                               !activeElection.isCompleted();
        model.addAttribute("isVotingOpen", isVotingOpen);
        
        return "election-control";
    }
    
    @PostMapping("/election/reset")
    public String resetElection(RedirectAttributes redirectAttributes) {
        try {
            // Reset all candidate vote counts (old system)
            List<Candidate> candidates = candidateRepository.findAll();
            for (Candidate candidate : candidates) {
                candidate.setVoteCount(0);
                candidateRepository.save(candidate);
            }
            
            // Reset all candidate ticket vote counts (new system)
            List<CandidateTicket> candidateTickets = candidateTicketRepository.findAll();
            for (CandidateTicket ticket : candidateTickets) {
                ticket.setVoteCount(0);
                candidateTicketRepository.save(ticket);
            }
            
            // Reset all user voted status
            List<User> voters = userRepository.findAllByRole(Role.VOTER);
            for (User voter : voters) {
                voter.setVoted(false);
                userRepository.save(voter);
            }
            
            // Delete all votes
            voteRepository.deleteAll();
            
            // Reset election status
            Election activeElection = electionService.getMostRecentElection();
            if (activeElection != null) {
                activeElection.setActive(false);
                activeElection.setCompleted(false);
                electionRepository.save(activeElection);
            }
            
            redirectAttributes.addFlashAttribute("success", "Election has been reset successfully! All vote counts and results have been cleared.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to reset election: " + e.getMessage());
        }
        
        return "redirect:/admin/election";
    }
    
    @GetMapping("/election/results")
    public String showElectionResults(Model model) {
        // Get ticket-based candidates (new system)
        List<CandidateTicket> tickets = candidateTicketRepository.findByIsActiveTrueOrderByVoteCountDesc();
        
        // Get old candidate-based candidates (for backward compatibility)
        List<Candidate> candidates = candidateRepository.findByIsActiveTrueOrderByPositionAscVoteCountDesc();
        
        // Calculate total votes
        int totalTicketVotes = tickets.stream().mapToInt(CandidateTicket::getVoteCount).sum();
        int totalCandidateVotes = candidates.stream().mapToInt(Candidate::getVoteCount).sum();
        int totalVotes = totalTicketVotes + totalCandidateVotes;
        
        Election election = electionService.getActiveElection();
        if (election == null) {
            election = new Election();
            election.setActive(false);
            election.setCompleted(false);
        }
        
        // Group old candidates by position
        Map<String, List<Candidate>> candidatesByPosition = candidates.stream()
            .collect(Collectors.groupingBy(Candidate::getPosition,
                     Collectors.collectingAndThen(
                         Collectors.toList(),
                         list -> list.stream()
                             .sorted(Comparator.comparingInt(Candidate::getVoteCount).reversed())
                             .collect(Collectors.toList())
                     )));
        
        // Determine winner from tickets
        CandidateTicket winningTicket = null;
        if (!tickets.isEmpty()) {
            winningTicket = tickets.get(0); // First one has highest votes
        }
        
        model.addAttribute("tickets", tickets);
        model.addAttribute("candidates", candidates);
        model.addAttribute("candidatesByPosition", candidatesByPosition);
        model.addAttribute("totalVotes", totalVotes);
        model.addAttribute("totalTicketVotes", totalTicketVotes);
        model.addAttribute("totalCandidateVotes", totalCandidateVotes);
        model.addAttribute("winningTicket", winningTicket);
        model.addAttribute("election", election);
        model.addAttribute("now", LocalDateTime.now());
        
        return "election-results";
    }
    
    // ===================== BULK VOTER IMPORT ENDPOINTS =====================
    
    /**
     * Show the bulk voter import page.
     */
    @GetMapping("/voters/import")
    public String showVoterImportPage(Model model) {
        model.addAttribute("importResult", null);
        return "voter-import";
    }
    
    /**
     * Handle bulk voter import from Excel or CSV file.
     */
    @PostMapping("/voters/import")
    public String importVoters(@RequestParam("file") MultipartFile file,
                               RedirectAttributes redirectAttributes,
                               Model model) {
        
        if (file == null || file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please select a file to upload");
            return "redirect:/admin/voters";
        }
        
        String filename = file.getOriginalFilename();
        if (filename == null || 
            (!filename.toLowerCase().endsWith(".csv") && 
             !filename.toLowerCase().endsWith(".xlsx") && 
             !filename.toLowerCase().endsWith(".xls"))) {
            redirectAttributes.addFlashAttribute("error", 
                "Invalid file format. Please upload .csv, .xlsx, or .xls files only");
            return "redirect:/admin/voters";
        }
        
        try {
            // Process the file (passwords are encoded in the service)
            VoterImportResult result = bulkImportService.importVoters(file);
            
            // Log details for debugging
            System.out.println("=== BULK IMPORT DEBUG ===");
            System.out.println("Total processed: " + result.getTotalProcessed());
            System.out.println("Successful: " + result.getSuccessfulCount());
            System.out.println("Failed: " + result.getFailedCount());
            System.out.println("Errors: " + result.getErrors());
            System.out.println("Failed records: " + (result.getFailedRecords() != null ? result.getFailedRecords().size() : 0));
            
            String message = String.format("Import completed: %d voters imported successfully!",
                result.getSuccessfulCount());
            
            if (result.isFullySuccessful()) {
                redirectAttributes.addFlashAttribute("success", message);
            } else if (result.getSuccessfulCount() > 0) {
                String warning = String.format("%d voters imported, but %d failed.",
                    result.getSuccessfulCount(), result.getFailedCount());
                redirectAttributes.addFlashAttribute("warning", warning);
                // Pass failed records with all attributes
                redirectAttributes.addFlashAttribute("failedRecords", result.getFailedRecords());
                redirectAttributes.addFlashAttribute("failedCount", result.getFailedCount());
                redirectAttributes.addFlashAttribute("successfulCount", result.getSuccessfulCount());
            } else {
                redirectAttributes.addFlashAttribute("error", 
                    "Import failed! No voters were imported.");
                // Pass failed records with all attributes
                if (result.getFailedRecords() != null && !result.getFailedRecords().isEmpty()) {
                    redirectAttributes.addFlashAttribute("failedRecords", result.getFailedRecords());
                }
                if (!result.getErrors().isEmpty()) {
                    redirectAttributes.addFlashAttribute("error", 
                        "Import failed! Errors: " + String.join("; ", result.getErrors()));
                }
            }
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error processing file: " + e.getMessage());
            e.printStackTrace();
        }
        
        return "redirect:/admin/voters";
    }
    
    /**
     * Download the Excel template for voter import.
     */
    @GetMapping("/voters/download-template")
    public ResponseEntity<byte[]> downloadTemplate() throws IOException {
        // Create Excel workbook
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Voters");
            
            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Full Name", "Email"};
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.GREEN.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            org.apache.poi.ss.usermodel.Font font = workbook.createFont();
            font.setBold(true);
            font.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(font);
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.autoSizeColumn(i);
            }
            
            // Add example data
            Row exampleRow = sheet.createRow(1);
            exampleRow.createCell(0).setCellValue("John Doe");
            exampleRow.createCell(1).setCellValue("john.doe@example.com");
            
            Row exampleRow2 = sheet.createRow(2);
            exampleRow2.createCell(0).setCellValue("Jane Doe");
            exampleRow2.createCell(1).setCellValue("jane.doe@example.com");
            
            Row exampleRow3 = sheet.createRow(3);
            exampleRow3.createCell(0).setCellValue("Bob Smith");
            exampleRow3.createCell(1).setCellValue("bob.smith@example.com");
            
            // Add notes row with registration instructions
            Row notesRow1 = sheet.createRow(5);
            notesRow1.createCell(0).setCellValue("INSTRUCTIONS:");
            
            Row notesRow2 = sheet.createRow(6);
            notesRow2.createCell(0).setCellValue("1. Email is optional but recommended");
            
            Row notesRow3 = sheet.createRow(7);
            notesRow3.createCell(0).setCellValue("2. Username will be the same as Admission Number");
            
            Row notesRow4 = sheet.createRow(8);
            notesRow4.createCell(0).setCellValue("3. Voters will set their own password during registration at /register");
            
            // Style the instructions header using the existing headerStyle
            notesRow1.getCell(0).setCellStyle(headerStyle);
            
            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            // Write to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            responseHeaders.setContentDispositionFormData("attachment", "voter_import_template.xlsx");
            responseHeaders.setContentLength(outputStream.size());
            
            return new ResponseEntity<>(outputStream.toByteArray(), responseHeaders, 200);
        }
    }
}

