package com.evoting.system.model;

import jakarta.persistence.*;

@Entity
@Table(name = "candidate_tickets")
public class CandidateTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    // President information
    @Column(name = "president_name", nullable = false)
    private String presidentName;
    
    @Column(name = "president_party", nullable = false)
    private String presidentParty;
    
    @Column(name = "president_photo_url")
    private String presidentPhotoUrl;
    
    // Vice President information
    @Column(name = "vice_president_name", nullable = false)
    private String vicePresidentName;
    
    @Column(name = "vice_president_party", nullable = false)
    private String vicePresidentParty;
    
    @Column(name = "vice_president_photo_url")
    private String vicePresidentPhotoUrl;
    
    // Vote tracking
    @Column(name = "vote_count", nullable = false)
    private int voteCount = 0;
    
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;
    
    @Column(name = "created_at")
    private java.time.LocalDateTime createdAt;

    public CandidateTicket() {
        this.createdAt = java.time.LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPresidentName() {
        return presidentName;
    }

    public void setPresidentName(String presidentName) {
        this.presidentName = presidentName;
    }

    public String getPresidentParty() {
        return presidentParty;
    }

    public void setPresidentParty(String presidentParty) {
        this.presidentParty = presidentParty;
    }

    public String getPresidentPhotoUrl() {
        return presidentPhotoUrl;
    }

    public void setPresidentPhotoUrl(String presidentPhotoUrl) {
        this.presidentPhotoUrl = presidentPhotoUrl;
    }

    public String getVicePresidentName() {
        return vicePresidentName;
    }

    public void setVicePresidentName(String vicePresidentName) {
        this.vicePresidentName = vicePresidentName;
    }

    public String getVicePresidentParty() {
        return vicePresidentParty;
    }

    public void setVicePresidentParty(String vicePresidentParty) {
        this.vicePresidentParty = vicePresidentParty;
    }

    public String getVicePresidentPhotoUrl() {
        return vicePresidentPhotoUrl;
    }

    public void setVicePresidentPhotoUrl(String vicePresidentPhotoUrl) {
        this.vicePresidentPhotoUrl = vicePresidentPhotoUrl;
    }

    public int getVoteCount() {
        return voteCount;
    }

    public void setVoteCount(int voteCount) {
        this.voteCount = voteCount;
    }
    
    public void incrementVoteCount() {
        this.voteCount++;
    }
    
    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }

    public java.time.LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(java.time.LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

