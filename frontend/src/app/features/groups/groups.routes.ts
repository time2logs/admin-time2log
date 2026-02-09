import { Routes } from '@angular/router';

export const GROUPS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./groups').then((m) => m.GroupsComponent),
  },
];
