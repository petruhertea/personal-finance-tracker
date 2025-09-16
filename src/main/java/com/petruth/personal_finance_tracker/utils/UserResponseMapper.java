package com.petruth.personal_finance_tracker.utils;

import com.petruth.personal_finance_tracker.dto.UserResponse;
import com.petruth.personal_finance_tracker.entity.User;

public class UserResponseMapper {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail()
        );
    }
}
