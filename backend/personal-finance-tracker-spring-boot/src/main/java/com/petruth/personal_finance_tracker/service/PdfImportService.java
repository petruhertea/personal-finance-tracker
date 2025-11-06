package com.petruth.personal_finance_tracker.service;

import com.petruth.personal_finance_tracker.entity.Category;
import com.petruth.personal_finance_tracker.entity.ImportedFile;
import com.petruth.personal_finance_tracker.entity.Transaction;
import com.petruth.personal_finance_tracker.entity.User;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface PdfImportService {
    PdfImportServiceImpl.ImportResult importFromPdf(MultipartFile file, User user, String bankType) throws Exception;

    String extractTextFromPdf(MultipartFile file) throws IOException;

    List<ParsedTransaction> parsePdfByBankType(String text, String bankType);

    public List<ParsedTransaction> parseBCRStatement(String text);

    Transaction convertToTransaction(ParsedTransaction parsed, User user);

    Category autoCategorize(String description, Transaction.TransactionType type);

    Category getCategoryByName(String name);

    String generateTransactionHash(Transaction transaction);

    boolean isDuplicate(String hash, User user);

    ImportedFile createImportBatch(MultipartFile file, User user, String bankType);

    void updateImportBatch(ImportedFile batch, int total, int success,
                           int failed, int duplicates, String error);

    void cleanupOldImports();

    // Inner class for parsed transactions
    class ParsedTransaction {
        LocalDate date;
        BigDecimal amount;
        String description;
        boolean isIncome;

        ParsedTransaction(LocalDate date, BigDecimal amount, String description, boolean isIncome) {
            this.date = date;
            this.amount = amount;
            this.description = description;
            this.isIncome = isIncome;
        }
    }

    class ImportResult {
        private int successCount;
        private List<String> errors;
        private int duplicateCount;

        public ImportResult(int successCount, List<String> errors, int duplicateCount) {
            this.successCount = successCount;
            this.errors = errors;
            this.duplicateCount = duplicateCount;
        }

        public int getSuccessCount() {
            return successCount;
        }

        public List<String> getErrors() {
            return errors;
        }

        public int getDuplicateCount() {
            return duplicateCount;
        }
    }
}
