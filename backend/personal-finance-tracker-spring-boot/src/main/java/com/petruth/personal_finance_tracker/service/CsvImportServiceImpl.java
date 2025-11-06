// CsvImportService.java
package com.petruth.personal_finance_tracker.service;

import com.petruth.personal_finance_tracker.entity.Category;
import com.petruth.personal_finance_tracker.entity.ImportedFile;
import com.petruth.personal_finance_tracker.entity.Transaction;
import com.petruth.personal_finance_tracker.entity.User;
import com.petruth.personal_finance_tracker.repository.ImportedFileRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Service
public class CsvImportServiceImpl implements CsvImportService{

    private static final Logger logger = LoggerFactory.getLogger(CsvImportServiceImpl.class);

    private final TransactionService transactionService;
    private final CategoryService categoryService;
    private final ImportedFileRepository importedFileRepository;

    // Common date formats used by banks
    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"),
            DateTimeFormatter.ISO_LOCAL_DATE
    };

    public CsvImportServiceImpl(TransactionService transactionService,
                            CategoryService categoryService,
                                ImportedFileRepository importedFileRepository) {
        this.transactionService = transactionService;
        this.categoryService = categoryService;
        this.importedFileRepository = importedFileRepository;
    }

    /**
     * Import transactions from CSV file
     */
    @Override
    public ImportResult importFromCsv(MultipartFile file, User user,
                                      CsvImportConfig config) throws Exception {

        // Create import batch record
        ImportedFile importBatch = new ImportedFile();
        importBatch.setUser(user);
        importBatch.setOriginalFilename(file.getOriginalFilename());
        importBatch.setFileSizeBytes(file.getSize());
        importBatch.setDateColumn(config.getDateColumn());
        importBatch.setAmountColumn(config.getAmountColumn());
        importBatch.setDescriptionColumn(config.getDescriptionColumn());
        importBatch.setTypeColumn(config.getTypeColumn());
        importBatch.setImportStatus("processing");
        importBatch = importedFileRepository.save(importBatch);

        List<Transaction> imported = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int totalRows = 0;
        int duplicates = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            CSVParser csvParser = CSVFormat.DEFAULT
                    .withFirstRecordAsHeader()
                    .withIgnoreHeaderCase()
                    .withTrim()
                    .parse(reader);

            int rowNumber = 1;
            for (CSVRecord record : csvParser) {
                rowNumber++;
                try {
                    Transaction transaction = parseRecord(record, user, config);
                    if (transaction != null) {
                        Transaction saved = transactionService.save(transaction);
                        imported.add(saved);
                    }
                } catch (Exception e) {
                    errors.add("Row " + rowNumber + ": " + e.getMessage());
                    logger.warn("Failed to parse row {}: {}", rowNumber, e.getMessage());
                }
            }

        } catch (Exception e) {
            logger.error("Failed to parse CSV file", e);
            throw new RuntimeException("Failed to parse CSV file: " + e.getMessage());
        }

        logger.info("✅ Imported {} transactions for user {}", imported.size(), user.getId());

        return new ImportResult(imported.size(), errors);
    }

    /**
     * Parse a single CSV record into a Transaction
     */
    @Override
    public Transaction parseRecord(CSVRecord record, User user, CsvImportConfig config) {
        // Get date
        String dateStr = getColumnValue(record, config.getDateColumn());
        LocalDateTime date = parseDate(dateStr);
        if (date == null) {
            throw new RuntimeException("Invalid date format: " + dateStr);
        }

        // Get amount
        String amountStr = getColumnValue(record, config.getAmountColumn());
        Double amount = parseAmount(amountStr);
        if (amount == null || amount == 0) {
            throw new RuntimeException("Invalid amount: " + amountStr);
        }

        // Determine transaction type
        Transaction.TransactionType type;
        if (config.getTypeColumn() != null) {
            String typeStr = getColumnValue(record, config.getTypeColumn());
            type = determineType(typeStr, amount);
        } else {
            type = amount > 0 ? Transaction.TransactionType.INCOME : Transaction.TransactionType.EXPENSE;
        }

        // Get description
        String description = getColumnValue(record, config.getDescriptionColumn());
        if (description == null || description.isEmpty()) {
            description = "Imported transaction";
        }

        // Auto-categorize based on description
        Category category = autoCategorize(description, type);

        // Build transaction
        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setDate(date);
        transaction.setAmount(Math.abs(amount));
        transaction.setType(type);
        transaction.setDescription(description);
        transaction.setCategory(category);

        return transaction;
    }

    /**
     * Get column value by name (case-insensitive)
     */
    @Override
    public String getColumnValue(CSVRecord record, String columnName) {
        if (columnName == null) return null;

        // Try exact match first
        if (record.isMapped(columnName)) {
            return record.get(columnName);
        }

        // Try case-insensitive match
        for (String header : record.toMap().keySet()) {
            if (header.equalsIgnoreCase(columnName)) {
                return record.get(header);
            }
        }

        return null;
    }

    /**
     * Parse date string using multiple formats
     */
    @Override
    public LocalDateTime parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;

        for (DateTimeFormatter formatter : DATE_FORMATS) {
            try {
                LocalDate date = LocalDate.parse(dateStr, formatter);
                return date.atStartOfDay();
            } catch (DateTimeParseException e) {
                // Try next format
            }
        }

        return null;
    }

    /**
     * Parse amount from string (handles various formats)
     */
    @Override
    public Double parseAmount(String amountStr) {
        if (amountStr == null || amountStr.isEmpty()) return null;

        try {
            // Remove currency symbols, spaces, and commas
            String cleaned = amountStr
                    .replaceAll("[^0-9.,-]", "")
                    .replace(",", ".");

            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Determine transaction type from type column or amount
     */
    @Override
    public Transaction.TransactionType determineType(String typeStr, Double amount) {
        if (typeStr != null) {
            typeStr = typeStr.toLowerCase();
            if (typeStr.contains("income") || typeStr.contains("credit") ||
                    typeStr.contains("deposit") || typeStr.contains("+")) {
                return Transaction.TransactionType.INCOME;
            }
            if (typeStr.contains("expense") || typeStr.contains("debit") ||
                    typeStr.contains("withdrawal") || typeStr.contains("-")) {
                return Transaction.TransactionType.EXPENSE;
            }
        }

        return amount > 0 ? Transaction.TransactionType.INCOME : Transaction.TransactionType.EXPENSE;
    }

    /**
     * Auto-categorize transaction based on description
     */
    @Override
    public Category autoCategorize(String description, Transaction.TransactionType type) {
        String lowerDesc = description.toLowerCase();

        // Income categories
        if (type == Transaction.TransactionType.INCOME) {
            if (lowerDesc.contains("salary") || lowerDesc.contains("wage")) {
                return getCategoryByName("Salary");
            }
            return getCategoryByName("Other Income");
        }

        // Expense categories
        if (lowerDesc.contains("groceries") || lowerDesc.contains("supermarket") ||
                lowerDesc.contains("market") || lowerDesc.contains("food")) {
            return getCategoryByName("Groceries");
        }
        if (lowerDesc.contains("restaurant") || lowerDesc.contains("cafe") ||
                lowerDesc.contains("pizza") || lowerDesc.contains("mcdonald")) {
            return getCategoryByName("Dining");
        }
        if (lowerDesc.contains("uber") || lowerDesc.contains("taxi") ||
                lowerDesc.contains("transport") || lowerDesc.contains("gas")) {
            return getCategoryByName("Transportation");
        }
        if (lowerDesc.contains("rent") || lowerDesc.contains("mortgage")) {
            return getCategoryByName("Housing");
        }
        if (lowerDesc.contains("electric") || lowerDesc.contains("water") ||
                lowerDesc.contains("utilities") || lowerDesc.contains("internet")) {
            return getCategoryByName("Utilities");
        }
        if (lowerDesc.contains("netflix") || lowerDesc.contains("spotify") ||
                lowerDesc.contains("entertainment")) {
            return getCategoryByName("Entertainment");
        }

        return getCategoryByName("Other");
    }

    /**
     * Get category by name (case-insensitive)
     */
    @Override
    public Category getCategoryByName(String name) {
        try {
            // This assumes you have predefined categories with IDs 1-10
            // Adjust based on your actual category setup
            return categoryService.findById(1L); // Default to first category
        } catch (Exception e) {
            logger.warn("Failed to find category: {}", name);
            return null;
        }
    }
}