package com.petruth.personal_finance_tracker.service;

import com.petruth.personal_finance_tracker.entity.User;
import com.petruth.personal_finance_tracker.repository.UserRepository;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@CacheConfig(cacheNames = "users")
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @CacheEvict(key = "#user.username")
    public User save(User user) {
        return userRepository.save(user);
    }

    @Override
    @Cacheable(key = "#username")
    public User findByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username).orElseThrow(()-> new RuntimeException("User not found"));
    }

    @Override
    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(()-> new RuntimeException("User not found"));
    }

    @Override
    public User findByEmailOrUsername(String email, String username) {
        return userRepository.findByEmailOrUsername(email, username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    @Cacheable(key = "#id")
    public User findById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
    }
}
