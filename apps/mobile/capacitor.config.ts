import type { CapacitorConfig } from "@capacitor/core";

/**
 * Capacitor 配置：用同一套 Next.js 静态产物(out/) 封装安卓与 iOS。
 *
 * - webDir: 指向全新静态前端产物（apps/lite/dist）。
 *
 * 【v2 架构重写】安卓端已弃用 Next.js 静态导出（React 水合 + 动态路由 +
 *   chunk 分包在老 WebView 上反复白屏），改为 apps/lite 生成的纯静态多页站：
 *   每页都是完整独立 HTML，题干/选项/导航全部预渲染，零 JS 也能显示内容。
 *   详见 apps/lite/build.mjs。
 *
 * 构建流程：
 *   npm run build:lite          # 生成 apps/lite/dist
 *   npm run sync:android        # 拷贝进安卓工程
 *   cd apps/mobile/android && ./gradlew assembleRelease
 */
const config: CapacitorConfig = {
  appId: "com.chinastudyfree.app",
  appName: "聪聪学堂",
  webDir: "../lite/dist",
  server: {
    // 使用 https scheme，避免 WebView 混合内容限制
    androidScheme: "https",
    iosScheme: "https",
  },
  plugins: {},
};

export default config;
