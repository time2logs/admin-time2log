import { Routes } from '@angular/router';

export const ORGANIZATIONS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./organizations').then((m) => m.OrganizationsComponent),
  },
  {
    path: ':id',
    loadComponent: () =>
      import('./organization-managing/organization-managing/organization-managing').then(
        (m) => m.OrganizationManaging,
      ),
  },
];
