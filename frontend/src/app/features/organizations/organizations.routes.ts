import { Routes } from '@angular/router';

export const ORGANIZATIONS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./organizations').then((m) => m.OrganizationsComponent),
  },
];
