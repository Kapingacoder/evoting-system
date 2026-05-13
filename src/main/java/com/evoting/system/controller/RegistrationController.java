package com.evoting.system.controller;

import com.evoting.system.model.User;
import com.evoting.system.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
public class RegistrationController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistrationController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/register")
    public String showRegistrationForm(
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String errorMessage,
            @RequestParam(required = false) Boolean success,
            Model model) {
        if (error != null && errorMessage != null) {
            model.addAttribute("error", true);
            model.addAttribute("errorMessage", errorMessage);
        }
        if (Boolean.TRUE.equals(success)) {
            model.addAttribute("success", true);
        }
        return "registration";
    }

    @PostMapping("/register")
    public String registerUser(
            @RequestParam String admissionNumber,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            RedirectAttributes redirectAttributes) {
        
        // Find user by admission number first (case-insensitive and trimmed)
        Optional<User> userOpt = userRepository.findByAdmissionNumberIgnoreCaseAndTrim(admissionNumber);
        
        // Check if admission number exists in the system
        if (userOpt.isEmpty()) {
            redirectAttributes.addAttribute("error", true);
            redirectAttributes.addAttribute("errorMessage", "Hauruhusiwi kusajili kwa sababu sio miongoni mwa wanaoruhusiwa.");
            return "redirect:/register";
        }
        
        User user = userOpt.get();
        
        // Allow users to set their own password regardless of whether they have a default one
        
        // Validate password match
        if (!password.equals(confirmPassword)) {
            redirectAttributes.addAttribute("error", true);
            redirectAttributes.addAttribute("errorMessage", "Neno la siri halifanani. Tafadhali hakikisha neno la siri na uthibitisho wake vinafanana.");
            redirectAttributes.addAttribute("admissionNumber", admissionNumber);
            return "redirect:/register";
        }
        
        // Validate password strength (at least 8 characters)
        if (password.length() < 8) {
            redirectAttributes.addAttribute("error", true);
            redirectAttributes.addAttribute("errorMessage", "Neno la siri lazima liwe na herufi 8 au zaidi.");
            redirectAttributes.addAttribute("admissionNumber", admissionNumber);
            return "redirect:/register";
        }
        
        // Update user with new password
        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);
        
        // Redirect to login page with success message
        redirectAttributes.addFlashAttribute("success", true);
        redirectAttributes.addFlashAttribute("successMessage", "Akaunti yako imesajiliwa kikamilifu. Tafadhali ingia kwa kutumia nambari yako ya usajili na neno lako la siri.");
        return "redirect:/login?registered";
    }
}
