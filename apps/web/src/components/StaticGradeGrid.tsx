/**
 * StaticGradeGrid —— 首页「静态优先」兜底入口（前端渲染层重写的核心之一）。
 *
 * 设计约束（针对老版安卓 WebView 白屏）：
 *   - 纯服务端可渲染的可见内容：不依赖 JS、不使用 framer-motion 入场动画
 *     （framer 的 initial={{opacity:0}} 在 SSR 会输出 opacity:0 的不可见 DOM，
 *      等于「白屏换了种形式」）。
 *   - 即使 JS 完全不执行，这段 HTML 也在，用户至少能看到「聪聪学堂 + 年级入口」，
 *     绝不会出现纯白屏。
 *   - 同时作为 HomeClient 的 SafeIsland 兜底：交互层在老 WebView 崩了也照样能进年级。
 */
import { gradeFullName } from "@cstf/core/stage";

interface StaticGradeGridProps {
  grades: number[];
  totalBooks: number;
  totalLessons: number;
  totalQuestions: number;
}

export function StaticGradeGrid({
  grades,
  totalBooks,
  totalLessons,
  totalQuestions,
}: StaticGradeGridProps) {
  return (
    <main className="min-h-screen flex flex-col items-center px-4 pt-10 pb-12">
      <h1 className="text-3xl font-extrabold text-ink">聪聪学堂</h1>
      <p className="mt-2 text-sm text-ink-light text-center max-w-md">
        全科免费，人人可学。共 {totalBooks} 册教材 · {totalLessons} 节小课 ·{" "}
        {totalQuestions} 道题。
      </p>
      <div className="mt-8 grid grid-cols-2 sm:grid-cols-3 gap-3 w-full max-w-2xl">
        {grades.map(g => (
          <a
            key={g}
            href={`/grade/${g}/`}
            className="btn-chunky-primary text-center py-3"
          >
            {gradeFullName(g)}
          </a>
        ))}
      </div>
      <a
        href={`/textbooks/`}
        className="mt-6 text-sm font-bold text-[#1CB0F6]"
      >
        浏览官方教材 →
      </a>
    </main>
  );
}
