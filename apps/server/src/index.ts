/**
 * index.ts — ChinaStudyFree 在线 AI 后端 + 静态资源服务。
 *
 * 职责：
 *   1. 托管 Next.js 静态导出产物（STATIC_DIR，默认 ../web/out）。
 *   2. 提供 /api/ai/* 在线生成接口：出题 / 讲解 / 故事 / TTS。
 *
 * 部署形态：
 *   - Web / Docker：同源，`NEXT_PUBLIC_AI_BASE` 留空（前端走 /api）。
 *   - 桌面(Tauri)：本进程被 Rust 拉起，STATIC_DIR 由启动脚本指向打包资源，
 *     前端以 http://localhost:8787 作为 AI_BASE。
 *   - 移动端(Capacitor)：前端构建时写入自托管地址，跨域已放开（CORS_ORIGIN）。
 */

import "dotenv/config";
import path from "node:path";
import fs from "node:fs";
import { fileURLToPath } from "node:url";
import express, { type Request, type Response, type NextFunction } from "express";
import { chatLLM, parseJSON, LLMNotConfigured, type ChatMessage } from "./llm.js";
import { dashscopeTTS, TTSNotConfigured, synthesize } from "./tts.js";
import { geminiImage, ImageNotConfigured } from "./image.js";

const HOST = process.env.HOST || "0.0.0.0";
const PORT = Number(process.env.PORT || 8787);
const CORS_ORIGIN = process.env.CORS_ORIGIN || "*";

/**
 * 解析静态站点目录（Next.js `output:"export"` 产物 web/out）。
 *
 * 候选优先级：
 *   1. 环境变量 STATIC_DIR —— 桌面版由 Rust 侧显式注入，最高优先级
 *   2. 「本文件所在目录」的 ../../web/out —— 唯一在全部布局下都正确的基准：
 *        开发   apps/server/dist/index.js      → apps/web/out
 *        便携   <root>/server/dist/index.js    → <root>/web/out
 *        安装   C:\...\聪聪学堂\server\dist\... → C:\...\聪聪学堂\web\out
 *        Docker /app/server/dist/index.js      → /app/web/out
 *   3. 相对 process.cwd() 的若干常见候选，作为兜底
 *
 * 注意：不能只用 cwd 相对路径（旧实现为 `../web/out`）。cwd 由启动方式决定
 * （双击 bat / 托盘 / systemd / docker -w），曾导致便携包与 Docker 下
 * 静态目录解析到父级、首页 404。此处以文件位置为主锚点，cwd 仅兜底。
 */
function resolveStaticDir(): string {
  const fromFile = path.resolve(
    path.dirname(fileURLToPath(import.meta.url)),
    "..",
    "..",
    "web",
    "out",
  );

  const candidates = [
    process.env.STATIC_DIR,
    fromFile,
    path.resolve(process.cwd(), "web", "out"),
    path.resolve(process.cwd(), "..", "web", "out"),
    path.resolve(process.cwd(), "apps", "web", "out"),
  ].filter((c): c is string => Boolean(c));

  for (const c of candidates) {
    try {
      if (fs.existsSync(c)) return c;
    } catch {
      /* 忽略不可访问的候选 */
    }
  }
  return fromFile;
}

const STATIC_DIR = resolveStaticDir();

const app = express();
// API 请求只包含短文本与数字。限制原始请求体并关闭自动解压，避免把
// 大请求或压缩炸弹转发给付费 AI / TTS 上游。
app.use(express.json({ limit: "16kb", inflate: false }));

// ---- CORS（移动端跨域调用）----
app.use((req, res, next) => {
  res.header("Access-Control-Allow-Origin", CORS_ORIGIN);
  res.header("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
  res.header("Access-Control-Allow-Headers", "Content-Type,Authorization");
  if (req.method === "OPTIONS") return res.sendStatus(204);
  next();
});

// ---- 安全响应头（纵深防御；作用于本服务托管的静态站 / API）----
// 说明：CSP 对静态 SPA 刻意保持宽松（依赖内联 boot 脚本与兜底 eval）；
// 如需更严格，应改 nonce 方案，见 SECURITY.md。
app.use((_req, res, next) => {
  res.setHeader("X-Content-Type-Options", "nosniff");
  res.setHeader("X-Frame-Options", "SAMEORIGIN");
  res.setHeader("Referrer-Policy", "no-referrer-when-downgrade");
  res.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=(), payment=(), usb=(), interest-cohort=()");
  res.setHeader("Cross-Origin-Opener-Policy", "same-origin");
  res.setHeader(
    "Content-Security-Policy",
    "default-src 'self'; " +
      "script-src 'self' 'unsafe-inline' 'unsafe-eval' https:; " +
      "style-src 'self' 'unsafe-inline' https:; " +
      "img-src 'self' data: blob: https:; " +
      "media-src 'self' data: blob: https:; " +
      "font-src 'self' data: https:; " +
      "connect-src 'self' https: http://localhost:* http://127.0.0.1:*; " +
      "frame-ancestors 'self'; base-uri 'self'; form-action 'self'",
  );
  next();
});

// ---- 结构化日志（时间戳 + 分级 + 消息 + 可选元数据，JSON 单行便于收集）----
// 启动横幅保持人类可读；运行时 / 错误用结构化输出，便于线上与桌面排障。
function log(level: "info" | "warn" | "error", msg: string, meta?: Record<string, unknown>) {
  const line = JSON.stringify({ t: new Date().toISOString(), level, msg, ...(meta ? { meta } : {}) });
  if (level === "error") console.error(line);
  else if (level === "warn") console.warn(line);
  else console.log(line);
}

// ---- 统一错误响应 ----
function fail(res: Response, status: number, message: string, code?: string) {
  res.status(status).json({ ok: false, error: message, code });
}

app.use((err: unknown, _req: Request, res: Response, _next: NextFunction) => {
  const status = Number((err as { status?: unknown })?.status);
  if (status === 400 || status === 413 || status === 415) {
    const message =
      status === 413 ? "请求体过大" : status === 415 ? "不支持的内容编码" : "请求格式无效";
    return fail(res, status, message);
  }
  log("error", "未捕获的中间件错误", { err: String((err as Error)?.stack || err) });
  fail(res, 500, "服务器内部错误");
});

function readBoundedText(value: unknown, fallback: string, maxLength: number): string | null {
  if (value === undefined || value === null || value === "") return fallback;
  if (typeof value !== "string") return null;
  const text = value.trim();
  if (!text) return fallback;
  return text.length <= maxLength ? text : null;
}

function readGrade(value: unknown, fallback: string): string | null {
  if (typeof value === "number" && Number.isFinite(value)) return String(value);
  return readBoundedText(value, fallback, 20);
}

function readInteger(
  value: unknown,
  fallback: number,
  min: number,
  max: number,
): number | null {
  if (value === undefined || value === null || value === "") return fallback;
  const parsed =
    typeof value === "number"
      ? value
      : typeof value === "string" && value.trim()
        ? Number(value)
        : Number.NaN;
  if (!Number.isFinite(parsed)) return null;
  return Math.min(Math.max(Math.trunc(parsed), min), max);
}

function readAnswer(value: unknown): string | null {
  if (value === undefined || value === null) return "";
  if (typeof value !== "string" && typeof value !== "number") return null;
  const text = String(value).trim();
  return text.length <= 500 ? text : null;
}

function readOptions(value: unknown): string[] | null {
  if (value === undefined) return [];
  if (!Array.isArray(value) || value.length > 8) return null;
  const options: string[] = [];
  for (const item of value) {
    if (typeof item !== "string") return null;
    const text = item.trim();
    if (!text || text.length > 500) return null;
    options.push(text);
  }
  return options;
}

// ---- 健康检查 ----
app.get("/api/health", (_req, res) => {
  const edgeEnabled = process.env.EDGE_TTS_ENABLED !== "false";
  res.json({
    ok: true,
    version: process.env.CSF_VERSION || "0.1.0",
    llm: !!process.env.LLM_API_KEY,
    tts: !!process.env.DASHSCOPE_API_KEY || edgeEnabled,
    ttsBackend: process.env.DASHSCOPE_API_KEY ? "dashscope" : edgeEnabled ? "edge" : "none",
    image: !!process.env.GEMINI_API_KEY,
  });
});

// ===== 1. 单元小测出题 =====
app.post("/api/ai/quiz", async (req: Request, res: Response) => {
  try {
    const b = req.body as Record<string, unknown>;
    const subject = readBoundedText(b.subject, "数学", 40);
    const grade = readGrade(b.grade, "三年级");
    const topic = readBoundedText(b.topic, "本单元知识点", 200);
    const count = readInteger(b.count, 5, 1, 15);
    const difficulty = readBoundedText(b.difficulty, "medium", 10);
    if (
      subject === null ||
      grade === null ||
      topic === null ||
      count === null ||
      difficulty === null ||
      !["easy", "medium", "hard"].includes(difficulty)
    ) {
      return fail(res, 400, "出题参数无效");
    }

    const system: ChatMessage = {
      role: "system",
      content:
        "你是小学" + subject + "老师，擅长依据课标知识点原创适龄题目。" +
        "只输出 JSON，不要解释。字段：questions 数组，每项含 " +
        "question(题干)、options(4个选项字符串数组)、answer(正确选项下标0-3)、" +
        "explanation(简明讲解)、knowledge_point(对应知识点名称)。",
    };
    const user: ChatMessage = {
      role: "user",
      content:
        `请为小学${grade}年级${subject}生成 ${count} 道关于「${topic}」的单选题，` +
        `难度${difficulty}。题干与讲解用中文，符合小学生认知，不超纲。`,
    };
    const raw = await chatLLM([system, user], { json: true, temperature: 0.9 });
    const parsed = parseJSON<{ questions?: unknown[] }>(raw);
    if (!Array.isArray(parsed.questions)) throw new Error("模型返回结构异常");
    res.json({ ok: true, questions: parsed.questions });
  } catch (e) {
    if (e instanceof LLMNotConfigured) return fail(res, 501, "AI 未配置（LLM_API_KEY 缺失）", "LLM_NOT_CONFIGURED");
    console.error("[quiz]", e);
    fail(res, 500, "服务暂时不可用，请稍后重试");
  }
});

// ===== 2. 知识点讲解 =====
app.post("/api/ai/explain", async (req: Request, res: Response) => {
  try {
    const b = req.body as Record<string, unknown>;
    const question = readBoundedText(b.question, "", 2000);
    const answer = readAnswer(b.answer);
    const options = readOptions(b.options);
    if (!question || answer === null || options === null) {
      return fail(res, 400, "讲解参数无效");
    }
    const sys: ChatMessage = {
      role: "system",
      content: "你是耐心的小学老师，用孩子能懂的话讲解知识点，不超过 120 字，中文。只输出讲解文本。",
    };
    const optText = options.length ? `选项：${options.join(" / ")}。` : "";
    const ansText = answer ? `正确答案：${answer}。` : "";
    const user: ChatMessage = {
      role: "user",
      content: `题目：${question}。${optText}${ansText}请用通俗语言讲解为什么。`,
    };
    const text = await chatLLM([sys, user], { temperature: 0.6, maxTokens: 400 });
    res.json({ ok: true, explanation: text.trim() });
  } catch (e) {
    if (e instanceof LLMNotConfigured) return fail(res, 501, "AI 未配置（LLM_API_KEY 缺失）", "LLM_NOT_CONFIGURED");
    console.error("[explain]", e);
    fail(res, 500, "服务暂时不可用，请稍后重试");
  }
});

// ===== 3. 课外故事 + 配图 =====
app.post("/api/ai/story", async (req: Request, res: Response) => {
  try {
    const b = req.body as Record<string, unknown>;
    const subject = readBoundedText(b.subject, "语文", 40);
    const grade = readGrade(b.grade, "二年级");
    const theme = readBoundedText(b.theme, "友谊与勇气", 200);
    const n = readInteger(b.sentences, 8, 4, 20);
    if (subject === null || grade === null || theme === null || n === null) {
      return fail(res, 400, "故事参数无效");
    }

    const sys: ChatMessage = {
      role: "system",
      content:
        "你是儿童文学作家。只输出 JSON：title(标题)、sentences(字符串数组，每句简短、" +
        "适合朗读，共 " + n + " 句)、questions(2-3 道阅读理解题，每项含 question、options(3项)、" +
        "answer(正确下标)、explanation)。语言中文、积极阳光、生词适中。",
    };
    const user: ChatMessage = {
      role: "user",
      content: `为小学${grade}年级${subject}写一篇关于「${theme}」的短篇分级故事。`,
    };
    const raw = await chatLLM([sys, user], { json: true, temperature: 0.9 });
    const story = parseJSON<{
      title?: string;
      sentences?: string[];
      questions?: unknown[];
    }>(raw);

    let image: { dataUrl: string; mimeType: string } | undefined;
    try {
      const img = await geminiImage(
        `儿童插画，扁平卡通风格，明亮色彩，主题：${story.title || theme}，无文字。`,
      );
      image = { dataUrl: `data:${img.mimeType};base64,${img.base64}`, mimeType: img.mimeType };
    } catch (ie) {
      if (!(ie instanceof ImageNotConfigured)) console.warn("[story] 配图失败:", (ie as Error).message);
    }

    res.json({ ok: true, story, image });
  } catch (e) {
    if (e instanceof LLMNotConfigured) return fail(res, 501, "AI 未配置（LLM_API_KEY 缺失）", "LLM_NOT_CONFIGURED");
    console.error("[story]", e);
    fail(res, 500, "服务暂时不可用，请稍后重试");
  }
});

// ===== 4. TTS 语音合成 =====
const MAX_ACTIVE_TTS_REQUESTS = 2;
let activeTTSRequests = 0;
app.post("/api/ai/tts", async (req: Request, res: Response) => {
  try {
    const b = req.body as Record<string, unknown>;
    if (typeof b.text !== "string" || !b.text.trim() || b.text.length > 1500) {
      return fail(res, 400, "朗读文本须为 1 至 1500 字符");
    }
    if (
      b.voice !== undefined &&
      (typeof b.voice !== "string" ||
        !/^[a-z]{2}-[A-Z]{2}-[A-Za-z]+Neural$/.test(b.voice) ||
        b.voice.length > 80)
    ) {
      return fail(res, 400, "语音名称无效");
    }
    if (activeTTSRequests >= MAX_ACTIVE_TTS_REQUESTS) {
      res.setHeader("Retry-After", "5");
      return fail(res, 429, "朗读服务繁忙，请稍后重试");
    }

    activeTTSRequests++;
    let result;
    try {
      result = await synthesize(b.text, b.voice);
    } finally {
      activeTTSRequests--;
    }
    const { buffer, contentType } = result;
    res.setHeader("Content-Type", contentType);
    res.setHeader("Cache-Control", "public, max-age=31536000, immutable");
    res.send(buffer);
  } catch (e) {
    if (e instanceof TTSNotConfigured) return fail(res, 501, "TTS 未配置（DASHSCOPE_API_KEY 缺失）", "TTS_NOT_CONFIGURED");
    console.error("[tts]", e);
    fail(res, 500, "服务暂时不可用，请稍后重试");
  }
});

// ===== 静态资源托管（Next.js 导出产物）=====
if (fs.existsSync(STATIC_DIR)) {
  app.use(express.static(STATIC_DIR, { extensions: ["html"] }));
  // SPA / 深链回退
  app.get(/^\/(?!api\/).*/, (_req, res) => {
    const idx = path.join(STATIC_DIR, "index.html");
    if (fs.existsSync(idx)) res.sendFile(idx);
    else res.status(404).send("静态资源未构建，请先运行 npm run build:web");
  });
} else {
  console.warn(`[ai-server] 静态目录不存在: ${STATIC_DIR}（仅 AI 接口可用）`);
}

// ---- 进程级兜底：未处理的 Promise 拒绝 / 未捕获异常记结构化日志 ----
// 这是「本地常驻 AI 服务」，刻意不主动退出进程（避免误杀桌面托盘内嵌的服务），
// 只落日志便于排障。
process.on("unhandledRejection", (reason) => {
  log("error", "unhandledRejection", { reason: String(reason) });
});
process.on("uncaughtException", (err) => {
  log("error", "uncaughtException", { error: String((err as Error)?.stack || err) });
});

app.listen(PORT, HOST, () => {
  console.log(`✅ ChinaStudyFree AI 服务已启动: http://${HOST}:${PORT}`);
  console.log(`   静态目录: ${STATIC_DIR} (${fs.existsSync(STATIC_DIR) ? "存在" : "缺失"})`);
  console.log(
    `   LLM=${process.env.LLM_API_KEY ? "已配置" : "未配置"}  ` +
      `TTS=${process.env.DASHSCOPE_API_KEY ? "DashScope" : process.env.EDGE_TTS_ENABLED !== "false" ? "Edge(免费)" : "未配置"}  ` +
      `Image=${process.env.GEMINI_API_KEY ? "已配置" : "未配置"}`,
  );
});
