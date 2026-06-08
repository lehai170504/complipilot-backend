package com.complipilot.backend.identity.service;

import java.util.UUID;

import com.complipilot.backend.common.error.NotFoundException;
import com.complipilot.backend.identity.dto.UpdateUserProfileRequest;
import com.complipilot.backend.identity.dto.UserProfileResponse;
import com.complipilot.backend.identity.entity.User;
import com.complipilot.backend.identity.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProfileService {

    private final UserRepository userRepository;

    public UserProfileService(UserRepository userRepository) {
        this.userRepository = userRepository;
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
}