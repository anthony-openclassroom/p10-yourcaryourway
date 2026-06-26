export interface ChatSession {
  id: string;
  userId: string;
  agencyId: string;
  status: 'open' | 'closed';
  createdAt: string;
}

export interface ChatMessage {
  id: string;
  sessionId: string;
  senderRole: 'client' | 'agent';
  content: string;
  sentAt: string;
}

export interface CreateSessionRequest {
  userId: string;
  agencyId: string;
}

export interface InboundMessage {
  content: string;
  senderRole: 'client' | 'agent';
}
