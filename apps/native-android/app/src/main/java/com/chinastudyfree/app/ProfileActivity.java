package com.chinastudyfree.app;

import android.content.Intent;
import android.widget.LinearLayout;
import android.widget.TextView;

/** 个人页：学习统计 + 入口（web 的 Profile）。 */
public class ProfileActivity extends BaseActivity {

    @Override
    protected String activeNav() { return "profile"; }

    @Override
    protected void buildContent(LinearLayout c) {
        // 头像 + 名称
        LinearLayout head = Ui.card(this, R.color.brandPale, 18);
        TextView badge = Ui.tv(this, "聪", 30, R.color.card, true);
        badge.setGravity(android.view.Gravity.CENTER);
        badge.setBackground(Ui.round(Ui.color(this, R.color.brand), Ui.dp(this, 34)));
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(Ui.dp(this, 64), Ui.dp(this, 64));
        blp.gravity = android.view.Gravity.CENTER;
        badge.setLayoutParams(blp);
        head.addView(badge);
        head.addView(Ui.gap(this, 8));
        TextView nm = Ui.tv(this, "聪聪学堂学员", 20, R.color.ink, true);
        nm.setGravity(android.view.Gravity.CENTER); head.addView(nm);
        head.addView(Ui.gap(this, 2));
        head.addView(Ui.tv(this, "当前段位：" + tierLabel(P.getLeagueTier()), 14, R.color.brandDark, true));
        c.addView(head);

        // 统计
        LinearLayout stats = Ui.card(this, R.color.card, 14);
        stats.setBackground(Ui.roundStroke(Ui.color(this, R.color.card),
                Ui.color(this, R.color.line), Ui.dp(this, 2), Ui.dp(this, 14)));
        stats.addView(statRow("总经验", "⭐ " + P.getXp()));
        stats.addView(Ui.divider(this));
        stats.addView(statRow("连续学习", "🔥 " + P.getStreak() + " 天"));
        stats.addView(Ui.divider(this));
        stats.addView(statRow("完成课程", "✓ " + P.getCompletedLessonCount() + " 节"));
        stats.addView(Ui.divider(this));
        stats.addView(statRow("累计宝石", "💎 " + P.getLifetimeGems()));
        stats.addView(Ui.divider(this));
        stats.addView(statRow("解锁成就", "🏅 " + P.getUnlockedAchievementCount() + " / " + Progress.ACHIEVEMENTS.length));
        c.addView(stats);

        // 入口
        c.addView(menuRow("🏅 我的成就", v -> { startActivity(new Intent(this, AchievementsActivity.class)); }));
        c.addView(menuRow("⚙️ 设置", v -> { startActivity(new Intent(this, SettingsActivity.class)); }));
    }

    private LinearLayout statRow(String key, String val) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.addView(Ui.tv(this, key, 15, R.color.inkLight, false));
        TextView v = Ui.tv(this, val, 15, R.color.ink, true);
        v.setLayoutParams(Ui.lpWeight(1f));
        v.setGravity(android.view.Gravity.END);
        row.addView(v);
        return row;
    }

    private LinearLayout menuRow(String label, android.view.View.OnClickListener onClick) {
        LinearLayout card = Ui.rowCard(this, R.color.card, 14);
        card.setBackground(Ui.roundStroke(Ui.color(this, R.color.card),
                Ui.color(this, R.color.line), Ui.dp(this, 2), Ui.dp(this, 14)));
        TextView t = Ui.tv(this, label, 16, R.color.ink, true);
        t.setLayoutParams(Ui.lpWeight(1f));
        card.addView(t);
        card.addView(Ui.tv(this, "›", 20, R.color.hare, true));
        card.setClickable(true); card.setFocusable(true);
        card.setOnClickListener(onClick);
        return card;
    }
}
