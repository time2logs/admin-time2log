import { Routes } from '@angular/router';

export const REPORTS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./reports/reports').then((m) => m.Reports),
  },
  {
    path: 'members/:userId',
    loadComponent: () => import('./member-detail/member-detail').then((m) => m.MemberDetail),
  },
];
