import { Routes } from '@angular/router';

export const REPORTS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./reports-page/reports-page').then((m) => m.ReportsPage),
  },
  {
    path: 'members/:userId',
    loadComponent: () => import('./member-detail/member-detail').then((m) => m.MemberDetail),
  },
];
