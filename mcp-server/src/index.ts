#!/usr/bin/env node
import { ApiClient } from "./api-client.js";
import { loadConfig } from "./config.js";
import { log } from "./redact.js";
import { createServer, logToolSummary } from "./server.js";
import { startHttp } from "./transports/http.js";
import { startStdio } from "./transports/stdio.js";

async function main(): Promise<void> {
  const config = loadConfig();
  const api = new ApiClient(config);
  logToolSummary(config);

  if (config.transport === "http") {
    await startHttp({
      port: config.httpPort,
      pathSecret: config.mcpPathSecret!,
      buildServer: () => createServer(config, api),
    });
  } else {
    await startStdio(createServer(config, api));
    log(`Ready on stdio. API: ${config.apiBaseUrl}`);
  }
}

main().catch((err: unknown) => {
  log(`Fatal: ${err instanceof Error ? err.message : String(err)}`);
  process.exit(1);
});
