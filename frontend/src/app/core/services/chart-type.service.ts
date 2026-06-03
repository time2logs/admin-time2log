import { Injectable, signal } from '@angular/core';

export type ChartTypeServiceEnum = 'bar' | 'column' | 'pie';

@Injectable({ providedIn: 'root' })
export class ChartTypeService {
  private readonly _current = signal<ChartTypeServiceEnum>('bar');
  current = this._current.asReadonly();

  set(chartType: ChartTypeServiceEnum): void {
    this._current.set(chartType);
  }
}
