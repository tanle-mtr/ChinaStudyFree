package com.chinastudyfree.app;

import android.widget.LinearLayout;
import android.widget.TextView;

/** 商店页：用宝石兑换红心补满 / 红心护盾（web 的 Shop）。 */
public class ShopActivity extends BaseActivity {

    @Override
    protected String activeNav() { return "shop"; }

    @Override
    protected void buildContent(LinearLayout c) {
        LinearLayout wallet = Ui.card(this, R.color.secondary, 14);
        wallet.setBackground(Ui.make3D(this, R.color.secondary, R.color.secondaryDark, 14, 4));
        wallet.addView(Ui.tv(this, "我的宝石", 14, R.color.card, true)); wallet.getChildAt(0).setAlpha(0.9f);
        wallet.addView(Ui.gap(this, 2));
        wallet.addView(Ui.tv(this, "💎 " + P.getGems(), 24, R.color.card, true));
        c.addView(wallet);
        c.addView(Ui.gap(this, 6));

        c.addView(shopItem(
                "❤️ 补满红心",
                "立即把红心恢复到 " + Progress.MAX_HEARTS + " 颗",
                Progress.HEART_REFILL_COST + " 💎",
                P.getHearts() >= Progress.MAX_HEARTS,
                P.getGems() < Progress.HEART_REFILL_COST,
                () -> { if (P.buyHeartRefill()) { Ui.toast(this, "红心已补满"); rebuild(); } else Ui.toast(this, "宝石不足或红心已满"); }
        ));

        c.addView(shopItem(
                "🛡️ 红心护盾",
                "错过一天学习也不掉连胜（最多 " + Progress.MAX_FREEZES + " 个，已有 " + P.getStreakFreezes() + "）",
                Progress.FREEZE_COST + " 💎",
                P.getStreakFreezes() >= Progress.MAX_FREEZES,
                P.getGems() < Progress.FREEZE_COST,
                () -> { if (P.buyStreakFreeze()) { Ui.toast(this, "已购买护盾"); rebuild(); } else Ui.toast(this, "宝石不足或已达上限"); }
        ));

        c.addView(Ui.gap(this, 6));
        c.addView(Ui.tv(this, "宝石可通过每日学习、完成任务、连胜里程碑、宝箱与联赛获得。",
                12, R.color.inkLight, false));
    }

    private LinearLayout shopItem(String title, String desc, String price,
                                  boolean ownedOut, boolean poor, Runnable onBuy) {
        LinearLayout card = Ui.card(this, R.color.card, 14);
        card.setBackground(Ui.roundStroke(Ui.color(this, R.color.card),
                Ui.color(this, R.color.line), Ui.dp(this, 2), Ui.dp(this, 14)));
        card.addView(Ui.tv(this, title, 16, R.color.ink, true));
        card.addView(Ui.gap(this, 4));
        card.addView(Ui.tv(this, desc, 13, R.color.inkLight, false));
        card.addView(Ui.gap(this, 10));
        TextView buy = Ui.button(this, price, R.color.warning, R.color.warningDark, R.color.eel, 15);
        if (ownedOut) { buy.setText("已达上限"); buy.setClickable(false); buy.setAlpha(0.5f); }
        else if (poor) { buy.setClickable(false); buy.setAlpha(0.5f); }
        else buy.setOnClickListener(v -> onBuy.run());
        card.addView(buy);
        return card;
    }
}
