import { TestBed } from '@angular/core/testing';

import { PdfImportService } from './pdf-import.service';

describe('PdfImportService', () => {
  let service: PdfImportService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(PdfImportService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
