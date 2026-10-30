import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { FundamentacaoComponent } from './fundamentacao';

const routes: Routes = [
  { path: '', component: FundamentacaoComponent }
];

@NgModule({
  imports: [
    FundamentacaoComponent,
    RouterModule.forChild(routes)
  ]
})
export class FundamentacaoModule { }
