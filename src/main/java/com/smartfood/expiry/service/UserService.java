package com.smartfood.expiry.service;

import com.smartfood.expiry.entity.User;
import com.smartfood.expiry.enums.RoleType;
import com.smartfood.expiry.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public User createUser(String username, String email, String rawPassword, String fullName, RoleType role) {
        String normalizedUsername = username == null ? "" : username.trim();
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        String normalizedName = fullName == null ? "" : fullName.trim();

        if (normalizedUsername.length() < 3 || normalizedUsername.length() > 50) {
            throw new IllegalArgumentException("Username must be between 3 and 50 characters");
        }
        if (normalizedEmail.isBlank() || normalizedEmail.length() > 100 || !normalizedEmail.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("Enter a valid email address (up to 100 characters)");
        }
        if (normalizedName.isBlank() || normalizedName.length() > 100) {
            throw new IllegalArgumentException("Full name is required and must be 100 characters or fewer");
        }
        if (rawPassword == null || rawPassword.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }
        if (role == null) {
            throw new IllegalArgumentException("User role is required");
        }
        if (userRepository.existsByUsername(normalizedUsername)) {
            throw new IllegalArgumentException("Username already exists: " + normalizedUsername);
        }
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Email already exists: " + normalizedEmail);
        }

        User user = new User(
                normalizedUsername,
                normalizedEmail,
                passwordEncoder.encode(rawPassword),
                normalizedName,
                role
        );
        return userRepository.save(user);
    }

    public User updateUser(Long id, String fullName, String email, RoleType role, String newPassword, Boolean active) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + id));

        user.setFullName(fullName.trim());
        user.setEmail(email.trim().toLowerCase());
        user.setRole(role);
        if (active != null) {
            user.setActive(active);
        }
        if (newPassword != null && !newPassword.trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(newPassword.trim()));
        }

        return userRepository.save(user);
    }

    public void toggleUserStatus(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + id));
        user.setActive(!user.isActive());
        userRepository.save(user);
    }
}
