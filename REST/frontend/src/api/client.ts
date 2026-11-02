/**
 * Thin wrapper around fetch for the Prism REST API.
 *
 * All paths are relative ("/api/..."): in production the UI is served by Spring Boot on the same
 * origin, and in development Vite proxies /api to http://localhost:8080 (see vite.config.ts).
 */

export class ApiError extends Error {
  readonly status: number;
  readonly body: string;

  constructor(status: number, body: string) {
    super(body ? `HTTP ${status}: ${body}` : `HTTP ${status}`);
    this.name = 'ApiError';
    this.status = status;
    this.body = body;
  }

  /** Parses the error body as JSON, or returns undefined if it isn't JSON. */
  json<T>(): T | undefined {
    try {
      return JSON.parse(this.body) as T;
    } catch {
      return undefined;
    }
  }
}

interface RequestOptions {
  headers?: Record<string, string>;
  signal?: AbortSignal;
}

async function request<T>(method: string, path: string, body?: unknown, options: RequestOptions = {}): Promise<T> {
  const headers: Record<string, string> = { Accept: 'application/json', ...options.headers };
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
  }

  const response = await fetch(path, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
    signal: options.signal,
  });

  const text = await response.text();
  if (!response.ok) {
    throw new ApiError(response.status, text);
  }
  return (text ? JSON.parse(text) : undefined) as T;
}

export const api = {
  get: <T>(path: string, options?: RequestOptions) => request<T>('GET', path, undefined, options),
  post: <T>(path: string, body: unknown, options?: RequestOptions) => request<T>('POST', path, body, options),
  put: <T>(path: string, body: unknown, options?: RequestOptions) => request<T>('PUT', path, body, options),
  delete: <T>(path: string, options?: RequestOptions) => request<T>('DELETE', path, undefined, options),
};

/**
 * For endpoints that answer failures (400/401/409) with a JSON body like
 * {"success": false, "message": "..."}: returns that body instead of throwing.
 * Network errors and non-JSON error bodies are still thrown.
 */
export async function withActionResult<T extends { success: boolean }>(call: () => Promise<T>): Promise<T> {
  try {
    return await call();
  } catch (error) {
    const result = error instanceof ApiError ? error.json<T>() : undefined;
    if (result && typeof result.success === 'boolean') {
      return result;
    }
    throw error;
  }
}

/** True for the error fetch throws when a request is cancelled via AbortController. */
export function isAbortError(error: unknown): boolean {
  return error instanceof DOMException && error.name === 'AbortError';
}

/** Best-effort human-readable message for any thrown value. */
export function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : String(error);
}

/** The server's {success:false, message} text when there is one, else the general error text. */
export function serverMessage(error: unknown): string {
  if (error instanceof ApiError) {
    const body = error.json<{ message?: string }>();
    if (body?.message) return body.message;
  }
  return errorMessage(error);
}

/** True for a 409 Conflict (e.g. a name already in use, or a record still in use). */
export function isConflict(error: unknown): boolean {
  return error instanceof ApiError && error.status === 409;
}
