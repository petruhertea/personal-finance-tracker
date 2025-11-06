import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CsvImportService, ImportResult } from '../../services/csv-import.service';
import { Router } from '@angular/router';
import { PdfImportService } from '../../services/pdf-import.service';

@Component({
  selector: 'app-csv-import',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: "./csv-import.component.html",
  styleUrl: "./csv-import.component.css"
})

export class CsvImportComponent {
  importType: 'csv' | 'pdf' = 'pdf'; // Default to PDF since it's more common
  
  // PDF upload
  selectedPdfFile: File | null = null;
  selectedBank: string = 'AUTO';
  
  // CSV upload
  selectedCsvFile: File | null = null;
  dateColumn = 'Date';
  amountColumn = 'Amount';
  descriptionColumn = 'Description';
  typeColumn = '';
  
  isImporting = false;
  importResult: ImportResult | null = null;

  constructor(
    private csvImportService: CsvImportService,
    private pdfImportService: PdfImportService,
    private router: Router
  ) {}

  setImportType(type: 'csv' | 'pdf'): void {
    this.importType = type;
    this.resetImport();
  }

  onPdfSelected(event: any): void {
    const file = event.target.files[0];
    if (file && file.type === 'application/pdf') {
      this.selectedPdfFile = file;
      this.importResult = null;
    } else {
      alert('Please select a valid PDF file');
    }
  }

  onCsvSelected(event: any): void {
    const file = event.target.files[0];
    if (file && file.type === 'text/csv') {
      this.selectedCsvFile = file;
      this.importResult = null;
    } else {
      alert('Please select a valid CSV file');
    }
  }

  importPdf(): void {
    if (!this.selectedPdfFile) return;

    this.isImporting = true;
    
    this.pdfImportService.importPdf(this.selectedPdfFile, this.selectedBank).subscribe({
      next: (result) => {
        this.isImporting = false;
        this.importResult = result;
        
        if (result.errors.length === 0) {
          setTimeout(() => this.router.navigate(['/transactions']), 3000);
        }
      },
      error: (err) => {
        this.isImporting = false;
        alert('PDF import failed: ' + (err.error || 'Unknown error'));
      }
    });
  }

  importCsv(): void {
    if (!this.selectedCsvFile) return;

    this.isImporting = true;
    
    this.csvImportService.importCsv(
      this.selectedCsvFile,
      this.dateColumn,
      this.amountColumn,
      this.descriptionColumn,
      this.typeColumn || undefined
    ).subscribe({
      next: (result) => {
        this.isImporting = false;
        this.importResult = result;
        
        if (result.errors.length === 0) {
          setTimeout(() => this.router.navigate(['/transactions']), 3000);
        }
      },
      error: (err) => {
        this.isImporting = false;
        alert('CSV import failed: ' + (err.error || 'Unknown error'));
      }
    });
  }

  resetImport(): void {
    this.selectedPdfFile = null;
    this.selectedCsvFile = null;
    this.importResult = null;
    this.selectedBank = 'AUTO';
  }

  viewTransactions(): void {
    this.router.navigate(['/transactions']);
  }
}