package com.chinastudyfree.app;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** 首页：品牌区 + 继续学习 + 每日任务 + 学段/年级入口。web（Duolingo）风格。 */
public class MainActivity extends BaseActivity {

    private static final String[] STAGE_NAMES = {"小学", "初中", "高中", "大学"};

    @Override
    protected String activeNav() { return "home"; }

    @Override
    protected void buildContent(LinearLayout c) {
        JSONObject idx = Data.index(this);
        String lastBook = P.getActiveBookId();
        String lastLesson = P.getActiveLessonId();

        // 品牌区
        LinearLayout hero = Ui.card(this, R.color.brandPale, 18);
        TextView badge = Ui.tv(this, "聪", 30, R.color.card, true);
        badge.setGravity(Gravity.CENTER);
        badge.setBackground(Ui.round(Ui.color(this, R.color.brand), Ui.dp(this, 34)));
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(Ui.dp(this, 64), Ui.dp(this, 64));
        blp.gravity = Gravity.CENTER;
        badge.setLayoutParams(blp);
        hero.addView(badge);
        hero.addView(Ui.gap(this, 10));
        TextView ht = Ui.tv(this, "聪聪学堂", 24, R.color.ink, true);
        ht.setGravity(Gravity.CENTER); hero.addView(ht);
        hero.addView(Ui.gap(this, 4));
        TextView hs = Ui.tv(this,
                Data.i(idx, "totalBooks") + " 本教材 · " + Data.i(idx, "totalLessons")
                        + " 节小课 · " + Data.i(idx, "totalQuestions") + " 道题",
                14, R.color.brandDark, false);
        hs.setGravity(Gravity.CENTER); hero.addView(hs);
        c.addView(hero);

        // 继续学习
        if (lastBook.length() > 0 && lastLesson.length() > 0) {
            final String bookId = lastBook, lessonId = lastLesson;
            LinearLayout cont = Ui.card(this, R.color.primary, 16);
            cont.setBackground(Ui.make3D(this, R.color.primary, R.color.primaryDark, 16, 4));
            TextView lab = Ui.tv(this, "继续学习", 14, R.color.card, true);
            lab.setAlpha(0.9f);
            cont.addView(lab);
            cont.addView(Ui.gap(this, 4));
            TextView lt = Ui.tv(this, P.getActiveLessonTitle(), 18, R.color.card, true);
            lt.setMaxLines(2);
            cont.addView(lt);
            cont.addView(Ui.gap(this, 12));
            TextView go = Ui.tv(this, "开始  ›", 16, R.color.card, true);
            go.setGravity(Gravity.END);
            cont.addView(go);
            cont.setClickable(true); cont.setFocusable(true);
            cont.setOnClickListener(v -> goLesson(bookId, lessonId));
            c.addView(cont);
            c.addView(Ui.gap(this, 6));
        } else {
            LinearLayout cont = Ui.card(this, R.color.card, 16);
            cont.setBackground(Ui.roundStroke(Ui.color(this, R.color.card),
                    Ui.color(this, R.color.line), Ui.dp(this, 2), Ui.dp(this, 16)));
            cont.addView(Ui.tv(this, "开始你的第一节课吧！", 16, R.color.ink, true));
            cont.addView(Ui.gap(this, 4));
            cont.addView(Ui.tv(this, "在下方选择年级，挑选教材开始学习", 13, R.color.inkLight, false));
            c.addView(cont);
            c.addView(Ui.gap(this, 6));
        }

        // 每日任务
        c.addView(Ui.section(this, "每日任务"));
        List<Progress.Quest> quests = P.todayQuests();
        for (Progress.Quest q : quests) c.addView(questCard(q));
        c.addView(Ui.gap(this, 6));

        // 学段 / 年级
        c.addView(Ui.section(this, "选择学段与年级"));
        JSONArray grades = Data.arr(idx, "grades");
        for (int si = 0; si < STAGE_NAMES.length; si++) {
            final String stageKey = stageKeyOf(si);
            List<JSONObject> list = new ArrayList<>();
            for (int k = 0; k < grades.length(); k++) {
                JSONObject g = grades.optJSONObject(k);
                if (g != null && stageKey.equals(Data.s(g, "stage"))) list.add(g);
            }
            if (list.isEmpty()) continue;
            c.addView(Ui.tv(this, STAGE_NAMES[si], 15, R.color.ink, true));
            c.addView(Ui.gap(this, 8));
            LinearLayout row = null;
            for (int k = 0; k < list.size(); k++) {
                if (k % 2 == 0) {
                    row = new LinearLayout(this);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setWeightSum(2f);
                    LinearLayout.LayoutParams rlp = Ui.lpMatch();
                    rlp.setMargins(0, 0, 0, Ui.dp(this, 10));
                    row.setLayoutParams(rlp);
                    c.addView(row);
                }
                LinearLayout card = gradeCard(list.get(k), si);
                LinearLayout.LayoutParams lp = Ui.lpWeight(1f);
                if (k % 2 == 0) lp.setMargins(0, 0, Ui.dp(this, 5), 0);
                else lp.setMargins(Ui.dp(this, 5), 0, 0, 0);
                card.setLayoutParams(lp);
                row.addView(card);
            }
            if (list.size() % 2 == 1 && row != null) {
                View spacer = new View(this);
                spacer.setLayoutParams(Ui.lpWeight(1f));
                row.addView(spacer);
            }
            c.addView(Ui.gap(this, 8));
        }
    }

    private String stageKeyOf(int i) {
        switch (i) { case 0: return "primary"; case 1: return "middle";
            case 2: return "high"; default: return "college"; }
    }

    private LinearLayout questCard(final Progress.Quest q) {
        LinearLayout card = Ui.card(this, R.color.card, 14);
        card.setBackground(Ui.roundStroke(Ui.color(this, R.color.card),
                Ui.color(this, R.color.line), Ui.dp(this, 2), Ui.dp(this, 14)));
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = Ui.tv(this, q.title, 15, R.color.ink, true);
        title.setLayoutParams(Ui.lpWeight(1f));
        top.addView(title);
        final int prog = P.questProgress(q.kind);
        final boolean claimed = P.questClaimed(P.todayStr() + ":" + q.id);
        TextView reward = Ui.tv(this, "💎+" + q.reward, 14, R.color.secondary, true);
        top.addView(reward);
        card.addView(top);
        card.addView(Ui.gap(this, 8));

        // 进度条
        float frac = q.target > 0 ? Math.min(1f, (float) prog / q.target) : 0f;
        LinearLayout track = barTrack();
        View fill = barFill(R.color.secondary);
        View spacer = barSpacer();
        ((LinearLayout.LayoutParams) fill.getLayoutParams()).weight = frac;
        ((LinearLayout.LayoutParams) spacer.getLayoutParams()).weight = 1f - frac;
        track.addView(fill); track.addView(spacer);
        card.addView(track);
        card.addView(Ui.gap(this, 6));
        TextView status = Ui.tv(this, prog + " / " + q.target, 13, R.color.inkLight, false);
        card.addView(status);

        if (claimed) {
            TextView done = Ui.tv(this, "✓ 已领取", 14, R.color.correct, true);
            done.setGravity(Gravity.END);
            card.addView(done);
        } else if (prog >= q.target) {
            TextView claim = Ui.button(this, "领取奖励", R.color.warning, R.color.warningDark, R.color.eel, 15);
            claim.setOnClickListener(v -> {
                P.claimQuest(q);
                P.save();
                rebuild();
            });
            card.addView(claim);
        }
        return card;
    }

    private LinearLayout gradeCard(final JSONObject g, int stageIdx) {
        int[] accents = {R.color.primary, R.color.secondary, R.color.cardinal, R.color.warning};
        int accent = accents[stageIdx];
        LinearLayout card = Ui.card(this, R.color.card, 14);
        card.setBackground(Ui.roundStroke(Ui.color(this, R.color.card), Ui.color(this, accent),
                Ui.dp(this, 2), Ui.dp(this, 14)));
        card.addView(Ui.tv(this, Data.s(g, "name"), 18, R.color.ink, true));
        card.addView(Ui.gap(this, 4));
        card.addView(Ui.tv(this, Data.i(g, "books") + " 本教材 ›", 13, R.color.inkLight, false));
        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(v -> {
            Intent in = new Intent(this, GradeActivity.class);
            in.putExtra("grade", Data.i(g, "grade"));
            startActivity(in);
        });
        return card;
    }

    private LinearLayout barTrack() {
        LinearLayout track = new LinearLayout(this);
        track.setOrientation(LinearLayout.HORIZONTAL);
        track.setBackgroundColor(Ui.color(this, R.color.swan));
        int h = Ui.dp(this, 12);
        track.setLayoutParams(Ui.lpMatch());
        ((LinearLayout.LayoutParams) track.getLayoutParams()).setMargins(0, 0, 0, 0);
        track.getLayoutParams().height = h;
        ((GradientDrawable) track.getBackground()).setCornerRadius(Ui.dp(this, 6));
        return track;
    }
    private View barFill(int colorRes) {
        View f = new View(this);
        f.setBackgroundColor(Ui.color(this, colorRes));
        f.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 0f));
        return f;
    }
    private View barSpacer() {
        View s = new View(this);
        s.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        return s;
    }

    private void goLesson(String bookId, String lessonId) {
        Intent in = new Intent(this, LessonActivity.class);
        in.putExtra("bookId", bookId);
        in.putExtra("lessonId", lessonId);
        startActivity(in);
    }
}
