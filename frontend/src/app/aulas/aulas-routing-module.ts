import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { Aulas } from './aulas';

const routes: Routes = [
  { path: '', component: Aulas }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class AulasRoutingModule { }
