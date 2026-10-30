import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { Quiz } from './quiz';

const routes: Routes = [
  { path: '', component: Quiz }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class QuizRoutingModule { }
