import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';

import { Chat } from './chat/chat';

const routes: Routes = [{ path: '', component: Chat }];

@NgModule({
  declarations: [Chat],
  imports: [CommonModule, FormsModule, RouterModule.forChild(routes)],
})
export class ChatModule {}
