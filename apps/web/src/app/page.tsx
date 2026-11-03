import { promises as fs } from "fs";
import path from "path";
import type { SiteIndex } from "@/types";
import { HomeClient } from "./HomeClient";
import SafeIsland from "@/components/SafeIsland";
import { gradeFullName } from "@cstf/core/stage";

async function getIndex(): Promise<SiteIndex> {
  const p = path.join(process.cwd(), "public", "data", "index.json");
  return JSON.parse(await fs.readFile(p, "utf-8"));
}

export default async function HomePage() {
  const index = await getIndex();

  // 按年级聚合（一年级有上下册两本）
  const byGrade: Record<number, typeof index.books> = {};
  for (const book of index.books) {
    if (!byGrade[book.grade]) byGrade[book.grade] = [];
    byGrade[book.grade].push(book);
  }
  const grades = Object.keys(byGrade)
    .map(Number)
    .sort((a, b) => a - b);

  return (
    <SafeIsland
      fallback={
        // 首页交互层在老 WebView 崩了时的静态兜底：保证仍能按年级进入学习
        <main className="min-h-screen flex flex-col items-center px-4 pt-10 pb-12">
          <h1 className="text-3xl font-extrabold text-ink">聪聪学堂</h1>
          <p className="mt-2 text-sm text-ink-light text-center max-w-md">
            全科免费，人人可学。共 {index.books.length} 册教材 · {index.totalLessons} 节小课 ·{" "}
            {index.totalQuestions} 道题。
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
      }
    >
      <HomeClient
        grades={grades}
        byGrade={Object.fromEntries(grades.map(g => [g, byGrade[g].length]))}
        totalBooks={index.books.length}
        totalLessons={index.totalLessons}
        totalQuestions={index.totalQuestions}
        validBookIds={index.books.map(b => b.id)}
      />
    </SafeIsland>
  );
}
