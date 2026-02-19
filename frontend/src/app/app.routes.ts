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
    path: 'auth/reset-password',
    loadComponent: () => import('./features/auth/reset-password/reset-password').then((m) => m.ResetPassword),
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
    path: 'reports',
    canActivate: [authGuard],
    loadChildren: () =>
      import('./features/reports/reports.routes').then(
        (m) => m.REPORTS_ROUTES
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
