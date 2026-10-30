import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { Feedback } from './feedback';

const routes: Routes = [
  {
    path: '',
    component: Feedback
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class FeedbackRoutingModule { }