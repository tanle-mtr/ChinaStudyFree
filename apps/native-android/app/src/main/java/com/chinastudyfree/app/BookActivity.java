package com.chinastudyfree.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;

import org.json.JSONArray;
import org.json.JSONObject;

/** 教材页：按单元列出所有小课，显示完成状态。 */
public class BookActivity extends Activity {

    private String bookId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bookId = getIntent().getStringExtra("bookId");
        if (bookId == null) bookId = "g1up";
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
            android.util.Log.e("csf-book", "build failed", e);
            Ui.error(this, "教材加载失败：" + e.getMessage());
        }
    }

    private void build() {
        JSONObject meta = Data.findBookMeta(this, bookId);
        JSONObject book = Data.book(this, bookId);
        String name = meta == null ? bookId : Data.s(meta, "fullName");
        String sub = (meta == null ? "" : Data.s(meta, "publisherName") + " · ")
                + Data.arr(book, "units").length() + " 个单元 · "
                + (meta == null ? 0 : Data.i(meta, "lessonsCount")) + " 节小课";

        Ui.Page page = Ui.page(this, name, sub, true);
        LinearLayout c = page.content;

        JSONArray units = Data.arr(book, "units");
        int total = 0, done = 0;
        for (int u = 0; u < units.length(); u++) {
            JSONArray lessons = Data.arr(units.optJSONObject(u), "lessons");
            total += lessons.length();
            for (int k = 0; k < lessons.length(); k++) {
                if (Progress.isDone(this, Data.s(lessons.optJSONObject(k), "id"))) done++;
            }
        }

        LinearLayout info = Ui.card(this, R.color.bluePale, 14);
        info.addView(Ui.tv(this, "学习进度", 15, R.color.ink, true));
        info.addView(Ui.gap(this, 6));
        info.addView(Ui.tv(this, "已完成 " + done + " / " + total + " 节课", 14, R.color.inkLight, false));
        c.addView(info);

        for (int u = 0; u < units.length(); u++) {
            JSONObject unit = units.optJSONObject(u);
            String title = Data.s(unit, "title");
            int num = Data.i(unit, "number");
            c.addView(Ui.section(this, "第 " + num + " 单元 · " + title));
            JSONArray lessons = Data.arr(unit, "lessons");
            for (int k = 0; k < lessons.length(); k++) {
                c.addView(lessonCard(lessons.optJSONObject(k)));
            }
        }
    }

    private LinearLayout lessonCard(final JSONObject L) {
        LinearLayout card = Ui.card(this, R.color.card, 14);
        final String id = Data.s(L, "id");
        boolean done = Progress.isDone(this, id);
        card.addView(Ui.tv(this, (done ? "✓  " : "") + Data.s(L, "title"), 16, R.color.ink, true));
        card.addView(Ui.gap(this, 6));
        card.addView(Ui.tv(this,
                Data.questionCount(L) + " 道题" + (done ? " · 已完成" : " · 点击开始"),
                13, done ? R.color.correct : R.color.inkLight, false));
        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(v -> {
            Intent in = new Intent(this, LessonActivity.class);
            in.putExtra("bookId", bookId);
            in.putExtra("lessonId", id);
            startActivity(in);
        });
        return card;
    }
}
