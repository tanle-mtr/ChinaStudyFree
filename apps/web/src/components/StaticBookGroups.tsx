import type { Book, StageId, SubjectId } from "@/types";
import { SUBJECTS } from "@/lib/subjects";

/**
 * StaticBookGroups —— /grade/[grade] 的「静态优先」教材列表（服务端组件）。
 *
 * 这是 GradeSubjectGroups（客户端，带版本快切/我的版本排序）的兜底：
 * 一旦客户端版在老 WebView 水合崩溃，SafeIsland 就渲染这份纯静态列表，
 * 保证用户永远能看到并按年级选教材，绝不白屏。
 *
 * 仅做最朴素的分组 + 默认版本排序，不含任何交互。
 */

// 与 GradeSubjectGroups 保持一致的学科主题色（Duolingo 动物色）
const THEMES: Record<SubjectId, { bg: string; shadow: string; glyph: string }> = {
  math: { bg: "#58CC02", shadow: "#58A700", glyph: "数" },
  chinese: { bg: "#FF4B4B", shadow: "#EA2B2B", glyph: "语" },
  english: { bg: "#1CB0F6", shadow: "#1899D6", glyph: "A" },
  science: { bg: "#FFC800", shadow: "#E0A800", glyph: "科" },
  moral: { bg: "#A855F7", shadow: "#9333EA", glyph: "德" },
  physics: { bg: "#1CB0F6", shadow: "#1899D6", glyph: "物" },
  chemistry: { bg: "#CE82FF", shadow: "#A560E8", glyph: "化" },
  biology: { bg: "#58CC02", shadow: "#58A700", glyph: "生" },
  history: { bg: "#FF9600", shadow: "#C2660A", glyph: "史" },
  geography: { bg: "#14D4F4", shadow: "#0F8FA8", glyph: "地" },
  politics: { bg: "#FF4B4B", shadow: "#EA2B2B", glyph: "政" },
  computer: { bg: "#235390", shadow: "#1A3E6B", glyph: "编" },
};

const SUBJECT_ORDER: SubjectId[] = [
  "math",
  "chinese",
  "english",
  "physics",
  "chemistry",
  "biology",
  "history",
  "geography",
  "politics",
  "moral",
  "science",
  "computer",
];

interface StaticBookGroupsProps {
  books: Book[];
  stage: StageId;
}

export function StaticBookGroups({ books }: StaticBookGroupsProps) {
  const bySubject = new Map<SubjectId, Book[]>();
  for (const b of books) {
    const sid = (b.subject ?? "math") as SubjectId;
    if (!bySubject.has(sid)) bySubject.set(sid, []);
    bySubject.get(sid)!.push(b);
  }

  return (
    <div className="space-y-6">
      {SUBJECT_ORDER.map(subjectId => {
        const subjectBooks = bySubject.get(subjectId);
        if (!subjectBooks || subjectBooks.length === 0) return null;
        const theme = THEMES[subjectId];
        const subject = SUBJECTS[subjectId];

        return (
          <section key={subjectId}>
            <div className="flex items-center gap-2.5 mb-3 px-1">
              <div
                className="w-1.5 h-6 rounded-full shrink-0"
                style={{ backgroundColor: theme.bg }}
              />
              <div className="flex-1 min-w-0">
                <div className="text-base lg:text-xl font-extrabold text-ink">
                  {subject.label}
                </div>
              </div>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 lg:gap-5">
              {subjectBooks.map(book => (
                <a
                  key={book.id}
                  href={`/book/${book.id}/`}
                  className="block rounded-2xl lg:rounded-3xl select-none overflow-hidden"
                  style={{
                    backgroundColor: theme.bg,
                    boxShadow: `0 5px 0 0 ${theme.shadow}`,
                  }}
                >
                  <div className="relative flex items-center gap-3 lg:gap-4 px-4 py-3.5 lg:px-5 lg:py-5">
                    <div
                      className="w-12 h-12 lg:w-16 lg:h-16 rounded-full bg-white flex items-center justify-center shrink-0"
                      style={{ boxShadow: "inset 0 -3px 0 0 rgba(0,0,0,0.08)" }}
                    >
                      <span
                        className="text-2xl lg:text-3xl font-extrabold"
                        style={{ color: theme.bg }}
                      >
                        {theme.glyph}
                      </span>
                    </div>
                    <div className="flex-1 min-w-0 text-white">
                      <div className="flex items-center gap-1.5 flex-wrap">
                        <span className="text-[10px] lg:text-xs font-extrabold uppercase tracking-widest opacity-80">
                          {subject.label}
                        </span>
                        {book.publisherName && (
                          <span className="shrink-0 rounded-md bg-white/25 px-1.5 py-px text-[10px] lg:text-[11px] font-extrabold leading-4">
                            {book.publisherName}
                          </span>
                        )}
                      </div>
                      <div className="text-base lg:text-xl font-extrabold leading-tight truncate mt-0.5">
                        {book.grade >= 13 ? book.textbookName : `${book.gradeName}${book.semesterName}`}
                      </div>
                      <div className="text-[11px] lg:text-xs font-semibold opacity-85 mt-0.5 lg:mt-1">
                        {book.unitsCount} 单元 · {book.lessonsCount} 节小课
                      </div>
                    </div>
                    <div className="text-white/90 text-xl lg:text-2xl font-extrabold shrink-0">
                      ›
                    </div>
                  </div>
                </a>
              ))}
            </div>
          </section>
        );
      })}
    </div>
  );
}
