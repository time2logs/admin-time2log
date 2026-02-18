import { Routes } from '@angular/router';

export const REPORTS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./reports/reports').then((m) => m.Reports),
  }
];
