"use client";

import { useEffect, useState } from "react";
import { useProgressStore } from "@/store/progress";
import { GradePicker } from "@/components/GradePicker";
import { StagePicker } from "@/components/StagePicker";
import { StaticGradeGrid } from "@/components/StaticGradeGrid";
import { stageOfGrade, type StageId } from "@cstf/core/stage";

interface HomeClientProps {
  grades: number[];
  byGrade: Record<number, number>;
  totalBooks: number;
  totalLessons: number;
  totalQuestions: number;
  /** 当前构建里真实存在的 bookId 集合（来自 index.json），用于校验 activeBookId。 */
  validBookIds?: string[];
}

/**
 * 首页路由分发（打开即路径）：
 *   - 未选学段 → 渲染 StagePicker 引导（小学 / 初中 / 高中 / 大学）
 *   - 已选学段 = 大学 → 直接进 /grade/13/
 *   - 已选学段但没选年级 → 渲染该学段内的 GradePicker
 *   - 已选年级且有当前教材 → 软跳 /book/{id}/
 *   - 已选年级无教材 → 软跳 /grade/{grade}/
 *
 * 关键约定（用户投诉修复）：**学段只选一次**。
 *   - GradesPicker 上提供显式的「切换学段」入口（清 selectedStage）；
 *   - 年级页的「切换教材」只清 selectedGrade/activeBookId，**不动 selectedStage**；
 *   - 学段历史 lastStage 兜底（v15）：切换学段会把它一起清空，
 *     但只要它是被"意外丢失 selectedStage"（老档 / 部分写入）导致的，
 *     首页仍会用它恢复，绝不把用户打回学段引导；
 *   - 若学段字段全丢但还记得年级，用 stageOfGrade(grade) 反推（自愈）。
 */
export function HomeClient({
  grades,
  byGrade,
  totalBooks,
  totalLessons,
  totalQuestions,
  validBookIds,
}: HomeClientProps) {
  void byGrade; // 预留：年级→书本数映射，当前静态入口未逐年级展示计数
  const selectedStage = useProgressStore(s => s.selectedStage);
  const lastStage = useProgressStore(s => s.lastStage);
  const selectedGrade = useProgressStore(s => s.selectedGrade);
  const activeBookId = useProgressStore(s => s.activeBookId);
  const storeHydrated = useProgressStore(s => s.hasHydrated);
  const [mounted, setMounted] = useState(false);

  useEffect(() => setMounted(true), []);
  // 首帧用默认态（与预渲染一致），挂载且 store 回填完成后才按真实存档决策，
  // 避免 skipHydration 模式下默认态误把老用户打回学段引导。
  const ready = mounted && storeHydrated;

  /**
   * 生效学段（三级自愈，越往后越保守）：
   *   1. 已选学段 selectedStage（正常路径）
   *   2. 学段历史 lastStage（字段级容错兜底）
   *   3. 年级反推 stageOfGrade(grade)（老档 / 字段缺失的最后一道防线）
   * 任何一级命中都不会把用户打回学段引导重选。
   */
  const effectiveStage: StageId | null =
    selectedStage ??
    lastStage ??
    (selectedGrade != null ? stageOfGrade(selectedGrade) : null);

  useEffect(() => {
    if (!ready) return;
    if (effectiveStage == null) return; // 交给 StagePicker
    // 一律 window.location.assign 硬跳转：静态导出（output: export + trailingSlash）
    // + 安卓 WebView 下 Next router 软导航偶发 404，会导致「选完学段打不开」。
    if (effectiveStage === "college" && selectedGrade == null) {
      // 大学：无年级概念，直接进入「大学」互动课程页（grade 13）
      window.location.assign("/grade/13/");
      return;
    }
    if (selectedGrade == null) return; // 交给 GradePicker(stage)
    // 防御：activeBookId 可能是旧版本遗留、指向当前构建已不存在的书，
    // 直接 assign 会把用户扔进 404 死页（无底栏、无法返回）。先校验存在性，
    // 不存在则回退到年级页（/grade/{grade}/ 一定存在）。validBookIds 缺省时信任 activeBookId。
    const bookOk =
      !!activeBookId &&
      (!validBookIds || validBookIds.length === 0 || validBookIds.includes(activeBookId));
    if (bookOk) {
      window.location.assign(`/book/${activeBookId}/`);
    } else {
      window.location.assign(`/grade/${selectedGrade}/`);
    }
  }, [ready, effectiveStage, selectedStage, lastStage, selectedGrade, activeBookId]);

  // 跳转前 / 预渲染阶段：渲染纯静态年级入口网格。
  // 关键：这段 HTML 在服务端就被渲染成可见内容（无 framer 动画、无 JS 依赖），
  // 即使 JS 在老 WebView 上完全不执行，首页也绝不会是白屏。
  // 服务端首帧与客户端首帧都渲染它 → 严格一致，不会触发 hydration mismatch。
  if (!ready) {
    return (
      <StaticGradeGrid
        grades={grades}
        totalBooks={totalBooks}
        totalLessons={totalLessons}
        totalQuestions={totalQuestions}
      />
    );
  }
  if (effectiveStage == null) {
    return <StagePicker />;
  }
  if (effectiveStage === "college") {
    // 大学无年级：由上面的 effect 直接 replace 到 /grade/13/
    return <div className="min-h-screen bg-bg" />;
  }
  if (selectedGrade == null) {
    return <GradePicker stage={effectiveStage} />;
  }
  return <div className="min-h-screen bg-bg" />;
}
