"use client";

import React from "react";

interface SafeIslandProps {
  children: React.ReactNode;
  /** 岛屿崩溃时渲染的兜底（通常是服务端渲染的静态内容），默认 null */
  fallback?: React.ReactNode;
  /** 崩溃详情是否写入 localStorage（默认 true，便于排障） */
  log?: boolean;
}

/**
 * SafeIsland —— 交互岛屿的局部错误边界（前端架构重写的核心）。
 *
 * 老版安卓 WebView 上，某个客户端组件水合崩溃绝不该连累整页：
 * 本边界只罩住自己的 children，崩溃时仅渲染 fallback（一般是一份
 * 服务端预渲染的静态兜底），与它平级的页面正文完全不受影响。
 *
 * 与 layout 里顶层 ErrorBoundary 的区别：顶层边界一触发就会替换整页
 * 内容（小屏上约等于白屏）；SafeIsland 把爆炸半径限制在单个岛屿内。
 */
export default class SafeIsland extends React.Component<
  SafeIslandProps,
  { error: Error | null }
> {
  constructor(props: SafeIslandProps) {
    super(props);
    this.state = { error: null };
  }

  static getDerivedStateFromError(error: Error) {
    return { error };
  }

  componentDidCatch(error: Error, info: React.ErrorInfo) {
    if (this.props.log !== false) {
      try {
        const key = "csf-island-log";
        const prev = JSON.parse(localStorage.getItem(key) || "[]");
        const list = Array.isArray(prev) ? prev : [];
        list.unshift({
          t: new Date().toISOString(),
          message: String(error?.message || error),
          stack: String(error?.stack || "").slice(0, 1500),
          component: String(info?.componentStack || "").slice(0, 800),
        });
        localStorage.setItem(key, JSON.stringify(list.slice(0, 10)));
      } catch {
        /* localStorage 不可用时忽略 */
      }
    }
    try {
      // eslint-disable-next-line no-console
      console.error("[SafeIsland] 岛屿崩溃(已隔离，不影响正文):", error, info?.componentStack);
    } catch {
      /* 忽略 */
    }
  }

  render() {
    if (!this.state.error) return this.props.children;
    return <>{this.props.fallback ?? null}</>;
  }
}
