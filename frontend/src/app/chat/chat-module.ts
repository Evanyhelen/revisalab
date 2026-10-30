import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { ChatComponent } from './chat';

const routes: Routes = [
  { path: '', component: ChatComponent }
];

@NgModule({
  imports: [ChatComponent, RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class ChatModule { }
