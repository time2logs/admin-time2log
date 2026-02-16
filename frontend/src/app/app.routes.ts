import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { guestGuard } from './core/guards/guest.guard';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'auth',
    pathMatch: 'full',
  },
  {
    path: 'auth',
    canActivate: [guestGuard],
    loadChildren: () => import('./features/auth/auth.routes').then((m) => m.AUTH_ROUTES),
  },
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadChildren: () =>
      import('./features/dashboard/dashboard.routes').then((m) => m.DASHBOARD_ROUTES),
  },
  {
    path: 'organizations',
    canActivate: [authGuard],
    loadChildren: () =>
      import('./features/organizations/organizations.routes').then(
        (m) => m.ORGANIZATIONS_ROUTES
      ),
  },
  {
    path: 'activities',
    canActivate: [authGuard],
    loadChildren: () =>
      import('./features/activities/activities.routes').then(
        (m) => m.ACTIVITIES_ROUTES
      )
  },
  {
    path: 'settings',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/settings/settings/settings').then((m) => m.Settings),
  },
  {
    path: '**',
    redirectTo: 'auth',
  },
];
