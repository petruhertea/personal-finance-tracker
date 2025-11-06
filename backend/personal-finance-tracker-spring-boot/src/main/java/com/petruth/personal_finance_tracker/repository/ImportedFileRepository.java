package com.petruth.personal_finance_tracker.repository;

import com.petruth.personal_finance_tracker.entity.ImportedFile;
import com.petruth.personal_finance_tracker.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ImportedFileRepository extends JpaRepository<ImportedFile, Long> {
    List<ImportedFile> findByUserOrderByImportedAtDesc(User user);
    List<ImportedFile> findByUserAndImportStatus(User user, String status);
    List<ImportedFile> findByImportedAtBefore(LocalDateTime threshold);

    void deleteByImportedAtBefore(LocalDateTime threshold);
}