#!/usr/bin/env node
/**
 * 校验安卓 assets/public 下所有 HTML 引用的本地资源是否存在。
 * 用法: node verify-android-assets.cjs <assets/public 目录>
 */
const fs = require("fs");
const path = require("path");

const ROOT = path.resolve(process.argv[2] || ".");
let checked = 0;
const missing = [];

function walk(dir) {
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, e.name);
    if (e.isDirectory()) walk(full);
    else if (e.name.endsWith(".html")) check(full);
  }
}

function check(file) {
  const html = fs.readFileSync(file, "utf8");
  const refs = [...html.matchAll(/(?:src|href)="(\/[^"#?]*)/g)].map((m) => m[1]);
  const uniq = [...new Set(refs)];
  for (const ref of uniq) {
    if (ref.startsWith("//")) continue; // 协议相对（外链）
    let p = ref;
    try { p = decodeURIComponent(ref); } catch {}
    let target = path.join(ROOT, p.replace(/^\//, ""));
    if (p.endsWith("/")) target = path.join(target, "index.html");
    checked++;
    if (!fs.existsSync(target)) {
      missing.push(path.relative(ROOT, file) + "  ->  " + ref);
    }
  }
}

walk(ROOT);
console.log(`检查 HTML 内本地引用 ${checked} 条`);
if (missing.length === 0) {
  console.log("== 全部引用均可解析 ==");
} else {
  console.log(`== 缺失 ${missing.length} 条 ==`);
  for (const m of missing.slice(0, 40)) console.log("  MISS " + m);
  process.exitCode = 1;
}

// 额外：全文搜 %5B（安卓不解码的雷）
const { execSync } = require("child_process");
try {
  const out = execSync(
    `grep -rl "%5B" "${ROOT}" --include=*.html --include=*.js --include=*.json 2>/dev/null | head -5`,
    { encoding: "utf8" },
  ).trim();
  if (out) {
    console.log("!! 仍存在 %5B 引用文件:");
    console.log(out);
    process.exitCode = 1;
  } else {
    console.log("== 无 %5B 残留 ==");
  }
} catch {}
