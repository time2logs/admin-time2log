import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Router } from '@angular/router';
import { AuthService } from '@services/auth.service';
import { combineLatest, filter, map, take } from 'rxjs';

function safeRedirect(redirectTo: string | null | undefined): string | null {
  if (!redirectTo) return null;
  if (!redirectTo.startsWith('/')) return null;
  if (redirectTo.startsWith('//')) return null;
  return redirectTo;
}

export const guestGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  return combineLatest([authService.isInitialized$, authService.currentUser$]).pipe(
    filter(([initialized]) => initialized),
    take(1),
    map(([, user]) => {
      if (!user) return true;
      const target = safeRedirect(route.queryParamMap.get('redirectTo')) ?? '/dashboard';
      return router.parseUrl(target);
    })
  );
};
