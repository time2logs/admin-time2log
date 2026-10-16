import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '@services/auth.service';
import { combineLatest, filter, map, take } from 'rxjs';

export const noMentorGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  return combineLatest([
    authService.isInitialized$,
    authService.currentUser$,
    authService.currentProfile$,
  ]).pipe(
    filter(([initialized, user, profile]) => initialized && (!user || profile !== null)),
    take(1),
    map(([, user, profile]) => {
      if (!user) {
        return router.createUrlTree(['/auth/login']);
      }
      if (profile?.role === 'mentor') {
        return router.createUrlTree(['/dashboard']);
      }
      return true;
    })
  );
};
