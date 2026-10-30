import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';

import { DashboardRoutingModule } from './dashboard-routing-module';
import { Dashboard } from './dashboard';

@NgModule({
  declarations: [Dashboard],
  imports: [
    CommonModule,
    RouterModule,
    MatIconModule,
    DashboardRoutingModule
  ]
})
export class DashboardModule { }
