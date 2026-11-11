package com.chinastudyfree.app;

import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;

import java.util.List;

/** 联赛页：16 人榜单（web 的 League）。 */
public class LeagueActivity extends BaseActivity {

    @Override
    protected String activeNav() { return "league"; }

    @Override
    protected void buildContent(LinearLayout c) {
        if (!P.leagueUnlocked()) {
            LinearLayout lock = Ui.card(this, R.color.card, 16);
            lock.setBackground(Ui.roundStroke(Ui.color(this, R.color.card),
                    Ui.color(this, R.color.line), Ui.dp(this, 2), Ui.dp(this, 16)));
            lock.addView(Ui.tv(this, "🏆 联赛尚未解锁", 18, R.color.ink, true));
            lock.addView(Ui.gap(this, 6));
            lock.addView(Ui.tv(this, "完成 " + Progress.UNLOCK_LESSONS + " 节课后即可参加每周联赛，与 15 名伙伴同场竞技！",
                    14, R.color.inkLight, false));
            c.addView(lock);
            return;
        }

        // 段位卡
        String tier = P.getLeagueTier();
        int tierColor = tierColorRes(tier);
        LinearLayout head = Ui.card(this, tierColor, 16);
        head.setBackground(Ui.make3D(this, tierColor, tierColor, 16, 4));
        head.addView(Ui.tv(this, "当前段位", 14, R.color.card, true)); head.getChildAt(0).setAlpha(0.9f);
        head.addView(Ui.gap(this, 2));
        head.addView(Ui.tv(this, tierLabel(tier), 22, R.color.card, true));
        head.addView(Ui.gap(this, 4));
        head.addView(Ui.tv(this, "本周一 0 点结算并晋级 / 降级", 12, R.color.card, false));
        head.getChildAt(head.getChildCount() - 1).setAlpha(0.85f);
        c.addView(head);

        c.addView(Ui.section(this, "本周排行榜"));

        List<Progress.LeagueRow> rows = P.leagueStandings();
        int rank = 1;
        for (Progress.LeagueRow r : rows) {
            c.addView(rankRow(r, rank++));
        }

        c.addView(Ui.gap(this, 6));
        c.addView(Ui.tv(this, "你本周经验来自日常学习与复习，周一结算后发放段位奖励。",
                12, R.color.inkLight, false));
    }

    private LinearLayout rankRow(Progress.LeagueRow r, int rank) {
        int bg = r.isUser ? R.color.brandPale : R.color.card;
        LinearLayout card = Ui.rowCard(this, bg, 12);
        card.setBackground(Ui.roundStroke(Ui.color(this, bg),
                r.isUser ? Ui.color(this, R.color.primary) : Ui.color(this, R.color.line),
                Ui.dp(this, 2), Ui.dp(this, 12)));
        LinearLayout.LayoutParams clp = (LinearLayout.LayoutParams) card.getLayoutParams();
        clp.setMargins(0, 0, 0, Ui.dp(this, 6));
        card.setLayoutParams(clp);

        TextView rankT = Ui.tv(this, String.valueOf(rank), 18, r.isUser ? R.color.primary : R.color.ink, true);
        rankT.setWidth(Ui.dp(this, 34));
        rankT.setGravity(android.view.Gravity.CENTER);
        card.addView(rankT);

        TextView name = Ui.tv(this, r.name, 16, r.isUser ? R.color.primary : R.color.ink,
                r.isUser);
        name.setLayoutParams(Ui.lpWeight(1f));
        card.addView(name);

        TextView xp = Ui.tv(this, r.weekXp + " XP", 14, R.color.inkLight, true);
        card.addView(xp);
        return card;
    }

    private int tierColorRes(String tier) {
        switch (tier) {
            case "bronze": return R.color.tierBronze;
            case "silver": return R.color.tierSilver;
            case "gold": return R.color.tierGold;
            case "sapphire": return R.color.tierSapphire;
            case "ruby": return R.color.tierRuby;
            case "diamond": return R.color.tierDiamond;
            default: return R.color.tierBronze;
        }
    }
}
