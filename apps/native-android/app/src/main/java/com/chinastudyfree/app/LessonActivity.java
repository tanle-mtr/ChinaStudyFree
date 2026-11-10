package com.chinastudyfree.app;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** 课程页：web 风逐题流程（顶部进度条+红心），7 题型判分，结束结算屏。 */
public class LessonActivity extends Activity {

    private String bookId;
    private String lessonId;
    private String lessonTitle;
    private JSONObject lesson;
    private JSONArray questions;
    private int total;

    private int currentIndex = 0;
    private int answeredCount = 0;
    private int correctCount = 0;
    private boolean lessonFinished = false;

    private LinearLayout bodyContainer;
    private View progressFill;
    private TextView heartsText;
    private TextView progressLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bookId = getIntent().getStringExtra("bookId");
        lessonId = getIntent().getStringExtra("lessonId");
        if (bookId == null) bookId = "g1up";
        if (lessonId == null) lessonId = "";

        lesson = Data.findLesson(this, bookId, lessonId);
        if (lesson == null) {
            Ui.error(this, "找不到这节小课：" + lessonId);
            return;
        }
        lessonTitle = clean(Data.s(lesson, "title"));
        questions = Data.arr(lesson, "questions");
        total = questions.length();
        Progress.get(this).setActiveBookId(bookId);
        Progress.get(this).setActiveLesson(lessonId, lessonTitle);

        buildShell();
        if (total == 0) { finishLesson(); return; }
        renderQuestion(0);
    }

    private void buildShell() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.color(this, R.color.card));

        // 顶部栏：返回 + 进度条 + 红心
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12));

        TextView back = Ui.tv(this, "‹", 26, R.color.secondary, true);
        back.setPadding(0, 0, Ui.dp(this, 10), 0);
        back.setClickable(true); back.setFocusable(true);
        back.setOnClickListener(v -> finish());
        top.addView(back);

        LinearLayout track = new LinearLayout(this);
        track.setOrientation(LinearLayout.HORIZONTAL);
        track.setBackgroundColor(Ui.color(this, R.color.swan));
        int h = Ui.dp(this, 14);
        track.setLayoutParams(Ui.lpWeight(1f));
        ((LinearLayout.LayoutParams) track.getLayoutParams()).height = h;
        ((GradientDrawable) track.getBackground()).setCornerRadius(Ui.dp(this, 7));
        progressFill = new View(this);
        progressFill.setBackgroundColor(Ui.color(this, R.color.primary));
        progressFill.setLayoutParams(new LinearLayout.LayoutParams(0, h, 0f));
        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(0, h, 1f));
        track.addView(progressFill); track.addView(spacer);
        top.addView(track);

        top.addView(Ui.gap(this, 10));
        heartsText = Ui.tv(this, "❤ " + Progress.get(this).getHearts(), 15, R.color.cardinal, true);
        top.addView(heartsText);
        root.addView(top);

        progressLabel = Ui.tv(this, "", 13, R.color.inkLight, false);
        progressLabel.setPadding(Ui.dp(this, 14), 0, Ui.dp(this, 14), Ui.dp(this, 8));
        root.addView(progressLabel);

        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        bodyContainer = new LinearLayout(this);
        bodyContainer.setOrientation(LinearLayout.VERTICAL);
        bodyContainer.setPadding(Ui.dp(this, 16), Ui.dp(this, 8), Ui.dp(this, 16), Ui.dp(this, 40));
        sv.addView(bodyContainer, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(sv, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);
    }

    private void renderQuestion(int idx) {
        bodyContainer.removeAllViews();
        currentIndex = idx;
        JSONObject q = questions.optJSONObject(idx);
        if (q == null) { finishLesson(); return; }
        final String type = Data.s(q, "type");
        final String ans = Data.s(q, "ans");
        final String exp = Data.s(q, "exp");

        // 进度
        float frac = total > 0 ? (float) idx / total : 0f;
        ((LinearLayout.LayoutParams) progressFill.getLayoutParams()).weight = frac;
        progressFill.requestLayout();
        progressLabel.setText("第 " + (idx + 1) + " / " + total + " 题 · " + typeLabel(type));

        LinearLayout card = Ui.card(this, R.color.card, 0);
        card.setBackground(Ui.roundStroke(Ui.color(this, R.color.card),
                Ui.color(this, R.color.line), Ui.dp(this, 2), Ui.dp(this, 16)));
        card.addView(Ui.tv(this, typeLabel(type) + " · " + Data.i(q, "score") + " 分", 13, R.color.inkLight, true));
        card.addView(Ui.gap(this, 8));
        card.addView(Ui.tv(this, Data.s(q, "q"), 17, R.color.ink, false));
        card.addView(Ui.gap(this, 12));
        bodyContainer.addView(card);

        // 知识点
        String knowledge = Data.s(lesson, "knowledge");
        if (knowledge.length() > 0 && !knowledge.contains("[object Object]")) {
            LinearLayout kc = Ui.card(this, R.color.brandPale, 12);
            kc.addView(Ui.tv(this, "📌 " + knowledge, 13, R.color.brandDark, false));
            bodyContainer.addView(kc);
        }

        final LinearLayout fbSlot = new LinearLayout(this);
        fbSlot.setOrientation(LinearLayout.VERTICAL);
        bodyContainer.addView(fbSlot);

        if ("choice".equals(type) || "true_false".equals(type)) {
            List<String> labels = new ArrayList<>();
            if ("true_false".equals(type)) { labels.add("对"); labels.add("错"); }
            else { JSONArray opts = Data.arr(q, "opts");
                for (int k = 0; k < opts.length(); k++) labels.add(opts.optString(k, "")); }
            for (final String label : labels) {
                final TextView opt = Ui.option(this, label);
                opt.setOnClickListener(v -> {
                    if (answeredCount > idx) return;
                    boolean ok = norm(label).equals(norm(ans));
                    gradeChoice(card, labels, ans, opt, ok, exp, fbSlot);
                });
                card.addView(opt);
            }
        } else if ("calculation".equals(type) || "fill_blank".equals(type) || "fill_blank_text".equals(type)) {
            final EditText et = new EditText(this);
            et.setHint("请输入答案");
            et.setTextSize(16);
            et.setSingleLine(true);
            et.setInputType(InputType.TYPE_CLASS_TEXT);
            et.setImeOptions(EditorInfo.IME_ACTION_DONE);
            et.setPadding(Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12));
            et.setBackground(Ui.roundStroke(Ui.color(this, R.color.card),
                    Ui.color(this, R.color.line), Ui.dp(this, 2), Ui.dp(this, 12)));
            LinearLayout.LayoutParams elp = Ui.lpMatch();
            elp.setMargins(0, 0, 0, Ui.dp(this, 10));
            card.addView(et, elp);

            TextView submit = Ui.button(this, "检查", R.color.secondary, R.color.secondaryDark, R.color.card, 16);
            card.addView(submit);
            View.OnClickListener doSubmit = v -> {
                if (answeredCount > idx) return;
                String user = et.getText().toString();
                if (user.trim().length() == 0) { Ui.toast(this, "先写个答案吧"); return; }
                boolean ok = norm(user).equals(norm(ans));
                et.setEnabled(false);
                submit.setClickable(false); submit.setAlpha(0.5f);
                gradeInput(ok, ans, exp, fbSlot);
            };
            submit.setOnClickListener(doSubmit);
            et.setOnEditorActionListener((v, action, ev) -> {
                if (action == EditorInfo.IME_ACTION_DONE) { doSubmit.onClick(v); return true; }
                return false;
            });
        } else {
            // matching / word_order：自测 + 参考答案
            JSONArray opts = Data.arr(q, "opts");
            for (int k = 0; k < opts.length(); k++) {
                TextView t = Ui.option(this, (k + 1) + ". " + opts.optString(k, ""));
                t.setClickable(false); t.setFocusable(false);
                Ui.paintMuted(this, t);
                card.addView(t);
            }
            TextView reveal = Ui.button(this, "查看参考答案", R.color.secondary, R.color.secondaryDark, R.color.card, 15);
            card.addView(reveal);
            reveal.setOnClickListener(v -> {
                if (answeredCount > idx) return;
                answeredCount++; correctCount++; // 自测计为完成
                fbSlot.removeAllViews();
                fbSlot.addView(Ui.feedback(this,
                        "参考答案：" + ans + (exp.length() > 0 ? "\n\n解析：" + exp : ""), true));
                reveal.setVisibility(View.GONE);
                addContinue();
            });
        }
    }

    private void gradeChoice(LinearLayout card, List<String> labels, String ans,
                             TextView chosen, boolean ok, String exp, LinearLayout fbSlot) {
        if (answeredCount > currentIndex) return;
        answeredCount++;
        if (ok) correctCount++; else loseHeart();
        for (int i = 0; i < card.getChildCount(); i++) {
            View v = card.getChildAt(i);
            if (v instanceof TextView && labels.contains(((TextView) v).getText().toString())) {
                TextView tv = (TextView) v;
                tv.setClickable(false);
                if (norm(tv.getText().toString()).equals(norm(ans))) Ui.paintCorrect(this, tv);
                else Ui.paintMuted(this, tv);
            }
        }
        if (!ok) Ui.paintWrong(this, chosen);
        fbSlot.removeAllViews();
        fbSlot.addView(Ui.feedback(this,
                (ok ? "✓ 答对了！" : "✗ 正确答案：" + ans) + (exp.length() > 0 ? "\n\n解析：" + exp : ""), ok));
        addContinue();
    }

    private void gradeInput(boolean ok, String ans, String exp, LinearLayout fbSlot) {
        if (answeredCount > currentIndex) return;
        answeredCount++;
        if (ok) correctCount++; else loseHeart();
        fbSlot.removeAllViews();
        fbSlot.addView(Ui.feedback(this,
                (ok ? "✓ 答对了！" : "✗ 正确答案：" + ans) + (exp.length() > 0 ? "\n\n解析：" + exp : ""), ok));
        addContinue();
    }

    private void addContinue() {
        TextView cont = Ui.button(this,
                currentIndex + 1 >= total ? "完成本课 →" : "继续 ›",
                R.color.primary, R.color.primaryDark, R.color.card, 16);
        cont.setOnClickListener(v -> {
            if (currentIndex + 1 >= total) finishLesson();
            else renderQuestion(currentIndex + 1);
        });
        bodyContainer.addView(cont);
    }

    private void loseHeart() {
        Progress P = Progress.get(this);
        P.loseHeart();
        if (heartsText != null) heartsText.setText("❤ " + P.getHearts());
        if (P.getHearts() <= 0) showNoHearts();
    }

    private void showNoHearts() {
        final Dialog d = new Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout dim = new LinearLayout(this);
        dim.setBackgroundColor(Color.argb(140, 0, 0, 0));
        dim.setGravity(Gravity.CENTER);
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(Ui.color(this, R.color.card));
        card.setPadding(Ui.dp(this, 22), Ui.dp(this, 22), Ui.dp(this, 22), Ui.dp(this, 22));
        card.setBackground(Ui.round(Ui.color(this, R.color.card), Ui.dp(this, 18)));
        card.addView(centered("💔 红心用完啦", 20, R.color.cardinal, true));
        card.addView(Ui.gap(this, 8));
        card.addView(centered("休息一下，红心会慢慢恢复（每 5 分钟 1 颗）", 14, R.color.inkLight, false));
        card.addView(Ui.gap(this, 16));
        Progress P = Progress.get(this);
        TextView refill = Ui.button(this, "花 350 💎 补满", R.color.cardinal, R.color.dangerDark, R.color.card, 15);
        refill.setOnClickListener(v -> {
            if (P.buyHeartRefill()) { if (heartsText != null) heartsText.setText("❤ " + P.getHearts());
                Ui.toast(this, "红心已补满"); d.dismiss(); }
            else Ui.toast(this, "宝石不足");
        });
        card.addView(refill);
        card.addView(Ui.gap(this, 8));
        TextView later = Ui.ghostButton(this, "稍后再说", R.color.wolf);
        later.setOnClickListener(v -> d.dismiss());
        card.addView(later);
        dim.addView(card);
        d.setContentView(dim);
        if (d.getWindow() != null) {
            d.getWindow().setBackgroundDrawable(new GradientDrawable());
            d.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
        d.show();
    }

    private void finishLesson() {
        if (lessonFinished) return;
        lessonFinished = true;
        Progress P = Progress.get(this);
        int before = P.getCompletedLessonCount();
        Progress.LessonOutcome o = P.completeLesson(lessonId, lessonTitle, correctCount, total, false);
        List<String> newly = P.checkAndUnlockAchievements();

        // 继续学习指向下一课
        List<JSONObject> all = Data.flatLessons(this, bookId);
        int cur = -1;
        for (int k = 0; k < all.size(); k++) if (lessonId.equals(Data.s(all.get(k), "id"))) { cur = k; break; }
        if (cur >= 0 && cur + 1 < all.size()) {
            P.setActiveLesson(Data.s(all.get(cur + 1), "id"), clean(Data.s(all.get(cur + 1), "title")));
        }

        showComplete(o, newly, cur, all);
    }

    private void showComplete(Progress.LessonOutcome o, List<String> newly, int cur, List<JSONObject> all) {
        bodyContainer.removeAllViews();
        float frac = 1f;
        ((LinearLayout.LayoutParams) progressFill.getLayoutParams()).weight = frac;
        progressFill.requestLayout();
        progressLabel.setText("本课完成！");

        LinearLayout card = Ui.card(this, R.color.card, 0);
        card.setBackground(Ui.roundStroke(Ui.color(this, R.color.card),
                Ui.color(this, R.color.line), Ui.dp(this, 2), Ui.dp(this, 16)));
        card.addView(centered("🎉 太棒了！", 24, R.color.primary, true));
        card.addView(Ui.gap(this, 10));
        card.addView(Ui.stars(this, o.stars, 3));
        card.addView(Ui.gap(this, 14));
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.addView(statBox("⭐ " + o.xpGained, "经验"));
        row.addView(Ui.gap(this, 14));
        row.addView(statBox("💎 +" + o.gemsGained, "宝石"));
        row.addView(Ui.gap(this, 14));
        row.addView(statBox("🔥 " + o.streakAfter, "连胜"));
        card.addView(row);
        if (o.dailyGoalReachedNow) {
            card.addView(Ui.gap(this, 10));
            card.addView(centered("🏁 达成今日目标！", 15, R.color.warning, true));
        }
        bodyContainer.addView(card);

        if (!newly.isEmpty()) {
            LinearLayout ach = Ui.card(this, R.color.warning, 12);
            ach.setBackground(Ui.roundStroke(Ui.color(this, R.color.purplePale),
                    Ui.color(this, R.color.beetle), Ui.dp(this, 2), Ui.dp(this, 12)));
            ach.addView(Ui.tv(this, "🏅 解锁成就：", 14, R.color.ink, true));
            for (String n : newly) ach.addView(Ui.tv(this, "  · " + n, 14, R.color.ink, false));
            bodyContainer.addView(ach);
        }

        // 按钮
        LinearLayout btns = new LinearLayout(this);
        btns.setOrientation(LinearLayout.HORIZONTAL);
        btns.setWeightSum(2f);
        if (cur >= 0 && cur + 1 < all.size()) {
            TextView next = Ui.button(this, "下一课 ›", R.color.primary, R.color.primaryDark, R.color.card, 16);
            next.setLayoutParams(Ui.lpWeight(1f));
            final String nid = Data.s(all.get(cur + 1), "id");
            next.setOnClickListener(v -> {
                android.content.Intent in = new android.content.Intent(this, LessonActivity.class);
                in.putExtra("bookId", bookId); in.putExtra("lessonId", nid);
                startActivity(in); finish();
            });
            btns.addView(next);
            btns.addView(Ui.gap(this, 8));
        }
        TextView back = Ui.ghostButton(this, "返回教材", R.color.wolf);
        back.setLayoutParams(Ui.lpWeight(1f));
        back.setOnClickListener(v -> {
            android.content.Intent in = new android.content.Intent(this, BookActivity.class);
            in.putExtra("bookId", bookId);
            startActivity(in); finish();
        });
        btns.addView(back);
        bodyContainer.addView(btns);
    }

    private LinearLayout statBox(String big, String small) {
        LinearLayout b = new LinearLayout(this);
        b.setOrientation(LinearLayout.VERTICAL);
        b.setGravity(Gravity.CENTER);
        b.addView(Ui.tv(this, big, 20, R.color.ink, true));
        b.addView(Ui.tv(this, small, 12, R.color.inkLight, false));
        return b;
    }

    private TextView centered(String s, float sp, int colorRes, boolean bold) {
        TextView t = Ui.tv(this, s, sp, colorRes, bold);
        t.setGravity(Gravity.CENTER);
        return t;
    }

    private static String clean(String s) {
        if (s == null) return "";
        return s.contains("[object Object]") ? "本节课" : s;
    }

    private static String norm(String s) {
        if (s == null) return "";
        return s.trim()
                .replaceAll("\\s+", "")
                .replace("，", ",").replace("、", ",").replace("。", "")
                .replace("：", ":").replace("；", ";")
                .replace("（", "(").replace("）", ")")
                .replace("(", "").replace(")", "")
                .replace("／", "/")
                .toLowerCase(Locale.ROOT);
    }

    private static String typeLabel(String type) {
        switch (type) {
            case "choice": return "选择题";
            case "true_false": return "判断题";
            case "calculation": return "计算题";
            case "fill_blank": return "填空题";
            case "fill_blank_text": return "填空题";
            case "matching": return "连线题（自测）";
            case "word_order": return "排序题（自测）";
            default: return "练习题";
        }
    }
}
