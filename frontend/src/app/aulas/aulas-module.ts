import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';

import { AulasRoutingModule } from './aulas-routing-module';
import { Aulas } from './aulas';

@NgModule({
  declarations: [Aulas],
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatIconModule,
    AulasRoutingModule
  ]
})
export class AulasModule { }
