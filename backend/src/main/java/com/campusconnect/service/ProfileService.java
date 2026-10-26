package com.campusconnect.service;

import org.springframework.stereotype.Service;

import com.campusconnect.dto.ProfileUpdateRequest;
import com.campusconnect.dto.UserResponse;
import com.campusconnect.exception.ResourceNotFoundException;
import com.campusconnect.model.User;
import com.campusconnect.repository.UserRepository;

@Service
public class ProfileService {

    private final UserRepository userRepository;

    public ProfileService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse getProfile(String userId) {
        return UserResponse.from(findUser(userId));
    }

    public UserResponse updateProfile(String userId, ProfileUpdateRequest request) {
        User user = findUser(userId);
        user.setName(request.name().trim());
        user.setDepartment(request.department().trim());
        return UserResponse.from(userRepository.save(user));
    }

    private User findUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
