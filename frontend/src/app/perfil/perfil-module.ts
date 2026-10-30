import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { PerfilComponent } from './perfil';

const routes: Routes = [
  { path: '', component: PerfilComponent }
];

@NgModule({
  imports: [PerfilComponent, RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class PerfilModule {}
