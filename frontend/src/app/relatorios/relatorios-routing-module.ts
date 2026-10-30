import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { Relatorios } from './relatorios';

const routes: Routes = [
  { path: '', component: Relatorios }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class RelatoriosRoutingModule { }
