
import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { finalize } from 'rxjs';

import { KeycloakService } from './auth/keycloak.service';

interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
}

@Component({
  selector: 'app-banking-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="layout">

      <aside class="sidebar">
        <h2>ADRIA BANK</h2>
        <p>Intelligent Banking Platform</p>

        <nav>
          <a href="#overview">Dashboard</a>
          <a href="#accounts">Accounts</a>
          <a href="#transactions">Transactions</a>
          <a href="#assistant">AI Assistant</a>
        </nav>

        <div class="profile">
          <p>{{ auth.username }}</p>
          <small>
            {{ auth.authenticated ? 'Authenticated' : 'Disconnected' }}
          </small>

          <button class="logout" (click)="logout()">
            Sign out
          </button>
        </div>
      </aside>

      <main>
        <header>
          <div>
            <h1>Banking Dashboard</h1>
            <p>Welcome, {{ auth.username }}</p>
          </div>
          <span class="status">Keycloak JWT</span>
        </header>

        <section id="overview" class="cards">
          <article>
            <h3>Account Management</h3>
            <p>Account Service</p>
            <strong>
              {{ auth.hasScope('accounts:manage')
                ? 'Authorized'
                : 'Access restricted' }}
            </strong>
          </article>

          <article>
            <h3>Transactions</h3>
            <p>Transaction Service</p>
            <strong>
              {{ auth.hasScope('transactions:manage')
                ? 'Authorized'
                : 'Access restricted' }}
            </strong>
          </article>

          <article>
            <h3>Security</h3>
            <p>OAuth 2.0 / OIDC</p>
            <strong>PKCE S256</strong>
          </article>
        </section>

        <section id="accounts" class="panel">
          <h2>Accounts</h2>
          <p>
            The authenticated Account Service connection
            will be integrated here.
          </p>
          <span class="notice">No account data loaded</span>
        </section>

        <section id="transactions" class="panel">
          <h2>Transactions</h2>
          <p>
            The authenticated debit, transfer and
            transaction-history interfaces are pending.
          </p>
          <span class="notice">No transaction data loaded</span>
        </section>

        <section id="assistant" class="panel">
          <h2>AI Banking Assistant</h2>
          <p>
            Secured with your Keycloak access token.
            Banking MCP tool execution remains disabled.
          </p>

          <div class="messages" aria-live="polite">
            <div
              *ngFor="let item of messages"
              class="message"
              [class.user]="item.role === 'user'">
              <strong>
                {{ item.role === 'user' ? 'You' : 'Assistant' }}
              </strong>
              <p>{{ item.content }}</p>
            </div>
          </div>

          <form (ngSubmit)="send()" class="composer">
            <input
              name="chatMessage"
              [(ngModel)]="input"
              maxlength="4000"
              [disabled]="loading"
              placeholder="Ask the banking assistant..."
              aria-label="Chat message"
            />

            <button
              type="submit"
              [disabled]="loading || !input.trim()">
              {{ loading ? 'Sending...' : 'Send' }}
            </button>
          </form>
        </section>
      </main>
    </div>
  `,
  styles: [`
    :host {
      display: block;
      min-height: 100vh;
      font-family: Arial, sans-serif;
      background: #f4f7fb;
      color: #172339;
    }

    .layout { display: flex; min-height: 100vh; }

    .sidebar {
      width: 240px;
      flex-shrink: 0;
      background: #102343;
      color: white;
      padding: 28px 20px;
    }

    .sidebar h2 { margin: 0; }
    .sidebar p, .sidebar small { color: #c0cce0; }

    nav { display: grid; gap: 12px; margin: 35px 0; }

    nav a {
      color: white;
      text-decoration: none;
      padding: 10px;
      border-radius: 8px;
    }

    nav a:hover { background: #263e63; }

    .profile { margin-top: 45px; }

    main {
      flex: 1;
      min-width: 0;
      padding: 32px;
    }

    header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 15px;
      margin-bottom: 28px;
    }

    header h1 { margin-bottom: 4px; }

    header p, article p, .panel > p {
      color: #617087;
    }

    .status, .notice {
      background: #e8eef7;
      border-radius: 18px;
      padding: 8px 12px;
      font-size: 12px;
    }

    .cards {
      display: grid;
      grid-template-columns: repeat(3, minmax(0, 1fr));
      gap: 16px;
    }

    article, .panel {
      background: white;
      border: 1px solid #e2e8f0;
      border-radius: 14px;
      padding: 22px;
      margin-bottom: 20px;
    }

    article strong { color: #183e78; }

    .messages {
      height: 300px;
      overflow-y: auto;
      background: #f6f8fc;
      border-radius: 10px;
      padding: 15px;
    }

    .message {
      background: white;
      padding: 12px;
      margin-bottom: 12px;
      border-radius: 10px;
      max-width: 85%;
      overflow-wrap: anywhere;
    }

    .message.user {
      background: #dce9fa;
      margin-left: auto;
    }

    .message p { white-space: pre-wrap; }

    .composer {
      display: flex;
      gap: 10px;
      margin-top: 15px;
    }

    input {
      flex: 1;
      min-width: 0;
      padding: 12px;
      border: 1px solid #cbd5e1;
      border-radius: 8px;
    }

    button {
      background: #183e78;
      color: white;
      padding: 12px 18px;
      border: 0;
      border-radius: 8px;
    }

    button:disabled {
      opacity: .5;
      cursor: not-allowed;
    }

    .logout {
      margin-top: 12px;
      background: #a43c45;
    }

    @media (max-width: 800px) {
      .layout { flex-direction: column; }
      .sidebar { width: 100%; }
      main { padding: 16px; }
      .cards { grid-template-columns: 1fr; }
      header { flex-direction: column; align-items: flex-start; }
    }
  `]
})
export class BankingDashboard {

  readonly auth = inject(KeycloakService);
  private readonly http = inject(HttpClient);

  input = '';
  loading = false;

  messages: ChatMessage[] = [{
    role: 'assistant',
    content: 'Welcome to ADRIA Banking Assistant.'
  }];

  send(): void {
    const message = this.input.trim();

    if (!message || this.loading) return;

    this.messages.push({ role: 'user', content: message });
    this.input = '';
    this.loading = true;

    this.http.post<{ reply: string }>(
      '/api/chat',
      { message }
    ).pipe(
      finalize(() => this.loading = false)
    ).subscribe({
      next: response => {
        this.messages.push({
          role: 'assistant',
          content: response.reply
        });
      },
      error: error => {
        this.messages.push({
          role: 'assistant',
          content: error.status === 401
            ? 'Your session is not authenticated.'
            : error.status === 403
              ? 'You do not have the required banking scope.'
              : 'Assistant unavailable. Please try again later.'
        });
      }
    });
  }

  async logout(): Promise<void> {
    await this.auth.logout();
  }
}
