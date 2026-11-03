import type { Book } from "@/types";

/**
 * StaticBookPath —— /book/[book] 单元路径的「静态优先」兜底（服务端组件）。
 *
 * BookPathSection 用 dynamic(ssr:false) 在客户端渲染，老 WebView 一旦崩，
 * SafeIsland 回退到这份纯静态的「单元→小节」链接列表，用户仍能进每一节小课，
 * 绝不白屏。
 */

interface StaticLesson {
  id: string;
  title?: string;
  unitNumber?: number;
  unitTitle?: string;
}

interface StaticBookPathProps {
  bookId: string;
  outline: { lessons: StaticLesson[]; units?: Array<{ unit_number: number; title?: string }> };
  book: Book;
}

export function StaticBookPath({ bookId, outline }: StaticBookPathProps) {
  const lessons = (outline.lessons ?? []) as StaticLesson[];
  if (lessons.length === 0) {
    return (
      <div className="max-w-md lg:max-w-3xl mx-auto px-4 mt-4">
        <div className="rounded-2xl bg-white border-2 border-bg-softer px-5 py-8 text-center text-ink-light">
          本章暂无可学习的小课。
        </div>
      </div>
    );
  }

  // 按单元分组（能拿到 unitNumber 时）
  const byUnit = new Map<number, StaticLesson[]>();
  const stray: StaticLesson[] = [];
  for (const l of lessons) {
    if (typeof l.unitNumber === "number") {
      if (!byUnit.has(l.unitNumber)) byUnit.set(l.unitNumber, []);
      byUnit.get(l.unitNumber)!.push(l);
    } else {
      stray.push(l);
    }
  }
  const unitNums = [...byUnit.keys()].sort((a, b) => a - b);

  const renderLesson = (l: StaticLesson) => (
    <a
      key={l.id}
      href={`/lesson/${bookId}__${l.id}/`}
      className="block rounded-2xl bg-white border border-bg-softer px-4 py-3 hover:border-primary/40 transition-colors"
    >
      <div className="text-sm font-bold text-ink truncate">
        {l.title || `第 ${l.id} 节`}
      </div>
    </a>
  );

  return (
    <div className="max-w-md lg:max-w-3xl mx-auto px-4 mt-4 space-y-5">
      {unitNums.map(u => (
        <div key={u}>
          <div className="text-xs font-extrabold text-ink-softer uppercase tracking-wider mb-2 px-1">
            第 {u} 单元
          </div>
          <div className="space-y-2 lg:space-y-0 lg:grid lg:grid-cols-2 lg:gap-2">
            {byUnit.get(u)!.map(renderLesson)}
          </div>
        </div>
      ))}
      {stray.length > 0 && (
        <div className="space-y-2">{stray.map(renderLesson)}</div>
      )}
    </div>
  );
}
