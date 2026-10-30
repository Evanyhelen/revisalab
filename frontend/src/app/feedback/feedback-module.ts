import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Feedback } from './feedback';
import { FeedbackRoutingModule } from './feedback-routing-module';

@NgModule({
  declarations: [],
  imports: [
    CommonModule,
    FeedbackRoutingModule,
    Feedback // Componente standalone
  ]
})
export class FeedbackModule { }