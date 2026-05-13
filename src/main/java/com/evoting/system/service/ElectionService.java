package com.evoting.system.service;

import com.evoting.system.model.Election;
import com.evoting.system.repository.ElectionRepository;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

@Service
public class ElectionService {

    private final ElectionRepository electionRepository;

    public ElectionService(ElectionRepository electionRepository) {
        this.electionRepository = electionRepository;
    }

    public Election getElection() {
        // For simplicity, we assume there is only one election record.
        // If it doesn't exist, create a default one.
        return electionRepository.findFirstByOrderByIdAsc().orElseGet(() -> {
            Election newElection = new Election();
            newElection.setName("General Election");
            newElection.setActive(false);
            newElection.setCompleted(false);
            return electionRepository.save(newElection);
        });
    }

    public void scheduleElection(LocalDateTime startTime, LocalDateTime endTime) {
        Election election = getElection();
        election.setStartTime(startTime);
        election.setEndTime(endTime);
        election.setActive(false);
        election.setCompleted(false);
        electionRepository.save(election);
    }

    public void startElection() {
        Election election = getElection();
        election.setActive(true);
        election.setCompleted(false);
        // Optionally set the start time to now if not already scheduled
        if (election.getStartTime() == null) {
            election.setStartTime(LocalDateTime.now());
        }
        electionRepository.save(election);
    }
    
    public Election createElection(Election election) {
        election.setActive(true);
        election.setCompleted(false);
        return electionRepository.save(election);
    }
    
    public Election getActiveElection() {
        return electionRepository.findFirstByActiveTrue().orElse(null);
    }
    
    public Election getMostRecentElection() {
        return electionRepository.findFirstByOrderByIdDesc().orElse(null);
    }
    
    public boolean isElectionInProgress() {
        Election election = getActiveElection();
        return election != null && election.isElectionInProgress();
    }
    
    public boolean isElectionScheduled() {
        Election election = getActiveElection();
        return election != null && election.isElectionScheduled();
    }
    
    public boolean isElectionEnded() {
        Election election = getActiveElection();
        return election != null && election.isElectionEnded();
    }

    public Map<String, Object> getElectionStatus() {
        Election election = getElection();
        Map<String, Object> status = new HashMap<>();
        status.put("active", election.isActive());
        status.put("completed", election.isCompleted());
        status.put("scheduled", election.isElectionScheduled());
        status.put("inProgress", election.isElectionInProgress());
        status.put("ended", election.isElectionEnded());
        status.put("startTime", election.getStartTime());
        status.put("endTime", election.getEndTime());
        return status;
    }

    public void stopElection() {
        Election election = getElection();
        election.setActive(false);
        election.setCompleted(true);
        // Optionally set the end time to now
        if (election.getEndTime() == null || election.getEndTime().isAfter(LocalDateTime.now())) {
            election.setEndTime(LocalDateTime.now());
        }
        electionRepository.save(election);
    }

    public boolean isElectionRunning() {
        Election election = getElection();
        LocalDateTime now = LocalDateTime.now();

        // Check if election is active and not completed
        if (election.isActive() && !election.isCompleted()) {
            return true;
        }
        
        // Handle scheduled start/stop
        if (election.getStartTime() != null && election.getEndTime() != null) {
            return now.isAfter(election.getStartTime()) && now.isBefore(election.getEndTime());
        }

        return false;
    }

    
}
