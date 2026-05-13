package com.evoting.system.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {

    /**
     * Handles requests for the root URL and returns the home page.
     * @return The name of the index HTML template ("index").
     */
    @GetMapping("/")
    public String home() {
        return "index"; // Returns src/main/resources/templates/index.html
    }

    /**
     * Handles requests for the /login URL and returns the login view.
     * This ensures that Spring knows how to render the login page.
     * @return The name of the login HTML template ("login").
     */
    @GetMapping("/login")
    public String login() {
        // Check if user is already authenticated
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && 
            !(authentication.getPrincipal() instanceof String && authentication.getPrincipal().equals("anonymousUser"))) {
            // If user is already logged in, redirect to appropriate dashboard
            if (authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
                return "redirect:/admin/dashboard";
            } else {
                return "redirect:/voter/dashboard";
            }
        }
        return "login"; // Returns src/main/resources/templates/login.html
    }
}