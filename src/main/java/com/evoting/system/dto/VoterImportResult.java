package com.evoting.system.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO for tracking the result of a bulk voter import operation.
 */
public class VoterImportResult {
    
    private int totalProcessed;
    private int successfulCount;
    private int failedCount;
    private List<String> errors;
    private List<VoterImportData> failedRecords;
    
    public VoterImportResult() {
        this.totalProcessed = 0;
        this.successfulCount = 0;
        this.failedCount = 0;
        this.errors = new ArrayList<>();
        this.failedRecords = new ArrayList<>();
    }
    
    public void incrementProcessed() {
        this.totalProcessed++;
    }
    
    public void incrementSuccessful() {
        this.successfulCount++;
    }
    
    public void incrementFailed() {
        this.failedCount++;
    }
    
    public void addError(String error) {
        this.errors.add(error);
    }
    
    public void addFailedRecord(VoterImportData record, String reason) {
        this.failedCount++;
        record.setImportError(reason);
        this.failedRecords.add(record);
    }
    
    // Getters and Setters
    public int getTotalProcessed() {
        return totalProcessed;
    }
    
    public void setTotalProcessed(int totalProcessed) {
        this.totalProcessed = totalProcessed;
    }
    
    public int getSuccessfulCount() {
        return successfulCount;
    }
    
    public void setSuccessfulCount(int successfulCount) {
        this.successfulCount = successfulCount;
    }
    
    public int getFailedCount() {
        return failedCount;
    }
    
    public void setFailedCount(int failedCount) {
        this.failedCount = failedCount;
    }
    
    public List<String> getErrors() {
        return errors;
    }
    
    public void setErrors(List<String> errors) {
        this.errors = errors;
    }
    
    public List<VoterImportData> getFailedRecords() {
        return failedRecords;
    }
    
    public void setFailedRecords(List<VoterImportData> failedRecords) {
        this.failedRecords = failedRecords;
    }
    
    public boolean isFullySuccessful() {
        return failedCount == 0;
    }
    
    public String getSummary() {
        return String.format("Processed: %d, Successful: %d, Failed: %d", 
            totalProcessed, successfulCount, failedCount);
    }
}

