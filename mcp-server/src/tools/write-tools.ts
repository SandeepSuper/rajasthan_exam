import { readFile } from "node:fs/promises";
import { basename, extname } from "node:path";
import { z } from "zod";
import { defineTool, WRITE_PREFIX, type ToolDef } from "./define.js";

const uuid = (what: string) => z.string().uuid().describe(`${what} (UUID)`);
const localDateTime = z
  .string()
  .regex(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(:\d{2})?$/, "Use format YYYY-MM-DDTHH:mm[:ss] (no timezone)");
const id = (v: string) => encodeURIComponent(v);

const IMPORT_EXTENSIONS = new Set([".csv", ".xlsx", ".xls"]);

export const writeTools: ToolDef<any>[] = [
  // ─── Exams ────────────────────────────────────────────────────────────────
  defineTool({
    name: "create_exam",
    title: "Create exam",
    description: WRITE_PREFIX + "Creates a new exam category.",
    write: true,
    inputSchema: {
      title: z.string().min(1),
      category: z.string().min(1),
      iconUrl: z.string().url().optional(),
      languageSupported: z.enum(["HI", "EN", "BOTH"]).optional(),
      negativeMarks: z.number().min(0).optional(),
      marksPerQuestion: z.number().positive().optional(),
      isPremium: z.boolean().optional(),
      price: z.number().min(0).optional(),
      discountPercent: z.number().int().min(0).max(100).optional(),
    },
    run: async (a, api) => (await api.request({ method: "POST", path: "/api/admin/exams", body: a, auth: true })).data,
  }),
  defineTool({
    name: "update_exam",
    title: "Update exam",
    description: WRITE_PREFIX + "Updates fields of an existing exam. Only the fields you pass are sent.",
    write: true,
    destructive: true,
    inputSchema: {
      examId: uuid("Exam ID"),
      title: z.string().min(1).optional(),
      category: z.string().min(1).optional(),
      iconUrl: z.string().url().optional(),
      isPremium: z.boolean().optional(),
      price: z.number().min(0).optional(),
      currency: z.string().optional(),
      discountPercent: z.number().int().min(0).max(100).optional(),
    },
    run: async ({ examId, ...body }, api) =>
      (await api.request({ method: "PUT", path: `/api/admin/exams/${id(examId)}`, body, auth: true })).data,
  }),
  defineTool({
    name: "set_exam_discount",
    title: "Set exam discount",
    description: WRITE_PREFIX + "Sets the discount percent (0-100) of an exam.",
    write: true,
    destructive: true,
    inputSchema: { examId: uuid("Exam ID"), discountPercent: z.number().int().min(0).max(100) },
    run: async (a, api) =>
      (
        await api.request({
          method: "PATCH",
          path: `/api/admin/exams/${id(a.examId)}/discount`,
          body: { discountPercent: a.discountPercent },
          auth: true,
        })
      ).data,
  }),
  defineTool({
    name: "set_exam_price",
    title: "Set exam price",
    description: WRITE_PREFIX + "Sets the price (INR) of an exam.",
    write: true,
    destructive: true,
    inputSchema: { examId: uuid("Exam ID"), price: z.number().min(0) },
    run: async (a, api) =>
      (
        await api.request({
          method: "PATCH",
          path: `/api/admin/exams/${id(a.examId)}/price`,
          body: { price: a.price },
          auth: true,
        })
      ).data,
  }),

  // ─── Tests & questions ────────────────────────────────────────────────────
  defineTool({
    name: "create_test",
    title: "Create test",
    description: WRITE_PREFIX + "Creates a new test/mock under an exam.",
    write: true,
    inputSchema: {
      examId: uuid("Exam ID"),
      title: z.string().min(1),
      type: z.enum(["MOCK", "TOPIC", "PYQ", "FULL", "DAILY_QUIZ"]),
      durationMinutes: z.number().int().positive(),
      totalQuestions: z.number().int().positive(),
      isPremium: z.boolean().optional(),
      isLive: z.boolean().optional(),
      startsAt: localDateTime.optional(),
      endsAt: localDateTime.optional(),
      allowPrevious: z.boolean().optional(),
      sectionLock: z.boolean().optional(),
      marksPerQuestion: z.number().positive().optional(),
      totalMarks: z.number().positive().optional(),
    },
    run: async (a, api) => (await api.request({ method: "POST", path: "/api/admin/tests", body: a, auth: true })).data,
  }),
  defineTool({
    name: "create_question",
    title: "Create question",
    description: WRITE_PREFIX + "Adds one question to a test.",
    write: true,
    inputSchema: {
      testId: uuid("Test ID"),
      textHi: z.string().optional(),
      textEn: z.string().optional(),
      optionsHi: z.array(z.string()).optional(),
      optionsEn: z.array(z.string()).optional(),
      correctOptionIndex: z.number().int().min(0).describe("0-based index of the correct option"),
      solutionHi: z.string().optional(),
      solutionEn: z.string().optional(),
      subject: z.string().optional(),
    },
    run: async (a, api) =>
      (await api.request({ method: "POST", path: "/api/admin/questions", body: a, auth: true })).data,
  }),
  defineTool({
    name: "import_questions",
    title: "Import questions from file",
    description:
      WRITE_PREFIX + "Uploads a local CSV/Excel file and bulk-imports its questions into a test.",
    write: true,
    inputSchema: {
      testId: uuid("Test ID"),
      filePath: z.string().min(1).describe("Absolute path of a local .csv, .xlsx or .xls file"),
    },
    run: async (a, api) => {
      const ext = extname(a.filePath).toLowerCase();
      if (!IMPORT_EXTENSIONS.has(ext)) {
        throw new Error(`Only ${[...IMPORT_EXTENSIONS].join(", ")} files can be imported.`);
      }
      const bytes = await readFile(a.filePath);
      const form = new FormData();
      form.append("file", new Blob([bytes]), basename(a.filePath));
      return (await api.request({ method: "POST", path: `/api/admin/tests/${id(a.testId)}/import`, form, auth: true }))
        .data;
    },
  }),

  // ─── Settings ─────────────────────────────────────────────────────────────
  defineTool({
    name: "update_app_config",
    title: "Update app config",
    description:
      WRITE_PREFIX +
      "Changes app settings. Fields you leave out keep their current value (the tool reads the config first and merges).",
    write: true,
    destructive: true,
    inputSchema: {
      playStoreUrl: z.string().url().optional(),
      referrerCoinReward: z.number().int().min(0).optional(),
      refereeCoinReward: z.number().int().min(0).optional(),
      shareMessageTemplate: z.string().max(1000).optional(),
    },
    run: async (a, api) => {
      // The backend overwrites every field, so merge with the current config first.
      const current = (await api.request({ method: "GET", path: "/api/admin/config", auth: true })).data as Record<
        string,
        unknown
      >;
      const changes = Object.fromEntries(Object.entries(a).filter(([, v]) => v !== undefined));
      const body = { ...current, ...changes };
      return (await api.request({ method: "PUT", path: "/api/admin/config", body, auth: true })).data;
    },
  }),

  // ─── Community moderation ─────────────────────────────────────────────────
  defineTool({
    name: "delete_community_post",
    title: "Delete community post",
    description: WRITE_PREFIX + "Permanently deletes a community post.",
    write: true,
    destructive: true,
    inputSchema: { postId: uuid("Post ID") },
    run: async (a, api) => {
      await api.request({ method: "DELETE", path: `/api/admin/community/posts/${id(a.postId)}`, auth: true });
      return { deleted: true, postId: a.postId };
    },
  }),
  defineTool({
    name: "delete_community_comment",
    title: "Delete community comment",
    description: WRITE_PREFIX + "Permanently deletes a community comment.",
    write: true,
    destructive: true,
    inputSchema: { commentId: uuid("Comment ID") },
    run: async (a, api) => {
      await api.request({ method: "DELETE", path: `/api/admin/community/comments/${id(a.commentId)}`, auth: true });
      return { deleted: true, commentId: a.commentId };
    },
  }),
  defineTool({
    name: "verify_community_post",
    title: "Add verified answer to post",
    description: WRITE_PREFIX + "Sets (or replaces) the verified answer on a community post.",
    write: true,
    destructive: true,
    inputSchema: { postId: uuid("Post ID"), answer: z.string().min(1) },
    run: async (a, api) =>
      (
        await api.request({
          method: "PUT",
          path: `/api/admin/community/posts/${id(a.postId)}/verify`,
          body: { answer: a.answer },
          auth: true,
        })
      ).data,
  }),
  defineTool({
    name: "block_user",
    title: "Block user",
    description: WRITE_PREFIX + "Blocks a user from the community.",
    write: true,
    inputSchema: { userId: uuid("User ID") },
    run: async (a, api) =>
      (await api.request({ method: "PUT", path: `/api/admin/community/users/${id(a.userId)}/block`, auth: true })).data,
  }),
  defineTool({
    name: "unblock_user",
    title: "Unblock user",
    description: WRITE_PREFIX + "Unblocks a user in the community.",
    write: true,
    inputSchema: { userId: uuid("User ID") },
    run: async (a, api) =>
      (await api.request({ method: "PUT", path: `/api/admin/community/users/${id(a.userId)}/unblock`, auth: true }))
        .data,
  }),
  defineTool({
    name: "reset_user_rate_limit",
    title: "Reset user rate limit",
    description: WRITE_PREFIX + "Resets one of a user's community rate-limit counters.",
    write: true,
    inputSchema: {
      userId: z.string().min(1).describe("User ID"),
      type: z.enum(["post_daily", "post_minute", "comment_daily", "comment_minute"]),
    },
    run: async (a, api) => {
      const suffix = {
        post_daily: "daily",
        post_minute: "minute",
        comment_daily: "comment/daily",
        comment_minute: "comment/minute",
      }[a.type];
      return (
        await api.request({
          method: "DELETE",
          path: `/api/admin/community/rate-limit/${id(a.userId)}/${suffix}`,
          auth: true,
        })
      ).data;
    },
  }),

  // ─── News ─────────────────────────────────────────────────────────────────
  defineTool({
    name: "create_news",
    title: "Create news item",
    description: WRITE_PREFIX + "Publishes one current-affairs news item in the app.",
    write: true,
    inputSchema: {
      titleHi: z.string().min(1),
      titleEn: z.string().optional(),
      descHi: z.string().optional(),
      descEn: z.string().optional(),
      date: z.string().regex(/^\d{4}-\d{2}-\d{2}$/, "Use YYYY-MM-DD").optional(),
      imageUrl: z.string().url().optional(),
    },
    run: async (a, api) => (await api.request({ method: "POST", path: "/api/news", body: a, auth: true })).data,
  }),
  defineTool({
    name: "bulk_import_news",
    title: "Bulk import news",
    description: WRITE_PREFIX + "Publishes many current-affairs news items at once.",
    write: true,
    inputSchema: {
      items: z
        .array(
          z.object({
            titleHi: z.string().min(1),
            titleEn: z.string().optional(),
            descHi: z.string().optional(),
            descEn: z.string().optional(),
            date: z.string().regex(/^\d{4}-\d{2}-\d{2}$/).optional(),
            imageUrl: z.string().url().optional(),
          }),
        )
        .min(1)
        .max(200),
    },
    run: async (a, api) => (await api.request({ method: "POST", path: "/api/news/bulk", body: a.items, auth: true })).data,
  }),
  defineTool({
    name: "delete_news",
    title: "Delete news item",
    description: WRITE_PREFIX + "Permanently deletes a news item.",
    write: true,
    destructive: true,
    inputSchema: { newsId: uuid("News ID") },
    run: async (a, api) => {
      await api.request({ method: "DELETE", path: `/api/news/${id(a.newsId)}`, auth: true });
      return { deleted: true, newsId: a.newsId };
    },
  }),

  // ─── Notifications ────────────────────────────────────────────────────────
  defineTool({
    name: "send_notification",
    title: "Send notification",
    description: WRITE_PREFIX + "Creates a notification that all app users will see.",
    write: true,
    inputSchema: {
      title: z.string().min(1),
      message: z.string().min(1),
      iconType: z.enum(["info", "success", "warning", "error", "gift"]).optional(),
    },
    run: async (a, api) => (await api.request({ method: "POST", path: "/api/notifications", body: a, auth: true })).data,
  }),
  defineTool({
    name: "delete_notification",
    title: "Delete notification",
    description: WRITE_PREFIX + "Permanently deletes a notification.",
    write: true,
    destructive: true,
    inputSchema: { notificationId: uuid("Notification ID") },
    run: async (a, api) => {
      await api.request({ method: "DELETE", path: `/api/notifications/${id(a.notificationId)}`, auth: true });
      return { deleted: true, notificationId: a.notificationId };
    },
  }),
];
