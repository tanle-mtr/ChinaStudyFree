package com.chinastudyfree.app;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** 纯代码构建原生 View 的工厂：不加载任何 XML 布局，不依赖任何 UI 库。 */
public final class Ui {
    private Ui() {}

    public static int dp(Context c, float v) {
        return (int) (v * c.getResources().getDisplayMetrics().density + 0.5f);
    }

    public static int color(Context c, int res) {
        return c.getColor(res);
    }

    public static GradientDrawable round(int fill, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(radius);
        return d;
    }

    public static GradientDrawable roundStroke(int fill, int stroke, int width, int radius) {
        GradientDrawable d = round(fill, radius);
        d.setStroke(width, stroke);
        return d;
    }

    public static TextView tv(Context c, String s, float sp, int colorRes, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s == null ? "" : s);
        t.setTextSize(sp);
        t.setTextColor(color(c, colorRes));
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setLineSpacing(dp(c, 3), 1f);
        return t;
    }

    public static LinearLayout.LayoutParams lpMatch() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    public static LinearLayout.LayoutParams lpWeight(float w) {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, w);
    }

    /** 带圆角背景的卡片容器 */
    public static LinearLayout card(Context c, int fillRes, int padDp) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        int p = dp(c, padDp);
        l.setPadding(p, p, p, p);
        l.setBackground(round(color(c, fillRes), dp(c, 14)));
        LinearLayout.LayoutParams lp = lpMatch();
        lp.setMargins(0, 0, 0, dp(c, 10));
        l.setLayoutParams(lp);
        return l;
    }

    public static View gap(Context c, int dp) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(c, dp)));
        return v;
    }

    public static View divider(Context c) {
        View v = new View(c);
        v.setBackgroundColor(color(c, R.color.line));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(c, 1));
        lp.setMargins(0, dp(c, 6), 0, dp(c, 10));
        v.setLayoutParams(lp);
        return v;
    }

    public static TextView section(Context c, String s) {
        TextView t = tv(c, s, 16, R.color.ink, true);
        LinearLayout.LayoutParams lp = lpMatch();
        lp.setMargins(0, dp(c, 12), 0, dp(c, 8));
        t.setLayoutParams(lp);
        return t;
    }

    /** 可点击的胶囊按钮（用 TextView 实现，样式完全可控、无系统按钮阴影） */
    public static TextView pill(Context c, String label, int fillRes, int textRes, float sp) {
        TextView t = tv(c, label, sp, textRes, true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(c, 14), dp(c, 12), dp(c, 14), dp(c, 12));
        t.setBackground(round(color(c, fillRes), dp(c, 12)));
        t.setMinHeight(dp(c, 48));
        t.setClickable(true);
        t.setFocusable(true);
        return t;
    }

    /** 题目选项（左对齐 + 描边） */
    public static TextView option(Context c, String label) {
        TextView t = tv(c, label, 15, R.color.ink, false);
        t.setPadding(dp(c, 14), dp(c, 12), dp(c, 14), dp(c, 12));
        t.setBackground(roundStroke(color(c, R.color.card), color(c, R.color.line), dp(c, 2), dp(c, 12)));
        t.setMinHeight(dp(c, 48));
        t.setClickable(true);
        t.setFocusable(true);
        LinearLayout.LayoutParams lp = lpMatch();
        lp.setMargins(0, 0, 0, dp(c, 8));
        t.setLayoutParams(lp);
        return t;
    }

    public static void paintCorrect(Context c, TextView t) {
        t.setBackground(roundStroke(color(c, R.color.correctPale), color(c, R.color.correct), dp(c, 2), dp(c, 12)));
        t.setTextColor(color(c, R.color.correct));
        t.setTypeface(Typeface.DEFAULT_BOLD);
    }

    public static void paintWrong(Context c, TextView t) {
        t.setBackground(roundStroke(color(c, R.color.wrongPale), color(c, R.color.wrong), dp(c, 2), dp(c, 12)));
        t.setTextColor(color(c, R.color.wrong));
        t.setTypeface(Typeface.DEFAULT_BOLD);
    }

    public static void paintMuted(Context c, TextView t) {
        t.setBackground(roundStroke(color(c, R.color.card), color(c, R.color.line), dp(c, 1), dp(c, 12)));
        t.setTextColor(color(c, R.color.inkLight));
    }

    /** 反馈文字块（正确 / 错误 + 解析） */
    public static TextView feedback(Context c, String s, boolean ok) {
        TextView t = tv(c, s, 14, ok ? R.color.correct : R.color.wrong, false);
        t.setPadding(dp(c, 12), dp(c, 10), dp(c, 12), dp(c, 10));
        t.setBackground(round(color(c, ok ? R.color.correctPale : R.color.wrongPale), dp(c, 10)));
        LinearLayout.LayoutParams lp = lpMatch();
        lp.setMargins(0, dp(c, 8), 0, 0);
        t.setLayoutParams(lp);
        return t;
    }

    public static void toast(Context c, String s) {
        Toast.makeText(c, s, Toast.LENGTH_SHORT).show();
    }

    public static final class Page {
        public final LinearLayout content;
        Page(LinearLayout content) { this.content = content; }
    }

    /** 统一页面骨架：顶部标题栏（含返回）+ 可滚动内容区 */
    public static Page page(Activity a, String title, String sub, boolean back) {
        LinearLayout root = new LinearLayout(a);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(color(a, R.color.bg));

        LinearLayout header = new LinearLayout(a);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setBackgroundColor(color(a, R.color.card));
        header.setPadding(dp(a, 14), dp(a, 12), dp(a, 14), dp(a, 12));

        if (back) {
            TextView b = tv(a, "‹ 返回", 16, R.color.blue, true);
            b.setPadding(0, dp(a, 6), dp(a, 14), dp(a, 6));
            b.setOnClickListener(v -> a.finish());
            header.addView(b);
        }
        TextView t = tv(a, title, 19, R.color.ink, true);
        header.addView(t, lpWeight(1f));
        root.addView(header, lpMatch());

        View line = new View(a);
        line.setBackgroundColor(color(a, R.color.line));
        root.addView(line, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(a, 1)));

        if (sub != null && sub.length() > 0) {
            TextView s = tv(a, sub, 13, R.color.inkLight, false);
            s.setPadding(dp(a, 16), dp(a, 10), dp(a, 16), dp(a, 2));
            root.addView(s, lpMatch());
        }

        ScrollView sv = new ScrollView(a);
        sv.setFillViewport(true);
        sv.setVerticalScrollBarEnabled(false);
        LinearLayout content = new LinearLayout(a);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(a, 14), dp(a, 10), dp(a, 14), dp(a, 30));
        sv.addView(content, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(sv, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        a.setContentView(root);
        return new Page(content);
    }

    /** 出错时也保证有可见内容，绝不空白 */
    public static void error(Activity a, String msg) {
        Page p = page(a, "出了点小问题", "", true);
        TextView t = tv(a, msg, 15, R.color.ink, false);
        t.setPadding(dp(a, 16), dp(a, 16), dp(a, 16), dp(a, 16));
        p.content.addView(t);
    }
}
