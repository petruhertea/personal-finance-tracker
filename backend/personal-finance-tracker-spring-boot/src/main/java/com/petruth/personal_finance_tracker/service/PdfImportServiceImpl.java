package com.petruth.personal_finance_tracker.service;

import com.petruth.personal_finance_tracker.entity.Category;
import com.petruth.personal_finance_tracker.entity.ImportedFile;
import com.petruth.personal_finance_tracker.entity.Transaction;
import com.petruth.personal_finance_tracker.entity.User;
import com.petruth.personal_finance_tracker.repository.ImportedFileRepository;
import com.petruth.personal_finance_tracker.repository.TransactionRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PdfImportServiceImpl implements PdfImportService{

    private static final Logger logger = LoggerFactory.getLogger(PdfImportService.class);

    private final TransactionService transactionService;
    private final CategoryService categoryService;
    private final ImportedFileRepository importedFileRepository;
    private final TransactionRepository transactionRepository;

    // Romanian date patterns
    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    };

    public PdfImportServiceImpl(TransactionService transactionService,
                            CategoryService categoryService,
                            ImportedFileRepository importedFileRepository,
                            TransactionRepository transactionRepository) {
        this.transactionService = transactionService;
        this.categoryService = categoryService;
        this.importedFileRepository = importedFileRepository;
        this.transactionRepository = transactionRepository;
    }

    /**
     * Import transactions from PDF bank statement
     */
    @Override
    public ImportResult importFromPdf(MultipartFile file, User user, String bankType) throws Exception {

        // Create import batch record
        ImportedFile importBatch = createImportBatch(file, user, bankType);

        List<Transaction> imported = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int duplicates = 0;

        try {
            // Extract text from PDF
            String pdfText = extractTextFromPdf(file);

            // Parse transactions based on bank type
            List<ParsedTransaction> parsedTransactions =
                    parsePdfByBankType(pdfText, bankType);

            // Convert to transactions
            for (ParsedTransaction parsed : parsedTransactions) {
                try {
                    Transaction transaction = convertToTransaction(parsed, user);

                    // Check for duplicates
                    String hash = generateTransactionHash(transaction);
                    transaction.setAmountHash(hash);

                    if (isDuplicate(hash, user)) {
                        duplicates++;
                        continue;
                    }

                    transaction.setSource("pdf_import");
                    transaction.setIsManual(false);
                    transaction.setImportBatchId(importBatch.getId());

                    Transaction saved = transactionService.save(transaction);
                    imported.add(saved);

                } catch (Exception e) {
                    errors.add("Transaction error: " + e.getMessage());
                    logger.warn("Failed to process transaction: {}", e.getMessage());
                }
            }

            // Update import batch
            updateImportBatch(importBatch, parsedTransactions.size(),
                    imported.size(), errors.size(), duplicates, null);

        } catch (Exception e) {
            logger.error("Failed to parse PDF", e);
            updateImportBatch(importBatch, 0, 0, 0, 0,
                    "Failed to parse PDF: " + e.getMessage());
            throw new RuntimeException("Failed to parse PDF: " + e.getMessage());
        }

        logger.info("✅ Imported {} transactions from PDF for user {}",
                imported.size(), user.getId());

        return new ImportResult(imported.size(), errors, duplicates);
    }

    /**
     * Extract text from PDF file
     */
    @Override
    public String extractTextFromPdf(MultipartFile file) throws IOException {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    /**
     * Parse PDF text based on bank type
     */
    @Override
    public List<ParsedTransaction> parsePdfByBankType(String text, String bankType) {
        switch (bankType.toUpperCase()) {
            case "BCR":
                return parseBCRStatement(text);
            case "BRD":
                return parseBRDStatement(text);
            case "ING":
                return parseINGStatement(text);
            case "REVOLUT":
                return parseRevolutStatement(text);
            case "RAIFFEISEN":
                return parseRaiffeisenStatement(text);
            case "AUTO":
                return parseAutoDetect(text);
            default:
                return parseGenericStatement(text);
        }
    }

    /**
     * Parse BCR (Banca Comercială Română) statement
     * Format: Date | Description | Debit | Credit
     */
    @Override
    public List<ParsedTransaction> parseBCRStatement(String text) {
        List<ParsedTransaction> transactions = new ArrayList<>();

        // BCR pattern: 15.01.2024 KAUFLAND BUCURESTI 50,00 -
        // Or: 20.01.2024 SALARIU IANUARIE - 2.500,00
        Pattern pattern = Pattern.compile(
                "(\\d{2}\\.\\d{2}\\.\\d{4})\\s+([A-Z0-9 \\-\\.]+?)\\s+(\\d+,\\d{2}|-)\\s+(\\d+,\\d{2}|-)"
        );

        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            try {
                String dateStr = matcher.group(1);
                String description = matcher.group(2).trim();
                String debit = matcher.group(3);
                String credit = matcher.group(4);

                LocalDate date = parseDate(dateStr);
                BigDecimal amount;
                boolean isIncome;

                if (!debit.equals("-")) {
                    amount = parseRomanianAmount(debit);
                    isIncome = false; // Debit = expense
                } else {
                    amount = parseRomanianAmount(credit);
                    isIncome = true; // Credit = income
                }

                transactions.add(new ParsedTransaction(date, amount, description, isIncome));

            } catch (Exception e) {
                logger.warn("Failed to parse BCR line: {}", matcher.group(0));
            }
        }

        return transactions;
    }

    /**
     * Parse BRD (Groupe Société Générale) statement
     */
    private List<ParsedTransaction> parseBRDStatement(String text) {
        List<ParsedTransaction> transactions = new ArrayList<>();

        // BRD pattern: 15/01/2024 CUMPARATURI CARREFOUR -50.00 RON
        Pattern pattern = Pattern.compile(
                "(\\d{2}/\\d{2}/\\d{4})\\s+(.+?)\\s+([+-]?\\d+\\.\\d{2})\\s*RON"
        );

        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            try {
                String dateStr = matcher.group(1);
                String description = matcher.group(2).trim();
                String amountStr = matcher.group(3);

                LocalDate date = parseDate(dateStr);
                BigDecimal amount = new BigDecimal(amountStr.replace(",", ".")).abs();
                boolean isIncome = amountStr.startsWith("+") ||
                        description.toUpperCase().contains("SALARIU") ||
                        description.toUpperCase().contains("VENIT");

                transactions.add(new ParsedTransaction(date, amount, description, isIncome));

            } catch (Exception e) {
                logger.warn("Failed to parse BRD line: {}", matcher.group(0));
            }
        }

        return transactions;
    }

    /**
     * Parse ING Bank Romania statement
     */
    private List<ParsedTransaction> parseINGStatement(String text) {
        List<ParsedTransaction> transactions = new ArrayList<>();

        // ING pattern: 15-01-2024 Kaufland -50.00
        Pattern pattern = Pattern.compile(
                "(\\d{2}-\\d{2}-\\d{4})\\s+(.+?)\\s+([+-]?\\d+\\.\\d{2})"
        );

        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            try {
                String dateStr = matcher.group(1);
                String description = matcher.group(2).trim();
                String amountStr = matcher.group(3);

                LocalDate date = parseDate(dateStr);
                BigDecimal amount = new BigDecimal(amountStr).abs();
                boolean isIncome = amountStr.startsWith("+") || Double.parseDouble(amountStr) > 0;

                transactions.add(new ParsedTransaction(date, amount, description, isIncome));

            } catch (Exception e) {
                logger.warn("Failed to parse ING line: {}", matcher.group(0));
            }
        }

        return transactions;
    }

    /**
     * Parse Revolut statement
     */
    private List<ParsedTransaction> parseRevolutStatement(String text) {
        List<ParsedTransaction> transactions = new ArrayList<>();

        // Revolut often has cleaner format
        Pattern pattern = Pattern.compile(
                "(\\d{2} [A-Za-z]{3} \\d{4})\\s+(.+?)\\s+([+-]?\\d+\\.\\d{2})"
        );

        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            try {
                String dateStr = matcher.group(1);
                String description = matcher.group(2).trim();
                String amountStr = matcher.group(3);

                LocalDate date = parseDate(dateStr);
                BigDecimal amount = new BigDecimal(amountStr).abs();
                boolean isIncome = amountStr.startsWith("+");

                transactions.add(new ParsedTransaction(date, amount, description, isIncome));

            } catch (Exception e) {
                logger.warn("Failed to parse Revolut line: {}", matcher.group(0));
            }
        }

        return transactions;
    }

    /**
     * Parse Raiffeisen Bank statement
     */
    private List<ParsedTransaction> parseRaiffeisenStatement(String text) {
        List<ParsedTransaction> transactions = new ArrayList<>();

        // Raiffeisen pattern similar to BRD
        Pattern pattern = Pattern.compile(
                "(\\d{2}\\.\\d{2}\\.\\d{4})\\s+(.+?)\\s+(\\d+,\\d{2})\\s+([+-])"
        );

        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            try {
                String dateStr = matcher.group(1);
                String description = matcher.group(2).trim();
                String amountStr = matcher.group(3);
                String sign = matcher.group(4);

                LocalDate date = parseDate(dateStr);
                BigDecimal amount = parseRomanianAmount(amountStr);
                boolean isIncome = sign.equals("+");

                transactions.add(new ParsedTransaction(date, amount, description, isIncome));

            } catch (Exception e) {
                logger.warn("Failed to parse Raiffeisen line: {}", matcher.group(0));
            }
        }

        return transactions;
    }

    /**
     * Auto-detect bank and parse
     */
    private List<ParsedTransaction> parseAutoDetect(String text) {
        // Try each parser and return the one with most results
        List<ParsedTransaction> bcrResults = parseBCRStatement(text);
        List<ParsedTransaction> brdResults = parseBRDStatement(text);
        List<ParsedTransaction> ingResults = parseINGStatement(text);
        List<ParsedTransaction> revolutResults = parseRevolutStatement(text);

        List<ParsedTransaction> bestResults = bcrResults;
        if (brdResults.size() > bestResults.size()) bestResults = brdResults;
        if (ingResults.size() > bestResults.size()) bestResults = ingResults;
        if (revolutResults.size() > bestResults.size()) bestResults = revolutResults;

        return bestResults;
    }

    /**
     * Generic parser for unknown banks
     */
    private List<ParsedTransaction> parseGenericStatement(String text) {
        List<ParsedTransaction> transactions = new ArrayList<>();

        // Try to find anything that looks like: date + description + amount
        Pattern pattern = Pattern.compile(
                "(\\d{2}[\\./\\-]\\d{2}[\\./\\-]\\d{4})\\s+(.+?)\\s+([+-]?\\d+[\\.,]\\d{2})"
        );

        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            try {
                String dateStr = matcher.group(1);
                String description = matcher.group(2).trim();
                String amountStr = matcher.group(3).replace(",", ".");

                LocalDate date = parseDate(dateStr);
                BigDecimal amount = new BigDecimal(amountStr).abs();
                boolean isIncome = amountStr.startsWith("+") ||
                        description.toLowerCase().contains("income") ||
                        description.toLowerCase().contains("salary");

                transactions.add(new ParsedTransaction(date, amount, description, isIncome));

            } catch (Exception e) {
                logger.warn("Failed to parse generic line: {}", matcher.group(0));
            }
        }

        return transactions;
    }

    /**
     * Parse Romanian amount format (1.234,56 or 1234,56)
     */
    private BigDecimal parseRomanianAmount(String amount) {
        // Remove dots (thousands separator) and replace comma with dot
        String cleaned = amount.replace(".", "").replace(",", ".");
        return new BigDecimal(cleaned);
    }

    /**
     * Parse date with multiple formats
     */
    private LocalDate parseDate(String dateStr) {
        for (DateTimeFormatter formatter : DATE_FORMATS) {
            try {
                return LocalDate.parse(dateStr, formatter);
            } catch (Exception e) {
                // Try next format
            }
        }
        throw new RuntimeException("Invalid date format: " + dateStr);
    }

    /**
     * Convert parsed transaction to Transaction entity
     */
    @Override
    public Transaction convertToTransaction(ParsedTransaction parsed, User user) {
        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setDate(parsed.date.atStartOfDay());
        transaction.setAmount(parsed.amount);
        transaction.setType(parsed.isIncome ?
                Transaction.TransactionType.INCOME : Transaction.TransactionType.EXPENSE);
        transaction.setDescription(parsed.description);

        // Auto-categorize
        Category category = autoCategorize(parsed.description, transaction.getType());
        transaction.setCategory(category);

        return transaction;
    }

    /**
     * Auto-categorize based on description
     */
    @Override
    public Category autoCategorize(String description, Transaction.TransactionType type) {
        String lower = description.toLowerCase();

        if (type == Transaction.TransactionType.INCOME) {
            if (lower.contains("salary") || lower.contains("salariu")) {
                return getCategoryByName("Salary");
            }
            return getCategoryByName("Other Income");
        }

        // Expense categories
        if (lower.contains("kaufland") || lower.contains("carrefour") ||
                lower.contains("mega image") || lower.contains("lidl") || lower.contains("auchan")) {
            return getCategoryByName("Groceries");
        }
        if (lower.contains("restaurant") || lower.contains("starbucks") ||
                lower.contains("mcdonald") || lower.contains("kfc")) {
            return getCategoryByName("Dining");
        }
        if (lower.contains("uber") || lower.contains("bolt") ||
                lower.contains("petrom") || lower.contains("omv")) {
            return getCategoryByName("Transportation");
        }
        if (lower.contains("rent") || lower.contains("chirie")) {
            return getCategoryByName("Housing");
        }
        if (lower.contains("enel") || lower.contains("eon") ||
                lower.contains("digi") || lower.contains("orange")) {
            return getCategoryByName("Utilities");
        }

        return getCategoryByName("Other Expenses");
    }

    @Override
    public Category getCategoryByName(String name) {
        try {
            return categoryService.findById(1L);
        } catch (Exception e) {
            logger.warn("Failed to find category: {}", name);
            return null;
        }
    }

    @Override
    public String generateTransactionHash(Transaction transaction) {
        String data = transaction.getAmount().toString() +
                transaction.getDate().toString() +
                transaction.getDescription();
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(data.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return data.hashCode() + "";
        }
    }
    @Override
    public boolean isDuplicate(String hash, User user) {
        return transactionRepository.existsByAmountHashAndUser(hash, user);
    }

    @Override
    public ImportedFile createImportBatch(MultipartFile file, User user, String bankType) {
        ImportedFile importBatch = new ImportedFile();
        importBatch.setUser(user);
        importBatch.setOriginalFilename(file.getOriginalFilename());
        importBatch.setFileSizeBytes(file.getSize());
        importBatch.setImportStatus("processing");
        return importedFileRepository.save(importBatch);
    }

    @Override
    public void updateImportBatch(ImportedFile batch, int total, int success,
                                   int failed, int duplicates, String error) {
        batch.setTotalRows(total);
        batch.setSuccessfulImports(success);
        batch.setFailedImports(failed);
        batch.setDuplicateSkipped(duplicates);
        batch.setImportStatus(error != null ? "failed" : "completed");
        if (error != null) {
            batch.setErrorLog(error);
        }
        importedFileRepository.save(batch);
    }
}
