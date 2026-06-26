import {
  Component,
  OnInit,
  OnDestroy,
  ViewChild,
  ElementRef,
  AfterViewChecked,
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Observable } from 'rxjs';

import { ChatService } from './chat.service';
import { ChatMessage } from './chat.model';

const DEMO_USER_ID = '00000000-0000-0000-0000-000000000001';
const DEMO_AGENCY_ID = '00000000-0000-0000-0000-000000000001';

@Component({
  selector: 'app-chat',
  templateUrl: './chat.html',
  styleUrls: ['./chat.scss'],
  standalone: false,
})
export class Chat implements OnInit, OnDestroy, AfterViewChecked {
  @ViewChild('messageList') private messageList!: ElementRef<HTMLUListElement>;

  messages$!: Observable<ChatMessage[]>;
  sessionId = '';
  currentRole: 'client' | 'agent' = 'client';
  inputText = '';
  isConnected = false;
  errorMessage = '';

  constructor(
    private chatService: ChatService,
    private route: ActivatedRoute,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.messages$ = this.chatService.messages$;

    const params = this.route.snapshot.queryParamMap;
    const existingSessionId = params.get('sessionId');
    const role = params.get('role');

    if (role === 'agent') this.currentRole = 'agent';

    if (existingSessionId) {
      this.joinSession(existingSessionId);
    } else {
      this.createAndRedirect();
    }
  }

  private createAndRedirect(): void {
    this.chatService.createSession({ userId: DEMO_USER_ID, agencyId: DEMO_AGENCY_ID }).subscribe({
      next: (session) => {
        this.router.navigate([], {
          queryParams: { sessionId: session.id, role: 'client' },
          replaceUrl: true,
        });
        this.joinSession(session.id);
      },
      error: () => {
        this.errorMessage =
          'Impossible de démarrer la session. Vérifiez que le backend est démarré.';
      },
    });
  }

  private joinSession(sessionId: string): void {
    this.sessionId = sessionId;
    this.chatService.connect(sessionId);
    this.isConnected = true;
    this.chatService.loadHistory(sessionId).subscribe((history) => {
      this.chatService.prependHistory(history);
    });
  }

  send(): void {
    const content = this.inputText.trim();
    if (!content || !this.sessionId) return;
    this.chatService.send(this.sessionId, content, this.currentRole);
    this.inputText = '';
  }

  onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.send();
    }
  }

  trackById(_index: number, message: ChatMessage): string {
    return message.id;
  }

  ngAfterViewChecked(): void {
    try {
      const el = this.messageList?.nativeElement;
      if (el) el.scrollTop = el.scrollHeight;
    } catch {
      /* ignoré */
    }
  }

  ngOnDestroy(): void {
    this.chatService.disconnect();
  }
}
