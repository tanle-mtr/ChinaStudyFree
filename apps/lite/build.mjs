/**
 * 聪聪学堂 · 全新静态前端构建器（v2）
 *
 * 设计目标：彻底抛弃 Next.js / React 水合 / 动态路由 / chunk 分包。
 * 每个页面都是「完整、独立、可见」的静态 HTML —— 题干、选项、导航全部
 * 预渲染写死在页面里，即使 JavaScript 完全不执行（老安卓 WebView 白屏的
 * 典型场景），用户依然能看到内容并层层点击进入学习。
 *
 * JS 只做渐进增强：答题判分 / 进度记录 / 主题，不参与内容呈现。
 *
 * 输入：apps/web/public/data/{index.json, books/**}
 * 输出：apps/lite/dist/  （Capacitor webDir 指向这里）
 */
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const HERE = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.resolve(HERE, "..", "..");
const DATA = path.join(ROOT, "apps", "web", "public", "data");
const SRC = path.join(HERE, "src");
const DIST = path.join(HERE, "dist");
const VERSION = "2.0.0";

// ── 工具 ────────────────────────────────────────────────────────────────
function esc(s) {
  return String(s == null ? "" : s)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

/** 极简 LaTeX → 可读纯文本（不依赖 KaTeX/MathJax，老 WebView 也能显示）。 */
function tex(input) {
  let s = String(input == null ? "" : input);
  const greek = {
    alpha: "α", beta: "β", gamma: "γ", delta: "δ", epsilon: "ε",
    theta: "θ", lambda: "λ", mu: "μ", pi: "π", sigma: "σ",
    phi: "φ", omega: "ω", Delta: "Δ", Sigma: "Σ", Omega: "Ω",
  };
  const sym = {
    times: "×", div: "÷", pm: "±", mp: "∓", cdot: "·", ast: "∗",
    le: "≤", leq: "≤", ge: "≥", geq: "≥", ne: "≠", neq: "≠",
    approx: "≈", equiv: "≡", infty: "∞", sqrt: "√", sum: "∑",
    int: "∫", to: "→", rightarrow: "→", leftarrow: "←",
    Rightarrow: "⇒", Leftrightarrow: "⇔", circ: "°", angle: "∠",
    triangle: "△", parallel: "∥", perp: "⊥", therefore: "∴",
    because: "∵", in: "∈", notin: "∉", subset: "⊂", cup: "∪",
    cap: "∩", emptyset: "∅", forall: "∀", exists: "∃",
    partial: "∂", nabla: "∇", degree: "°", ldots: "…", cdots: "⋯",
    quad: " ", qquad: "  ", " ": " ", ",": " ", ";": " ", "!": "",
  };
  // 反复展开 \frac{a}{b} / \dfrac / \tfrac（支持一层嵌套）
  for (let pass = 0; pass < 4; pass++) {
    const before = s;
    s = s.replace(/\\(?:d|t)?frac\s*\{([^{}]*)\}\s*\{([^{}]*)\}/g,
      (_m, a, b) => `(${a.trim()}/${b.trim()})`);
    if (s === before) break;
  }
  // \sqrt{...} → √( ... )
  s = s.replace(/\\sqrt\s*\{([^{}]*)\}/g, (_m, a) => `√(${a.trim()})`);
  // \text{...} / \mathrm{...} / \mathbf{...} → 内容
  s = s.replace(/\\(?:text|mathrm|mathbf|operatorname|textbf)\s*\{([^{}]*)\}/g, "$1");
  // \xrightarrow{...} / \overline{...} 等
  s = s.replace(/\\x(?:rightarrow|leftarrow)with\s*\{[^}]*\}/g, "→");
  s = s.replace(/\\(?:overline|underline|vec|hat|bar)\s*\{([^{}]*)\}/g, "$1");
  // ^ {..} / _ {..} 去花括号
  s = s.replace(/([_^])\s*\{([^{}]*)\}/g, "$1$2");
  // 通用命令 \name → 符号或去掉
  s = s.replace(/\\([a-zA-Z]+|.)/g, (m, name) => {
    if (Object.prototype.hasOwnProperty.call(greek, name)) return greek[name];
    if (Object.prototype.hasOwnProperty.call(sym, name)) return sym[name];
    return "";
  });
  // 环境残留 / 定界符 / 数学美元符
  s = s.replace(/\\(begin|end)\{[^}]*\}/g, "");
  s = s.replace(/[{}]/g, "");
  s = s.replace(/\$/g, "");
  s = s.replace(/\\/g, "");
  s = s.replace(/\s+/g, " ").trim();
  return s || String(input);
}

function write(rel, html) {
  const out = path.join(DIST, rel);
  fs.mkdirSync(path.dirname(out), { recursive: true });
  fs.writeFileSync(out, html, "utf8");
}

/** 每页最前面：注销旧 Service Worker + 清缓存。旧版 SW 会拦截导航导致白屏。 */
const SW_KILL = `<script>(function(){try{if(navigator.serviceWorker&&navigator.serviceWorker.getRegistrations){navigator.serviceWorker.getRegistrations().then(function(r){for(var i=0;i<r.length;i++){r[i].unregister();}});}}catch(e){}try{if(window.caches&&caches.keys){caches.keys().then(function(k){for(var j=0;j<k.length;j++){caches.delete(k[j]);}});}}catch(e){}})();</script>`;

function page(opts) {
  const {
    title, main, bodyClass = "", nav = "", desc = "全科免费，人人可学",
    crumbs = "", extraHead = "",
  } = opts;
  return `<!doctype html>
<html lang="zh-CN">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1,viewport-fit=cover">
<meta name="theme-color" content="#58A700">
<meta name="description" content="${esc(desc)}">
<title>${esc(title)} · 聪聪学堂</title>
${SW_KILL}
<link rel="stylesheet" href="/assets/style.css">
${extraHead}
</head>
<body class="${esc(bodyClass)}">
<div class="topbar">
  <a class="brand" href="/index.html"><span class="brand-mark">聪</span><span class="brand-name">聪聪学堂</span></a>
  <div class="topbar-right"><span class="ver-badge">全新版 v${VERSION}</span></div>
</div>
${crumbs}
<main class="wrap">
${main}
</main>
<footer class="footer">
  <div>聪聪学堂 · 全新版 v${VERSION}　|　纯静态渲染，零依赖</div>
  <div class="footer-links"><a href="/index.html">首页</a><a href="/textbooks/index.html">教材中心</a></div>
</footer>
${nav}
<script src="/assets/app.js" defer></script>
</body>
</html>
`;
}

function crumb(items) {
  const html = items.map((it, i) => {
    const last = i === items.length - 1;
    return last
      ? `<span class="crumb-cur">${esc(it.label)}</span>`
      : `<a href="${esc(it.href)}">${esc(it.label)}</a><span class="crumb-sep">›</span>`;
  }).join("");
  return `<nav class="crumbs"><div class="wrap">${html}</div></nav>`;
}

const STAGE_OF = g => (g <= 6 ? "小学" : g <= 9 ? "初中" : g <= 12 ? "高中" : "大学");
const GRADE_NAME = {
  1: "一年级", 2: "二年级", 3: "三年级", 4: "四年级", 5: "五年级", 6: "六年级",
  7: "七年级", 8: "八年级", 9: "九年级", 10: "高一", 11: "高二", 12: "高三", 13: "大学",
};
const SUBJECT_LABEL = {
  math: "数学", chinese: "语文", english: "英语", science: "科学", moral: "道德与法治",
  physics: "物理", chemistry: "化学", biology: "生物", history: "历史",
  geography: "地理", politics: "政治", computer: "信息技术",
};
const SUBJECT_COLOR = {
  math: "#58A700", chinese: "#FF4B4B", english: "#1CB0F6", science: "#FFC800",
  moral: "#A855F7", physics: "#1CB0F6", chemistry: "#CE82FF", biology: "#58A700",
  history: "#FF9600", geography: "#14D4F4", politics: "#FF4B4B", computer: "#235390",
};
const SUBJECT_ORDER = ["chinese", "math", "english", "science", "moral", "physics", "chemistry", "biology", "history", "geography", "politics", "computer"];
function subLabel(id) { return SUBJECT_LABEL[id] || id || "综合"; }
function subColor(id) { return SUBJECT_COLOR[id] || "#58A700"; }

// ── 读取数据 ─────────────────────────────────────────────────────────────
const index = JSON.parse(fs.readFileSync(path.join(DATA, "index.json"), "utf8"));
const books = index.books.slice();
const byGrade = new Map();
for (const b of books) {
  if (!byGrade.has(b.grade)) byGrade.set(b.grade, []);
  byGrade.get(b.grade).push(b);
}
for (const arr of byGrade.values()) {
  arr.sort((a, b) => {
    const ai = SUBJECT_ORDER.indexOf(a.subject), bi = SUBJECT_ORDER.indexOf(b.subject);
    return (ai < 0 ? 99 : ai) - (bi < 0 ? 99 : bi) || String(a.id).localeCompare(String(b.id));
  });
}

/** 读取某本书的全部课时，按单元 → kpIndex 排序（单元挑战排最后）。 */
function loadLessons(bookId) {
  const dir = path.join(DATA, "books", bookId, "lessons");
  if (!fs.existsSync(dir)) return [];
  const list = [];
  for (const f of fs.readdirSync(dir)) {
    if (!f.endsWith(".json")) continue;
    try {
      const L = JSON.parse(fs.readFileSync(path.join(dir, f), "utf8"));
      L.__file = f.replace(/\.json$/, "");
      L.questions = Array.isArray(L.questions) ? L.questions : [];
      list.push(L);
    } catch (e) { /* 跳过损坏文件 */ }
  }
  list.sort((a, b) => {
    if (a.unitNumber !== b.unitNumber) return (a.unitNumber || 0) - (b.unitNumber || 0);
    const ae = /exam/i.test(a.__file) || /挑战/.test(a.title), be = /exam/i.test(b.__file) || /挑战/.test(b.title);
    if (ae !== be) return ae ? 1 : -1;
    return (a.kpIndex || 0) - (b.kpIndex || 0);
  });
  return list;
}

function loadOutline(bookId) {
  const p = path.join(DATA, "books", bookId, "outline.json");
  if (!fs.existsSync(p)) return null;
  try { return JSON.parse(fs.readFileSync(p, "utf8")); } catch (e) { return null; }
}

// ── 题目渲染 ─────────────────────────────────────────────────────────────
const INPUT_TYPES = new Set(["calculation", "fill_blank", "fill_blank_text"]);
const SELF_TYPES = new Set(["matching", "word_order"]);

function renderQuestion(q, n) {
  const type = q.type || "choice";
  const qtext = esc(tex(q.question));
  const explain = esc(tex(q.explanation || ""));
  const kp = esc(tex(q.knowledge_point || ""));
  const ansRaw = String(q.answer == null ? "" : q.answer);
  let body = "";

  if (type === "choice" || type === "true_false") {
    const opts = type === "true_false" ? ["对", "错"] : (Array.isArray(q.options) ? q.options : []);
    body = `<div class="opts">${opts.map((o, i) =>
      `<button type="button" class="opt" data-val="${esc(o)}"><span class="opt-key">${String.fromCharCode(65 + i)}</span><span>${esc(tex(o))}</span></button>`
    ).join("")}</div>`;
  } else if (INPUT_TYPES.has(type)) {
    body = `<div class="fill-row"><input class="fill-input" type="text" inputmode="text" autocomplete="off" placeholder="在这里填写答案"><button type="button" class="btn btn-primary fill-submit">提交</button></div>`;
  } else if (SELF_TYPES.has(type)) {
    const opts = Array.isArray(q.options) ? q.options : [];
    body = `<div class="opts">${opts.map((o, i) =>
      `<button type="button" class="opt self-opt" data-val="${esc(o)}"><span class="opt-key">${String.fromCharCode(65 + i)}</span><span>${esc(tex(o))}</span></button>`
    ).join("")}</div><div class="self-answer">参考答案：<b>${esc(ansRaw)}</b></div>`;
  } else {
    // 未知题型：至少把题干和答案显示出来
    body = `<div class="self-answer">参考答案：<b>${esc(ansRaw)}</b></div>`;
  }

  const mode = SELF_TYPES.has(type) || !INPUT_TYPES.has(type) && type !== "choice" && type !== "true_false"
    ? "self" : (INPUT_TYPES.has(type) ? "input" : "click");

  return `<section class="q" data-mode="${mode}" data-type="${esc(type)}" data-answer="${esc(ansRaw)}" data-qid="${esc(q.id)}">
<div class="q-head"><span class="q-no">${n}</span>${kp ? `<span class="q-kp">${kp}</span>` : ""}<span class="q-score">${esc(q.score || 0)} 分</span></div>
<div class="q-text">${qtext}</div>
${body}
<div class="q-feedback" hidden></div>
${explain ? `<details class="q-explain"><summary>查看解析</summary><div>${explain}</div></details>` : ""}
</section>`;
}

// ── 页面：首页 ───────────────────────────────────────────────────────────
function buildHome(totalLessons, totalQuestions) {
  const stages = [
    { name: "小学", g: [1, 2, 3, 4, 5, 6], color: "#58A700", tag: "1–6 年级" },
    { name: "初中", g: [7, 8, 9], color: "#1CB0F6", tag: "7–9 年级" },
    { name: "高中", g: [10, 11, 12], color: "#CE82FF", tag: "10–12 年级" },
    { name: "大学", g: [13], color: "#FF9600", tag: "按学科 / 课程" },
  ];
  const cards = stages.map(st => `<section class="stage">
  <div class="stage-head"><span class="stage-dot" style="background:${st.color}"></span><h2>${st.name}</h2><span class="stage-tag">${st.tag}</span></div>
  <div class="grade-grid">${st.g.map(g => {
    const cnt = (byGrade.get(g) || []).length;
    return `<a class="grade-card" href="/grade/${g}/index.html"><span class="grade-name">${GRADE_NAME[g]}</span><span class="grade-meta">${cnt} 册教材</span></a>`;
  }).join("")}</div>
</section>`).join("");

  const main = `<div class="hero">
  <div class="hero-mascot">聪</div>
  <h1>聪聪学堂</h1>
  <p class="hero-sub">全科免费，人人可学。选一个年级，开始今天的学习吧。</p>
  <div class="hero-stats"><span><b>${books.length}</b> 册教材</span><span><b>${totalLessons}</b> 节小课</span><span><b>${totalQuestions}</b> 道题</span></div>
</div>
<div id="resume-slot"></div>
${cards}
<div class="hero-links"><a class="btn btn-ghost" href="/textbooks/index.html">浏览官方教材 →</a></div>
<noscript><div class="notice">当前设备未启用 JavaScript，但你依然可以浏览全部教材与题目；答题判分功能需要启用脚本。</div></noscript>`;

  write("index.html", page({ title: "首页", main, bodyClass: "page-home", desc: "聪聪学堂 · 全科免费 AI 学习平台" }));
}

// ── 页面：年级 ───────────────────────────────────────────────────────────
function buildGrade(g) {
  const list = byGrade.get(g) || [];
  const groups = new Map();
  for (const b of list) {
    const k = b.subject || "other";
    if (!groups.has(k)) groups.set(k, []);
    groups.get(k).push(b);
  }
  const sections = [...groups.entries()].map(([sid, arr]) => `<section class="subject-sec">
  <div class="subject-head"><span class="subject-bar" style="background:${subColor(sid)}"></span><h2>${esc(subLabel(sid))}</h2><span class="subject-count">${arr.length} 册</span></div>
  <div class="book-grid">${arr.map(b => `<a class="book-card" href="/book/${esc(b.id)}/index.html" style="--card:${subColor(sid)}">
    <span class="book-glyph">${esc(subLabel(sid).slice(0, 1))}</span>
    <span class="book-info"><span class="book-title">${esc(b.textbookName || b.fullName || b.id)}</span>
    <span class="book-sub">${esc(b.publisherName || "")} · ${esc(subLabel(sid))}</span>
    <span class="book-meta">${b.unitsCount || 0} 单元 · ${b.lessonsCount || 0} 节小课</span></span>
    <span class="book-arrow">›</span></a>`).join("")}</div>
</section>`).join("");

  const main = `<div class="page-head"><h1>${GRADE_NAME[g]} <span class="muted">（${STAGE_OF(g)}）</span></h1>
<p>共 ${list.length} 册教材。选择一册开始学习，内容按单元 → 小课组织。</p></div>
${list.length ? sections : `<div class="empty">该年级的互动课程还在制作中，可以先去 <a href="/textbooks/index.html">教材中心</a> 浏览官方课本。</div>`}`;

  write(`grade/${g}/index.html`, page({
    title: GRADE_NAME[g], main,
    crumbs: crumb([{ label: "首页", href: "/index.html" }, { label: GRADE_NAME[g] }]),
  }));
}

// ── 页面：教材 ───────────────────────────────────────────────────────────
function buildBook(b, lessons) {
  const outline = loadOutline(b.id);
  const unitTitle = new Map();
  if (outline && Array.isArray(outline.units)) {
    for (const u of outline.units) unitTitle.set(Number(u.unit_number), String(u.title || ""));
  }
  const byUnit = new Map();
  for (const L of lessons) {
    const u = Number(L.unitNumber || 0);
    if (!byUnit.has(u)) byUnit.set(u, []);
    byUnit.get(u).push(L);
  }
  const units = [...byUnit.keys()].sort((a, b) => a - b).map(u => {
    const arr = byUnit.get(u);
    return `<section class="unit">
  <div class="unit-head"><span class="unit-no">${u}</span><h2>${esc(unitTitle.get(u) || (arr[0] && arr[0].unitTitle) || `第 ${u} 单元`)}</h2><span class="unit-count">${arr.length} 节</span></div>
  <div class="lesson-list">${arr.map((L, i) => `<a class="lesson-row" href="/lesson/${esc(b.id)}/${esc(L.__file)}/index.html" data-lesson="${esc(L.__file)}">
    <span class="lesson-no">${i + 1}</span><span class="lesson-title">${esc(L.title || L.__file)}</span>
    <span class="lesson-meta">${L.questions.length} 题</span><span class="lesson-state"></span><span class="lesson-arrow">›</span></a>`).join("")}</div>
</section>`;
  }).join("");

  const main = `<div class="page-head book-head" style="--card:${subColor(b.subject)}">
  <span class="book-big-glyph">${esc(subLabel(b.subject).slice(0, 1))}</span>
  <div><h1>${esc(b.textbookName || b.fullName || b.id)}</h1>
  <p>${esc(b.fullName || "")}　·　${esc(b.publisherName || "")}　·　${b.unitsCount || 0} 单元 / ${lessons.length} 节小课</p></div>
</div>
${units || `<div class="empty">这本教材暂时没有互动小课。</div>`}`;

  write(`book/${b.id}/index.html`, page({
    title: b.textbookName || b.id, main,
    crumbs: crumb([
      { label: "首页", href: "/index.html" },
      { label: GRADE_NAME[b.grade] || `年级 ${b.grade}`, href: `/grade/${b.grade}/index.html` },
      { label: b.textbookName || b.id },
    ]),
  }));
}

// ── 页面：课程 ───────────────────────────────────────────────────────────
function buildLesson(b, L, allLessons) {
  const idxInBook = allLessons.findIndex(x => x.__file === L.__file);
  const prev = idxInBook > 0 ? allLessons[idxInBook - 1] : null;
  const next = idxInBook >= 0 && idxInBook < allLessons.length - 1 ? allLessons[idxInBook + 1] : null;
  const qs = L.questions.map((q, i) => renderQuestion(q, i + 1)).join("");
  const totalScore = L.questions.reduce((s, q) => s + (Number(q.score) || 0), 0);

  const main = `<div class="page-head lesson-head">
  <h1>${esc(L.title || L.__file)}</h1>
  <p>${esc(b.fullName || b.textbookName || b.id)}　·　第 ${L.unitNumber || "?"} 单元 ${esc(L.unitTitle || "")}　·　共 ${L.questions.length} 题 / ${totalScore} 分</p>
  <div class="lesson-progress"><span id="lp-label">已完成 0 / ${L.questions.length}</span><div class="lp-track"><div class="lp-fill" id="lp-fill"></div></div></div>
</div>
<div class="q-list" data-book="${esc(b.id)}" data-lesson="${esc(L.__file)}" data-total="${L.questions.length}">
${qs || `<div class="empty">这节课暂时没有题目。</div>`}
</div>
<div class="lesson-done" id="lesson-done" hidden><div class="done-icon">★</div><h2>这节课完成啦！</h2><p id="done-summary"></p><a class="btn btn-primary" href="/book/${esc(b.id)}/index.html">返回教材</a></div>
<div class="lesson-nav">
  ${prev ? `<a class="btn btn-ghost" href="/lesson/${esc(b.id)}/${esc(prev.__file)}/index.html">← 上一课</a>` : `<a class="btn btn-ghost" href="/book/${esc(b.id)}/index.html">← 返回教材</a>`}
  ${next ? `<a class="btn btn-primary" href="/lesson/${esc(b.id)}/${esc(next.__file)}/index.html">下一课 →</a>` : `<a class="btn btn-primary" href="/book/${esc(b.id)}/index.html">完成，返回教材</a>`}
</div>`;

  write(`lesson/${b.id}/${L.__file}/index.html`, page({
    title: L.title || L.__file, main,
    crumbs: crumb([
      { label: "首页", href: "/index.html" },
      { label: GRADE_NAME[b.grade] || `年级 ${b.grade}`, href: `/grade/${b.grade}/index.html` },
      { label: b.textbookName || b.id, href: `/book/${b.id}/index.html` },
      { label: L.title || L.__file },
    ]),
  }));
}

// ── 页面：教材中心（按学段列出全部教材） ─────────────────────────────────
function buildTextbooks() {
  const stages = [["小学", [1, 2, 3, 4, 5, 6]], ["初中", [7, 8, 9]], ["高中", [10, 11, 12]], ["大学", [13]]];
  const secs = stages.map(([name, gs]) => {
    let cnt = 0; for (const g of gs) cnt += (byGrade.get(g) || []).length;
    return `<section class="subject-sec"><div class="subject-head"><span class="subject-bar" style="background:#58A700"></span><h2>${name}</h2><span class="subject-count">${cnt} 册</span></div>
    <div class="grade-grid">${gs.map(g => `<a class="grade-card" href="/grade/${g}/index.html"><span class="grade-name">${GRADE_NAME[g]}</span><span class="grade-meta">${(byGrade.get(g) || []).length} 册</span></a>`).join("")}</div></section>`;
  }).join("");
  write("textbooks/index.html", page({
    title: "教材中心", main: `<div class="page-head"><h1>教材中心</h1><p>共 ${books.length} 册教材，按学段 / 年级浏览。</p></div>${secs}`,
    crumbs: crumb([{ label: "首页", href: "/index.html" }, { label: "教材中心" }]),
  }));
}

// ── 生成 ────────────────────────────────────────────────────────────────
function main() {
  fs.rmSync(DIST, { recursive: true, force: true });
  fs.mkdirSync(DIST, { recursive: true });

  // 静态资源
  fs.mkdirSync(path.join(DIST, "assets"), { recursive: true });
  fs.copyFileSync(path.join(SRC, "style.css"), path.join(DIST, "assets", "style.css"));
  fs.copyFileSync(path.join(SRC, "app.js"), path.join(DIST, "assets", "app.js"));

  // 先一次性读入全部课时：既避免重复 IO，也让首页统计与真实内容一致
  // （index.json 里的 totalLessons/totalQuestions 是旧值，已滞后于题库扩充）。
  const allBookLessons = new Map();
  let totalLessons = 0, totalQuestions = 0;
  for (const b of books) {
    const arr = loadLessons(b.id);
    allBookLessons.set(b.id, arr);
    totalLessons += arr.length;
    for (const L of arr) totalQuestions += L.questions.length;
  }

  buildHome(totalLessons, totalQuestions);
  buildTextbooks();
  for (let g = 1; g <= 13; g++) buildGrade(g);

  let lessonCount = 0, questionCount = 0, bookCount = 0;
  for (const b of books) {
    const lessons = allBookLessons.get(b.id);
    buildBook(b, lessons);
    bookCount++;
    for (const L of lessons) {
      buildLesson(b, L, lessons);
      lessonCount++;
      questionCount += L.questions.length;
    }
  }

  // 404 兜底（静态、可见）
  write("404.html", page({
    title: "页面没找到",
    main: `<div class="page-head"><h1>页面没找到</h1><p>这个地址不存在，回到首页继续学习吧。</p></div><a class="btn btn-primary" href="/index.html">返回首页</a>`,
  }));

  console.log(JSON.stringify({
    version: VERSION, books: bookCount, grades: 13, lessons: lessonCount,
    questions: questionCount, output: DIST,
  }, null, 2));
}

main();
