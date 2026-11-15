import { existsSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

export interface Config {
  apiBaseUrl: string;
  adminUsername?: string;
  adminPassword?: string;
  enableWriteTools: boolean;
  requestTimeoutMs: number;
  maxOutputChars: number;
  transport: "stdio" | "http";
  /** HTTP only: port to listen on. */
  httpPort: number;
  /** HTTP only: secret path segment; the endpoint is /mcp/<secret>. */
  mcpPathSecret?: string;
}

/**
 * Loads mcp-server/.env if it exists. Variables that are already set in the
 * environment (e.g. from the Claude config "env" block) are not overwritten.
 */
function loadDotEnv(): void {
  const here = dirname(fileURLToPath(import.meta.url));
  const envPath = resolve(here, "..", ".env");
  if (existsSync(envPath)) {
    process.loadEnvFile(envPath);
  }
}

function parseBaseUrl(raw: string | undefined): string {
  if (!raw || raw.trim() === "") {
    throw new Error("API_BASE_URL is not set. Copy .env.example to .env and fill it in.");
  }
  let url: URL;
  try {
    url = new URL(raw.trim());
  } catch {
    throw new Error("API_BASE_URL is not a valid URL.");
  }
  if (url.username || url.password) {
    throw new Error("API_BASE_URL must not contain credentials. Use ADMIN_USERNAME / ADMIN_PASSWORD instead.");
  }
  const isLocal = ["localhost", "127.0.0.1", "::1"].includes(url.hostname);
  if (url.protocol !== "https:" && !(isLocal && url.protocol === "http:")) {
    throw new Error("API_BASE_URL must use https (http is allowed only for localhost).");
  }
  return url.toString().replace(/\/+$/, "");
}

function parseIntEnv(name: string, fallback: number): number {
  const raw = process.env[name];
  if (!raw) return fallback;
  const n = Number.parseInt(raw, 10);
  return Number.isFinite(n) && n > 0 ? n : fallback;
}

export function loadConfig(): Config {
  loadDotEnv();

  const transport = (process.env.MCP_TRANSPORT ?? "stdio").trim().toLowerCase();
  if (transport !== "stdio" && transport !== "http") {
    throw new Error(`MCP_TRANSPORT="${transport}" is not supported. Use "stdio" or "http".`);
  }

  const mcpPathSecret = process.env.MCP_PATH_SECRET?.trim() || undefined;
  if (transport === "http") {
    if (!mcpPathSecret || !/^[A-Za-z0-9_-]{32,}$/.test(mcpPathSecret)) {
      throw new Error(
        "MCP_PATH_SECRET must be set for http transport: at least 32 characters, only letters, digits, - and _.",
      );
    }
  }

  return {
    apiBaseUrl: parseBaseUrl(process.env.API_BASE_URL),
    adminUsername: process.env.ADMIN_USERNAME || undefined,
    adminPassword: process.env.ADMIN_PASSWORD || undefined,
    enableWriteTools: (process.env.ENABLE_WRITE_TOOLS ?? "").trim().toLowerCase() === "true",
    requestTimeoutMs: parseIntEnv("REQUEST_TIMEOUT_MS", 60_000),
    maxOutputChars: parseIntEnv("MAX_OUTPUT_CHARS", 50_000),
    transport,
    // AppSail gives the port in X_ZOHO_CATALYST_LISTEN_PORT; most other hosts use PORT.
    httpPort: parseIntEnv("X_ZOHO_CATALYST_LISTEN_PORT", parseIntEnv("PORT", 3000)),
    mcpPathSecret,
  };
}
