
import { bootstrapApplication } from '@angular/platform-browser';
import { KeycloakService } from './app/auth/keycloak.service';
import { appConfig } from './app/app.config';
import { App } from './app/app';

const authentication = new KeycloakService();

async function bootstrap(): Promise<void> {
  await authentication.initialize();

  await bootstrapApplication(App, {
    ...appConfig,
    providers: [
      ...appConfig.providers,
      {
        provide: KeycloakService,
        useValue: authentication
      }
    ]
  });
}

bootstrap().catch(error => {
  console.error('Authentication or application startup failed', error);

  const element = document.createElement('p');
  element.textContent =
    'Unable to start ADRIA Banking. Check Keycloak availability.';

  document.body.replaceChildren(element);
});
