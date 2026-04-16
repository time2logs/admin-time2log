import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { DatePipe } from '@angular/common';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import Papa from 'papaparse';
import { Profession } from '@app/core/models/organizations.models';
import { CurriculumImport } from '@app/core/models/curriculum-import.models';
import { CurriculumImportService } from '@services/curriculum-import.service';
import { ToastService } from '@services/toast.service';

@Component({
  selector: 'app-professions-managing',
  standalone: true,
  imports: [TranslateModule, DatePipe],
  templateUrl: './professions-managing.html',
})
export class ProfessionsManaging implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly curriculumImportService = inject(CurriculumImportService);
  private readonly toast = inject(ToastService);
  private readonly translate = inject(TranslateService);

  protected readonly profession = signal<Profession | null>(null);
  protected readonly imports = signal<CurriculumImport[]>([]);
  protected readonly selectedFileName = signal<string>('');
  protected readonly isUploading = signal(false);
  protected readonly isApplying = signal(false);

  protected readonly activeImport = computed(() =>
    this.imports().find(i => i.status === 'applied') ?? null
  );

  protected readonly pendingImport = computed(() =>
    this.imports().find(i => i.status === 'pending') ?? null
  );

  protected readonly history = computed(() =>
    this.imports().filter(i => i.status !== 'pending')
  );

  private organizationId = '';
  private professionId = '';
  private selectedPayload: object | null = null;

  ngOnInit(): void {
    this.organizationId = this.route.snapshot.params['id'];
    this.professionId = this.route.snapshot.params['professionId'];
    this.loadProfession(this.professionId);
    this.loadImports();
  }

  protected goBack(): void {
    this.router.navigate(['/organizations', this.organizationId], { queryParams: { tab: 'curriculums' } });
  }

  protected onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    this.selectedFileName.set(file.name);

    if (file.name.endsWith('.csv')) {
      this.parseCsvFile(file);
    } else {
      this.parseJsonFile(file);
    }
  }

  private parseJsonFile(file: File): void {
    const reader = new FileReader();
    reader.onload = () => {
      try {
        this.selectedPayload = JSON.parse(reader.result as string);
      } catch {
        this.toast.error(this.translate.instant('professionManaging.import.invalidJson'));
        this.resetFileSelection();
      }
    };
    reader.readAsText(file);
  }

  private parseCsvFile(file: File): void {
    const reader = new FileReader();
    reader.onload = () => {
      try {
        const text = reader.result as string;
        const { nodesCsv, competencyMap } = this.splitCsvSections(text);

        Papa.parse(nodesCsv, {
          header: true,
          skipEmptyLines: 'greedy',
          complete: (result) => {
            try {
              this.selectedPayload = this.csvRowsToJson(result.data as CsvRow[], competencyMap);
            } catch (e) {
              const msg = e instanceof Error ? e.message : this.translate.instant('professionManaging.import.invalidCsv');
              this.toast.error(msg);
              this.resetFileSelection();
            }
          },
          error: () => {
            this.toast.error(this.translate.instant('professionManaging.import.invalidCsv'));
            this.resetFileSelection();
          },
        });
      } catch (e) {
        const msg = e instanceof Error ? e.message : this.translate.instant('professionManaging.import.invalidCsv');
        this.toast.error(msg);
        this.resetFileSelection();
      }
    };
    reader.readAsText(file);
  }

  private splitCsvSections(text: string): { nodesCsv: string; competencyMap: Map<string, string> } {
    const lines = text.split(/\r?\n/);
    const competencyMap: Map<string, string> = new Map();

    const compHeaderIndex = lines.findIndex(line => {
      const parts = line.split(';').map(p => p.trim().toLowerCase());
      return parts[0] === 'competencies' && parts[1] === 'description';
    });

    if (compHeaderIndex === -1) {
      return { nodesCsv: text, competencyMap };
    }

    const nodesCsv = lines.slice(0, compHeaderIndex).join('\n');

    for (let i = compHeaderIndex + 1; i < lines.length; i++) {
      const line = lines[i].trim();
      if (!line || line === ';;;;;' || line === ';;;;') continue;
      const [code, ...descParts] = line.split(';');
      const trimmedCode = code.trim();
      if (trimmedCode) {
        competencyMap.set(trimmedCode, descParts.join(';').trim());
      }
    }

    return { nodesCsv, competencyMap };
  }

private csvRowsToJson(rows: CsvRow[], competencyMap: Map<string, string> = new Map()): object {
  this.validateCsvRows(rows);
  const nodeMap: Record<string, CsvNode> = {};
  const competencySet = new Set<string>();

  for (const row of rows) {
    if (!['category', 'activity'].includes(row.type)) {
      throw new Error(`Ungültiger type: "${row.type}" bei key "${row.key}"`);
    }

    const comps = row.competencies
      ? row.competencies.split(',').map(c => c.trim()).filter(c => c)
      : undefined;

    comps?.forEach(c => competencySet.add(c));

    nodeMap[row.key] = {
      key: row.key,
      type: row.type,
      label: row.label,
      order: Number(row.order),
      competencies: comps,
      children: row.type === 'category' ? [] : undefined,
      _parent: row.parent_key || null,
    };
  }

  const roots: Omit<CsvNode, '_parent'>[] = [];
  for (const node of Object.values(nodeMap)) {
    const { _parent, ...clean } = node;
    if (_parent && nodeMap[_parent]) {
      nodeMap[_parent].children!.push(clean as CsvNode);
    } else {
      roots.push(clean);
    }
  }

  const sortChildren = (nodes: Omit<CsvNode, '_parent'>[]) => {
    nodes.sort((a, b) => a.order - b.order);
    nodes.forEach(n => n.children && sortChildren(n.children));
  };
  sortChildren(roots);

  return {
    schema_version: 1,
    version: '1.0',
    competencies: [...competencySet].map(code => ({
      code,
      description: competencyMap.get(code) ?? '',
    })),
    nodes: roots,
  };
}

  protected uploadImport(): void {
    if (!this.selectedPayload) return;

    this.isUploading.set(true);
    this.curriculumImportService.createImport(this.organizationId, this.professionId, this.selectedPayload).subscribe({
      next: () => {
        this.isUploading.set(false);
        this.resetFileSelection();
        this.toast.success(this.translate.instant('professionManaging.import.uploadSuccess'));
        this.loadImports();
      },
      error: () => {
        this.isUploading.set(false);
      },
    });
  }

  protected applyImport(): void {
    const pending = this.pendingImport();
    if (!pending) return;

    this.isApplying.set(true);
    this.curriculumImportService.applyImport(this.organizationId, this.professionId, pending.id).subscribe({
      next: (result) => {
        this.isApplying.set(false);
        if (result.status === 'applied') {
          this.toast.success(this.translate.instant('professionManaging.import.applySuccess'));
        } else if (result.status === 'failed') {
          this.toast.error(result.error ?? this.translate.instant('professionManaging.import.applyFailed'));
        }
        this.loadImports();
      },
      error: () => {
        this.isApplying.set(false);
      },
    });
  }

  private resetFileSelection(): void {
    this.selectedFileName.set('');
    this.selectedPayload = null;
  }

  private loadImports(): void {
    this.curriculumImportService.getImports(this.organizationId, this.professionId).subscribe({
      next: (imports) => this.imports.set(imports),
    });
  }

  private loadProfession(id: string): void {
    // TODO: replace with real API call
    this.profession.set({ id, key: 'maurer', label: 'Maurer EFZ' });
  }

  private validateCsvRows(rows: CsvRow[]): void {
  if (rows.length === 0) {
    throw new Error('CSV ist leer');
  }

  const seenKeys = new Set<string>();

  for (const [i, row] of rows.entries()) {
    const line = `Zeile ${i + 2}`; // +2 wegen Header

    // Pflichtfelder
    if (!row.key?.trim())   throw new Error(`${line}: "key" fehlt`);
    if (!row.label?.trim()) throw new Error(`${line}: "label" fehlt`);
    if (!row.type?.trim())  throw new Error(`${line}: "type" fehlt`);
    if (!row.order?.trim()) throw new Error(`${line}: "order" fehlt`);

    // Doppelte keys
    if (seenKeys.has(row.key)) throw new Error(`${line}: key "${row.key}" ist doppelt`);
    seenKeys.add(row.key);

    // Type-Validierung
    if (!['category', 'activity'].includes(row.type)) {
      throw new Error(`${line}: ungültiger type "${row.type}"`);
    }

    // order muss eine Zahl sein
    if (isNaN(Number(row.order))) {
      throw new Error(`${line}: "order" ist keine Zahl ("${row.order}")`);
    }
  }

  // parent_key-Referenzen prüfen
  const allKeys = new Set(rows.map(r => r.key));
  for (const [i, row] of rows.entries()) {
    if (row.parent_key && !allKeys.has(row.parent_key)) {
      throw new Error(`Zeile ${i + 2}: parent_key "${row.parent_key}" existiert nicht`);
    }
  }
}
}

interface CsvRow {
  key: string;
  type: string;
  label: string;
  order: string;
  competencies?: string;
  parent_key?: string;
}

interface CsvNode {
  key: string;
  type: string;
  label: string;
  order: number;
  competencies?: string[];
  children?: CsvNode[];
  _parent: string | null;
}
