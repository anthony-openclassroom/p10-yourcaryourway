import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { Chat } from './chat/chat';

const routes: Routes = [
  { path: '', component: Chat }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class ChatRoutingModule { }
