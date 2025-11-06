package com.petruth.personal_finance_tracker.rest;

import com.petruth.personal_finance_tracker.entity.User;
import com.petruth.personal_finance_tracker.security.SecurityUtil;
import com.petruth.personal_finance_tracker.service.CsvImportService;
import com.petruth.personal_finance_tracker.service.PdfImportService;
import com.petruth.personal_finance_tracker.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/import")
public class CsvImportRestController {

    private final CsvImportService csvImportService;
    private final SecurityUtil securityUtil;
    private final UserService userService;
    private final PdfImportService pdfImportService;

    public CsvImportRestController(CsvImportService csvImportService,
                                   SecurityUtil securityUtil,
                                   UserService userService,
                                   PdfImportService pdfImportService) {
        this.csvImportService = csvImportService;
        this.securityUtil = securityUtil;
        this.userService = userService;
        this.pdfImportService = pdfImportService;
    }

    /**
     * Import transactions from CSV
     * POST /api/import/csv
     */
    @PostMapping("/csv")
    public ResponseEntity<?> importCsv(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "dateColumn", defaultValue = "Date") String dateColumn,
            @RequestParam(value = "amountColumn", defaultValue = "Amount") String amountColumn,
            @RequestParam(value = "descriptionColumn", defaultValue = "Description") String descriptionColumn,
            @RequestParam(value = "typeColumn", required = false) String typeColumn) {

        try {
            Long userId = securityUtil.getCurrentUserId();
            User user = userService.findById(userId);

            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("File is empty");
            }

            if (!file.getOriginalFilename().endsWith(".csv")) {
                return ResponseEntity.badRequest().body("File must be a CSV");
            }

            // Build config
            CsvImportService.CsvImportConfig config = new CsvImportService.CsvImportConfig();
            config.setDateColumn(dateColumn);
            config.setAmountColumn(amountColumn);
            config.setDescriptionColumn(descriptionColumn);
            config.setTypeColumn(typeColumn);

            // Import
            CsvImportService.ImportResult result = csvImportService.importFromCsv(file, user, config);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Import failed: " + e.getMessage());
        }
    }

    @PostMapping("/pdf")
    public ResponseEntity<?> importPdf(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "bankType", defaultValue = "AUTO") String bankType) {

        try {
            Long userId = securityUtil.getCurrentUserId();
            User user = userService.findById(userId);

            if (file.isEmpty() || !file.getOriginalFilename().endsWith(".pdf")) {
                return ResponseEntity.badRequest().body("Invalid PDF file");
            }

            PdfImportService.ImportResult result =
                    pdfImportService.importFromPdf(file, user, bankType);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("Import failed: " + e.getMessage());
        }
    }
}