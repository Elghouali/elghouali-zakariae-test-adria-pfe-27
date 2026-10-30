
import { Injectable } from '@angular/core';
import Keycloak from 'keycloak-js';

@Injectable({ providedIn: 'root' })
export class KeycloakService {

  private readonly keycloak = new Keycloak({
    url: 'http://localhost:8180',
    realm: 'bank',
    clientId: 'bank-web'
  });

  private initialized = false;

  async initialize(): Promise<void> {
    if (this.initialized) return;

    await this.keycloak.init({
      onLoad: 'login-required',
      flow: 'standard',
      pkceMethod: 'S256',
      checkLoginIframe: false
    });

    this.initialized = true;
  }

  get authenticated(): boolean {
    return this.keycloak.authenticated === true;
  }

  get username(): string {
    return this.keycloak.tokenParsed?.preferred_username
      ?? 'Banking User';
  }

  get scopes(): string[] {
    return (this.keycloak.tokenParsed?.scope ?? '')
      .split(' ')
      .filter(Boolean);
  }

  hasScope(scope: string): boolean {
    return this.scopes.includes(scope);
  }

  async getToken(): Promise<string> {
    if (!this.authenticated) {
      throw new Error('User is not authenticated');
    }

    await this.keycloak.updateToken(30);

    if (!this.keycloak.token) {
      throw new Error('Access token unavailable');
    }

    return this.keycloak.token;
  }

  async login(): Promise<void> {
    await this.keycloak.login({
      redirectUri: window.location.origin
    });
  }

  async logout(): Promise<void> {
    await this.keycloak.logout({
      redirectUri: window.location.origin
    });
  }
}
