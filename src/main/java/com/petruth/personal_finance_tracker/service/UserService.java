package com.petruth.personal_finance_tracker.service;

import com.petruth.personal_finance_tracker.entity.User;

import java.util.Optional;

public interface UserService {
    User save(User user);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    User findById(Long id);

}
