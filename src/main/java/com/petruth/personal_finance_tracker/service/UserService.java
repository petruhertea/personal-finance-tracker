package com.petruth.personal_finance_tracker.service;

import com.petruth.personal_finance_tracker.entity.User;

public interface UserService {
    User save(User user);
    User findByUsername(String username);
    User findByEmail(String email);
    User findByEmailOrUsername(String email, String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    User findById(Long id);

}
