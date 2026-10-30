import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';

import { QuizRoutingModule } from './quiz-routing-module';
import { Quiz } from './quiz';

@NgModule({
  declarations: [Quiz],
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatIconModule,
    QuizRoutingModule
  ]
})
export class QuizModule { }
