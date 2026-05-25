package com.evoting.system.controller;

import com.evoting.system.model.SupportMessage;
import com.evoting.system.model.User;
import com.evoting.system.repository.SupportMessageRepository;
import com.evoting.system.repository.UserRepository;
import com.evoting.system.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private SupportMessageRepository supportMessageRepository;

    @Autowired
    private JavaMailSender mailSender;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");

        User user = userRepository.findByUsername(username).orElse(null);

        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            return ResponseEntity.status(401)
                .body(Map.of("error", "Admission number au password si sahihi"));
        }

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().toString());

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("role", user.getRole().toString());
        response.put("fullName", user.getFullName());
        response.put("username", user.getUsername());
        response.put("admissionNumber", user.getAdmissionNumber());

        return ResponseEntity.ok(response);
    }

    // POST /api/auth/support-message
    @PostMapping("/support-message")
    public ResponseEntity<?> sendSupportMessage(@RequestBody Map<String, String> request) {
        try {
            String admissionNumber = request.get("admissionNumber");
            String message = request.get("message");

            if (admissionNumber == null || admissionNumber.isEmpty() ||
                message == null || message.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Admission number na message zinahitajika"));
            }

            SupportMessage supportMessage = new SupportMessage(admissionNumber, message);
            supportMessageRepository.save(supportMessage);

            return ResponseEntity.ok(Map.of("message", "Ujumbe umetumwa!"));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // POST /api/auth/forgot-password
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(
            @RequestBody Map<String, String> request) {
        try {
            String admissionNumber = request.get("admissionNumber");

            // Tafuta user kwa admissionNumber AU username
            User user = userRepository.findByAdmissionNumber(admissionNumber)
                .orElse(null);

            if (user == null) {
                user = userRepository.findByUsername(admissionNumber)
                    .orElse(null);
            }

            if (user == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Admission number haipatikani"));
            }

            if (user.getEmail() == null || user.getEmail().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Akaunti hii haina email iliyosajiliwa"));
            }

            // Tengeneza password
            String firstName = user.getFullName().split(" ")[0].toLowerCase();
            String defaultPassword = firstName + "123";

            // Tuma email — LAZIMA itumie mailSender
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setTo(user.getEmail());
            mail.setSubject("Forgot Password — E-Voting System");
            mail.setText(
                "Habari " + user.getFullName() + ",\n\n" +
                "Umesahau password yako.\n\n" +
                "Taarifa zako za kuingia:\n" +
                "Username: " + user.getAdmissionNumber() + "\n" +
                "Password: " + defaultPassword + "\n\n" +
                "Kama hukuhitaji hili — ignore ujumbe huu.\n\n" +
                "Asante,\nMfumo wa E-Voting"
            );
            mailSender.send(mail);

            return ResponseEntity.ok(Map.of(
                "message", "Password imetumwa kwa email yako!"
            ));

        } catch (Exception e) {
            // Hii itaonyesha error halisi
            return ResponseEntity.status(500)
                .body(Map.of("error", "Imeshindwa: " + e.getMessage()));
        }
    }
}
