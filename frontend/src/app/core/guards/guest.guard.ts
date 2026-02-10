import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '@services/auth.service';
import { combineLatest, filter, map, take } from 'rxjs';

export const guestGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  return combineLatest([authService.isInitialized$, authService.currentUser$]).pipe(
    filter(([initialized]) => initialized),
    take(1),
    map(([, user]) => (user ? router.createUrlTree(['/dashboard']) : true))
  );
};
