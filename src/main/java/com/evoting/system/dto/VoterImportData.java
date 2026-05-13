package com.evoting.system.dto;

/**
 * DTO for representing a voter record during import.
 */
public class VoterImportData {
    
    private String username;
    private String fullName;
    private String email;
    private String admissionNumber;
    private String generatedPassword;
    private String importError;
    private int rowNumber;
    
    public VoterImportData() {
        this.generatedPassword = "";
        this.importError = null;
        this.rowNumber = 0;
    }
    
    public VoterImportData(String username, String fullName, String email) {
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.generatedPassword = "";
        this.importError = null;
        this.rowNumber = 0;
    }
    
    /**
     * Generates a default password based on the first name.
     * Format: firstName + "123"
     * Example: "John Doe" -> "john123"
     */
    public void generateDefaultPassword() {
        if (fullName != null && !fullName.isEmpty()) {
            String firstName = fullName.trim().split("\\s+")[0].toLowerCase();
            this.generatedPassword = firstName + "123";
        } else {
            this.generatedPassword = "default123";
        }
    }
    
    // Getters and Setters
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getFullName() {
        return fullName;
    }
    
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getGeneratedPassword() {
        return generatedPassword;
    }
    
    public void setGeneratedPassword(String generatedPassword) {
        this.generatedPassword = generatedPassword;
    }
    
    public String getImportError() {
        return importError;
    }
    
    public void setImportError(String importError) {
        this.importError = importError;
    }
    
    public int getRowNumber() {
        return rowNumber;
    }
    
    public void setRowNumber(int rowNumber) {
        this.rowNumber = rowNumber;
    }
    
    public String getAdmissionNumber() {
        return admissionNumber;
    }
    
    public void setAdmissionNumber(String admissionNumber) {
        this.admissionNumber = admissionNumber;
    }
    
    @Override
    public String toString() {
        return String.format("Row %d: [Username: %s, FullName: %s, Email: %s]", 
            rowNumber, username, fullName, email);
    }
}

