package com.evoting.system.config;

import com.evoting.system.model.Role;
import com.evoting.system.model.User;
import com.evoting.system.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataLoader(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // Ongeza watumiaji tu ikiwa database haina watumiaji
        if (userRepository.count() == 0) {
            // 1. Tengeneza Admin
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123")); // Nenosiri: admin123
            admin.setRole(Role.ADMIN);
            admin.setVoted(false);
            userRepository.save(admin);

            // 2. Tengeneza Mpiga Kura
            User voter = new User();
            voter.setUsername("voter");
            voter.setPassword(passwordEncoder.encode("voter123")); // Nenosiri: voter123
            voter.setRole(Role.VOTER);
            voter.setVoted(false);
            userRepository.save(voter);

            System.out.println("Watumiaji wa mfano (admin, voter) wameongezwa kwenye database.");
        }
    }
}