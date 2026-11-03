"use client";

import { useEffect } from "react";
import { useProgressStore } from "@/store/progress";

/**
 * StoreHydrator —— 在客户端挂载后回填持久化 store。
 *
 * 配合 progress.ts 的 `skipHydration: true`：静态导出预渲染时 store 用默认空状态，
 * 客户端首帧也用默认空状态（与预渲染严格一致，不再触发 React #418 水合不匹配）。
 * 本组件挂载后用 `persist.rehydrate()` 把本机 localStorage 存档读回 store，
 * 触发一次普通客户端更新（非水合），页面再据此渲染真实进度。
 *
 * 无渲染输出，仅副作用。放在 layout 全局挂载一次即可。
 */
export function StoreHydrator() {
  useEffect(() => {
    try {
      void useProgressStore.persist.rehydrate();
    } catch {
      /* 老 WebView 上极端情况下 rehydrate 失败也不应阻塞页面 */
    }
  }, []);
  return null;
}
