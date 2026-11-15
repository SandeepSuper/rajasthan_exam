import type { Config } from "./config.js";
import { addSecret, log, removeSecret } from "./redact.js";

export type HttpMethod = "GET" | "POST" | "PUT" | "PATCH" | "DELETE";

export interface RequestOptions {
  method: HttpMethod;
  path: string;
  /** Query parameters; undefined values are skipped. */
  query?: Record<string, string | number | boolean | undefined>;
  /** JSON body. */
  body?: unknown;
  /** Multipart body (used for file uploads). */
  form?: FormData;
  /** Send the admin JWT (logs in automatically when needed). */
  auth: boolean;
}

export interface ApiResponse {
  status: number;
  data: unknown;
}

export class ApiError extends Error {
  constructor(
    readonly status: number | null,
    message: string,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

const RETRYABLE_STATUS = new Set([502, 503, 504]);
const RETRY_DELAY_MS = 2_000;

function sleep(ms: number): Promise<void> {
  return new Promise((r) => setTimeout(r, ms));
}

function isTimeout(err: unknown): boolean {
  return err instanceof Error && (err.name === "TimeoutError" || err.name === "AbortError");
}

async function parseBody(res: Response): Promise<unknown> {
  const text = await res.text();
  if (text === "") return null;
  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}

function errorMessageFrom(data: unknown): string {
  if (data && typeof data === "object") {
    const d = data as Record<string, unknown>;
    for (const key of ["message", "error", "detail", "title"]) {
      if (typeof d[key] === "string" && d[key]) return d[key] as string;
    }
    return JSON.stringify(data).slice(0, 500);
  }
  if (typeof data === "string" && data.trim()) return data.trim().slice(0, 500);
  return "(no response body)";
}

export class ApiClient {
  private token: string | undefined;
  private loginInFlight: Promise<string> | undefined;

  constructor(private readonly config: Config) {
    addSecret(config.adminPassword);
  }

  async request(opts: RequestOptions): Promise<ApiResponse> {
    const url = this.buildUrl(opts.path, opts.query);

    if (!opts.auth) {
      return this.finish(await this.send(url, opts, undefined));
    }

    let token = await this.getToken();
    let res = await this.send(url, opts, token);

    // Spring returns 403 (not 401) for an expired/replaced token, so treat both as "log in again".
    if (res.status === 401 || res.status === 403) {
      log(`Got ${res.status} on ${opts.method} ${opts.path}; logging in again and retrying once.`);
      this.invalidate(token);
      token = await this.getToken();
      res = await this.send(url, opts, token);
    }
    return this.finish(res);
  }

  private buildUrl(path: string, query?: RequestOptions["query"]): URL {
    const url = new URL(this.config.apiBaseUrl + path);
    for (const [k, v] of Object.entries(query ?? {})) {
      if (v !== undefined) url.searchParams.set(k, String(v));
    }
    return url;
  }

  private async finish(res: { status: number; statusText: string; data: unknown }): Promise<ApiResponse> {
    if (res.status >= 200 && res.status < 300) {
      return { status: res.status, data: res.data };
    }
    throw new ApiError(res.status, `HTTP ${res.status} ${res.statusText}: ${errorMessageFrom(res.data)}`);
  }

  /**
   * Sends one request with a timeout. Cold starts on AppSail can take 20-40s, so
   * failures are retried once: GETs on timeout / network error / 502-504, and
   * writes only on network error or 502-504 (a timed-out write may already have run).
   */
  private async send(
    url: URL,
    opts: RequestOptions,
    token: string | undefined,
  ): Promise<{ status: number; statusText: string; data: unknown }> {
    const headers: Record<string, string> = { Accept: "application/json" };
    if (token) headers.Authorization = `Bearer ${token}`;
    let body: BodyInit | undefined;
    if (opts.form) {
      body = opts.form;
    } else if (opts.body !== undefined) {
      headers["Content-Type"] = "application/json";
      body = JSON.stringify(opts.body);
    }
    const idempotent = opts.method === "GET";

    for (let attempt = 1; ; attempt++) {
      try {
        const res = await fetch(url, {
          method: opts.method,
          headers,
          body,
          signal: AbortSignal.timeout(this.config.requestTimeoutMs),
        });
        if (RETRYABLE_STATUS.has(res.status) && attempt === 1) {
          log(`${opts.method} ${opts.path} returned ${res.status}; retrying once (server may be cold-starting).`);
          await res.body?.cancel();
          await sleep(RETRY_DELAY_MS);
          continue;
        }
        return { status: res.status, statusText: res.statusText, data: await parseBody(res) };
      } catch (err) {
        const timeout = isTimeout(err);
        const canRetry = attempt === 1 && (idempotent || !timeout);
        if (canRetry) {
          log(`${opts.method} ${opts.path} failed (${timeout ? "timeout" : "network error"}); retrying once.`);
          await sleep(RETRY_DELAY_MS);
          continue;
        }
        if (timeout) {
          throw new ApiError(
            null,
            `Request timed out after ${this.config.requestTimeoutMs / 1000}s (${opts.method} ${opts.path}). ` +
              "The server may be cold-starting; try again.",
          );
        }
        const reason = err instanceof Error ? (err.cause instanceof Error ? err.cause.message : err.message) : String(err);
        throw new ApiError(null, `Network error calling ${opts.method} ${opts.path}: ${reason}`);
      }
    }
  }

  private invalidate(token: string | undefined): void {
    if (token && this.token === token) {
      removeSecret(this.token);
      this.token = undefined;
    }
  }

  /** Returns the in-memory admin JWT, logging in if needed. Concurrent callers share one login. */
  private async getToken(): Promise<string> {
    if (this.token) return this.token;
    if (!this.loginInFlight) {
      this.loginInFlight = this.login().finally(() => {
        this.loginInFlight = undefined;
      });
    }
    return this.loginInFlight;
  }

  private async login(): Promise<string> {
    const { adminUsername, adminPassword } = this.config;
    if (!adminUsername || !adminPassword) {
      throw new ApiError(null, "ADMIN_USERNAME and ADMIN_PASSWORD must be set to use this tool.");
    }
    log("Logging in as admin (note: this ends any other active admin session, e.g. the browser panel).");
    const url = this.buildUrl("/api/admin-auth/login");
    const res = await this.send(
      url,
      { method: "POST", path: "/api/admin-auth/login", body: { username: adminUsername, password: adminPassword }, auth: false },
      undefined,
    );
    if (res.status === 401) {
      throw new ApiError(401, "Admin login failed: invalid credentials. Check ADMIN_USERNAME / ADMIN_PASSWORD.");
    }
    if (res.status < 200 || res.status >= 300) {
      throw new ApiError(res.status, `Admin login failed: HTTP ${res.status} ${res.statusText}: ${errorMessageFrom(res.data)}`);
    }
    const token = (res.data as { token?: unknown } | null)?.token;
    if (typeof token !== "string" || token === "") {
      throw new ApiError(res.status, "Admin login failed: response did not contain a token.");
    }
    addSecret(token);
    this.token = token;
    log("Admin login successful.");
    return token;
  }
}
