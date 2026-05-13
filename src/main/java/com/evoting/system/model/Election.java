package com.evoting.system.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import java.time.LocalDateTime;

@Entity
public class Election {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;
    
    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;
    
    @Column(nullable = false)
    private boolean active = false;
    
    @Column(nullable = false)
    private boolean completed = false;
    
    @Column(name = "ended", nullable = false)
    private LocalDateTime ended = LocalDateTime.now();

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

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
        if (completed) {
            this.ended = LocalDateTime.now();
        }
        // Don't set ended to null when completed is false
        // as the database has a NOT NULL constraint on this column
    }
    
    public LocalDateTime getEnded() {
        return ended;
    }
    
    public void setEnded(LocalDateTime ended) {
        this.ended = ended;
    }
    
    // Helper methods for election status
    public boolean isElectionScheduled() {
        return startTime != null && startTime.isAfter(LocalDateTime.now());
    }
    
    public boolean isElectionInProgress() {
        LocalDateTime now = LocalDateTime.now();
        return active && !completed && startTime != null && endTime != null &&
               now.isAfter(startTime) && now.isBefore(endTime);
    }
    
    public boolean isElectionEnded() {
        return completed || (endTime != null && LocalDateTime.now().isAfter(endTime));
    }
    
    public boolean isVotingOpen() {
        LocalDateTime now = LocalDateTime.now();
        return startTime != null && endTime != null &&
               now.isAfter(startTime) && now.isBefore(endTime) &&
               active && !completed;
    }
}
