package com.petruth.personal_finance_tracker.service;

import com.petruth.personal_finance_tracker.entity.Category;
import com.petruth.personal_finance_tracker.entity.Transaction;
import com.petruth.personal_finance_tracker.entity.User;
import org.apache.commons.csv.CSVRecord;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

public interface CsvImportService {
    CsvImportServiceImpl.ImportResult importFromCsv(MultipartFile file, User user,
                                                    CsvImportServiceImpl.CsvImportConfig config) throws Exception;

    Transaction parseRecord(CSVRecord record, User user, CsvImportServiceImpl.CsvImportConfig config);
    String getColumnValue(CSVRecord record, String columnName);
    LocalDateTime parseDate(String dateStr);
    Double parseAmount(String amountStr);
    Transaction.TransactionType determineType(String typeStr, Double amount);
    Category autoCategorize(String description, Transaction.TransactionType type);
    Category getCategoryByName(String name);

    public static class CsvImportConfig {
        private String dateColumn = "Date";
        private String amountColumn = "Amount";
        private String descriptionColumn = "Description";
        private String typeColumn = null; // Optional

        // Constructors, getters, setters
        public CsvImportConfig() {}

        public String getDateColumn() { return dateColumn; }
        public void setDateColumn(String dateColumn) { this.dateColumn = dateColumn; }

        public String getAmountColumn() { return amountColumn; }
        public void setAmountColumn(String amountColumn) { this.amountColumn = amountColumn; }

        public String getDescriptionColumn() { return descriptionColumn; }
        public void setDescriptionColumn(String descriptionColumn) {
            this.descriptionColumn = descriptionColumn;
        }

        public String getTypeColumn() { return typeColumn; }
        public void setTypeColumn(String typeColumn) { this.typeColumn = typeColumn; }
    }

    class ImportResult {
        private int successCount;
        private List<String> errors;

        public ImportResult(int successCount, List<String> errors) {
            this.successCount = successCount;
            this.errors = errors;
        }

        public int getSuccessCount() { return successCount; }
        public List<String> getErrors() { return errors; }
    }
}
