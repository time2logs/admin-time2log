import { Injectable, signal, effect } from '@angular/core';

const STORAGE_KEY = 'app_theme';

@Injectable({
  providedIn: 'root',
})
export class ThemeService {
  readonly isDark = signal(this.loadInitialTheme());

  constructor() {
    effect(() => {
      const dark = this.isDark();
      document.documentElement.classList.toggle('dark', dark);
      localStorage.setItem(STORAGE_KEY, dark ? 'dark' : 'light');
    });
  }

  toggle(): void {
    this.isDark.update(v => !v);
  }

  private loadInitialTheme(): boolean {
    const saved = localStorage.getItem(STORAGE_KEY);
    if (saved) return saved === 'dark';
    return window.matchMedia('(prefers-color-scheme: dark)').matches;
  }
}
