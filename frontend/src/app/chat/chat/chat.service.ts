import { Injectable, OnDestroy } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable } from 'rxjs';
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

import { environment } from '../../../environments/environment';
import { ChatMessage, ChatSession, CreateSessionRequest } from './chat.model';

@Injectable({ providedIn: 'root' })
export class ChatService implements OnDestroy {
  private stompClient: Client | null = null;
  private messagesSubject = new BehaviorSubject<ChatMessage[]>([]);

  readonly messages$: Observable<ChatMessage[]> = this.messagesSubject.asObservable();

  constructor(private http: HttpClient) {}

  createSession(request: CreateSessionRequest): Observable<ChatSession> {
    return this.http.post<ChatSession>(`${environment.apiUrl}/api/chat/sessions`, request);
  }

  loadHistory(sessionId: string): Observable<ChatMessage[]> {
    return this.http.get<ChatMessage[]>(
      `${environment.apiUrl}/api/chat/sessions/${sessionId}/messages`,
    );
  }

  connect(sessionId: string): void {
    this.stompClient = new Client({
      webSocketFactory: () => new SockJS(environment.wsUrl) as WebSocket,
      onConnect: () => {
        this.stompClient!.subscribe(`/topic/chat/${sessionId}`, (frame: IMessage) => {
          const message: ChatMessage = JSON.parse(frame.body);
          this.messagesSubject.next([...this.messagesSubject.value, message]);
        });
      },
      onDisconnect: () => console.info('WebSocket déconnecté'),
    });
    this.stompClient.activate();
  }

  send(sessionId: string, content: string, senderRole: 'client' | 'agent'): void {
    if (!this.stompClient?.connected) return;
    this.stompClient.publish({
      destination: `/app/chat/${sessionId}`,
      body: JSON.stringify({ content, senderRole }),
    });
  }

  closeSession(sessionId: string, closedBy: string): Observable<void> {
    return this.http.patch<void>(
      `${environment.apiUrl}/api/chat/sessions/${sessionId}/close?closedBy=${encodeURIComponent(closedBy)}`,
      {}
    );
  }

  prependHistory(messages: ChatMessage[]): void {
    this.messagesSubject.next([...messages, ...this.messagesSubject.value]);
  }

  disconnect(): void {
    this.stompClient?.deactivate();
    this.stompClient = null;
    this.messagesSubject.next([]);
  }

  ngOnDestroy(): void {
    this.disconnect();
  }
}
