export const AUTH_SESSION_SYNC_CHANNEL = "audita-auth-session";

export type AuthSessionSyncEventType =
  | "session-restored"
  | "session-logged-out";

export interface AuthSessionSyncEvent {
  sourceTabId: string;
  timestamp: number;
  type: AuthSessionSyncEventType;
}

export function createAuthSessionSyncEvent(
  type: AuthSessionSyncEventType,
  sourceTabId: string,
): AuthSessionSyncEvent {
  return {
    sourceTabId,
    timestamp: Date.now(),
    type,
  };
}
