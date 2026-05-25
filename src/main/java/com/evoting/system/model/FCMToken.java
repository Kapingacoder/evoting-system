package com.evoting.system.model;

import jakarta.persistence.*;

@Entity
@Table(name = "fcm_tokens")
public class FCMToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String username;
    private String token;
    private String role; // VOTER au ADMIN
    
    @Column(name = "created_at")
    private java.time.LocalDateTime createdAt;

    // Constructors
    public FCMToken() {}

    public FCMToken(String username, String token, String role) {
        this.username = username;
        this.token = token;
        this.role = role;
        this.createdAt = java.time.LocalDateTime.now();
    }

    // Getters & Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public java.time.LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(java.time.LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
