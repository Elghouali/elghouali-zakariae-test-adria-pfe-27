
import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { finalize } from 'rxjs';

interface Message {
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
        <p>Digital Banking Platform</p>
        <nav>
          <a href="#overview">Overview</a>
          <a href="#accounts">Accounts</a>
          <a href="#transactions">Transactions</a>
          <a href="#assistant">AI Assistant</a>
        </nav>
        <small>ENSET PFE 2027</small>
      </aside>

      <main>
        <header>
          <div>
            <h1>Banking Dashboard</h1>
            <p>Accounts, transactions and AI assistance</p>
          </div>
          <span class="status">Authentication required</span>
        </header>

        <section id="overview" class="cards">
          <article>
            <h3>Accounts</h3>
            <p>Account management</p>
            <strong>Not connected</strong>
          </article>
          <article>
            <h3>Transactions</h3>
            <p>Transfers and debits</p>
            <strong>Not connected</strong>
          </article>
          <article>
            <h3>Security</h3>
            <p>Keycloak JWT</p>
            <strong>Integration pending</strong>
          </article>
        </section>

        <section id="accounts" class="panel">
          <h2>My Accounts</h2>
          <p>
            Account lookup and balance display will use
            authenticated Account Service endpoints.
          </p>
          <span class="notice">No banking data loaded</span>
        </section>

        <section id="transactions" class="panel">
          <h2>Transactions</h2>
          <p>
            Transaction history and transfer forms will
            be connected after authorization setup.
          </p>
          <span class="notice">No transactions loaded</span>
        </section>

        <section id="assistant" class="panel chat">
          <h2>AI Banking Assistant</h2>

          <div class="messages" aria-live="polite">
            <div
              *ngFor="let message of messages"
              class="message"
              [class.user]="message.role === 'user'">
              <strong>
                {{ message.role === 'user' ? 'You' : 'Assistant' }}
              </strong>
              <p>{{ message.content }}</p>
            </div>
          </div>

          <form (ngSubmit)="sendMessage()" class="composer">
            <input
              name="message"
              [(ngModel)]="input"
              [disabled]="loading"
              maxlength="4000"
              placeholder="Ask your banking assistant..."
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
      font-family: Arial, sans-serif;
      color: #172339;
      background: #f4f7fb;
      min-height: 100vh;
    }
    .layout { display: flex; min-height: 100vh; }
    .sidebar {
      width: 230px;
      flex-shrink: 0;
      background: #102343;
      color: white;
      padding: 28px 22px;
      box-sizing: border-box;
    }
    .sidebar h2 { margin: 0; letter-spacing: 1px; }
    .sidebar p, .sidebar small { color: #b9c7dc; }
    nav { display: grid; gap: 12px; margin: 40px 0; }
    nav a { color: white; text-decoration: none; padding: 10px; }
    nav a:hover { background: #253c61; border-radius: 8px; }
    main { flex: 1; min-width: 0; padding: 32px; }
    header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 15px;
      margin-bottom: 30px;
    }
    header h1 { margin-bottom: 5px; }
    header p { color: #617087; }
    .status, .notice {
      background: #e8edf5;
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
      padding: 22px;
      border-radius: 14px;
      border: 1px solid #e3e9f1;
      margin-bottom: 20px;
    }
    article p, .panel > p { color: #617087; }
    article strong { font-size: 13px; }
    .messages {
      height: 280px;
      overflow-y: auto;
      background: #f7f9fc;
      padding: 16px;
      border-radius: 10px;
    }
    .message {
      background: white;
      padding: 12px;
      border-radius: 10px;
      margin-bottom: 12px;
      max-width: 85%;
      overflow-wrap: anywhere;
    }
    .message.user {
      background: #dce9fa;
      margin-left: auto;
    }
    .message p { margin-bottom: 0; white-space: pre-wrap; }
    .composer { display: flex; gap: 10px; margin-top: 16px; }
    input {
      flex: 1;
      min-width: 0;
      padding: 13px;
      border: 1px solid #ccd6e2;
      border-radius: 8px;
    }
    button {
      background: #183e78;
      color: white;
      border: 0;
      border-radius: 8px;
      padding: 0 20px;
      cursor: pointer;
    }
    button:disabled { opacity: .5; cursor: not-allowed; }
    @media (max-width: 800px) {
      .layout { flex-direction: column; }
      .sidebar { width: 100%; }
      nav { margin: 15px 0; }
      main { padding: 16px; }
      .cards { grid-template-columns: 1fr; }
      header { flex-direction: column; align-items: flex-start; }
    }
  `]
})
export class BankingDashboard {
  private readonly http = inject(HttpClient);

  input = '';
  loading = false;

  messages: Message[] = [{
    role: 'assistant',
    content: 'Welcome to ADRIA Banking Assistant. Sign-in integration is pending.'
  }];

  sendMessage(): void {
    const question = this.input.trim();

    if (!question || this.loading) return;

    this.messages.push({ role: 'user', content: question });
    this.input = '';
    this.loading = true;

    this.http.post<{ reply: string }>(
      '/api/chat',
      { message: question }
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
            ? 'Please sign in with Keycloak before using chat.'
            : error.status === 403
              ? 'You do not have the required banking permissions.'
              : 'Assistant unavailable. Check the backend connection.'
        });
      }
    });
  }
}
