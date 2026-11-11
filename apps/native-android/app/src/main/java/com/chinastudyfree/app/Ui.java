package com.chinastudyfree.app;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
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

    /** 垂直容器里的行间距：宽 MATCH_PARENT，高 dp。切勿用于水平容器。 */
    public static View gap(Context c, int dp) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(c, dp)));
        return v;
    }

    /** 水平容器里的列间距：宽 dp，高 0（不影响行高）。 */
    public static View hgap(Context c, int dp) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(dp(c, dp), 0));
        return v;
    }

    /** 横向圆角卡片（rankRow / menuRow / toggleRow 等「一行多列」场景）。
     *  与 card() 的区别：orientation 为 HORIZONTAL，子项可用 lpWeight()。 */
    public static LinearLayout rowCard(Context c, int fillRes, int padDp) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        int p = dp(c, padDp);
        l.setPadding(p, p, p, p);
        l.setBackground(round(color(c, fillRes), dp(c, 14)));
        LinearLayout.LayoutParams lp = lpMatch();
        lp.setMargins(0, 0, 0, dp(c, 10));
        l.setLayoutParams(lp);
        return l;
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

    // ===== Duolingo 风组件（厚底 3D 按钮 / 星 / 底部导航 / HUD 统计） =====

    /** 厚底 3D 按钮背景：底层深色圆角 + 顶层亮色圆角上移，留出底部色条形成立体感 */
    public static LayerDrawable make3D(Context c, int faceRes, int shadowRes, int radiusDp, int depthDp) {
        GradientDrawable shadow = round(color(c, shadowRes), dp(c, radiusDp));
        GradientDrawable face = round(color(c, faceRes), dp(c, radiusDp));
        LayerDrawable ld = new LayerDrawable(new Drawable[]{ shadow, face });
        int d = dp(c, depthDp);
        ld.setLayerInset(1, 0, 0, 0, d);
        return ld;
    }

    /** 给 3D 按钮绑定按下下沉效果（不改变点击逻辑） */
    public static void applyPress(final View v, final int depthDp) {
        final float depth = dp(v.getContext(), depthDp);
        v.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent e) {
                switch (e.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        view.setTranslationY(depth);
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        view.setTranslationY(0);
                        break;
                }
                return false; // 不消费事件，保证 onClick 仍触发
            }
        });
    }

    /** 主行动按钮（绿/蓝等实色 3D 按钮） */
    public static TextView button(Context c, String label, int faceRes, int shadowRes, int textRes, float sp) {
        TextView t = tv(c, label, sp, textRes, true);
        t.setGravity(Gravity.CENTER);
        t.setMinHeight(dp(c, 50));
        t.setPadding(dp(c, 16), dp(c, 14), dp(c, 16), dp(c, 14));
        t.setBackground(make3D(c, faceRes, shadowRes, 16, 4));
        t.setClickable(true);
        t.setFocusable(true);
        applyPress(t, 4);
        return t;
    }

    /** 幽灵按钮（白底描边，按下下沉） */
    public static TextView ghostButton(Context c, String label, int textRes) {
        TextView t = tv(c, label, 15, textRes, true);
        t.setGravity(Gravity.CENTER);
        t.setMinHeight(dp(c, 48));
        t.setPadding(dp(c, 16), dp(c, 12), dp(c, 16), dp(c, 12));
        t.setBackground(roundStroke(color(c, R.color.card), color(c, R.color.swan), dp(c, 2), dp(c, 14)));
        t.setClickable(true);
        t.setFocusable(true);
        applyPress(t, 3);
        return t;
    }

    /** 星行：filled 颗金色实心星，其余灰色空心星 */
    public static LinearLayout stars(Context c, int filled, int max) {
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        for (int i = 0; i < max; i++) {
            TextView s = new TextView(c);
            s.setText(i < filled ? "★" : "☆");
            s.setTextSize(18);
            s.setTextColor(color(c, i < filled ? R.color.warning : R.color.swan));
            if (i > 0) s.setPadding(dp(c, 4), 0, 0, 0);
            row.addView(s);
        }
        return row;
    }

    /** HUD 小统计块（图标 + 数值），用于顶部栏 */
    public static LinearLayout statChip(Context c, String icon, String value, int colorRes) {
        LinearLayout chip = new LinearLayout(c);
        chip.setOrientation(LinearLayout.HORIZONTAL);
        chip.setGravity(Gravity.CENTER_VERTICAL);
        TextView ic = new TextView(c);
        ic.setText(icon); ic.setTextSize(16);
        TextView v = tv(c, value, 15, colorRes, true);
        v.setPadding(dp(c, 4), 0, dp(c, 12), 0);
        chip.addView(ic); chip.addView(v);
        return chip;
    }

    /** 底部 5 导航项定义 */
    public static final String[] NAV_IDS = {"home","review","league","shop","profile"};
    public static final String[] NAV_ICONS = {"🏠","📚","🏆","🛒","👤"};
    public static final String[] NAV_LABELS = {"首页","复习","联赛","商店","我"};

    /** 底部 Tab 导航栏 */
    public static LinearLayout bottomNav(Context c, String active, View.OnClickListener onTap) {
        LinearLayout bar = new LinearLayout(c);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setWeightSum(NAV_IDS.length);
        bar.setBackgroundColor(color(c, R.color.card));
        bar.setPadding(0, dp(c, 6), 0, dp(c, 6));
        int h = dp(c, 56);
        for (int i = 0; i < NAV_IDS.length; i++) {
            final String id = NAV_IDS[i];
            LinearLayout item = new LinearLayout(c);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            item.setWeightSum(1);
            TextView ic = new TextView(c);
            ic.setText(NAV_ICONS[i]); ic.setTextSize(22);
            ic.setGravity(Gravity.CENTER);
            TextView lb = new TextView(c);
            lb.setText(NAV_LABELS[i]); lb.setTextSize(11);
            lb.setGravity(Gravity.CENTER);
            boolean act = id.equals(active);
            int tint = act ? R.color.primary : R.color.wolf;
            ic.setTextColor(color(c, tint));
            lb.setTextColor(color(c, tint));
            if (act) lb.setTypeface(Typeface.DEFAULT_BOLD);
            item.addView(ic); item.addView(lb);
            LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(0, h, 1f);
            item.setLayoutParams(ilp);
            item.setClickable(true);
            item.setFocusable(true);
            final int idx = i;
            item.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { onTap.onClick(v); }
            });
            item.setTag(id);
            bar.addView(item);
        }
        return bar;
    }

    /** 横向进度条：frac∈[0,1]，fillRes 为已填充颜色 */
    public static LinearLayout progressBar(Context c, float frac, int fillRes) {
        LinearLayout track = new LinearLayout(c);
        track.setOrientation(LinearLayout.HORIZONTAL);
        int h = dp(c, 12);
        LinearLayout.LayoutParams lp = lpMatch();
        track.setLayoutParams(lp);
        track.getLayoutParams().height = h;
        track.setBackground(round(color(c, R.color.swan), dp(c, 6)));
        View fill = new View(c);
        fill.setBackgroundColor(color(c, fillRes));
        float f = Math.max(0f, Math.min(1f, frac));
        fill.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, f));
        View spacer = new View(c);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f - f));
        track.addView(fill);
        track.addView(spacer);
        return track;
    }

    /** 出错时也保证有可见内容，绝不空白 */
    public static void error(Activity a, String msg) {
        Page p = page(a, "出了点小问题", "", true);
        TextView t = tv(a, msg, 15, R.color.ink, false);
        t.setPadding(dp(a, 16), dp(a, 16), dp(a, 16), dp(a, 16));
        p.content.addView(t);
    }
}
