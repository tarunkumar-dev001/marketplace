import { isAxiosError } from 'axios'
import { apiClient } from './client'
import type { ApiResponse } from '../types/api'
import type { Credentials, LoginResult, RegisteredUser } from '../types/auth'

export async function registerCustomer(credentials: Credentials): Promise<RegisteredUser> {
  const response = await apiClient.post<ApiResponse<RegisteredUser>>('/api/auth/register', credentials)
  if (!response.data.success || response.data.data === null) {
    throw new Error(response.data.message || 'Unable to create your account.')
  }
  return response.data.data
}

export async function login(credentials: Credentials): Promise<LoginResult> {
  const response = await apiClient.post<ApiResponse<LoginResult>>('/api/auth/login', credentials)
  if (!response.data.success || response.data.data === null) {
    throw new Error(response.data.message || 'Unable to sign in.')
  }
  return response.data.data
}

export function authErrorMessage(error: unknown): string {
  if (isAxiosError<ApiResponse<null>>(error)) {
    return error.response?.data?.message ?? 'We could not complete your request. Please try again.'
  }
  return error instanceof Error ? error.message : 'We could not complete your request. Please try again.'
}
