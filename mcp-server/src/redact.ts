/**
 * Keeps secrets (admin password, JWTs) out of tool output and logs.
 */
const JWT_PATTERN = /eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+/g;
const BEARER_PATTERN = /Bearer\s+[A-Za-z0-9._~+/=-]+/gi;
const SENSITIVE_KEY = /(password|passwd|token|secret|api[_-]?key)|^otp/i;

const secrets = new Set<string>();

/** Register a value that must never appear in output (e.g. the password or current JWT). */
export function addSecret(value: string | undefined): void {
  if (value && value.length >= 4) secrets.add(value);
}

export function removeSecret(value: string | undefined): void {
  if (value) secrets.delete(value);
}

export function redactText(text: string): string {
  let out = text;
  for (const s of secrets) {
    out = out.split(s).join("[REDACTED]");
  }
  return out.replace(BEARER_PATTERN, "Bearer [REDACTED]").replace(JWT_PATTERN, "[REDACTED_JWT]");
}

/** Deep-copies JSON data, masking values of sensitive-looking keys. */
export function redactData(data: unknown): unknown {
  if (Array.isArray(data)) return data.map(redactData);
  if (data && typeof data === "object") {
    const out: Record<string, unknown> = {};
    for (const [k, v] of Object.entries(data as Record<string, unknown>)) {
      out[k] = SENSITIVE_KEY.test(k) && v !== null && v !== undefined ? "[REDACTED]" : redactData(v);
    }
    return out;
  }
  return data;
}

/** All logging goes to stderr (stdout is reserved for the MCP stdio protocol). */
export function log(message: string): void {
  process.stderr.write(`[rajasthan-exam-mcp] ${redactText(message)}\n`);
}
