import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { ApiClient, ApiError } from "./api-client.js";
import type { Config } from "./config.js";
import { log, redactData, redactText } from "./redact.js";
import type { ToolDef } from "./tools/define.js";
import { readTools } from "./tools/read-tools.js";
import { writeTools } from "./tools/write-tools.js";

type ToolResult = { content: { type: "text"; text: string }[]; isError?: boolean };

function formatOutput(data: unknown, maxChars: number): string {
  const text = typeof data === "string" ? data : JSON.stringify(redactData(data), null, 2) ?? "null";
  const safe = redactText(text);
  if (safe.length <= maxChars) return safe;
  return (
    safe.slice(0, maxChars) +
    `\n\n… [output truncated: showing ${maxChars} of ${safe.length} characters. Use a narrower tool or filter.]`
  );
}

function formatError(err: unknown): string {
  if (err instanceof ApiError) return redactText(err.message);
  if (err instanceof Error) return redactText(`Error: ${err.message}`);
  return redactText(`Error: ${String(err)}`);
}

function register(server: McpServer, tool: ToolDef<any>, api: ApiClient, config: Config): void {
  server.registerTool(
    tool.name,
    {
      title: tool.title,
      description: tool.description,
      inputSchema: tool.inputSchema,
      annotations: {
        title: tool.title,
        readOnlyHint: !tool.write,
        destructiveHint: tool.write ? Boolean(tool.destructive) : false,
        openWorldHint: true,
      },
    },
    async (args: Record<string, unknown>): Promise<ToolResult> => {
      try {
        const data = await tool.run(args, api);
        return { content: [{ type: "text", text: formatOutput(data, config.maxOutputChars) }] };
      } catch (err) {
        const message = formatError(err);
        log(`Tool ${tool.name} failed: ${message}`);
        return { isError: true, content: [{ type: "text", text: message }] };
      }
    },
  );
}

/**
 * Builds the MCP server with all tools registered. Transport-independent: stdio
 * uses one instance, HTTP builds one per request but shares the same ApiClient
 * (so the admin JWT is reused instead of logging in on every request).
 */
export function createServer(config: Config, api: ApiClient): McpServer {
  const server = new McpServer({ name: "rajasthan-exam-api", version: "0.1.0" });
  const tools = config.enableWriteTools ? [...readTools, ...writeTools] : readTools;
  for (const tool of tools) register(server, tool, api, config);
  return server;
}

export function logToolSummary(config: Config): void {
  log(
    `Registered ${readTools.length} read-only tools` +
      (config.enableWriteTools
        ? ` and ${writeTools.length} WRITE tools (ENABLE_WRITE_TOOLS=true).`
        : `; ${writeTools.length} write tools are disabled (set ENABLE_WRITE_TOOLS=true to enable).`),
  );
}
