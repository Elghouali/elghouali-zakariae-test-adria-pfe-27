
import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { from, switchMap } from 'rxjs';

import { KeycloakService } from './keycloak.service';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(KeycloakService);

  if (!request.url.startsWith('/api/')) {
    return next(request);
  }

  return from(auth.getToken()).pipe(
    switchMap(token =>
      next(request.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      }))
    )
  );
};
