package com.chinastudyfree.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** 首页：品牌区 + 学习进度 + 按学段分组的年级入口。纯原生 View，零 WebView。 */
public class MainActivity extends Activity {

    private static final String[] STAGES = {"primary", "middle", "high", "college"};
    private static final String[] STAGE_NAMES = {"小学", "初中", "高中", "大学"};
    private static final int[] ACCENTS = {R.color.brand, R.color.blue, R.color.orange, R.color.purple};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 内容在 onResume 统一构建（返回首页时刷新进度）；这里先铺底色防止瞬时空白
        LinearLayout placeholder = new LinearLayout(this);
        placeholder.setBackgroundColor(Ui.color(this, R.color.bg));
        setContentView(placeholder);
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            build();
        } catch (Exception e) {
            android.util.Log.e("csf-home", "build failed", e);
            Ui.error(this, "首页加载失败：" + e.getMessage());
        }
    }

    private void build() {
        JSONObject idx = Data.index(this);
        Ui.Page page = Ui.page(this, "聪聪学堂", "全科免费 · 完全离线 · 无需联网", false);
        LinearLayout c = page.content;

        // 品牌区
        LinearLayout hero = Ui.card(this, R.color.brandPale, 18);
        TextView badge = Ui.tv(this, "聪", 28, R.color.card, true);
        badge.setGravity(Gravity.CENTER);
        badge.setBackground(Ui.round(Ui.color(this, R.color.brand), Ui.dp(this, 32)));
        hero.addView(badge, new LinearLayout.LayoutParams(Ui.dp(this, 62), Ui.dp(this, 62)));
        hero.addView(Ui.gap(this, 12));
        hero.addView(Ui.tv(this, "聪聪学堂", 24, R.color.ink, true));
        hero.addView(Ui.gap(this, 4));
        hero.addView(Ui.tv(this,
                Data.i(idx, "totalBooks") + " 本教材 · " + Data.i(idx, "totalLessons")
                        + " 节小课 · " + Data.i(idx, "totalQuestions") + " 道题",
                14, R.color.inkLight, false));
        hero.addView(Ui.gap(this, 8));
        hero.addView(Ui.tv(this, "原声原生，不加载网页，打开就能学", 13, R.color.brandDark, false));
        c.addView(hero);

        // 继续学习
        final String lb = Progress.lastBook(this);
        final String ll = Progress.lastLesson(this);
        if (lb.length() > 0 && ll.length() > 0) {
            TextView resume = Ui.pill(this,
                    "继续学习 · " + Progress.lastTitle(this),
                    R.color.brand, R.color.card, 16);
            resume.setOnClickListener(v -> goLesson(lb, ll));
            c.addView(resume);
            c.addView(Ui.gap(this, 10));
        }

        // 进度卡
        LinearLayout stat = Ui.card(this, R.color.card, 14);
        stat.addView(Ui.tv(this, "我的学习进度", 15, R.color.ink, true));
        stat.addView(Ui.gap(this, 6));
        stat.addView(Ui.tv(this,
                "已完成 " + Progress.doneLessons(this) + " 节课 · 累计答对 "
                        + Progress.correct(this) + " / " + Progress.answers(this) + " 题",
                14, R.color.inkLight, false));
        c.addView(stat);

        JSONArray grades = Data.arr(idx, "grades");
        for (int si = 0; si < STAGES.length; si++) {
            List<JSONObject> list = new ArrayList<>();
            for (int k = 0; k < grades.length(); k++) {
                JSONObject g = grades.optJSONObject(k);
                if (g != null && STAGES[si].equals(Data.s(g, "stage"))) list.add(g);
            }
            if (list.isEmpty()) continue;
            c.addView(Ui.section(this, STAGE_NAMES[si]));

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
                LinearLayout card = gradeCard(list.get(k), ACCENTS[si]);
                LinearLayout.LayoutParams lp = Ui.lpWeight(1f);
                if (k % 2 == 0) lp.setMargins(0, 0, Ui.dp(this, 5), 0);
                else lp.setMargins(Ui.dp(this, 5), 0, 0, 0);
                card.setLayoutParams(lp);
                row.addView(card);
            }
            // 奇数个时补空位，避免最后一个卡片被拉宽
            if (list.size() % 2 == 1 && row != null) {
                View spacer = new View(this);
                spacer.setLayoutParams(Ui.lpWeight(1f));
                row.addView(spacer);
            }
        }
    }

    private LinearLayout gradeCard(final JSONObject g, int accent) {
        LinearLayout card = Ui.card(this, R.color.card, 14);
        card.setBackground(Ui.roundStroke(
                Ui.color(this, R.color.card), Ui.color(this, accent),
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

    private void goLesson(String bookId, String lessonId) {
        Intent in = new Intent(this, LessonActivity.class);
        in.putExtra("bookId", bookId);
        in.putExtra("lessonId", lessonId);
        startActivity(in);
    }
}
