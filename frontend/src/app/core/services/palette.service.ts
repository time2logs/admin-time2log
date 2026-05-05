import { Injectable, signal } from '@angular/core';

export type Palette = 'default' | 'deuteranopia' | 'protanopia' | 'monochrome';
const ALL: Palette[] = ['default', 'deuteranopia', 'protanopia', 'monochrome'];

@Injectable({ providedIn: 'root' })
export class PaletteService {
  readonly current = signal<Palette>(
    (localStorage.getItem('palette') as Palette) ?? 'default'
  );

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
