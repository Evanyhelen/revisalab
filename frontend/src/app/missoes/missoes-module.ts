import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { MissoesComponent } from './missoes';

const routes: Routes = [
  { path: '', component: MissoesComponent }
];

@NgModule({
  imports: [
    MissoesComponent,
    RouterModule.forChild(routes)
  ]
})
export class MissoesModule { }
