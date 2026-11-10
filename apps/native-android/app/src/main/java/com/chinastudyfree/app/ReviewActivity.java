package com.chinastudyfree.app;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/** 复习页：错题本 SRS 复习（web 的「练习」）。 */
public class ReviewActivity extends BaseActivity {

    private int sessionCorrect = 0;
    private int sessionReviewed = 0;

    @Override
    protected String activeNav() { return "review"; }

    @Override
    protected void buildContent(LinearLayout c) {
        List<JSONObject> due = P.getDueMistakes();
        int totalMistakes = P.getMistakeCount();

        LinearLayout info = Ui.card(this, R.color.secondary, 14);
        info.setBackground(Ui.roundStroke(Ui.color(this, R.color.bluePale),
                Ui.color(this, R.color.secondary), Ui.dp(this, 2), Ui.dp(this, 14)));
        info.addView(Ui.tv(this, "错题复习", 16, R.color.ink, true));
        info.addView(Ui.gap(this, 4));
        info.addView(Ui.tv(this, "共 " + totalMistakes + " 道错题 · 今日待复习 " + due.size() + " 道",
                13, R.color.inkLight, false));
        c.addView(info);

        if (due.isEmpty()) {
            LinearLayout empty = Ui.card(this, R.color.card, 16);
            empty.setBackground(Ui.roundStroke(Ui.color(this, R.color.card),
                    Ui.color(this, R.color.line), Ui.dp(this, 2), Ui.dp(this, 16)));
            empty.addView(Ui.tv(this, "🎉 暂无需要复习的错题", 17, R.color.ink, true));
            empty.addView(Ui.gap(this, 6));
            empty.addView(Ui.tv(this, "把做错的题复习到毕业，就能彻底掌握啦", 13, R.color.inkLight, false));
            c.addView(empty);
            return;
        }

        c.addView(Ui.section(this, "待复习（点击「答对 / 答错」完成复习）"));
        for (final JSONObject e : due) c.addView(reviewCard(e));

        TextView finish = Ui.button(this, "完成复习并领取经验", R.color.primary, R.color.primaryDark, R.color.card, 16);
        finish.setOnClickListener(v -> {
            if (sessionReviewed > 0) {
                int xp = P.awardReviewXP(sessionCorrect, sessionReviewed);
                int hearts = P.awardReviewHeart(sessionCorrect);
                String msg = "复习完成！+ " + xp + " 经验";
                if (hearts > 0) msg += "，+" + hearts + " 红心";
                Ui.toast(this, msg);
                sessionCorrect = 0; sessionReviewed = 0;
                P.checkAndUnlockAchievements();
            }
            rebuild();
        });
        c.addView(finish);
    }

    private LinearLayout reviewCard(final JSONObject e) {
        final JSONObject q = e.optJSONObject("question");
        LinearLayout card = Ui.card(this, R.color.card, 14);
        card.setBackground(Ui.roundStroke(Ui.color(this, R.color.card),
                Ui.color(this, R.color.line), Ui.dp(this, 2), Ui.dp(this, 14)));
        card.addView(Ui.tv(this, "📚 " + e.optString("lessonTitle", ""), 12, R.color.inkLight, true));
        card.addView(Ui.gap(this, 4));
        card.addView(Ui.tv(this, q == null ? "" : q.optString("q", ""), 16, R.color.ink, false));
        card.addView(Ui.gap(this, 8));

        final JSONArray opts = q == null ? new JSONArray() : q.optJSONArray("opts");
        String type = q == null ? "" : q.optString("type", "");
        if ("choice".equals(type)) {
            for (int k = 0; k < opts.length(); k++) {
                TextView t = Ui.option(this, opts.optString(k, ""));
                t.setClickable(false); t.setFocusable(false);
                Ui.paintMuted(this, t);
                card.addView(t);
            }
        } else if (q != null) {
            card.addView(Ui.tv(this, "参考答案：" + q.optString("ans", ""), 14, R.color.secondary, true));
        }
        card.addView(Ui.gap(this, 8));

        final LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setWeightSum(2f);
        TextView right = Ui.button(this, "我答对了", R.color.correct, R.color.correctDark, R.color.card, 15);
        right.setLayoutParams(Ui.lpWeight(1f));
        TextView wrong = Ui.button(this, "答错了", R.color.cardinal, R.color.dangerDark, R.color.card, 15);
        wrong.setLayoutParams(Ui.lpWeight(1f));
        actions.addView(right);
        actions.addView(Ui.gap(this, 8));
        actions.addView(wrong);
        card.addView(actions);

        View.OnClickListener mark = v -> {
            boolean correct = (v == right);
            P.reviewMistake(e.optString("lessonId", ""), q == null ? -1 : q.optInt("id", -1), correct);
            P.save();
            sessionReviewed++; if (correct) sessionCorrect++;
            // 灰掉操作区
            right.setClickable(false); right.setAlpha(0.5f);
            wrong.setClickable(false); wrong.setAlpha(0.5f);
            TextView res = Ui.tv(this, correct ? "✓ 已记为答对" : "✗ 已记为答错，已加入下一轮复习", 13,
                    correct ? R.color.correct : R.color.cardinal, true);
            card.addView(res);
        };
        right.setOnClickListener(mark);
        wrong.setOnClickListener(mark);
        return card;
    }
}
