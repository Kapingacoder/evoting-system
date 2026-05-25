package com.evoting.system.service;

import com.evoting.system.model.Role;
import com.evoting.system.model.User;
import com.evoting.system.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void addVoter(String fullName, String username, 
                         String admissionNumber, String email, String password) {
        User user = new User();
        user.setFullName(fullName);
        
        // Username iwe admission number
        user.setUsername(admissionNumber); 
        user.setAdmissionNumber(admissionNumber);
        user.setEmail(email);
        
        // Password iwe firstname + "123"
        String firstName = fullName.split(" ")[0].toLowerCase();
        String autoPassword = firstName + "123";
        user.setPassword(passwordEncoder.encode(autoPassword));
        
        user.setRole(Role.VOTER);
        user.setVoted(false);
        userRepository.save(user);
    }

    public void changePassword(String username, String currentPassword, String newPassword) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found");
        }

        User user = userOpt.get();
        
        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public void changeUsername(String username, String newUsername) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found");
        }

        // Check if new username already exists
        if (userRepository.findByUsername(newUsername).isPresent()) {
            throw new RuntimeException("Username already exists");
        }

        User user = userOpt.get();
        user.setUsername(newUsername);
        userRepository.save(user);
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public void deleteAllVoters() {
        // Futa voters wote — sio admin
        List<User> voters = userRepository.findAllByRole(Role.VOTER);
        userRepository.deleteAll(voters);
    }
}
