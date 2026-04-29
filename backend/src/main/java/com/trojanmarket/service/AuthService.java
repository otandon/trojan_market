package com.trojanmarket.service;

import com.trojanmarket.dto.AuthResponseDTO;
import com.trojanmarket.entity.User;
import com.trojanmarket.repository.UserRepository;
import com.trojanmarket.security.ForbiddenException;
import com.trojanmarket.security.JwtService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final String USC_DOMAIN = "@usc.edu";
    private static final int BAN_THRESHOLD = 10;

    private final UserRepository userRepository;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, JwtService jwtService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponseDTO handleSSOLogin(User userFromSSO) {
        validateUSCEmail(userFromSSO.getEmail());
        User user = createUserIfNotExists(userFromSSO);

        String token = jwtService.issueToken(user.getUserID(), user.getEmail(), user.getUsername());
        return AuthResponseDTO.builder()
                .token(token)
                .userID(user.getUserID())
                .username(user.getUsername())
                .email(user.getEmail())
                .isVerified(user.getIsVerified())
                .build();
    }

    @Transactional
    public User createUserIfNotExists(User input) {
        return userRepository.findByEmail(input.getEmail())
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .username(deriveUsername(input.getEmail()))
                            .password("") // SSO accounts have no local password
                            .email(input.getEmail())
                            .isVerified(true) // SSO-attested
                            .review(0)
                            .reviewCount(0)
                            .isActive(true)
                            .build();
                    return userRepository.save(newUser);
                });
    }

    public void validateUSCEmail(String email) {
        if (email == null || !email.toLowerCase().endsWith(USC_DOMAIN)) {
            throw new ForbiddenException("Only @usc.edu emails are accepted");
        }
    }

    public boolean isBannedUser(Integer userID) {
        // TODO: when a Reports table is added, count reports where reportedUserID = userID
        //       and return count >= BAN_THRESHOLD. CLAUDE.md schema does not yet define
        //       this table, so treat all users as not-banned for now.
        return false;
    }

    private String deriveUsername(String email) {
        String local = email.substring(0, email.indexOf('@'));
        String base = local.replaceAll("[^A-Za-z0-9_]", "");
        if (base.length() > 50) {
            base = base.substring(0, 50);
        }
        if (userRepository.existsByUsername(base)) {
            return base + "_" + System.currentTimeMillis();
        }
        return base;
    }
}
