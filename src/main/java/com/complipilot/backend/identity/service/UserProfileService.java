package com.complipilot.backend.identity.service;

import java.util.UUID;

import com.complipilot.backend.common.error.BadRequestException;
import com.complipilot.backend.common.error.NotFoundException;
import com.complipilot.backend.identity.dto.ChangePasswordRequest;
import com.complipilot.backend.identity.dto.UpdateUserProfileRequest;
import com.complipilot.backend.identity.dto.UserProfileResponse;
import com.complipilot.backend.identity.entity.User;
import com.complipilot.backend.identity.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserProfileService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID currentUserId) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        return toResponse(user);
    }

    @Transactional
    public UserProfileResponse updateProfile(
            UUID currentUserId,
            UpdateUserProfileRequest request
    ) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        user.updateProfile(request.fullName());

        return toResponse(user);
    }

    private UserProfileResponse toResponse(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getStatus(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    @Transactional
    public void changePassword(
            UUID currentUserId,
            ChangePasswordRequest request
    ) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (!passwordEncoder.matches(
                request.currentPassword(),
                user.getPasswordHash()
        )) {
            throw new BadRequestException("Current password is incorrect");
        }

        if (passwordEncoder.matches(
                request.newPassword(),
                user.getPasswordHash()
        )) {
            throw new BadRequestException("New password must be different from the current password");
        }

        user.changePassword(passwordEncoder.encode(request.newPassword()));
    }
}