import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { ImpactoComponent } from './impacto';

const routes: Routes = [
  { path: '', component: ImpactoComponent }
];

@NgModule({
  imports: [
    ImpactoComponent,
    RouterModule.forChild(routes)
  ]
})
export class ImpactoModule { }
