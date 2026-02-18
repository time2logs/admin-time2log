import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { DatePipe } from '@angular/common';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
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
}
