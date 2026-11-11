package com.chinastudyfree.app;

import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/** 设置页：静音 / 自动朗读 / 每日目标 / 重置进度。 */
public class SettingsActivity extends BaseActivity {

    @Override
    protected boolean useBack() { return true; }
    @Override
    protected boolean useBottomNav() { return false; }

    @Override
    protected String titleText() { return "设置"; }

    @Override
    protected void buildContent(LinearLayout c) {
        c.addView(toggleRow("🔊 静音", "关闭所有音效", P.isMuted(), v -> {
            P.setMuted(!P.isMuted()); rebuild();
        }));
        c.addView(toggleRow("🗣️ 自动朗读", "进入题目自动朗读题干", P.isAutoNarrate(), v -> {
            P.setAutoNarrate(!P.isAutoNarrate()); rebuild();
        }));

        // 每日目标
        LinearLayout goalCard = Ui.rowCard(this, R.color.card, 14);
        goalCard.setBackground(Ui.roundStroke(Ui.color(this, R.color.card),
                Ui.color(this, R.color.line), Ui.dp(this, 2), Ui.dp(this, 14)));
        TextView gt = Ui.tv(this, "🎯 每日目标", 16, R.color.ink, true);
        gt.setLayoutParams(Ui.lpWeight(1f));
        goalCard.addView(gt);
        final TextView gv = Ui.tv(this, P.getDailyGoal() + " XP", 15, R.color.primary, true);
        goalCard.addView(gv);
        goalCard.setClickable(true); goalCard.setFocusable(true);
        goalCard.setOnClickListener(v -> {
            int[] opts = Progress.DAILY_GOAL_OPTIONS;
            int cur = P.getDailyGoal(); int idx = 0;
            for (int i = 0; i < opts.length; i++) if (opts[i] == cur) idx = i;
            P.setDailyGoal(opts[(idx + 1) % opts.length]);
            rebuild();
        });
        c.addView(goalCard);

        c.addView(Ui.gap(this, 14));
        TextView reset = Ui.button(this, "重置全部进度", R.color.cardinal, R.color.dangerDark, R.color.card, 15);
        reset.setOnClickListener(v -> confirmReset());
        c.addView(reset);

        c.addView(Ui.gap(this, 10));
        c.addView(Ui.tv(this, "聪聪学堂 · 完全离线 · 数据仅存于本机", 12, R.color.inkLight, false));
    }

    private LinearLayout toggleRow(String label, String desc, boolean on, View.OnClickListener flip) {
        LinearLayout card = Ui.rowCard(this, R.color.card, 14);
        card.setBackground(Ui.roundStroke(Ui.color(this, R.color.card),
                Ui.color(this, R.color.line), Ui.dp(this, 2), Ui.dp(this, 14)));
        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        left.setLayoutParams(Ui.lpWeight(1f));
        left.addView(Ui.tv(this, label, 16, R.color.ink, true));
        left.addView(Ui.tv(this, desc, 13, R.color.inkLight, false));
        card.addView(left);

        TextView pill = Ui.tv(this, on ? "开" : "关", 15, on ? R.color.card : R.color.hare, true);
        pill.setGravity(Gravity.CENTER);
        pill.setMinWidth(Ui.dp(this, 56));
        pill.setPadding(Ui.dp(this, 10), Ui.dp(this, 8), Ui.dp(this, 10), Ui.dp(this, 8));
        pill.setBackground(Ui.round(on ? Ui.color(this, R.color.primary) : Ui.color(this, R.color.swan),
                Ui.dp(this, 12)));
        card.addView(pill);
        card.setClickable(true); card.setFocusable(true);
        card.setOnClickListener(flip);
        return card;
    }

    private void confirmReset() {
        if (isFinishing() || isDestroyed()) return;
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(Ui.color(this, R.color.card));
        card.setPadding(Ui.dp(this, 22), Ui.dp(this, 22), Ui.dp(this, 22), Ui.dp(this, 22));
        card.setBackground(Ui.round(Ui.color(this, R.color.card), Ui.dp(this, 18)));
        card.addView(centered("确定重置？", 20, R.color.cardinal, true));
        card.addView(Ui.gap(this, 8));
        card.addView(centered("所有学习进度、宝石、连胜将清空，且无法恢复。", 14, R.color.inkLight, false));
        card.addView(Ui.gap(this, 16));
        TextView yes = Ui.button(this, "重置", R.color.cardinal, R.color.dangerDark, R.color.card, 15);
        yes.setOnClickListener(v -> {
            P.resetAll();
            Ui.toast(this, "已重置");
            dismissCurrent();
            rebuild();
        });
        card.addView(yes);
        card.addView(Ui.gap(this, 8));
        TextView no = Ui.ghostButton(this, "取消", R.color.wolf);
        no.setOnClickListener(v -> dismissCurrent());
        card.addView(no);
        showDialog(card, true); // 走统一弹窗，受 onPause 保护，避免窗口泄漏
    }

    private TextView centered(String s, float sp, int colorRes, boolean bold) {
        TextView t = Ui.tv(this, s, sp, colorRes, bold);
        t.setGravity(Gravity.CENTER);
        return t;
    }
}
