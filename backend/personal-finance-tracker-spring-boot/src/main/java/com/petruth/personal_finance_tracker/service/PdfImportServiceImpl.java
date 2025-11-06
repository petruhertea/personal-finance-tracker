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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.HashMap;
import java.util.Map;

// mai sus în clasă (static)


@Service
public class PdfImportServiceImpl implements PdfImportService{

    private static final Logger logger = LoggerFactory.getLogger(PdfImportService.class);
    private static final Map<String, Integer> ROMANIAN_MONTHS = new HashMap<>();

    static {
                // Abrevieri + forme complete (fără/ cu punct). Adaugă altele dacă apar diferit în PDF-uri.
                ROMANIAN_MONTHS.put("ian", 1); ROMANIAN_MONTHS.put("ian.", 1); ROMANIAN_MONTHS.put("ianuarie", 1);
                ROMANIAN_MONTHS.put("feb", 2); ROMANIAN_MONTHS.put("feb.", 2); ROMANIAN_MONTHS.put("februarie", 2);
                ROMANIAN_MONTHS.put("mar", 3); ROMANIAN_MONTHS.put("mar.", 3); ROMANIAN_MONTHS.put("martie", 3);
                ROMANIAN_MONTHS.put("apr", 4); ROMANIAN_MONTHS.put("apr.", 4); ROMANIAN_MONTHS.put("aprilie", 4);
                ROMANIAN_MONTHS.put("mai", 5); ROMANIAN_MONTHS.put("mai.", 5);
                ROMANIAN_MONTHS.put("iun", 6); ROMANIAN_MONTHS.put("iun.", 6); ROMANIAN_MONTHS.put("iunie", 6);
                ROMANIAN_MONTHS.put("iul", 7); ROMANIAN_MONTHS.put("iul.", 7); ROMANIAN_MONTHS.put("iulie", 7);
                ROMANIAN_MONTHS.put("aug", 8); ROMANIAN_MONTHS.put("aug.", 8); ROMANIAN_MONTHS.put("august", 8);
                ROMANIAN_MONTHS.put("sep", 9); ROMANIAN_MONTHS.put("sep.", 9); ROMANIAN_MONTHS.put("septembrie", 9);
                ROMANIAN_MONTHS.put("oct", 10); ROMANIAN_MONTHS.put("oct.", 10); ROMANIAN_MONTHS.put("octombrie", 10);
                ROMANIAN_MONTHS.put("nov", 11); ROMANIAN_MONTHS.put("nov.", 11); ROMANIAN_MONTHS.put("noiembrie", 11);
                ROMANIAN_MONTHS.put("dec", 12); ROMANIAN_MONTHS.put("dec.", 12); ROMANIAN_MONTHS.put("decembrie", 12);
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
    @Transactional
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
            case "CEC":
                return parseCECStatement(text);
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

                // Split rest from right to get last 1 or 2 amount-like tokens
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
                    // if description empty, try to recover by using whole rest minus amounts
                    description = rest.replace(amountTokens.stream().reduce((a,b)->a+"\\s*"+b).orElse(""), "").trim();
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

    private List<ParsedTransaction> parseCECStatement(String text) {
        List<ParsedTransaction> transactions = new ArrayList<>();
        if (text == null || text.isBlank()) return transactions;

        String[] lines = text.split("\\r?\\n");
        Pattern dateStart = Pattern.compile("^(\\d{2}[\\-\\.]\\d{2}[\\-\\.]\\d{4})\\s+(.*)$");

        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty()) continue;

            Matcher m = dateStart.matcher(line);
            if (!m.find()) continue;

            try {
                String datePart = m.group(1).trim();
                String rest = m.group(2).trim();

                // try to find last numeric token as amount
                String[] tokens = rest.split("\\s+");
                String possibleAmount = tokens[tokens.length - 1];
                // sometimes reference or GL code is before amount, so search backwards for numeric
                int i = tokens.length - 1;
                String amountToken = null;
                while (i >= 0) {
                    if (tokens[i].matches(".*\\d.*")) {
                        // candidate, clean punctuation
                        String cleaned = tokens[i].replaceAll("[^0-9,\\.\\-+]", "");
                        if (cleaned.matches(".*\\d.*")) {
                            amountToken = tokens[i];
                            break;
                        }
                    }
                    i--;
                }
                if (amountToken == null) continue;

                // description = everything between datePart and amountToken
                String description = String.join(" ", java.util.Arrays.copyOfRange(tokens, 0, i)).trim();
                if (description.isEmpty()) description = rest.replace(amountToken, "").trim();

                LocalDate date = parseDate(datePart);
                BigDecimal amount = parseRomanianAmount(amountToken);

                // Heuristic: if description contains "Alimentare", "Depunere", "Intrari" => income
                boolean isIncome = false;
                String low = description.toLowerCase();
                if (low.contains("alimentare") || low.contains("alimentare") || low.contains("depunere")
                        || low.contains("intrare") || low.contains("credit") || low.contains("intrări")) {
                    isIncome = true;
                } else {
                    // otherwise try to infer from context tokens around amount (e.g., a '-' before amount often means debit out)
                    // but default to expense (many bank PDFs list expense rows)
                    isIncome = false;
                }

                transactions.add(new ParsedTransaction(date, amount.abs(), description, isIncome));
            } catch (Exception e) {
                logger.warn("Failed to parse CEC line: {} (err: {})", raw, e.getMessage());
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
    // Înlocuiește metoda parseDate cu asta:
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

        // 2) Dacă nu e un nume de lună, încearcă formatele numerice/time (folosește DATE_FORMATS definit deja)
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

    // ✅ Auto-delete import records after 90 days
    @Scheduled(cron = "0 0 2 * * *") // Daily at 2 AM
    @Override
    public void cleanupOldImports() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(90);
        importedFileRepository.deleteByImportedAtBefore(threshold);
    }
}
