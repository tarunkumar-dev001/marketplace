const ACCESS_TOKEN_KEY = 'marketplace.access-token'
export const AUTH_SESSION_INVALIDATED_EVENT = 'marketplace:auth-session-invalidated'

export function getAccessToken(): string | null {
  return window.localStorage.getItem(ACCESS_TOKEN_KEY)
}

export function setAccessToken(accessToken: string): void {
  window.localStorage.setItem(ACCESS_TOKEN_KEY, accessToken)
}

export function clearAccessToken(): void {
  window.localStorage.removeItem(ACCESS_TOKEN_KEY)
}

export function notifySessionInvalidated(): void {
  window.dispatchEvent(new Event(AUTH_SESSION_INVALIDATED_EVENT))
}
