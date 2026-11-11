package com.chinastudyfree.app;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONObject;

import java.util.Random;

/**
 * 所有页面的统一外壳：顶部 HUD（红心/宝石/连胜/今日 XP）+ 底部 5 标签导航 + 通用弹窗。
 * 子类只实现 buildContent()。每次 onResume 重建内容，保证进度/资源实时刷新。
 */
public abstract class BaseActivity extends Activity {

    protected Progress P;
    protected LinearLayout contentArea;

    private TextView hudHearts, hudGems, hudStreak, hudXpText;
    private View hudXpFill, hudXpSpacer;
    protected Dialog currentDialog;

    protected boolean useHud() { return true; }
    protected boolean useBack() { return false; }
    protected boolean useBottomNav() { return true; }
    protected String activeNav() { return "home"; }
    protected String titleText() { return ""; }
    protected String subtitleText() { return ""; }

    protected abstract void buildContent(LinearLayout content);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        P = Progress.get(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        P.ensureDateRoll();
        setContentView(buildRoot());
        refreshHud();
        maybeShowPendingDialogs();
    }

    @Override
    protected void onPause() {
        // 必须先关弹窗再让 Activity 退到后台 / finish，否则窗口会泄漏
        dismissCurrent();
        super.onPause();
    }

    // ===================== 外壳构建 =====================
    private View buildRoot() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.color(this, R.color.bgSoft));

        if (useHud() && !useBack()) {
            root.addView(buildHud());
        } else if (useBack()) {
            root.addView(buildBackHeader());
        }

        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        sv.setVerticalScrollBarEnabled(false);
        contentArea = new LinearLayout(this);
        contentArea.setOrientation(LinearLayout.VERTICAL);
        contentArea.setPadding(Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 30));
        sv.addView(contentArea, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(sv, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        if (useBottomNav()) {
            root.addView(buildBottomNav());
        }

        buildContent(contentArea);
        return root;
    }

    private View buildHud() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.VERTICAL);
        bar.setBackgroundColor(Ui.color(this, R.color.card));
        bar.setPadding(Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        hudHearts = statChip("❤", "0", R.color.cardinal);
        hudGems = statChip("💎", "0", R.color.secondary);
        hudStreak = statChip("🔥", "0", R.color.fox);
        top.addView(hudHearts);
        top.addView(hudGems);
        top.addView(hudStreak);
        bar.addView(top);

        bar.addView(Ui.gap(this, 10));

        LinearLayout xpRow = new LinearLayout(this);
        xpRow.setOrientation(LinearLayout.HORIZONTAL);
        xpRow.setGravity(Gravity.CENTER_VERTICAL);
        hudXpText = Ui.tv(this, "今日 XP 0 / 50", 13, R.color.inkLight, true);
        xpRow.addView(hudXpText, Ui.lpWeight(1f));
        bar.addView(xpRow);

        bar.addView(Ui.gap(this, 6));

        LinearLayout track = new LinearLayout(this);
        track.setOrientation(LinearLayout.HORIZONTAL);
        int barH = Ui.dp(this, 14);
        track.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, barH));
        track.setBackground(Ui.round(Ui.color(this, R.color.swan), Ui.dp(this, 7)));
        hudXpFill = new View(this);
        hudXpFill.setBackgroundColor(Ui.color(this, R.color.primary));
        hudXpFill.setLayoutParams(new LinearLayout.LayoutParams(0, barH, 0f));
        hudXpSpacer = new View(this);
        hudXpSpacer.setLayoutParams(new LinearLayout.LayoutParams(0, barH, 1f));
        track.addView(hudXpFill);
        track.addView(hudXpSpacer);
        bar.addView(track);

        View line = new View(this);
        line.setBackgroundColor(Ui.color(this, R.color.line));
        bar.addView(line, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 1)));

        return bar;
    }

    private TextView statChip(String icon, String value, int colorRes) {
        return Ui.tv(this, icon + " " + value, 15, colorRes, true);
    }

    private View buildBackHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setBackgroundColor(Ui.color(this, R.color.card));
        header.setPadding(Ui.dp(this, 12), Ui.dp(this, 10), Ui.dp(this, 12), Ui.dp(this, 10));

        TextView back = Ui.tv(this, "‹ 返回", 16, R.color.secondary, true);
        back.setPadding(0, Ui.dp(this, 6), Ui.dp(this, 12), Ui.dp(this, 6));
        back.setClickable(true); back.setFocusable(true);
        back.setOnClickListener(v -> finish());
        header.addView(back);

        TextView t = Ui.tv(this, titleText(), 18, R.color.ink, true);
        t.setLayoutParams(Ui.lpWeight(1f));
        header.addView(t);

        LinearLayout mini = new LinearLayout(this);
        mini.setOrientation(LinearLayout.HORIZONTAL);
        mini.setGravity(Gravity.CENTER_VERTICAL);
        mini.addView(statChip("❤", String.valueOf(P.getHearts()), R.color.cardinal));
        mini.addView(statChip("💎", String.valueOf(P.getGems()), R.color.secondary));
        header.addView(mini);

        return header;
    }

    private View buildBottomNav() {
        return Ui.bottomNav(this, activeNav(), v -> {
            String id = (String) v.getTag();
            if (id == null || id.equals(activeNav())) return;
            Intent in = new Intent();
            switch (id) {
                case "home": in.setClass(this, MainActivity.class); break;
                case "review": in.setClass(this, ReviewActivity.class); break;
                case "league": in.setClass(this, LeagueActivity.class); break;
                case "shop": in.setClass(this, ShopActivity.class); break;
                default: in.setClass(this, ProfileActivity.class); break;
            }
            in.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(in);
            finish();
        });
    }

    /** 重新构建内容区（用于领奖/进度变化后就地刷新） */
    protected void rebuild() {
        setContentView(buildRoot());
        refreshHud();
    }

    protected void refreshHud() {
        if (hudHearts != null) {
            P.refreshHearts();
            hudHearts.setText("❤ " + P.getHearts());
        }
        if (hudGems != null) hudGems.setText("💎 " + P.getGems());
        if (hudStreak != null) hudStreak.setText("🔥 " + P.getStreak());
        if (hudXpText != null) {
            int today = P.getTodayXp(), goal = P.getDailyGoal();
            hudXpText.setText("今日 XP " + today + " / " + goal);
            float pct = goal > 0 ? Math.min(1f, Math.max(0f, (float) today / goal)) : 0f;
            if (hudXpFill != null) {
                ((LinearLayout.LayoutParams) hudXpFill.getLayoutParams()).weight = pct;
                hudXpFill.requestLayout();
            }
            if (hudXpSpacer != null) {
                ((LinearLayout.LayoutParams) hudXpSpacer.getLayoutParams()).weight = 1f - pct;
                hudXpSpacer.requestLayout();
            }
        }
    }

    // ===================== 通用弹窗 =====================
    protected Dialog showDialog(View card, boolean outsideDismiss) {
        // Activity 正在结束时弹窗会抛 BadTokenException / 造成 WindowLeaked
        if (isFinishing() || isDestroyed()) return null;
        dismissCurrent(); // 任何情况下都不叠加第二层弹窗
        final Dialog d = new Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        FrameLayout dim = new FrameLayout(this);
        dim.setBackgroundColor(Color.argb(140, 0, 0, 0));
        dim.setPadding(Ui.dp(this, 24), Ui.dp(this, 24), Ui.dp(this, 24), Ui.dp(this, 24));
        FrameLayout.LayoutParams clp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clp.gravity = Gravity.CENTER;
        card.setLayoutParams(clp);
        dim.addView(card);
        if (outsideDismiss) dim.setOnClickListener(v -> d.dismiss());
        d.setContentView(dim);
        Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new GradientDrawable());
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
        currentDialog = d;
        d.show();
        return d;
    }

    protected void dismissCurrent() {
        if (currentDialog != null) currentDialog.dismiss();
        currentDialog = null;
    }

    protected LinearLayout dialogCard(Context c) {
        LinearLayout card = new LinearLayout(c);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(Ui.color(c, R.color.card));
        card.setPadding(Ui.dp(c, 22), Ui.dp(c, 22), Ui.dp(c, 22), Ui.dp(c, 22));
        card.setBackground(Ui.round(Ui.color(c, R.color.card), Ui.dp(c, 18)));
        return card;
    }

    private void maybeShowPendingDialogs() {
        JSONObject lr = P.getPendingLeagueResult();
        if (lr != null) { showLeagueResult(lr); return; }
        JSONObject sm = P.getPendingStreakMilestone();
        if (sm != null) { showStreakMilestone(sm); return; }
        int done = P.getCompletedLessonCount();
        // 遍历所有应得宝箱：跨过多个 5 的倍数时不能只弹最后一个，否则会丢宝箱
        for (int n = Progress.CHEST_EVERY_N_LESSONS; n <= done; n += Progress.CHEST_EVERY_N_LESSONS) {
            String cid = "c" + n;
            if (!P.chestClaimed(cid)) { showChest(cid); return; }
        }
        if (!P.dailyRewardClaimedToday()) { showDailyReward(); }
    }

    private void showDailyReward() {
        int reward = P.claimDailyReward();
        P.save();
        LinearLayout card = dialogCard(this);
        card.addView(centeredText(this, "🎁 每日奖励", 22, R.color.ink, true));
        card.addView(Ui.gap(this, 8));
        card.addView(centeredText(this, "连续学习第 " + P.effectiveStreak() + " 天", 15, R.color.inkLight, false));
        card.addView(Ui.gap(this, 14));
        card.addView(centeredText(this, "💎 +" + reward, 30, R.color.secondary, true));
        card.addView(Ui.gap(this, 18));
        TextView ok = Ui.button(this, "收下", R.color.primary, R.color.primaryDark, R.color.card, 17);
        ok.setOnClickListener(v -> { refreshHud(); dismissCurrent(); });
        card.addView(ok);
        showDialog(card, false);
    }

    private void showChest(final String cid) {
        // 显示即结算落盘：避免「不点领取就返回」后 onResume 重新随机刷出史诗奖励
        if (!P.claimChest(cid)) return;
        Progress.ChestReward r = Progress.rollChestReward(new Random());
        P.addGems(r.gems);
        P.save();
        LinearLayout card = dialogCard(this);
        card.addView(centeredText(this, "🎉 恭喜获得宝箱！", 20, R.color.ink, true));
        card.addView(Ui.gap(this, 8));
        card.addView(centeredText(this, "开启第 " + cid.replace("c", "") + " 课宝箱", 14, R.color.inkLight, false));
        card.addView(Ui.gap(this, 14));
        String tierName = r.tier.equals("epic") ? "史诗" : (r.tier.equals("rare") ? "稀有" : "普通");
        card.addView(centeredText(this, tierName + "宝箱", 16, R.color.warning, true));
        card.addView(centeredText(this, "💎 +" + r.gems, 30, R.color.secondary, true));
        card.addView(Ui.gap(this, 18));
        TextView ok = Ui.button(this, "收下", R.color.primary, R.color.primaryDark, R.color.card, 17);
        ok.setOnClickListener(v -> { refreshHud(); dismissCurrent(); });
        card.addView(ok);
        showDialog(card, false);
    }

    private void showStreakMilestone(JSONObject sm) {
        int streak = sm.optInt("streak", 0);
        int gems = sm.optInt("gems", 0);
        P.clearPendingStreakMilestone(); // 显示即消费，避免返回键关掉后重复弹
        LinearLayout card = dialogCard(this);
        card.addView(centeredText(this, "🔥 连胜 " + streak + " 天！", 22, R.color.fox, true));
        card.addView(Ui.gap(this, 10));
        card.addView(centeredText(this, "坚持就是胜利，奖励", 15, R.color.inkLight, false));
        card.addView(centeredText(this, "💎 +" + gems, 30, R.color.secondary, true));
        card.addView(Ui.gap(this, 18));
        TextView ok = Ui.button(this, "太棒了", R.color.primary, R.color.primaryDark, R.color.card, 17);
        ok.setOnClickListener(v -> { refreshHud(); dismissCurrent(); });
        card.addView(ok);
        showDialog(card, false);
    }

    private void showLeagueResult(JSONObject lr) {
        String tierAfter = lr.optString("tierAfter", "");
        boolean promoted = lr.optBoolean("promoted", false);
        boolean demoted = lr.optBoolean("demoted", false);
        int gems = lr.optInt("gems", 0);
        int rank = lr.optInt("rank", 0);
        P.clearPendingLeagueResult(); // 显示即消费，避免返回键关掉后重复弹
        String title = promoted ? "🏆 晋级！" : (demoted ? "📉 遗憾降级" : "本周联赛结束");
        LinearLayout card = dialogCard(this);
        card.addView(centeredText(this, title, 20, R.color.ink, true));
        card.addView(Ui.gap(this, 8));
        card.addView(centeredText(this, "本周排名 第 " + rank + " 名", 15, R.color.inkLight, false));
        card.addView(Ui.gap(this, 6));
        card.addView(centeredText(this, "当前段位：" + tierLabel(tierAfter), 15, R.color.secondary, true));
        if (gems > 0) card.addView(centeredText(this, "💎 +" + gems, 28, R.color.secondary, true));
        card.addView(Ui.gap(this, 18));
        TextView ok = Ui.button(this, "知道了", R.color.primary, R.color.primaryDark, R.color.card, 17);
        ok.setOnClickListener(v -> { refreshHud(); dismissCurrent(); });
        card.addView(ok);
        showDialog(card, false);
    }

    protected String tierLabel(String tier) {
        String[] ids = Progress.LEAGUE_TIERS, names = Progress.LEAGUE_TIER_NAMES;
        for (int i = 0; i < ids.length; i++) if (ids[i].equals(tier)) return names[i];
        return tier;
    }

    protected TextView centeredText(Context c, String s, float sp, int colorRes, boolean bold) {
        TextView t = Ui.tv(c, s, sp, colorRes, bold);
        t.setGravity(Gravity.CENTER);
        return t;
    }
}
