// File: src/main/java/com/expense/service/AuthService.java

package com.expense.service;

import com.expense.model.User;
import com.expense.repo.TransactionRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class AuthService {
    // --- THIS IS THE CRUCIAL PART ---
    // A static field to hold the currently logged-in user's ID globally.
    private static int currentUserId = -1;

    private final TransactionRepository repo = new TransactionRepository();

    public boolean login(String username, String password) throws Exception {
        User user = repo.findUserByUsername(username);
        if (user == null) {
            return false;
        }
        String passwordHash = hashPassword(password);
        if (user.getPasswordHash().equals(passwordHash)) {
            // If login is successful, store the user's ID.
            currentUserId = user.getId();
            return true;
        }
        return false;
    }

    public boolean signup(String username, String password) throws Exception {
        if (repo.findUserByUsername(username) != null) {
            return false;
        }
        String passwordHash = hashPassword(password);
        repo.createUser(username, passwordHash);
        return true;
    }

    public void logout() {
        currentUserId = -1; // Reset the user ID on logout.
    }

    public static int getCurrentUserId() {
        if (currentUserId == -1) {
            throw new IllegalStateException("No user is currently logged in.");
        }
        return currentUserId;
    }

    private String hashPassword(String password) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] encodedhash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
        for (byte b : encodedhash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}