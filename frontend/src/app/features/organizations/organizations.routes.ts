import { Routes } from '@angular/router';

export const ORGANIZATIONS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./organizations').then((m) => m.OrganizationsComponent),
  },
  {
    path: ':id',
    loadComponent: () =>
      import('./organization-managing/organization-managing').then(
        (m) => m.OrganizationManaging,
      ),
  },
  {
    path: ':id/professions/:professionId',
    loadComponent: () =>
      import('./organization-managing/professions-managing/professions-managing').then(
        (m) => m.ProfessionsManaging,
      ),
  },
  {
    path: ':id/teams/:teamId',
    loadComponent: () =>
      import('./organization-managing/team-managing/team-managing').then(
        (m) => m.TeamManaging,
      ),
  },
];
