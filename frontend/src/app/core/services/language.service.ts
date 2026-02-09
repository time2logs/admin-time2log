import { Injectable, inject } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';

const STORAGE_KEY = 'app_language';
const DEFAULT_LANG = 'de';
const SUPPORTED_LANGS = ['de', 'en'];

@Injectable({
  providedIn: 'root',
})
export class LanguageService {
  private readonly translate = inject(TranslateService);

  init(): void {
    const savedLang = localStorage.getItem(STORAGE_KEY);
    const browserLang = this.translate.getBrowserLang();

    let lang = DEFAULT_LANG;

    if (savedLang && SUPPORTED_LANGS.includes(savedLang)) {
      lang = savedLang;
    } else if (browserLang && SUPPORTED_LANGS.includes(browserLang)) {
      lang = browserLang;
    }

    this.translate.setFallbackLang(DEFAULT_LANG);
    this.translate.use(lang);
  }

  get currentLang(): string {
    return this.translate.getCurrentLang() || DEFAULT_LANG;
  }

  switchLanguage(lang: string): void {
    if (SUPPORTED_LANGS.includes(lang)) {
      localStorage.setItem(STORAGE_KEY, lang);
      this.translate.use(lang);
    }
  }

  get supportedLanguages() {
    return SUPPORTED_LANGS;
  }
}
