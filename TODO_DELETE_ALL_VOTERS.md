# Delete All Voters Feature - Implementation Plan

## Task: Add "Delete All Voters" button to /admin/voters page

## Steps to Complete:

### Step 1: Add deleteAllVoters endpoint to AdminController.java
- Location: Line ~312 (after deleteVoter method)
- Add new POST endpoint: `/admin/voters/delete-all`
- Delete all voters with Role.VOTER
- Add success/error flash attributes

### Step 2: Add "Delete All Voters" button to voter-management.html
- Location: In the voters list section, near the header
- Add button with danger styling (red)
- Add JavaScript confirmation dialog
- Use a form for POST request with CSRF token

### Step 3: Test the functionality
- Navigate to http://localhost:8080/admin/voters
- Verify the button appears
- Test the deletion with confirmation
- Verify all voters are deleted

## Status:
- [x] Step 1: Add endpoint to AdminController.java
- [x] Step 2: Add button to voter-management.html
- [ ] Step 3: Test the functionality

