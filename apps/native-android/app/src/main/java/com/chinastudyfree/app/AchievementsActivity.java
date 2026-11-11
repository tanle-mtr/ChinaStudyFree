package com.chinastudyfree.app;

import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;

/** 成就页：列出全部成就，已解锁高亮（web 的 Achievements）。 */
public class AchievementsActivity extends BaseActivity {

    @Override
    protected boolean useBack() { return true; }
    @Override
    protected boolean useBottomNav() { return false; }

    @Override
    protected String titleText() { return "我的成就"; }

    @Override
    protected void buildContent(LinearLayout c) {
        JSONObject ua = P.getUnlockedAchievementsObj();
        int n = 0;
        for (String[] a : Progress.ACHIEVEMENTS) if (ua.optBoolean(a[0], false)) n++;
        LinearLayout head = Ui.card(this, R.color.beetle, 14);
        head.setBackground(Ui.make3D(this, R.color.beetle, R.color.beetle, 14, 4));
        head.addView(Ui.tv(this, "已解锁 " + n + " / " + Progress.ACHIEVEMENTS.length, 16, R.color.card, true));
        c.addView(head);
        c.addView(Ui.gap(this, 6));

        for (String[] a : Progress.ACHIEVEMENTS) {
            boolean got = ua.optBoolean(a[0], false);
            LinearLayout card = Ui.rowCard(this, R.color.card, 14);
            card.setBackground(Ui.roundStroke(Ui.color(this, R.color.card),
                    got ? Ui.color(this, R.color.beetle) : Ui.color(this, R.color.line),
                    Ui.dp(this, 2), Ui.dp(this, 14)));
            TextView t = Ui.tv(this, (got ? "🏅 " : "🔒 ") + a[1], 16,
                    got ? R.color.ink : R.color.hare, true);
            t.setLayoutParams(Ui.lpWeight(1f));
            card.addView(t);
            card.addView(Ui.tv(this, "💎+" + a[2], 14, got ? R.color.secondary : R.color.hare, true));
            c.addView(card);
        }
    }
}
