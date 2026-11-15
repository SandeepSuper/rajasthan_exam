import { timingSafeEqual } from "node:crypto";
import { createServer as createHttpServer, type IncomingMessage, type ServerResponse } from "node:http";
import type { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StreamableHTTPServerTransport } from "@modelcontextprotocol/sdk/server/streamableHttp.js";
import { addSecret, log } from "../redact.js";

const MAX_BODY_BYTES = 4 * 1024 * 1024;

function sendJson(res: ServerResponse, status: number, body: unknown): void {
  if (res.headersSent) return;
  res.writeHead(status, { "Content-Type": "application/json" });
  res.end(JSON.stringify(body));
}

function rpcError(res: ServerResponse, status: number, message: string): void {
  sendJson(res, status, { jsonrpc: "2.0", error: { code: -32000, message }, id: null });
}

function safeEqual(a: string, b: string): boolean {
  const x = Buffer.from(a);
  const y = Buffer.from(b);
  return x.length === y.length && timingSafeEqual(x, y);
}

async function readJsonBody(req: IncomingMessage): Promise<unknown> {
  const chunks: Buffer[] = [];
  let size = 0;
  for await (const chunk of req) {
    size += (chunk as Buffer).length;
    if (size > MAX_BODY_BYTES) throw new Error("Request body too large");
    chunks.push(chunk as Buffer);
  }
  return JSON.parse(Buffer.concat(chunks).toString("utf8"));
}

/**
 * Streamable HTTP transport in stateless mode, for remote clients such as a
 * claude.ai custom connector. The only MCP endpoint is /mcp/<secret>; every
 * other path returns 404 (except "/" which is a plain health check).
 */
export async function startHttp(opts: {
  port: number;
  pathSecret: string;
  buildServer: () => McpServer;
}): Promise<void> {
  addSecret(opts.pathSecret);
  const mcpPrefix = "/mcp/";

  const httpServer = createHttpServer(async (req, res) => {
    const path = (req.url ?? "/").split("?")[0];

    if (path === "/" && req.method === "GET") {
      res.writeHead(200, { "Content-Type": "text/plain" });
      res.end("ok");
      return;
    }

    const secret = path.startsWith(mcpPrefix) ? path.slice(mcpPrefix.length).replace(/\/+$/, "") : "";
    if (!secret || !safeEqual(secret, opts.pathSecret)) {
      res.writeHead(404, { "Content-Type": "text/plain" });
      res.end("Not found");
      return;
    }

    // Stateless: no sessions, so there is no server-initiated GET stream or DELETE.
    if (req.method !== "POST") {
      res.writeHead(405, { Allow: "POST" });
      res.end();
      return;
    }

    let body: unknown;
    try {
      body = await readJsonBody(req);
    } catch (err) {
      rpcError(res, 400, err instanceof Error && err.message.includes("too large") ? "Request body too large" : "Invalid JSON");
      return;
    }

    const server = opts.buildServer();
    const transport = new StreamableHTTPServerTransport({ sessionIdGenerator: undefined });
    res.on("close", () => {
      void transport.close();
      void server.close();
    });
    try {
      await server.connect(transport);
      await transport.handleRequest(req, res, body);
    } catch (err) {
      log(`HTTP request failed: ${err instanceof Error ? err.message : String(err)}`);
      rpcError(res, 500, "Internal server error");
    }
  });

  await new Promise<void>((resolve, reject) => {
    httpServer.once("error", reject);
    httpServer.listen(opts.port, "0.0.0.0", () => resolve());
  });
  log(`Listening on port ${opts.port}. MCP endpoint: /mcp/<MCP_PATH_SECRET>`);
}
