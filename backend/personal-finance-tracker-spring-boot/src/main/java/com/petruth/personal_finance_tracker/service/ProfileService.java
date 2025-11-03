package com.petruth.personal_finance_tracker.service;

import com.petruth.personal_finance_tracker.dto.AccountStatsDTO;
import com.petruth.personal_finance_tracker.dto.UserDataExportDTO;
import com.petruth.personal_finance_tracker.dto.UserResponse;

public interface ProfileService {
    UserResponse updateEmail(Long userId, String newEmail, String password);
    void changePassword(Long userId, String currentPassword, String newPassword);
    AccountStatsDTO getAccountStats(Long userId);
    UserDataExportDTO exportUserData(Long userId);
    void deleteAccount(Long userId, String password);
    void updateLastLogin(Long userId);
}
