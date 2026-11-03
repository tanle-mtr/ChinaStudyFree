"use client";

import React from "react";

interface State {
  error: Error | null;
}

/**
 * 全局 React 错误边界。
 *
 * 作用：捕获渲染期 / 生命周期内的未捕获异常，避免整棵树崩溃成「白屏死壳」，
 * 把小朋友困在无法操作的界面里。这是结构 / 渲染崩溃的兜底，不承载产品逻辑。
 *
 * 抓不到：事件回调、setTimeout、fetch 等异步里的异常——那些各自 try/catch。
 *
 * 排障：崩溃详情写入 localStorage（`csf-crash-log`，最多 5 条），
 * 桌面版另有 `ai-server.log`；无任何后端上报，纯本地。
 */
export default class ErrorBoundary extends React.Component<
  { children: React.ReactNode },
  State
> {
  constructor(props: { children: React.ReactNode }) {
    super(props);
    this.state = { error: null };
  }

  static getDerivedStateFromError(error: Error): State {
    return { error };
  }

  componentDidCatch(error: Error, info: React.ErrorInfo) {
    try {
      const key = "csf-crash-log";
      const prev: unknown = JSON.parse(localStorage.getItem(key) || "[]");
      const list = Array.isArray(prev) ? prev : [];
      list.unshift({
        t: new Date().toISOString(),
        message: String(error?.message || error),
        stack: String(error?.stack || "").slice(0, 2000),
        component: String(info?.componentStack || "").slice(0, 1000),
      });
      localStorage.setItem(key, JSON.stringify(list.slice(0, 5)));
    } catch {
      /* localStorage 不可用时忽略 */
    }
    console.error("[ErrorBoundary] 渲染异常:", error, info?.componentStack);
  }

  componentDidMount() {
    // hydration 成功的信号：bootScript 的启动诊断据此判断「已正常启动」。
    (window as unknown as { __csfHydrated?: boolean }).__csfHydrated = true;
  }

  private reload = () => {
    if (typeof window !== "undefined") window.location.reload();
  };

  private retry = () => this.setState({ error: null });

  render() {
    if (!this.state.error) return this.props.children;
    // 兜底页：用内联样式渲染，完全不依赖 tailwind/外部 CSS，
    // 确保即使样式表加载失败也绝不会是「白屏」。深色背景 + 高对比文字。
    return (
      <div
        style={{
          position: "fixed",
          inset: 0,
          zIndex: 2147483646,
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          padding: "24px",
          background: "#0F1419",
          color: "#E8EEF2",
          fontFamily:
            "system-ui, -apple-system, 'PingFang SC', 'Microsoft YaHei', sans-serif",
          textAlign: "center",
        }}
      >
        <div style={{ maxWidth: 360 }}>
          <div style={{ fontSize: 48 }}>🐣</div>
          <h1 style={{ marginTop: 12, fontSize: 20, fontWeight: 800 }}>
            页面出了一点小问题
          </h1>
          <p style={{ marginTop: 8, fontSize: 14, lineHeight: 1.6, color: "#A8B5BD" }}>
            聪聪学堂遇到了一点故障，但你的学习进度已保存在本机。
            点下面的按钮重新载入，就能继续学习。
          </p>
          <div style={{ marginTop: 20, display: "flex", justifyContent: "center", gap: 12 }}>
            <button
              onClick={this.retry}
              style={{
                borderRadius: 16,
                border: "2px solid #3A4750",
                padding: "10px 20px",
                fontSize: 14,
                fontWeight: 700,
                color: "#E8EEF2",
                background: "transparent",
              }}
            >
              重试
            </button>
            <button
              onClick={this.reload}
              style={{
                borderRadius: 16,
                padding: "10px 20px",
                fontSize: 14,
                fontWeight: 700,
                color: "#0F1419",
                background: "#58CC02",
                border: "none",
              }}
            >
              重新载入
            </button>
          </div>
          <details style={{ marginTop: 16, fontSize: 12, color: "#7E8C94", textAlign: "left" }}>
            <summary style={{ cursor: "pointer" }}>错误详情（反馈时可附上）</summary>
            <pre
              style={{
                marginTop: 4,
                maxHeight: 128,
                overflow: "auto",
                whiteSpace: "pre-wrap",
                wordBreak: "break-word",
              }}
            >
              {String(this.state.error?.message || this.state.error)}
            </pre>
          </details>
        </div>
      </div>
    );
  }
}
