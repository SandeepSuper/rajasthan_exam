import { z } from "zod";
import { defineTool, type ToolDef } from "./define.js";

const uuid = (what: string) => z.string().uuid().describe(`${what} (UUID)`);

/** GET helper: returns only the response data. */
const get =
  (path: string, auth: boolean, query?: Record<string, string | number | boolean | undefined>) =>
  async (api: Parameters<ToolDef["run"]>[1]) =>
    (await api.request({ method: "GET", path, query, auth })).data;

export const readTools: ToolDef<any>[] = [
  // ─── Exams & tests ────────────────────────────────────────────────────────
  defineTool({
    name: "list_exams",
    title: "List exams",
    description: "List all exam categories (e.g. REET, Patwari) with test counts, price and discount.",
    write: false,
    inputSchema: {},
    run: (_a, api) => get("/api/tests/exams", true)(api),
  }),
  defineTool({
    name: "list_tests",
    title: "List tests of an exam",
    description: "List all tests/mocks under one exam. Use list_exams first to get the examId.",
    write: false,
    inputSchema: { examId: uuid("Exam ID") },
    run: (a, api) => get("/api/tests", true, { examId: a.examId })(api),
  }),
  defineTool({
    name: "get_test",
    title: "Get test details",
    description: "Get details of one test (type, duration, question count, marks, live window).",
    write: false,
    inputSchema: { testId: uuid("Test ID") },
    run: (a, api) => get(`/api/tests/${encodeURIComponent(a.testId)}`, true)(api),
  }),
  defineTool({
    name: "list_live_tests",
    title: "List live tests",
    description: "List tests that are currently marked live.",
    write: false,
    inputSchema: {},
    run: (_a, api) => get("/api/tests/live", true)(api),
  }),
  defineTool({
    name: "list_questions",
    title: "List questions of a test",
    description: "List all questions of a test (Hindi/English text, options, correct answer, solution).",
    write: false,
    inputSchema: { testId: uuid("Test ID") },
    run: (a, api) => get("/api/tests/questions", true, { testId: a.testId })(api),
  }),
  defineTool({
    name: "get_content_overview",
    title: "Content overview (admin)",
    description: "Admin dashboard counts: exams, tests, questions, users and related totals.",
    write: false,
    inputSchema: {},
    run: (_a, api) => get("/api/admin/content/overview", true)(api),
  }),

  // ─── Users ────────────────────────────────────────────────────────────────
  defineTool({
    name: "search_users",
    title: "Search users (admin)",
    description: "Search users by name, email or mobile number.",
    write: false,
    inputSchema: { q: z.string().min(1).describe("Search text (name, email or mobile)") },
    run: (a, api) => get("/api/admin/users/search", true, { q: a.q })(api),
  }),
  defineTool({
    name: "list_top_referrers",
    title: "Top referrers",
    description: "List the users with the most referrals (public endpoint).",
    write: false,
    inputSchema: {},
    run: (_a, api) => get("/api/users/top-referrers", false)(api),
  }),
  defineTool({
    name: "get_leaderboard",
    title: "Leaderboard",
    description: "Get top rankers for one test, or the global leaderboard if testId is omitted.",
    write: false,
    inputSchema: { testId: z.string().optional().describe("Test ID; omit for the global leaderboard") },
    run: (a, api) => get("/api/leaderboard", true, { testId: a.testId })(api),
  }),

  // ─── Payments ─────────────────────────────────────────────────────────────
  defineTool({
    name: "get_payment_stats",
    title: "Payment stats (admin)",
    description: "Revenue and order statistics.",
    write: false,
    inputSchema: {},
    run: (_a, api) => get("/api/admin/payments/stats", true)(api),
  }),
  defineTool({
    name: "list_orders",
    title: "List orders (admin)",
    description: "List all payment orders. Can be large; output is truncated if too long.",
    write: false,
    inputSchema: {},
    run: (_a, api) => get("/api/admin/payments/orders", true)(api),
  }),
  defineTool({
    name: "list_exam_pricing",
    title: "Exam pricing (admin)",
    description: "List exams with their price and discount settings.",
    write: false,
    inputSchema: {},
    run: (_a, api) => get("/api/admin/payments/exams", true)(api),
  }),

  // ─── Reports ──────────────────────────────────────────────────────────────
  defineTool({
    name: "list_question_reports",
    title: "Question bug reports (admin)",
    description: "List bug reports that users submitted about questions.",
    write: false,
    inputSchema: {},
    run: (_a, api) => get("/api/admin/reports", true)(api),
  }),

  // ─── News & notifications ─────────────────────────────────────────────────
  defineTool({
    name: "list_news",
    title: "List news / current affairs",
    description: "List current-affairs news items (public endpoint).",
    write: false,
    inputSchema: {},
    run: (_a, api) => get("/api/news", false)(api),
  }),
  defineTool({
    name: "list_notifications",
    title: "List notifications",
    description: "List app notifications (public endpoint).",
    write: false,
    inputSchema: {},
    run: (_a, api) => get("/api/notifications", false)(api),
  }),

  // ─── Community ────────────────────────────────────────────────────────────
  defineTool({
    name: "list_community_posts",
    title: "List community posts",
    description: "List community posts, optionally filtered by user or exam.",
    write: false,
    inputSchema: {
      userId: uuid("Filter by user ID").optional(),
      examId: uuid("Filter by exam ID").optional(),
    },
    run: (a, api) => get("/api/community/posts", true, { userId: a.userId, examId: a.examId })(api),
  }),
  defineTool({
    name: "list_post_comments",
    title: "List comments of a post",
    description: "List comments on one community post.",
    write: false,
    inputSchema: { postId: uuid("Post ID") },
    run: (a, api) => get(`/api/community/posts/${encodeURIComponent(a.postId)}/comments`, true)(api),
  }),
  defineTool({
    name: "get_community_stats",
    title: "Community stats (admin)",
    description: "Community totals (posts, comments, etc.).",
    write: false,
    inputSchema: {},
    run: (_a, api) => get("/api/admin/community/stats", true)(api),
  }),
  defineTool({
    name: "list_blocked_users",
    title: "Blocked users (admin)",
    description: "List users blocked from the community.",
    write: false,
    inputSchema: {},
    run: (_a, api) => get("/api/admin/community/users/blocked", true)(api),
  }),
  defineTool({
    name: "get_user_rate_limit",
    title: "User rate-limit status (admin)",
    description: "Show a user's community rate-limit counters (posts/comments per minute and per day).",
    write: false,
    inputSchema: { userId: z.string().min(1).describe("User ID") },
    run: (a, api) => get(`/api/admin/community/rate-limit/${encodeURIComponent(a.userId)}`, true)(api),
  }),

  // ─── Settings ─────────────────────────────────────────────────────────────
  defineTool({
    name: "get_admin_app_config",
    title: "App config (admin)",
    description: "Admin app settings: Play Store URL, referral coin rewards, share message template.",
    write: false,
    inputSchema: {},
    run: (_a, api) => get("/api/admin/config", true)(api),
  }),
  defineTool({
    name: "get_public_app_config",
    title: "App config (public)",
    description: "The public app config that the mobile/web app reads.",
    write: false,
    inputSchema: {},
    run: (_a, api) => get("/api/config", false)(api),
  }),
];
