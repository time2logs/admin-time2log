import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { ToastService } from '@services/toast.service';
import { TranslateService } from '@ngx-translate/core';
import { environment } from '@env/environment';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  if (!req.url.startsWith(environment.apiBaseUrl)) {
    return next(req);
  }

  const toast = inject(ToastService);
  const translate = inject(TranslateService);

  return next(req).pipe(
    catchError((error) => {
      const detail = error.error?.detail;
      const code = error.error?.code;
      const status = error.status;

      const translationKey = code ? `errors.${code}` : null;
      const translated = translationKey ? translate.instant(translationKey) : null;
      const hasTranslation = translated && translated !== translationKey;

      if (hasTranslation) {
        toast.error(translated);
      } else if (detail) {
        toast.error(detail);
      } else if (status === 403) {
        toast.error(translate.instant('errors.forbidden'));
      } else if (status === 0) {
        toast.error(translate.instant('errors.network'));
      } else {
        toast.error(translate.instant('errors.generic'));
      }

      return throwError(() => error);
    }),
  );
};
