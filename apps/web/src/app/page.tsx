import { promises as fs } from "fs";
import path from "path";
import type { SiteIndex } from "@/types";
import { HomeClient } from "./HomeClient";
import SafeIsland from "@/components/SafeIsland";
import { StaticGradeGrid } from "@/components/StaticGradeGrid";

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
        // 首页交互层在老 WebView 崩了时的静态兜底：保证仍能按年级进入学习，绝不白屏
        <StaticGradeGrid
          grades={grades}
          totalBooks={index.books.length}
          totalLessons={index.totalLessons}
          totalQuestions={index.totalQuestions}
        />
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
