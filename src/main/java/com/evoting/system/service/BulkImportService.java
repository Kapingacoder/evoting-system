package com.evoting.system.service;

import com.evoting.system.dto.VoterImportData;
import com.evoting.system.dto.VoterImportResult;
import com.evoting.system.model.Role;
import com.evoting.system.model.User;
import com.evoting.system.repository.UserRepository;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Service for bulk importing voters from Excel or CSV files.
 */
@Service
public class BulkImportService {

    private static final String[] EXCEL_REQUIRED_HEADERS = {"Full Name", "Admission Number"};
    private static final String[] CSV_REQUIRED_HEADERS = {"Full Name", "Admission Number"};
    // Email is now optional, and no default password is set - users will set their own during registration
    
    private final UserRepository userRepository;

    public BulkImportService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Process an uploaded file (Excel or CSV) and import voters.
     * 
     * @param file The uploaded file
     * @return VoterImportResult containing details of the import operation
     */
    @Transactional(rollbackFor = Exception.class)
    public VoterImportResult importVoters(MultipartFile file) {
        VoterImportResult result = new VoterImportResult();
        
        if (file == null || file.isEmpty()) {
            result.addError("No file uploaded or file is empty");
            return result;
        }

        String filename = file.getOriginalFilename();
        if (filename == null || filename.isEmpty()) {
            result.addError("Invalid filename");
            return result;
        }

        try {
            // Parse the file first to validate its contents
            List<VoterImportData> voters = parseFile(file, filename);
            
            if (voters.isEmpty()) {
                result.addError("No valid voter data found in file. Please check file format and column headers.");
                return result;
            }
            
            // Process each voter
            return processVoters(voters, result);

        } catch (IOException e) {
            result.addError("Error reading file: " + e.getMessage());
            return result;
        } catch (Exception e) {
            result.addError("Unexpected error during import: " + e.getMessage());
            throw new RuntimeException("Failed to import voters: " + e.getMessage(), e);
        }
    }
    
    /**
     * Parse the uploaded file based on its type.
     */
    private List<VoterImportData> parseFile(MultipartFile file, String filename) throws IOException, CsvValidationException {
        try (InputStream inputStream = file.getInputStream()) {
            if (filename.toLowerCase().endsWith(".csv")) {
                return parseCSV(inputStream);
            } else if (filename.toLowerCase().endsWith(".xlsx") || filename.toLowerCase().endsWith(".xls")) {
                return parseExcel(inputStream);
            } else {
                throw new IllegalArgumentException("Unsupported file format. Please upload .xlsx, .xls, or .csv files only.");
            }
        }
    }
    
    /**
     * Process the list of voters and save them to the database.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected VoterImportResult processVoters(List<VoterImportData> voters, VoterImportResult result) {
        result.setTotalProcessed(voters.size());
        List<User> usersToSave = new ArrayList<>();
        Set<String> admissionNumbersInFile = new HashSet<>();
        
        // First pass: validate all records
        for (VoterImportData voter : voters) {
            try {
                // Validate required fields
                if (voter.getAdmissionNumber() == null || voter.getAdmissionNumber().trim().isEmpty()) {
                    result.addFailedRecord(voter, "Admission number is required");
                    continue;
                }
                
                // Trim and clean data
                String admissionNumber = voter.getAdmissionNumber().trim();
                String fullName = voter.getFullName() != null ? voter.getFullName().trim() : "";
                String email = voter.getEmail() != null ? voter.getEmail().trim() : null;
                
                // Validate full name is provided
                if (fullName.isEmpty()) {
                    result.addFailedRecord(voter, "Full name is required");
                    continue;
                }
                
                // Check for duplicate admission numbers in the file
                if (admissionNumbersInFile.contains(admissionNumber.toLowerCase())) {
                    result.addFailedRecord(voter, "Duplicate admission number in file");
                    continue;
                }
                
                // Check if user already exists in the database
                if (userRepository.existsByAdmissionNumber(admissionNumber)) {
                    result.addFailedRecord(voter, "Voter with this admission number already exists");
                    continue;
                }
                
                try {
                    // Create new user
                    User newUser = new User();
                    newUser.setFullName(fullName);
                    newUser.setAdmissionNumber(admissionNumber);
                    newUser.setUsername(admissionNumber); // Username same as admission number
                    
                    // Set email if provided and valid
                    if (email != null && !email.isEmpty()) {
                        if (email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                            newUser.setEmail(email);
                        } else {
                            result.addFailedRecord(voter, "Invalid email format");
                            continue;
                        }
                    }
                    
                    // No password set - user will set it during registration
                    newUser.setPassword(null);
                    newUser.setRole(Role.VOTER);
                    newUser.setVoted(false);
                    
                    usersToSave.add(newUser);
                    admissionNumbersInFile.add(admissionNumber.toLowerCase());
                    result.incrementSuccessful();
                    
                } catch (Exception e) {
                    result.addFailedRecord(voter, "Error creating user: " + e.getMessage());
                    continue;
                }
            } catch (Exception e) {
                result.addFailedRecord(voter, "Error processing voter: " + e.getMessage());
            }
        }

        // Second pass: save all valid users in a batch
        if (!usersToSave.isEmpty()) {
            try {
                userRepository.saveAll(usersToSave);
                System.out.println("Successfully saved " + usersToSave.size() + " voters to database");
            } catch (Exception e) {
                // Convert to unchecked exception to trigger rollback
                throw new RuntimeException("Failed to save voters: " + e.getMessage(), e);
            }
        }
        
        return result;
    }

    /**
     * Parse Excel file and extract voter data.
     * Dynamically finds columns by header name and ignores extra columns.
     */
    private List<VoterImportData> parseExcel(InputStream inputStream) throws IOException {
        List<VoterImportData> voters = new ArrayList<>();
        
        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new IOException("No sheet found in the Excel file");
            }

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new IOException("No header row found in the Excel file");
            }

            // Read all headers and store in a map (header name -> column index)
            Map<String, Integer> headerMap = new HashMap<>();
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                Cell cell = headerRow.getCell(i);
                String header = getCellStringValue(cell);
                if (!header.isEmpty()) {
                    headerMap.put(header.toLowerCase(), i);
                }
            }

            // Check if required headers are present
            for (String required : EXCEL_REQUIRED_HEADERS) {
                if (!headerMap.containsKey(required.toLowerCase())) {
                    throw new IOException("Missing required column: " + required);
                }
            }

            // Skip header row, iterate through data rows
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isEmptyRow(row)) {
                    continue;
                }

                VoterImportData voter = new VoterImportData();
                voter.setRowNumber(i + 1);

                // Get full name - find column by header name
                Integer fullNameCol = headerMap.get("full name");
                if (fullNameCol != null && fullNameCol < row.getLastCellNum()) {
                    String fullName = getCellStringValue(row.getCell(fullNameCol));
                    voter.setFullName(fullName);
                }

                // Get admission number - find column by header name
                Integer admissionNumberCol = headerMap.get("admission number");
                if (admissionNumberCol != null && admissionNumberCol < row.getLastCellNum()) {
                    String admissionNumber = getCellStringValue(row.getCell(admissionNumberCol));
                    voter.setAdmissionNumber(admissionNumber);
                }

                // Get email - find column by header name (optional)
                Integer emailCol = headerMap.get("email");
                if (emailCol != null && emailCol < row.getLastCellNum()) {
                    String email = getCellStringValue(row.getCell(emailCol));
                    voter.setEmail(email.isEmpty() ? null : email);
                }

                // Skip rows with missing required data
                if (voter.getFullName() == null || voter.getFullName().trim().isEmpty()) {
                    continue;
                }

                voters.add(voter);
            }
        }

        return voters;
    }

    /**
     * Parse CSV file and extract voter data.
     * Dynamically finds columns by header name and ignores extra columns.
     */
    private List<VoterImportData> parseCSV(InputStream inputStream) throws IOException, CsvValidationException {
        List<VoterImportData> voters = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String[] headers = reader.readNext();
            if (headers == null || headers.length == 0) {
                throw new IOException("No header row found in the CSV file");
            }

            // Read all headers and store in a map (header name -> column index)
            Map<String, Integer> headerMap = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                String header = headers[i].trim();
                if (!header.isEmpty()) {
                    headerMap.put(header.toLowerCase(), i);
                }
            }

            // Check if required headers are present
            for (String required : CSV_REQUIRED_HEADERS) {
                if (!headerMap.containsKey(required.toLowerCase())) {
                    throw new IOException("Missing required column: " + required);
                }
            }

            String[] row;
            int rowNumber = 2; // Start from row 2 (after header)
            while ((row = reader.readNext()) != null) {
                if (row.length == 0 || (row.length == 1 && row[0].trim().isEmpty())) {
                    rowNumber++;
                    continue;
                }

                VoterImportData voter = new VoterImportData();
                voter.setRowNumber(rowNumber);

                // Get full name - find column by header name
                Integer fullNameCol = headerMap.get("full name");
                if (fullNameCol != null && fullNameCol < row.length) {
                    String fullName = row[fullNameCol].trim();
                    voter.setFullName(fullName);
                }

                // Get admission number - find column by header name
                Integer admissionNumberCol = headerMap.get("admission number");
                if (admissionNumberCol != null && admissionNumberCol < row.length) {
                    String admissionNumber = row[admissionNumberCol].trim();
                    voter.setAdmissionNumber(admissionNumber);
                }

                // Get email - find column by header name (optional)
                Integer emailCol = headerMap.get("email");
                if (emailCol != null && emailCol < row.length) {
                    String email = row[emailCol].trim();
                    voter.setEmail(email.isEmpty() ? null : email);
                }

                // Skip rows with missing required data
                if (voter.getFullName() == null || voter.getFullName().trim().isEmpty()) {
                    rowNumber++;
                    continue;
                }

                voters.add(voter);
                rowNumber++;
            }
        }

        return voters;
    }

    // Method removed as we're now creating users directly in the processVoters method
    
    /**
     * Generate a random password. Not used since users will set their own password during registration.
     * @return A randomly generated password (not currently used)
     */
    // Method removed as it's no longer needed since users will set their own passwords

    /**
     * Get string value from a cell.
     */
    private String getCellStringValue(Cell cell) {
        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toString();
                }
                double numValue = cell.getNumericCellValue();
                if (numValue == Math.floor(numValue)) {
                    return String.valueOf((long) numValue);
                }
                return String.valueOf(numValue);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }

    /**
     * Check if a row is empty (all cells are null or empty).
     */
    private boolean isEmptyRow(Row row) {
        for (int i = 0; i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            String value = getCellStringValue(cell);
            if (!value.isEmpty()) {
                return false;
            }
        }
        return true;
    }
}

