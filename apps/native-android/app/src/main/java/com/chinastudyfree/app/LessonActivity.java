package com.chinastudyfree.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** 课程页：7 种题型全部原生渲染与判分，进度写本地 SharedPreferences。 */
public class LessonActivity extends Activity {

    private String bookId;
    private String lessonId;
    private String lessonTitle;
    private JSONObject lesson;
    private JSONArray questions;

    private final Set<Integer> answered = new HashSet<>();
    private int correctCount = 0;
    private TextView progressText;
    private ProgressBar progressBar;

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
        lessonTitle = Data.s(lesson, "title");
        questions = Data.arr(lesson, "questions");
        Progress.touch(this, bookId, lessonId, lessonTitle);

        try {
            build();
        } catch (Exception e) {
            android.util.Log.e("csf-lesson", "build failed", e);
            Ui.error(this, "题目加载失败：" + e.getMessage());
        }
    }

    private void build() {
        JSONObject meta = Data.findBookMeta(this, bookId);
        String sub = (meta == null ? "" : Data.s(meta, "fullName") + " · ")
                + questions.length() + " 道题";
        Ui.Page page = Ui.page(this, lessonTitle, sub, true);
        LinearLayout c = page.content;

        // 进度卡
        LinearLayout pc = Ui.card(this, R.color.card, 14);
        progressText = Ui.tv(this, "", 14, R.color.ink, true);
        pc.addView(progressText);
        pc.addView(Ui.gap(this, 8));
        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(Math.max(questions.length(), 1));
        progressBar.setProgress(0);
        pc.addView(progressBar, Ui.lpMatch());
        c.addView(pc);

        // 知识点
        String knowledge = Data.s(lesson, "knowledge");
        if (knowledge.length() > 0) {
            LinearLayout kc = Ui.card(this, R.color.brandPale, 14);
            kc.addView(Ui.tv(this, "本节知识点", 14, R.color.brandDark, true));
            kc.addView(Ui.gap(this, 6));
            kc.addView(Ui.tv(this, knowledge, 14, R.color.ink, false));
            c.addView(kc);
        }

        for (int k = 0; k < questions.length(); k++) {
            JSONObject q = questions.optJSONObject(k);
            if (q != null) c.addView(questionCard(k, q));
        }

        TextView next = Ui.pill(this, "下一课 →", R.color.brand, R.color.card, 16);
        next.setOnClickListener(v -> goNext());
        c.addView(next);
        c.addView(Ui.gap(this, 10));

        TextView back = Ui.pill(this, "返回教材目录", R.color.card, R.color.blue, 15);
        back.setBackground(Ui.roundStroke(Ui.color(this, R.color.card),
                Ui.color(this, R.color.line), Ui.dp(this, 2), Ui.dp(this, 12)));
        back.setOnClickListener(v -> {
            Intent in = new Intent(this, BookActivity.class);
            in.putExtra("bookId", bookId);
            in.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(in);
            finish();
        });
        c.addView(back);

        updateProgress();
    }

    private LinearLayout questionCard(final int idx, final JSONObject q) {
        LinearLayout card = Ui.card(this, R.color.card, 14);
        final String type = Data.s(q, "type");
        final String ans = Data.s(q, "ans");
        final String exp = Data.s(q, "exp");

        card.addView(Ui.tv(this,
                "第 " + (idx + 1) + " 题 · " + typeLabel(type) + " · " + Data.i(q, "score") + " 分",
                13, R.color.inkLight, true));
        card.addView(Ui.gap(this, 8));
        card.addView(Ui.tv(this, Data.s(q, "q"), 16, R.color.ink, false));
        card.addView(Ui.gap(this, 10));

        // 反馈插槽放在选项之后，判分时动态填充
        final LinearLayout fbSlot = new LinearLayout(this);
        fbSlot.setOrientation(LinearLayout.VERTICAL);

        JSONArray opts = Data.arr(q, "opts");

        if ("choice".equals(type) || "true_false".equals(type)) {
            final List<TextView> buttons = new ArrayList<>();
            List<String> labels = new ArrayList<>();
            if ("true_false".equals(type)) {
                labels.add("对");
                labels.add("错");
            } else {
                for (int k = 0; k < opts.length(); k++) labels.add(opts.optString(k, ""));
            }
            for (final String label : labels) {
                final TextView opt = Ui.option(this, label);
                buttons.add(opt);
                card.addView(opt);
                opt.setOnClickListener(v -> {
                    if (answered.contains(idx)) return;
                    boolean ok = norm(label).equals(norm(ans));
                    onAnswer(idx, q, ok, buttons, opt, fbSlot);
                });
            }

        } else if ("calculation".equals(type)
                || "fill_blank".equals(type)
                || "fill_blank_text".equals(type)) {
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
            elp.setMargins(0, 0, 0, Ui.dp(this, 8));
            card.addView(et, elp);

            final TextView submit = Ui.pill(this, "提交答案", R.color.blue, R.color.card, 15);
            card.addView(submit);
            final View.OnClickListener doSubmit = v -> {
                if (answered.contains(idx)) return;
                String user = et.getText().toString();
                if (user.trim().length() == 0) {
                    Ui.toast(this, "先写个答案吧");
                    return;
                }
                boolean ok = norm(user).equals(norm(ans));
                onAnswer(idx, q, ok, new ArrayList<TextView>(), null, fbSlot);
                et.setEnabled(false);
                submit.setClickable(false);
                submit.setAlpha(0.55f);
            };
            submit.setOnClickListener(doSubmit);
            et.setOnEditorActionListener((v, action, ev) -> {
                if (action == EditorInfo.IME_ACTION_DONE) {
                    doSubmit.onClick(v);
                    return true;
                }
                return false;
            });

        } else {
            // matching / word_order：连线与排序改为自测 + 参考答案（避免复杂拖拽，保证零崩溃）
            for (int k = 0; k < opts.length(); k++) {
                TextView t = Ui.option(this, (k + 1) + ". " + opts.optString(k, ""));
                t.setClickable(false);
                t.setFocusable(false);
                Ui.paintMuted(this, t);
                card.addView(t);
            }
            final TextView reveal = Ui.pill(this, "查看参考答案", R.color.bluePale, R.color.blue, 15);
            card.addView(reveal);
            reveal.setOnClickListener(v -> {
                String msg = "参考答案：" + ans + (exp.length() > 0 ? "\n\n解析：" + exp : "");
                fbSlot.removeAllViews();
                fbSlot.addView(Ui.feedback(this, msg, true));
                reveal.setVisibility(View.GONE);
                if (!answered.contains(idx)) {
                    answered.add(idx);
                    updateProgress();
                }
            });
        }

        card.addView(fbSlot, Ui.lpMatch());
        return card;
    }

    private void onAnswer(int idx, JSONObject q, boolean ok,
                          List<TextView> buttons, TextView chosen, LinearLayout fbSlot) {
        if (answered.contains(idx)) return;
        answered.add(idx);
        if (ok) correctCount++;
        Progress.record(this, ok);

        String ans = Data.s(q, "ans");
        String exp = Data.s(q, "exp");
        for (TextView t : buttons) {
            t.setClickable(false);
            if (norm(t.getText().toString()).equals(norm(ans))) Ui.paintCorrect(this, t);
            else Ui.paintMuted(this, t);
        }
        if (!ok && chosen != null) Ui.paintWrong(this, chosen);

        String msg = (ok ? "✓ 答对了！" : "✗ 正确答案：" + ans)
                + (exp.length() > 0 ? "\n\n解析：" + exp : "");
        fbSlot.removeAllViews();
        fbSlot.addView(Ui.feedback(this, msg, ok));
        updateProgress();
    }

    private void updateProgress() {
        int n = questions.length();
        if (progressText != null) {
            progressText.setText("已完成 " + answered.size() + " / " + n
                    + " 题 · 答对 " + correctCount + " 题");
        }
        if (progressBar != null) progressBar.setProgress(answered.size());
        if (n > 0 && answered.size() >= n) Progress.markDone(this, lessonId);
    }

    private void goNext() {
        List<JSONObject> all = Data.flatLessons(this, bookId);
        int cur = -1;
        for (int k = 0; k < all.size(); k++) {
            if (lessonId.equals(Data.s(all.get(k), "id"))) { cur = k; break; }
        }
        if (cur >= 0 && cur + 1 < all.size()) {
            Intent in = new Intent(this, LessonActivity.class);
            in.putExtra("bookId", bookId);
            in.putExtra("lessonId", Data.s(all.get(cur + 1), "id"));
            startActivity(in);
            finish();
        } else {
            Ui.toast(this, "已经是这本教材的最后一课啦");
        }
    }

    /** 答案归一化：忽略空白、中英文标点差异与括号，兼容 1/4 与 (1)/(4) 等写法 */
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
