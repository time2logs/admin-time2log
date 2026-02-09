import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { AuthService } from '@services/auth.service';
import { LanguageService } from '@services/language.service';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, TranslateModule],
  templateUrl: './header.html',
})
export class HeaderComponent {
  protected readonly authService = inject(AuthService);
  protected readonly languageService = inject(LanguageService);
  private readonly router = inject(Router);

  protected readonly languages = [
    { code: 'de', label: 'Deutsch' },
    { code: 'en', label: 'English' },
  ];

  protected readonly isLangMenuOpen = signal(false);

  protected get currentLang() {
    return this.languages.find((l) => l.code === this.languageService.currentLang) ?? this.languages[0];
  }

  toggleLangMenu(): void {
    this.isLangMenuOpen.update((v) => !v);
  }

  switchLanguage(lang: string): void {
    this.languageService.switchLanguage(lang);
    this.isLangMenuOpen.set(false);
  }

  logout(): void {
    this.authService.logout().subscribe(() => {
      this.router.navigate(['/auth/login']);
    });
  }
}
