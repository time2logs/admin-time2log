import { Component, inject, signal } from '@angular/core';
import { AsyncPipe } from '@angular/common';
import { Router } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { AuthService } from '@services/auth.service';
import { LanguageService } from '@services/language.service';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [TranslateModule, AsyncPipe],
  templateUrl: './settings.html',
})
export class Settings {
  private readonly router = inject(Router);
  protected readonly authService = inject(AuthService);
  protected readonly languageService = inject(LanguageService);

  protected readonly showDeleteConfirm = signal(false);
  protected readonly isDeleting = signal(false);
  protected readonly idCopied = signal(false);

  protected readonly languages = [
    { code: 'de', label: 'Deutsch' },
    { code: 'en', label: 'English' },
  ];

  protected copyId(id: string): void {
    navigator.clipboard.writeText(id).then(() => {
      this.idCopied.set(true);
      setTimeout(() => this.idCopied.set(false), 2000);
    });
  }

  protected switchLanguage(lang: string): void {
    this.languageService.switchLanguage(lang);
  }

  protected deleteAccount(): void {
    this.isDeleting.set(true);
    this.authService.deleteProfile().subscribe({
      next: () => {
        this.authService.logout().subscribe(() => {
          this.router.navigate(['/auth/login']);
        });
      },
      error: () => this.isDeleting.set(false),
    });
  }
}
