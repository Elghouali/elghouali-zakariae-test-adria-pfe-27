
import { Routes } from '@angular/router';
import { BankingDashboard } from './banking-dashboard';

export const routes: Routes = [
  {
    path: '',
    component: BankingDashboard,
    title: 'ADRIA Bank | Dashboard'
  },
  {
    path: '**',
    redirectTo: ''
  }
];
