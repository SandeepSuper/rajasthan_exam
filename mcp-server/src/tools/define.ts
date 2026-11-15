import type { z } from "zod";
import type { ApiClient } from "../api-client.js";

export interface ToolDef<S extends z.ZodRawShape = z.ZodRawShape> {
  name: string;
  title: string;
  description: string;
  /** true = modifies data; only registered when ENABLE_WRITE_TOOLS=true. */
  write: boolean;
  /** For write tools: deletes or overwrites data. */
  destructive?: boolean;
  inputSchema: S;
  run: (args: z.infer<z.ZodObject<S>>, api: ApiClient) => Promise<unknown>;
}

/** Helper that keeps the handler's argument type tied to its schema. */
export function defineTool<S extends z.ZodRawShape>(def: ToolDef<S>): ToolDef<S> {
  return def;
}

export const WRITE_PREFIX = "⚠️ MODIFIES DATA on the live backend. ";
