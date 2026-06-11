import { Injectable, computed, signal } from '@angular/core';

export type Palette = 'default' | 'deuteranopia' | 'protanopia' | 'monochrome';
const ALL: Palette[] = ['default', 'deuteranopia', 'protanopia', 'monochrome'];

/** Diagramm-Farben je Palette (ngx-charts colorScheme domain). */
const PALETTE_COLORS: Record<Palette, string[]> = {
  default:      ['#29b6d6','#1a8fa8','#5ecde0','#4a7fc1','#2ea89e','#6090d8','#3db8ad','#5580c8'],
  deuteranopia: ['#4575b4','#d4a017','#7b3f9e','#74c2a8','#d4d400'],
  protanopia:   ['#2166ac','#d4a017','#4393c3','#d4d400','#1a3a5c'],
  monochrome:   ['#404040','#737373','#999999','#bfbfbf','#e0e0e0'],
};

@Injectable({ providedIn: 'root' })
export class PaletteService {
  readonly current = signal<Palette>(
    (localStorage.getItem('palette') as Palette) ?? 'default'
  );

  /** Farb-Domain der aktuell gewählten Palette, reaktiv auf Wechsel. */
  readonly domain = computed<string[]>(() => PALETTE_COLORS[this.current()] ?? PALETTE_COLORS.default);

  constructor() { this.apply(this.current()); }

  set(p: Palette) {
    localStorage.setItem('palette', p);
    this.apply(p);
    this.current.set(p);
  }

  private apply(p: Palette) {
    ALL.forEach(n => document.documentElement.classList.remove(`palette-${n}`));
    if (p !== 'default') document.documentElement.classList.add(`palette-${p}`);
  }
}
