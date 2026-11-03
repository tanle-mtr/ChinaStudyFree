import { Suspense } from "react";
import { TextbooksClient } from "./TextbooksClient";
import SafeIsland from "@/components/SafeIsland";

/**
 * 教材中心 —— 浏览 ChinaTextbook 开源教材库的真实课本目录。
 * 客户端组件（静态导出下 useSearchParams 需包在 Suspense 内）。
 */
export default function TextbooksPage() {
  return (
    <Suspense fallback={<div className="min-h-screen bg-bg" />}>
      <SafeIsland
        fallback={
          <main className="min-h-screen flex flex-col items-center justify-center px-6 text-center">
            <h1 className="text-2xl font-extrabold text-ink">教材中心</h1>
            <p className="mt-3 text-sm text-ink-light">教材目录组件加载失败，可返回首页重试。</p>
            <a href="/" className="btn-chunky-primary mt-6 px-8">
              返回首页
            </a>
          </main>
        }
      >
        <TextbooksClient />
      </SafeIsland>
    </Suspense>
  );
}
