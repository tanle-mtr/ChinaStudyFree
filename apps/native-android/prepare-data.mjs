/**
 * 纯原生安卓端数据预处理
 * ------------------------------------------------------------------
 * 输入：apps/web/public/data/{index.json, books/<id>/lessons/*.json}
 * 输出：app/src/main/assets/data/{index.json, books/<id>.json}
 *
 * 设计目标（与 WebView 版本彻底切割）：
 *  1. 每本教材合并成一个 JSON，运行时一次 open 就能拿到整本书，
 *     避免 3000+ 小文件在 assets 里被 aapt 打包/枚举拖慢启动。
 *  2. LaTeX 在构建期转成可读纯文本，运行时零解析、零数学渲染库。
 *  3. 字段精简，答案与选项走同一套转换，保证判分字符串一致。
 *  4. 只用 Node 内置模块，不引入任何依赖。
 */
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const HERE = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.resolve(HERE, "..", "..");
const SRC = path.join(ROOT, "apps", "web", "public", "data");
const OUT = path.join(HERE, "app", "src", "main", "assets", "data");

const STAGE_NAME = { primary: "小学", middle: "初中", high: "高中", college: "大学" };
const GRADE_NAME = {
  1: "一年级", 2: "二年级", 3: "三年级", 4: "四年级", 5: "五年级", 6: "六年级",
  7: "七年级", 8: "八年级", 9: "九年级", 10: "高一", 11: "高二", 12: "高三", 13: "大学",
};

/** LaTeX / 数学标记 → 可读纯文本（构建期一次性处理） */
function tex(input) {
  if (input === null || input === undefined) return "";
  let s = String(input);

  // LaTeX 换行符 \\（方程组 / cases / 矩阵）必须最先处理，
  // 否则 "\\x" 会被「未知命令」规则吃掉一半，留下落单反斜杠。
  s = s.replace(/\\\\/g, "；");

  // \text{...} / \mathrm{...} / \operatorname{...} → 裸文本
  for (let i = 0; i < 3; i++) {
    s = s.replace(/\\(?:text|mathrm|mathbf|mathit|operatorname|textbf)\s*\{([^{}]*)\}/g, "$1");
  }
  // \frac{a}{b} → a/b（不加括号，便于用户填 "1/4" 直接判分）
  for (let i = 0; i < 4; i++) {
    s = s.replace(/\\frac\s*\{([^{}]*)\}\s*\{([^{}]*)\}/g, "$1/$2");
  }
  // \frac12 这类无花括号写法
  s = s.replace(/\\frac\s*([0-9A-Za-z])\s*([0-9A-Za-z])/g, "$1/$2");
  // \sqrt{x} → √(x)
  for (let i = 0; i < 3; i++) {
    s = s.replace(/\\sqrt\s*\{([^{}]*)\}/g, "√($1)");
  }
  // \xrightarrow{点燃} / \xleftarrow[下]{上} → 带箭头的文字
  s = s.replace(/\\x(?:rightarrow|leftarrow|Rightarrow|Leftarrow)\s*(?:\[[^\]]*\])?\s*\{([^{}]*)\}/g, "→$1→");
  // 上下标：^ 保留成 ^，_ 直接拼接（化学式 O_2 → O2 更易读）
  s = s.replace(/\^\s*\{([^{}]*)\}/g, "^$1");
  s = s.replace(/\^\s*([A-Za-z0-9+\-])/g, "^$1");
  s = s.replace(/_\s*\{([^{}]*)\}/g, "$1");
  s = s.replace(/_\s*([A-Za-z0-9+\-])/g, "$1");
  // \left \right 与数组/矩阵环境
  s = s.replace(/\\(?:left|right)\b/g, "");
  s = s.replace(/\\(?:begin|end)\s*\{[^}]*\}/g, "");
  // 数学定界符
  s = s.replace(/\$\$/g, "").replace(/\$/g, "");
  // 常用符号表
  const MAP = {
    "\\times": "×", "\\cdot": "·", "\\div": "÷", "\\pm": "±", "\\mp": "∓",
    "\\leq": "≤", "\\le": "≤", "\\geq": "≥", "\\ge": "≥", "\\neq": "≠", "\\ne": "≠",
    "\\approx": "≈", "\\equiv": "≡", "\\propto": "∝", "\\infty": "∞",
    "\\alpha": "α", "\\beta": "β", "\\gamma": "γ", "\\delta": "δ", "\\Delta": "Δ",
    "\\theta": "θ", "\\pi": "π", "\\sigma": "σ", "\\Sigma": "Σ", "\\omega": "ω",
    "\\circ": "°", "\\angle": "∠", "\\triangle": "△", "\\perp": "⊥", "\\parallel": "∥",
    "\\rightarrow": "→", "\\leftarrow": "←", "\\Rightarrow": "⇒", "\\Leftrightarrow": "⇔", "\\to": "→",
    "\\sum": "∑", "\\int": "∫", "\\partial": "∂", "\\forall": "∀", "\\exists": "∃",
    "\\in": "∈", "\\notin": "∉", "\\subset": "⊂", "\\cup": "∪", "\\cap": "∩",
    "\\square": "□", "\\%": "%", "\\&": "&", "\\{": "{", "\\}": "}",
    "\\_": "_", "\\!": "", "\\,": " ", "\\;": " ", "\\ ": " ", "\\quad": "  ",
  };
  for (const k of Object.keys(MAP)) s = s.split(k).join(MAP[k]);
  // 仍未识别的命令：去掉反斜杠只留命令名，避免满屏反斜杠
  s = s.replace(/\\([A-Za-z]+)/g, "$1");
  // 花括号、& 对齐符
  s = s.replace(/[{}]/g, "").replace(/&/g, " ");
  // 兜底：清掉任何残留的落单反斜杠，保证运行时零 LaTeX 痕迹
  s = s.replace(/\\/g, "");
  // 空白收敛（保留中文之间的单空格，去掉多余）
  s = s.replace(/[ \t]+/g, " ").replace(/\s*\n\s*/g, " ").trim();
  return s;
}

function readJson(file) {
  return JSON.parse(fs.readFileSync(file, "utf8"));
}

/** 课程排序：单元升序 → 知识点升序 → 单元挑战放最后 */
function lessonKey(L, filename) {
  const unit = Number(L.unitNumber) || 0;
  const isExam = /-exam\.json$/i.test(filename) || /挑战|测试|单元测/i.test(L.title || "");
  const kp = Number(L.kpIndex) || 0;
  return unit * 10000 + (isExam ? 9000 : 0) + kp;
}

function main() {
  if (!fs.existsSync(path.join(SRC, "index.json"))) {
    throw new Error("找不到源数据：" + SRC);
  }
  fs.rmSync(OUT, { recursive: true, force: true });
  fs.mkdirSync(path.join(OUT, "books"), { recursive: true });

  const srcIndex = readJson(path.join(SRC, "index.json"));
  const books = [];
  let totalLessons = 0;
  let totalQuestions = 0;
  let skipped = 0;

  for (const b of srcIndex.books) {
    const lessonDir = path.join(SRC, "books", b.id, "lessons");
    if (!fs.existsSync(lessonDir)) { skipped++; continue; }

    const raw = [];
    for (const f of fs.readdirSync(lessonDir)) {
      if (!f.endsWith(".json")) continue;
      let L;
      try { L = readJson(path.join(lessonDir, f)); } catch (e) { continue; }
      const questions = (L.questions || []).map(q => ({
        id: q.id,
        type: q.type || "choice",
        score: q.score || 0,
        diff: q.difficulty || 1,
        kp: tex(q.knowledge_point || ""),
        q: tex(q.question || ""),
        opts: Array.isArray(q.options) ? q.options.map(tex) : [],
        ans: tex(q.answer),
        exp: tex(q.explanation || ""),
      }));
      raw.push({
        _key: lessonKey(L, f),
        id: L.id || f.replace(/\.json$/, ""),
        title: tex(L.title || "小课"),
        unit: Number(L.unitNumber) || 1,
        unitTitle: tex(L.unitTitle || ""),
        kpIndex: Number(L.kpIndex) || 0,
        kpTotal: Number(L.kpTotal) || 0,
        knowledge: tex(L.knowledge || ""),
        questions,
      });
      totalLessons++;
      totalQuestions += questions.length;
    }
    raw.sort((a, c) => a._key - c._key);

    // 按单元分组
    const unitMap = new Map();
    for (const L of raw) {
      const k = L.unit;
      if (!unitMap.has(k)) {
        unitMap.set(k, { number: k, title: L.unitTitle || ("第 " + k + " 单元"), lessons: [] });
      }
      unitMap.get(k).lessons.push({
        id: L.id, title: L.title, kpIndex: L.kpIndex, kpTotal: L.kpTotal,
        knowledge: L.knowledge, questions: L.questions,
      });
    }
    const units = [...unitMap.values()].sort((a, c) => a.number - c.number);
    fs.writeFileSync(
      path.join(OUT, "books", b.id + ".json"),
      JSON.stringify({ id: b.id, units })
    );

    books.push({
      id: b.id,
      grade: b.grade,
      stage: b.stage,
      subject: b.subject,
      subjectName: b.subjectName,
      fullName: b.fullName,
      publisherName: b.publisherName,
      gradeName: b.gradeName,
      semesterName: b.semesterName,
      textbookName: b.textbookName,
      unitsCount: units.length,
      lessonsCount: raw.length,
      questionsCount: raw.reduce((n, L) => n + L.questions.length, 0),
    });
  }

  // 年级列表按实际有教材的年级生成（不存在空年级页）
  const gradeMap = new Map();
  for (const b of books) {
    if (!gradeMap.has(b.grade)) {
      gradeMap.set(b.grade, { grade: b.grade, name: GRADE_NAME[b.grade] || ("年级 " + b.grade), stage: b.stage, books: 0 });
    }
    gradeMap.get(b.grade).books++;
  }
  const grades = [...gradeMap.values()].sort((a, c) => a.grade - c.grade);

  const index = {
    generatedAt: new Date().toISOString(),
    totalBooks: books.length,
    totalLessons,
    totalQuestions,
    stageNames: STAGE_NAME,
    grades,
    books,
  };
  fs.writeFileSync(path.join(OUT, "index.json"), JSON.stringify(index));

  const size = fs.readdirSync(path.join(OUT, "books")).reduce((n, f) =>
    n + fs.statSync(path.join(OUT, "books", f)).size, 0);
  console.log("原生数据生成完成");
  console.log("  教材 " + books.length + " 本 / 年级 " + grades.length + " 个 / 课程 " + totalLessons + " 节 / 题目 " + totalQuestions + " 道");
  console.log("  跳过无 lessons 目录的教材: " + skipped);
  console.log("  assets/data/books 合计 " + (size / 1024 / 1024).toFixed(1) + " MB / " + fs.readdirSync(path.join(OUT, "books")).length + " 个文件");
  console.log("  输出: " + OUT);
}

main();
