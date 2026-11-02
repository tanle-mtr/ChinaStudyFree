import type { Metadata, Viewport } from "next";
import { Nunito } from "next/font/google";
import "./globals.css";
import { ThemeProvider } from "@/components/ThemeProvider";
import { BottomNav } from "@/components/BottomNav";
import { ToastProvider } from "@/components/Toast";
import { DailyRewardWatcher } from "@/components/DailyRewardWatcher";
import { AchievementWatcher } from "@/components/AchievementWatcher";
import { LeagueWatcher } from "@/components/LeagueWatcher";
import ErrorBoundary from "@/components/ErrorBoundary";

const nunito = Nunito({
  subsets: ["latin"],
  weight: ["600", "700", "800", "900"],
  variable: "--font-display",
  display: "swap",
});

export const metadata: Metadata = {
  title: "聪聪学堂",
  description: "全科免费，人人可学的小学AI学习平台",
  manifest: "/manifest.webmanifest",
  appleWebApp: {
    capable: true,
    title: "聪聪学堂",
    statusBarStyle: "default",
  },
  icons: {
    icon: [{ url: "/icons/icon.svg", type: "image/svg+xml" }],
    apple: [{ url: "/icons/apple-touch-icon.png", sizes: "180x180" }],
  },
};

export const viewport: Viewport = {
  themeColor: [
    { media: "(prefers-color-scheme: light)", color: "#58CC02" },
    { media: "(prefers-color-scheme: dark)", color: "#131F24" },
  ],
};

/**
 * 首屏内联脚本：水合前读 localStorage，提前把 .theme-dark 挂到 <html>，
 * 防止深色用户看到一帧白底闪屏。逻辑与 ThemeProvider 保持一致：
 *   装备暗色美妆主题 强制暗；否则看三态偏好（dark / system+系统深色）。
 * 顺带注册 Service Worker（壳层预缓存 + 课程 JSON/音频 SWR）。
 */
const bootScript = `
(function () {
  // ── 诊断浮层（临时）：捕获 JS 错误 / Promise 异常 / 资源 404 / React 内部报错，
  //    水合 6 秒未完成也弹出，直接显示在屏幕上，供真机截图定位白屏根因。
  //    纯 ES5 书写（本内联脚本不经过 SWC 转译，需兼容老 WebView）。
  var __errs = [];
  var __fails = [];
  function __diagRender() {
    if (document.getElementById("csf-diag")) return;
    var d = document.createElement("div");
    d.id = "csf-diag";
    d.style.cssText = "position:fixed;left:0;right:0;bottom:0;z-index:2147483647;" +
      "max-height:60%;overflow:auto;background:#111;color:#4f4;" +
      "font:12px/1.5 monospace;padding:10px;white-space:pre-wrap;" +
      "word-break:break-all;border-top:2px solid #4f4;";
    var info = "[CSF诊断 diag-20261103]\\nURL: " + location.href +
      "\\nUA: " + navigator.userAgent;
    try {
      var bt = document.body ? String(document.body.innerText).replace(/\\s+/g, " ").trim() : "(无body)";
      info += "\\n正文长度: " + bt.length + "  正文开头: " + bt.slice(0, 80);
    } catch (e) {}
    if (__errs.length) info += "\\n\\n-- 错误 " + __errs.length + " 条 --\\n" + __errs.slice(0, 8).join("\\n-----\\n");
    if (__fails.length) info += "\\n\\n-- 资源加载失败 " + __fails.length + " 条 --\\n" + __fails.slice(0, 8).join("\\n");
    if (!__errs.length && !__fails.length) info += "\\n\\n(未捕获到错误，但页面未完成启动)";
    d.textContent = info;
    (document.body || document.documentElement).appendChild(d);
  }
  function __rec(msg) {
    if (__errs.length > 30) return;
    __errs.push(String(msg).slice(0, 500));
    try { __diagRender(); } catch (e) {}
  }
  window.addEventListener("error", function (e) {
    var t = e && e.target;
    if (t && t !== window && (t.src || t.href)) {
      if (__fails.length < 20) __fails.push((t.tagName || "?") + " " + (t.src || t.href));
      try { __diagRender(); } catch (er) {}
      return;
    }
    __rec("JS: " + ((e && e.message) || "?") + " @ " + ((e && e.filename) || "") + ":" + ((e && e.lineno) || ""));
  }, true);
  window.addEventListener("unhandledrejection", function (e) {
    var r = e && e.reason;
    __rec("Promise: " + ((r && (r.stack || r.message)) || String(r)));
  });
  window.onerror = function (m, s, l) { __rec("onerror: " + m + " @ " + s + ":" + l); };
  // React 渲染错误走 console.error（不冒泡），必须拦
  (function () {
    var orig = console.error;
    console.error = function () {
      var parts = [];
      for (var i = 0; i < arguments.length; i++) {
        var a = arguments[i];
        parts.push(a && a.stack ? String(a.stack).slice(0, 400) : String(a));
      }
      __rec("console.error: " + parts.join(" "));
      try { orig.apply(console, arguments); } catch (e) {}
    };
  })();
  setTimeout(function () {
    if (!window.__csfHydrated) {
      try { __diagRender(); } catch (e) {}
    }
  }, 6000);

  try {
    var DARK_THEMES = { theme_midnight: "#0F1419", theme_obsidian: "#08090C" };
    var equipped = "";
    try {
      var raw = localStorage.getItem("csf-progress-v1");
      if (raw) equipped = (JSON.parse(raw).state || {}).equippedTheme || "";
    } catch (e) {}
    var mode = localStorage.getItem("csf-theme-mode");
    var sysDark = window.matchMedia && window.matchMedia("(prefers-color-scheme: dark)").matches;
    var cosmeticDark = Object.prototype.hasOwnProperty.call(DARK_THEMES, equipped);
    var dark = cosmeticDark || mode === "dark" || (mode !== "light" && sysDark);
    if (dark) {
      var root = document.documentElement;
      root.classList.add("theme-dark");
      root.style.colorScheme = "dark";
      root.style.backgroundColor = cosmeticDark ? DARK_THEMES[equipped] : "#131F24";
    }
  } catch (e) {}
  if ("serviceWorker" in navigator) {
    // 原生 App（Capacitor: https://localhost / Tauri: tauri.localhost）资源已随包本地打包，
    // Service Worker 只会带来「APK 更新后旧缓存残留 + 静态 chunk / 导航请求被 SW 拦截」
    // 导致白屏卡死（安卓真机复现：首页卡住不动）。故原生端：立即注销遗留 SW 并清空缓存，
    // 且不再注册。Web 端（真实域名 / 离线包 LAN IP）才注册，保留离线能力。
    var host = location.hostname;
    var proto = location.protocol;
    var isNativeApp =
      proto === "capacitor:" ||
      proto === "file:" ||
      (proto === "https:" && host === "localhost") ||
      host === "tauri.localhost" ||
      host === "appassets.androidplatform.net";
    if (isNativeApp) {
      navigator.serviceWorker.getRegistrations().then(function (regs) {
        regs.forEach(function (r) { r.unregister(); });
      }).catch(function () {});
      if (window.caches && caches.keys) {
        caches.keys().then(function (keys) {
          keys.forEach(function (k) { caches.delete(k); });
        }).catch(function () {});
      }
    } else {
      window.addEventListener("load", function () {
        navigator.serviceWorker.register("/sw.js").catch(function () {});
      });
    }
  }

})();
`;

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="zh-CN" className={nunito.variable} suppressHydrationWarning>
      <head>
        <script dangerouslySetInnerHTML={{ __html: bootScript }} />
      </head>
      {/* 底部导航的高度补偿由 BottomNav 自带的占位条负责（web-lesson-15）：
          课程 / 阅读器等沉浸页导航隐藏时不再留 64px 死空隙 */}
      <body className="min-h-screen bg-bg-soft">
        <ErrorBoundary>
          <ThemeProvider>
            <ToastProvider>
              <DailyRewardWatcher />
              <AchievementWatcher />
              <LeagueWatcher />
              {children}
              <BottomNav />
            </ToastProvider>
          </ThemeProvider>
        </ErrorBoundary>
      </body>
    </html>
  );
}
