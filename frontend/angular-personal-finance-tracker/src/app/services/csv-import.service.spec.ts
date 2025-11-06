import { TestBed } from '@angular/core/testing';

import { CsvImportService } from './csv-import.service';

describe('CsvImportService', () => {
  let service: CsvImportService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(CsvImportService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
