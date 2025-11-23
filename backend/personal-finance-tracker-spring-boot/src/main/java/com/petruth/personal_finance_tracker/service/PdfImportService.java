package com.petruth.personal_finance_tracker.service;

import com.petruth.personal_finance_tracker.entity.Category;
import com.petruth.personal_finance_tracker.entity.ImportedFile;
import com.petruth.personal_finance_tracker.entity.Transaction;
import com.petruth.personal_finance_tracker.entity.User;
import com.petruth.personal_finance_tracker.repository.ImportedFileRepository;
import com.petruth.personal_finance_tracker.repository.TransactionRepository;
import jakarta.transaction.Transactional;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// mai sus în clasă (static)


@Service
public class PdfImportService {

    private static final Logger logger = LoggerFactory.getLogger(PdfImportService.class);
    private static final Map<String, Integer> ROMANIAN_MONTHS = new HashMap<>();
    private static final Map<String, List<Pattern>> CATEGORY_PATTERNS = new LinkedHashMap<>();

    static {
        // Abrevieri + forme complete (fără/ cu punct). Adaugă altele dacă apar diferit în PDF-uri.
        ROMANIAN_MONTHS.put("ian", 1);
        ROMANIAN_MONTHS.put("ian.", 1);
        ROMANIAN_MONTHS.put("ianuarie", 1);
        ROMANIAN_MONTHS.put("feb", 2);
        ROMANIAN_MONTHS.put("feb.", 2);
        ROMANIAN_MONTHS.put("februarie", 2);
        ROMANIAN_MONTHS.put("mar", 3);
        ROMANIAN_MONTHS.put("mar.", 3);
        ROMANIAN_MONTHS.put("martie", 3);
        ROMANIAN_MONTHS.put("apr", 4);
        ROMANIAN_MONTHS.put("apr.", 4);
        ROMANIAN_MONTHS.put("aprilie", 4);
        ROMANIAN_MONTHS.put("mai", 5);
        ROMANIAN_MONTHS.put("mai.", 5);
        ROMANIAN_MONTHS.put("iun", 6);
        ROMANIAN_MONTHS.put("iun.", 6);
        ROMANIAN_MONTHS.put("iunie", 6);
        ROMANIAN_MONTHS.put("iul", 7);
        ROMANIAN_MONTHS.put("iul.", 7);
        ROMANIAN_MONTHS.put("iulie", 7);
        ROMANIAN_MONTHS.put("aug", 8);
        ROMANIAN_MONTHS.put("aug.", 8);
        ROMANIAN_MONTHS.put("august", 8);
        ROMANIAN_MONTHS.put("sep", 9);
        ROMANIAN_MONTHS.put("sep.", 9);
        ROMANIAN_MONTHS.put("septembrie", 9);
        ROMANIAN_MONTHS.put("oct", 10);
        ROMANIAN_MONTHS.put("oct.", 10);
        ROMANIAN_MONTHS.put("octombrie", 10);
        ROMANIAN_MONTHS.put("nov", 11);
        ROMANIAN_MONTHS.put("nov.", 11);
        ROMANIAN_MONTHS.put("noiembrie", 11);
        ROMANIAN_MONTHS.put("dec", 12);
        ROMANIAN_MONTHS.put("dec.", 12);
        ROMANIAN_MONTHS.put("decembrie", 12);

        CATEGORY_PATTERNS.put("Groceries", Arrays.asList(
                Pattern.compile("\\b(kaufland|carrefour|lidl|auchan|mega\\s*image|profi|discount|corona)\\b", Pattern.CASE_INSENSITIVE)
        ));
        CATEGORY_PATTERNS.put("Dining", Arrays.asList(
                Pattern.compile("\\b(restaurant|pizzeri|mcdonald|kfc|burger|starbucks|cafe|bistro)\\b", Pattern.CASE_INSENSITIVE)
        ));
        CATEGORY_PATTERNS.put("Transportation", Arrays.asList(
                Pattern.compile("\\b(uber|bolt|taxi|petrom|omv|rompetrol|benzinarie|bus|tramvai|metro)\\b", Pattern.CASE_INSENSITIVE)
        ));
        CATEGORY_PATTERNS.put("Housing", Arrays.asList(
                Pattern.compile("\\b(chirie|rent|apartament|locuinta|garsoniera)\\b", Pattern.CASE_INSENSITIVE)
        ));
        CATEGORY_PATTERNS.put("Utilities", Arrays.asList(
                Pattern.compile("\\b(enel|eon|digi|orange|vodafone|gaz|energie|apă|apa|water|electric)\\b", Pattern.CASE_INSENSITIVE)
        ));
        CATEGORY_PATTERNS.put("Salary", Arrays.asList(
                Pattern.compile("\\b(salari(u|u)|plata\\s+salariului|salary|venit)\\b", Pattern.CASE_INSENSITIVE)
        ));
        CATEGORY_PATTERNS.put("Fees", Arrays.asList(
                Pattern.compile("\\b(comision|taxa|fee|penaliz|dobanda|dobândă)\\b", Pattern.CASE_INSENSITIVE)
        ));
        // fallback categories (keep at end)
        CATEGORY_PATTERNS.put("Other Expenses", Arrays.asList(
                Pattern.compile(".*", Pattern.CASE_INSENSITIVE)
        ));
        CATEGORY_PATTERNS.put("Other Income", Arrays.asList(
                Pattern.compile(".*", Pattern.CASE_INSENSITIVE)
        ));
    }

    private final TransactionService transactionService;
    private final CategoryService categoryService;
    private final ImportedFileRepository importedFileRepository;
    private final TransactionRepository transactionRepository;

    // Romanian date patterns (cover date-only, date+time and month names in Romanian)
    private static final DateTimeFormatter[] DATE_FORMATS = new DateTimeFormatter[]{
            // date-only (common separators)
            DateTimeFormatter.ofPattern("d.MM.yyyy"),
            DateTimeFormatter.ofPattern("dd.MM.yyyy"),
            DateTimeFormatter.ofPattern("d/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("d-MM-yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),

            // date + time (if PDF contains timestamps)
            DateTimeFormatter.ofPattern("d.MM.yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("d/MM/yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("d-MM-yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),

            // month names in Romanian (e.g. "4 nov. 2025" or "4 nov 2025")
            DateTimeFormatter.ofPattern("d MMM yyyy", new Locale("ro")),
            DateTimeFormatter.ofPattern("dd MMM yyyy", new Locale("ro")),
            DateTimeFormatter.ofPattern("d MMM. yyyy", new Locale("ro")),
            DateTimeFormatter.ofPattern("dd MMM. yyyy", new Locale("ro"))
    };

    public PdfImportService(TransactionService transactionService,
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

    @Transactional
    public ImportResult importFromPdf(MultipartFile file, User user, String bankType) throws Exception {
        ImportedFile importBatch = createImportBatch(file, user, bankType);

        List<Transaction> imported = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int duplicates = 0;

        try {
            logger.info("📄 Processing PDF: {} ({}KB) for user: {} with bankType: {}",
                    file.getOriginalFilename(),
                    file.getSize() / 1024,
                    user.getUsername(),
                    bankType);

            // Extract text from PDF
            String pdfText = extractTextFromPdf(file);
            logger.info("📝 Extracted {} characters from PDF", pdfText.length());

            // Log first 500 characters for debugging
            logger.debug("📄 PDF Text Sample:\n{}",
                    pdfText.substring(0, Math.min(500, pdfText.length())));

            // Parse transactions based on bank type
            List<ParsedTransaction> parsedTransactions =
                    parsePdfByBankType(pdfText, bankType);

            logger.info("🔍 Found {} transactions in PDF", parsedTransactions.size());

            // If no transactions found, try auto-detect
            if (parsedTransactions.isEmpty() && !"AUTO".equalsIgnoreCase(bankType)) {
                logger.warn("⚠️ No transactions found with {} parser, trying AUTO-DETECT...", bankType);
                parsedTransactions = parseAutoDetect(pdfText);
                logger.info("🔍 Auto-detect found {} transactions", parsedTransactions.size());
            }

            // Convert to transactions
            int processedCount = 0;
            for (ParsedTransaction parsed : parsedTransactions) {
                processedCount++;
                try {
                    Transaction transaction = convertToTransaction(parsed, user);
                    transaction = sanitizeTransaction(transaction); // Add sanitization

                    // Check for duplicates
                    String hash = generateTransactionHash(transaction);
                    transaction.setAmountHash(hash);

                    if (isDuplicate(hash, user)) {
                        duplicates++;
                        logger.debug("⏭️ Skipped duplicate: {} - {}", parsed.date, parsed.description);
                        continue;
                    }

                    transaction.setSource("pdf_import");
                    transaction.setIsManual(false);
                    transaction.setImportBatchId(importBatch.getId());

                    Transaction saved = transactionService.save(transaction);
                    imported.add(saved);

                    logger.debug("✅ Imported: {} | {} | {} | {}",
                            saved.getDate().toLocalDate(),
                            saved.getType(),
                            saved.getAmount(),
                            saved.getDescription().substring(0, Math.min(30, saved.getDescription().length())));

                } catch (Exception e) {
                    String errorMsg = String.format("Row %d: %s - %s",
                            processedCount,
                            e.getMessage(),
                            parsed.description.substring(0, Math.min(50, parsed.description.length())));
                    errors.add(errorMsg);
                    logger.error("❌ Failed to process transaction {}: {}", processedCount, e.getMessage());
                }
            }

            // Update import batch
            updateImportBatch(importBatch, parsedTransactions.size(),
                    imported.size(), errors.size(), duplicates, null);

            logger.info("✅ Import complete: {} imported, {} duplicates, {} errors",
                    imported.size(), duplicates, errors.size());

        } catch (Exception e) {
            logger.error("❌ PDF import failed for file: {}", file.getOriginalFilename(), e);
            updateImportBatch(importBatch, 0, 0, 0, 0,
                    "Failed to parse PDF: " + e.getMessage());
            throw new RuntimeException("Failed to parse PDF: " + e.getMessage());
        }

        return new ImportResult(imported.size(), errors, duplicates);
    }

    // CRITICAL: Remove account numbers during import
    private Transaction sanitizeTransaction(Transaction tx) {
        String description = tx.getDescription();

        // Remove IBAN patterns (RO##...)
        description = description.replaceAll("\\bRO\\d{2}[A-Z]{4}\\d{16}\\b", "[IBAN]");

        // Remove card numbers (****1234)
        description = description.replaceAll("\\*{4}\\d{4}", "[CARD]");

        // Remove reference numbers that might be sensitive
        description = description.replaceAll("\\bREF:\\s*[A-Z0-9-]+\\b", "REF:[REDACTED]");

        tx.setDescription(description);
        return tx;
    }

    /**
     * Extract text from PDF file
     */

    private String extractTextFromPdf(MultipartFile file) throws IOException {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    /**
     * Parse PDF text based on bank type
     */

    public List<ParsedTransaction> parsePdfByBankType(String text, String bankType) {
        logger.info("🏦 Selected bank type: {}", bankType);

        List<ParsedTransaction> transactions;

        switch (bankType.toUpperCase()) {
            case "BCR":
                logger.info("Using BCR parser");
                transactions = parseBCRStatement(text);
                break;
            case "BRD":
                logger.info("Using BRD parser");
                transactions = parseBRDStatement(text);
                break;
            case "ING":
                logger.info("Using ING parser");
                transactions = parseINGStatement(text);
                break;
            case "REVOLUT":
                logger.info("Using Revolut parser");
                transactions = parseRevolutStatement(text);
                break;
            case "RAIFFEISEN":
                logger.info("Using Raiffeisen parser");
                transactions = parseRaiffeisenStatement(text);
                break;
            case "CEC":
                logger.info("Using CEC parser");
                transactions = parseCECStatement(text);
                logger.info("🔍 Found {} transactions with CEC parser", transactions.size());

                // If CEC parser fails, try auto-detect
                if (transactions.isEmpty()) {
                    logger.warn("⚠️ No transactions found with CEC parser, trying AUTO-DETECT...");
                    transactions = parseAutoDetect(text);
                }
                break;
            case "AUTO":
                logger.info("Using AUTO-DETECT parser");
                transactions = parseAutoDetect(text);
                break;
            default:
                logger.info("Using generic parser");
                transactions = parseGenericStatement(text);
                break;
        }

        logger.info("✅ Parser returned {} transactions", transactions.size());
        return transactions;
    }

    /**
     * Parse BCR (Banca Comercială Română) statement
     * Format: Date | Description | Debit | Credit
     */

    private List<ParsedTransaction> parseBCRStatement(String text) {
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
    /**
     * Parse Revolut statement (Enhanced for Romanian format)
     */
    private List<ParsedTransaction> parseRevolutStatement(String text) {
        List<ParsedTransaction> transactions = new ArrayList<>();
        if (text == null || text.isBlank()) return transactions;

        // Split pe linii; fiecare linie conține de obicei: "4 nov. 2025  Descriere  €1.00  €..."
        String[] lines = text.split("\\r?\\n");
        Pattern dateStart = Pattern.compile("^(\\d{1,2}\\s+[\\p{L}]{3,}\\.?.*?\\d{4})\\s+(.*)$", Pattern.CASE_INSENSITIVE);

        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty()) continue;

            Matcher m = dateStart.matcher(line);
            if (!m.find()) continue;

            try {
                String datePart = m.group(1).trim();
                String rest = m.group(2).trim();

                // Split controller from right to get last 1 or 2 amount-like tokens
                String[] tokens = rest.split("\\s+");
                // find tokens from end that look like amounts
                List<String> amountTokens = new ArrayList<>();
                int idx = tokens.length - 1;
                while (idx >= 0 && amountTokens.size() < 2) {
                    String t = tokens[idx].replaceAll("[^0-9,\\.\\-+€RONron]", "");
                    if (t.matches(".*\\d.*")) {
                        amountTokens.add(0, tokens[idx]); // keep original order
                    } else {
                        break;
                    }
                    idx--;
                }

                String description = String.join(" ", java.util.Arrays.copyOfRange(tokens, 0, Math.max(0, idx + 1))).trim();
                if (description.isEmpty() && amountTokens.size() > 0) {
                    // if description empty, try to recover by using whole controller minus amounts
                    description = rest.replace(amountTokens.stream().reduce((a, b) -> a + "\\s*" + b).orElse(""), "").trim();
                }

                BigDecimal debit = null;
                BigDecimal credit = null;
                if (amountTokens.size() == 2) {
                    debit = parseRomanianAmount(amountTokens.get(0));
                    credit = parseRomanianAmount(amountTokens.get(1));
                } else if (amountTokens.size() == 1) {
                    // single amount -> ambiguous, treat as debit unless description suggests inflow
                    BigDecimal val = parseRomanianAmount(amountTokens.get(0));
                    // heuristics
                    if (description.toLowerCase().contains("alimentare") ||
                            description.toLowerCase().contains("deposit") ||
                            description.toLowerCase().contains("admiss") ||
                            description.toLowerCase().contains("added") ||
                            description.toLowerCase().contains("alimenta")) {
                        credit = val;
                    } else {
                        debit = val;
                    }
                }

                LocalDate date = parseDate(datePart);
                if (debit != null && debit.compareTo(BigDecimal.ZERO) > 0) {
                    transactions.add(new ParsedTransaction(date, debit, description, false));
                }
                if (credit != null && credit.compareTo(BigDecimal.ZERO) > 0) {
                    transactions.add(new ParsedTransaction(date, credit, description, true));
                }
            } catch (Exception e) {
                logger.warn("Failed to parse Revolut line: {} (err: {})", raw, e.getMessage());
            }
        }

        // fallback: keep old simple parser if nothing parsed
        if (transactions.isEmpty()) {
            transactions = parseRevolutSimpleFormat(text);
        }

        return transactions;
    }

    private List<ParsedTransaction> parseRevolutSimpleFormat(String text) {
        List<ParsedTransaction> transactions = new ArrayList<>();
        Pattern pattern = Pattern.compile("(\\d{1,2}\\s+[\\p{L}]{3,}\\.??\\s+\\d{4})\\s+(.+?)\\s+([+-]?€?[\\d,\\.]+)\\s*(?:([+-]?€?[\\d,\\.]+))?", Pattern.CASE_INSENSITIVE);

        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            try {
                String dateStr = matcher.group(1);
                String description = matcher.group(2).trim();
                String amt1 = matcher.group(3);
                String amt2 = matcher.group(4);

                LocalDate date = parseDate(dateStr);
                BigDecimal amount = parseRomanianAmount(amt1);
                boolean isIncome = amt1.startsWith("+") || (amt2 != null && amt2.startsWith("+"));

                // heuristics: if second amount exists and is larger -> decide which is credit/debit
                if (amt2 != null && !amt2.isBlank()) {
                    BigDecimal alt = parseRomanianAmount(amt2);
                    // assume the larger is balance/other, keep both? We'll add the one that fits positive logic
                    // For safety, add both as separate entries (common in revolut export last column reflects balance)
                    transactions.add(new ParsedTransaction(date, amount.abs(), description, amt1.startsWith("+")));
                    // don't add alt as transaction (often it's balance)
                } else {
                    transactions.add(new ParsedTransaction(date, amount.abs(), description, isIncome));
                }

            } catch (Exception e) {
                logger.warn("Failed to parse Revolut simple line: {}", matcher.group(0));
            }
        }

        return transactions;
    }

    /**
     * Parse CEC Bank statement - Final version handling all quirks
     */
    private List<ParsedTransaction> parseCECStatement(String text) {
        List<ParsedTransaction> transactions = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) {
            return transactions;
        }

        logger.info("🔍 Parsing CEC Bank statement...");

        // Find the transaction section (after "Detalii tranzacții")
        int transactionStartIndex = text.indexOf("Detalii tranzacții");
        if (transactionStartIndex == -1) {
            logger.warn("⚠️ Could not find 'Detalii tranzacții' section in PDF");
            return transactions;
        }

        // Only parse text after the transaction header
        String transactionText = text.substring(transactionStartIndex);
        String[] lines = transactionText.split("\\r?\\n");

        logger.debug("📊 Processing {} lines from transaction section", lines.length);

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();

            // Look for transaction date pattern (DD-MM-YYYY at start of line)
            if (line.matches("^\\d{2}-\\d{2}-\\d{4}$")) {
                try {
                    // Extract date from first column
                    String dateStr = line.substring(0, 10);
                    LocalDate transactionDate = parseDate(dateStr);

                    if (transactionDate == null) {
                        continue;
                    }

                    // Look ahead for description and amount
                    String description = "";
                    BigDecimal amount = null;
                    boolean isIncome = false;

                    // Scan next lines for transaction details
                    for (int j = i + 1; j < Math.min(i + 20, lines.length); j++) {
                        String currentLine = lines[j].trim();

                        // Stop if we hit another transaction date
                        if (currentLine.matches("^\\d{2}-\\d{2}-\\d{4}.*")) {
                            break;
                        }

                        // Check for amount line (starts with + or -)
                        if (currentLine.matches("^[+\\-]\\s*[\\d,\\.]+$")) {
                            String sign = currentLine.substring(0, 1);
                            String amountStr = currentLine.substring(1).trim();
                            amount = parseRomanianAmount(amountStr);
                            isIncome = sign.equals("+");
                            i = j; // Move main loop past this transaction
                            break;
                        }

                        // Build description from relevant lines
                        if (!currentLine.isEmpty() &&
                                !currentLine.matches("^\\d+$") && // Skip pure numbers
                                !currentLine.matches("^[A-Z]{2}\\d{2}[A-Z0-9]+$") && // Skip IBANs
                                !currentLine.matches("^GL\\d+$") && // Skip GL references
                                !currentLine.matches("^ISS\\d+$") && // Skip ISS references
                                !currentLine.matches("^\\d{12,}$") && // Skip long reference numbers
                                !currentLine.contains("IBAN") &&
                                !currentLine.contains("Platitor:") &&
                                !currentLine.contains("Banca Platitor:") &&
                                !currentLine.startsWith("RO") && // Skip IBANs
                                !currentLine.startsWith("FTSB")) { // Skip bank codes

                            description += (description.isEmpty() ? "" : " ") + currentLine;
                        }
                    }

                    // If we found both date and amount, create transaction
                    if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {

                        // Clean up description
                        description = description
                                .replaceAll("\\s+", " ")
                                .replaceAll("Card:\\d{4}\\s*[X\\s]+\\d{4}", "Card")
                                .replaceAll("POS comerciant", "")
                                .trim();

                        // Extract merchant name from card transactions
                        if (description.contains("Card") && description.contains("RON")) {
                            String[] parts = description.split("RON");
                            if (parts.length > 1) {
                                description = parts[1].trim();
                            }
                        }

                        // Additional type detection from keywords
                        String descLower = description.toLowerCase();

                        // Detect income transactions
                        if (descLower.contains("incasare") ||
                                descLower.contains("salariu") ||
                                descLower.contains("plata salariu") ||
                                descLower.contains("dobanda") ||
                                descLower.contains("alimentare")) {
                            isIncome = true;
                        }

                        // Detect expense transactions
                        if (descLower.contains("card:") ||
                                descLower.contains("pos comerciant") ||
                                descLower.contains("retragere") ||
                                descLower.contains("comision") ||
                                descLower.contains("impozit")) {
                            isIncome = false;
                        }

                        // Ensure we have some description
                        if (description.isEmpty() || description.length() < 3) {
                            description = isIncome ? "Income transaction" : "Expense transaction";
                        }

                        transactions.add(new ParsedTransaction(
                                transactionDate,
                                amount,
                                description,
                                isIncome
                        ));

                        logger.debug("📝 Parsed: {} | {} | {} | {}",
                                transactionDate,
                                isIncome ? "INCOME" : "EXPENSE",
                                amount,
                                description.substring(0, Math.min(50, description.length())));
                    }

                } catch (Exception e) {
                    logger.warn("⚠️ Failed to parse CEC line {}: {}", i, e.getMessage());
                }
            }
        }

        logger.info("✅ CEC parser found {} transactions", transactions.size());
        return transactions;
    }

    /**
     * Auto-categorize based on description
     */
    private Category autoCategorize(String description, Transaction.TransactionType type) {
        String lower = description.toLowerCase();

        // Income categories
        if (type == Transaction.TransactionType.INCOME) {
            if (lower.contains("salariu") ||
                    lower.contains("plata salariu") ||
                    lower.contains("salary") ||
                    lower.contains("cognizant") ||
                    lower.contains("wage")) {
                return getCategoryByName("Salary");
            }
            if (lower.contains("dobanda") ||
                    lower.contains("dobândă") ||
                    lower.contains("interest")) {
                return getCategoryByName("Interest");
            }
            return getCategoryByName("Other Income");
        }

        // Expense categories - Romanian merchants

        // Groceries
        if (lower.contains("kaufland") ||
                lower.contains("carrefour") ||
                lower.contains("profi") ||
                lower.contains("cora") ||
                lower.contains("mega image") ||
                lower.contains("lidl") ||
                lower.contains("penny") ||
                lower.contains("auchan") ||
                lower.contains("magazin alimentar")) {
            return getCategoryByName("Groceries");
        }

        // Dining & Restaurants
        if (lower.contains("restaurant") ||
                lower.contains("pizz") ||
                lower.contains("japanese food") ||
                lower.contains("riki") ||
                lower.contains("chopstix") ||
                lower.contains("mcdonald") ||
                lower.contains("kfc") ||
                lower.contains("burger")) {
            return getCategoryByName("Dining");
        }

        // Fuel & Transportation
        if (lower.contains("omv") ||
                lower.contains("petrom") ||
                lower.contains("mol") ||
                lower.contains("rompetrol") ||
                lower.contains("lukoil") ||
                lower.contains("laur el-bia")) { // Auto service
            return getCategoryByName("Transportation");
        }

        // Shopping & Electronics
        if (lower.contains("altex") ||
                lower.contains("emag") ||
                lower.contains("b&b") ||
                lower.contains("fashion days") ||
                lower.contains("h&m") ||
                lower.contains("zara") ||
                lower.contains("decathlon")) {
            return getCategoryByName("Shopping");
        }

        // Utilities & Bills
        if (lower.contains("vodafone") ||
                lower.contains("orange") ||
                lower.contains("digi") ||
                lower.contains("telekom") ||
                lower.contains("enel") ||
                lower.contains("eon") ||
                lower.contains("payu*vodafone")) {
            return getCategoryByName("Utilities");
        }

        // Pharmacy & Health
        if (lower.contains("farmaci") ||
                lower.contains("delifarm") ||
                lower.contains("catena") ||
                lower.contains("sensiblu") ||
                lower.contains("help net")) {
            return getCategoryByName("Health");
        }

        // Entertainment & Gaming
        if (lower.contains("blizzard") ||
                lower.contains("steam") ||
                lower.contains("netflix") ||
                lower.contains("spotify") ||
                lower.contains("cinema") ||
                lower.contains("aws") || // Cloud services
                lower.contains("city park mall")) {
            return getCategoryByName("Entertainment");
        }

        // Banking operations
        if (lower.contains("comision") ||
                lower.contains("commission") ||
                lower.contains("taxa") ||
                lower.contains("impozit")) {
            return getCategoryByName("Bank Fees");
        }

        // ATM Withdrawals
        if (lower.contains("retragere numerar") ||
                lower.contains("retragere atm") ||
                lower.contains("cash withdrawal") ||
                lower.contains("atm")) {
            return getCategoryByName("Cash Withdrawal");
        }

        // Transfers
        if (lower.contains("revolut") ||
                lower.contains("transfer") ||
                lower.contains("virament")) {
            return getCategoryByName("Transfers");
        }

        // Construction/Hardware stores
        if (lower.contains("constructii") ||
                lower.contains("bricolaj")) {
            return getCategoryByName("Home & Garden");
        }

        return getCategoryByName("Other Expenses");
    }

    /**
     * Parse CEC transaction buffer - handles combined reference+amount
     */
    private ParsedTransaction parseCECBuffer(List<String> buffer) {
        if (buffer.size() < 3) {
            logger.debug("CEC: Buffer too small ({}): {}", buffer.size(), buffer);
            return null;
        }

        try {
            LocalDate date = null;
            StringBuilder descBuilder = new StringBuilder();
            BigDecimal amount = null;
            String amountSign = "";

            int idx = 0;

            // Line 1: Transaction date
            if (buffer.get(idx).matches("\\d{2}-\\d{2}-\\d{4}")) {
                date = parseDate(buffer.get(idx));
                idx++;
            } else {
                logger.debug("CEC: No valid date at start");
                return null;
            }

            // Line 2: Settlement date (skip)
            if (idx < buffer.size() && buffer.get(idx).matches("\\d{2}-\\d{2}-\\d{4}")) {
                idx++;
            }

            // Collect all description lines until we hit the GL reference + amount line
            while (idx < buffer.size()) {
                String line = buffer.get(idx);

                // Check if this line contains GL reference + amount
                // Pattern: "GL000017870368 -  0.03" or "GL000017870368  - 0.03"
                Pattern refAmountPattern = Pattern.compile("(GL\\d{8,})\\s+([-+])\\s*(\\d+[,\\.]\\d{2})");
                Matcher matcher = refAmountPattern.matcher(line);

                if (matcher.find()) {
                    // Found the reference + amount line
                    amountSign = matcher.group(2);
                    String amountStr = matcher.group(3);
                    amount = parseRomanianAmount(amountStr);

                    // Don't add this line to description
                    break;
                }

                // Skip standalone numbers (column artifacts)
                if (line.matches("^\\d+$")) {
                    idx++;
                    continue;
                }

                // Add to description
                if (descBuilder.length() > 0) {
                    descBuilder.append(" ");
                }
                descBuilder.append(line);
                idx++;
            }

            String description = descBuilder.toString().trim();

            // Validation
            if (date == null || amount == null || description.isEmpty()) {
                logger.debug("CEC: Incomplete - date={}, amount={}, desc={}",
                        date, amount, description);
                return null;
            }

            // Determine if income or expense
            boolean isIncome = isCECIncome(description, amountSign);

            logger.debug("CEC Parsed: {} | {} {} | {}",
                    date,
                    isIncome ? "+" : "-",
                    amount,
                    description.substring(0, Math.min(50, description.length())));

            return new ParsedTransaction(date, amount.abs(), description, isIncome);

        } catch (Exception e) {
            logger.warn("CEC: Failed to parse buffer - {}", e.getMessage());
            logger.debug("CEC: Buffer was: {}", buffer);
            return null;
        }
    }

    /**
     * Determine if CEC transaction is income
     */
    private boolean isCECIncome(String description, String amountSign) {
        String lower = description.toLowerCase();

        // In CEC Bank statements:
        // "-" prefix means EXPENSE (money out)
        // "+" prefix means INCOME (money in)
        // NO prefix usually means INCOME (interest, deposits, etc.)

        // Romanian income keywords
        if (lower.contains("dobanda") || lower.contains("dobândă")) {
            // "Plata dobanda" = interest PAYMENT (could be either way)
            // If it's "plata dobanda" with minus, it's interest you paid (expense)
            // If it's "dobanda creditata" with no minus, it's interest received (income)
            if (lower.contains("plata") && amountSign.equals("-")) {
                return false; // You paid interest (expense)
            }
            return true; // You received interest (income)
        }

        if (lower.contains("alimentare") ||      // Top-up
                lower.contains("depunere") ||        // Deposit
                lower.contains("transfer primit") || // Received transfer
                lower.contains("salariu") ||         // Salary
                lower.contains("venit") ||           // Income
                lower.contains("intrare")) {         // Incoming
            return true;
        }

        // Expense keywords
        if (lower.contains("retragere") ||       // Withdrawal
                lower.contains("cumparare") ||       // Purchase
                lower.contains("plata") ||           // Payment
                lower.contains("comision") ||        // Fee
                lower.contains("iesire")) {          // Outgoing
            return false;
        }

        // Default: Use the sign from the PDF
        // In CEC: "-" = expense, "+" or empty = income
        return !amountSign.equals("-");
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
        if (amount == null) throw new RuntimeException("Amount null");

        // remove currency symbols and non-number chars except + - . ,
        String cleaned = amount.replaceAll("[^0-9,\\.\\-+]", "");

        // If both dot and comma present -> assume dot is thousands sep, comma decimal: "1.234,56"
        if (cleaned.contains(".") && cleaned.contains(",")) {
            cleaned = cleaned.replaceAll("\\.", ""); // remove thousands dots
            cleaned = cleaned.replace(',', '.');    // comma -> dot decimal
        } else {
            // If only comma present -> decimal comma
            if (cleaned.contains(",") && !cleaned.contains(".")) {
                cleaned = cleaned.replace(',', '.');
            }
            // if only dot present -> keep as-is (dot as decimal)
        }

        // If string is like "+10" or "-50", still ok for BigDecimal
        if (cleaned.isEmpty() || cleaned.equals("+") || cleaned.equals("-")) {
            throw new RuntimeException("Invalid amount: " + amount);
        }

        return new BigDecimal(cleaned);
    }


    /**
     * Parse date with multiple formats
     */
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null) throw new RuntimeException("Date string null");
        String s = dateStr.trim().replaceAll("\\s+", " ");

        // 1) CASE: "4 nov. 2025" sau "1 noiembrie 2025"
        Matcher m = Pattern.compile("^(\\d{1,2})\\s+([\\p{L}\\.]+)\\s+(\\d{4})$", Pattern.CASE_INSENSITIVE).matcher(s);
        if (m.matches()) {
            String day = m.group(1);
            String monthWord = m.group(2).toLowerCase(Locale.ROOT).replaceAll("\\.", "");
            String year = m.group(3);

            Integer monthNum = ROMANIAN_MONTHS.get(monthWord);
            if (monthNum != null) {
                String normalized = day + "." + (monthNum < 10 ? "0" + monthNum : monthNum) + "." + year;
                try {
                    return LocalDate.parse(normalized, DateTimeFormatter.ofPattern("d.MM.yyyy"));
                } catch (Exception e) {
                    // fallthrough to other parsers
                }
            }
        }

        for (DateTimeFormatter formatter : DATE_FORMATS) {
            try {
                return LocalDate.parse(s, formatter);
            } catch (Exception e1) {
                try {
                    return LocalDateTime.parse(s, formatter).toLocalDate();
                } catch (Exception e2) {
                    // next formatter
                }
            }
        }

        // 3) Ultima încercare: normalizează separatori comuni și parse numeric simplu
        String alt = s.replace("/", ".").replace("-", ".").replaceAll("\\s+", " ");
        try {
            return LocalDate.parse(alt, DateTimeFormatter.ofPattern("d.MM.yyyy"));
        } catch (Exception e) {
            throw new RuntimeException("Invalid date format: " + dateStr);
        }
    }


    /**
     * Convert parsed transaction to Transaction entity
     */
    private Transaction convertToTransaction(ParsedTransaction parsed, User user) {
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


    private Category getCategoryByName(String name) {
        try {
            return categoryService.findByName(name);
        } catch (Exception e) {
            logger.warn("Failed to find category: {}", name);
            return null;
        }
    }

    private String generateTransactionHash(Transaction transaction) {
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

    private boolean isDuplicate(String hash, User user) {
        return transactionRepository.existsByAmountHashAndUser(hash, user);
    }


    private ImportedFile createImportBatch(MultipartFile file, User user, String bankType) {
        ImportedFile importBatch = new ImportedFile();
        importBatch.setUser(user);
        importBatch.setOriginalFilename(file.getOriginalFilename());
        importBatch.setFileSizeBytes(file.getSize());
        importBatch.setImportStatus("processing");
        return importedFileRepository.save(importBatch);
    }


    private void updateImportBatch(ImportedFile batch, int total, int success,
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

    // ✅ Auto-delete import records after 90 days
    @Scheduled(cron = "0 0 2 * * *") // Daily at 2 AM
    public void cleanupOldImports() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(90);
        importedFileRepository.deleteByImportedAtBefore(threshold);
    }

    // Inner class for parsed transactions
    public static class ParsedTransaction {
        LocalDate date;
        public BigDecimal amount;
        public String description;
        public boolean isIncome;

        ParsedTransaction(LocalDate date, BigDecimal amount, String description, boolean isIncome) {
            this.date = date;
            this.amount = amount;
            this.description = description;
            this.isIncome = isIncome;
        }
    }

    public static class ImportResult {
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
