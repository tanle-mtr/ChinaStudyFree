package com.chinastudyfree.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** 教材页：web 风「学习路径」——单元节点 + 课程气泡（含星级/锁），顺序解锁。 */
public class BookActivity extends BaseActivity {

    private String bookId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bookId = getIntent().getStringExtra("bookId");
        if (bookId == null) bookId = "g1up";
    }

    @Override
    protected boolean useBack() { return true; }
    @Override
    protected boolean useBottomNav() { return false; }

    @Override
    protected String titleText() {
        JSONObject meta = Data.findBookMeta(this, bookId);
        if (meta != null) return Data.s(meta, "fullName");
        return bookId;
    }

    @Override
    protected void buildContent(LinearLayout c) {
        JSONObject meta = Data.findBookMeta(this, bookId);
        JSONObject book = Data.book(this, bookId);
        int total = 0, done = 0;
        List<JSONObject> flat = Data.flatLessons(this, bookId);
        for (JSONObject L : flat) {
            total++;
            if (P.isLessonDone(Data.s(L, "id"))) done++;
        }
        float frac = total > 0 ? (float) done / total : 0f;

        LinearLayout info = Ui.card(this, R.color.primary, 14);
        info.setBackground(Ui.make3D(this, R.color.primary, R.color.primaryDark, 14, 4));
        info.addView(Ui.tv(this, "学习进度 " + done + " / " + total, 16, R.color.card, true));
        info.addView(Ui.gap(this, 8));
        info.addView(Ui.progressBar(this, frac, R.color.warning));
        c.addView(info);

        JSONArray units = Data.arr(book, "units");
        boolean prevDone = true;
        for (int u = 0; u < units.length(); u++) {
            JSONObject unit = units.optJSONObject(u);
            if (unit == null) continue;
            c.addView(Ui.section(this, "第 " + Data.i(unit, "number") + " 单元 · " + Data.s(unit, "title")));
            JSONArray lessons = Data.arr(unit, "lessons");
            for (int k = 0; k < lessons.length(); k++) {
                JSONObject L = lessons.optJSONObject(k);
                if (L == null) continue;
                final String id = Data.s(L, "id");
                boolean isDone = P.isLessonDone(id);
                boolean unlocked = (k == 0 && u == 0) || isDone || prevDone;
                c.addView(lessonNode(L, isDone, unlocked));
                if (isDone) prevDone = true;
                else prevDone = unlocked; // 未完成但已解锁则下一节也可点（保持 web 的连续解锁）
            }
        }
    }

    private LinearLayout lessonNode(final JSONObject L, boolean isDone, boolean unlocked) {
        final String id = Data.s(L, "id");
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rlp = Ui.lpMatch();
        rlp.setMargins(0, 0, 0, Ui.dp(this, 10));
        row.setLayoutParams(rlp);

        // 节点圆
        TextView node = new TextView(this);
        int n = Ui.dp(this, 40);
        node.setWidth(n); node.setHeight(n);
        node.setGravity(Gravity.CENTER);
        node.setTextSize(16);
        if (isDone) { node.setText("✓"); node.setTextColor(Ui.color(this, R.color.card));
            node.setBackground(Ui.round(Ui.color(this, R.color.primary), Ui.dp(this, 20))); }
        else if (!unlocked) { node.setText("🔒"); node.setTextSize(14);
            node.setBackground(Ui.round(Ui.color(this, R.color.swan), Ui.dp(this, 20))); }
        else { node.setText(String.valueOf(P.getStars(id) == 0 ? "▶" : "★"));
            node.setTextColor(Ui.color(this, R.color.primary));
            node.setBackground(Ui.roundStroke(Ui.color(this, R.color.card),
                    Ui.color(this, R.color.primary), Ui.dp(this, 2), Ui.dp(this, 20))); }
        LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(n, n);
        nlp.setMargins(0, 0, Ui.dp(this, 12), 0);
        node.setLayoutParams(nlp);
        row.addView(node);

        // 课程卡
        LinearLayout card = Ui.card(this, R.color.card, 12);
        card.setBackground(Ui.roundStroke(Ui.color(this, R.color.card),
                Ui.color(this, R.color.line), Ui.dp(this, 2), Ui.dp(this, 14)));
        card.setLayoutParams(Ui.lpWeight(1f));
        String title = Data.s(L, "title");
        if (title.contains("[object Object]")) title = "本节课";
        card.addView(Ui.tv(this, title, 15, unlocked ? R.color.ink : R.color.hare, true));
        card.addView(Ui.gap(this, 4));
        LinearLayout sub = new LinearLayout(this);
        sub.setOrientation(LinearLayout.HORIZONTAL);
        sub.setGravity(Gravity.CENTER_VERTICAL);
        TextView qc = Ui.tv(this, Data.questionCount(L) + " 题", 13, R.color.inkLight, false);
        sub.addView(qc);
        if (isDone) {
            sub.addView(Ui.gap(this, 10));
            sub.addView(Ui.stars(this, P.getStars(id), 3));
        } else if (!unlocked) {
            sub.addView(Ui.gap(this, 10));
            sub.addView(Ui.tv(this, "未解锁", 13, R.color.hare, false));
        }
        card.addView(sub);

        if (unlocked) {
            card.setClickable(true); card.setFocusable(true);
            card.setOnClickListener(v -> {
                Intent in = new Intent(this, LessonActivity.class);
                in.putExtra("bookId", bookId);
                in.putExtra("lessonId", id);
                startActivity(in);
            });
        }
        row.addView(card);
        return row;
    }
}
