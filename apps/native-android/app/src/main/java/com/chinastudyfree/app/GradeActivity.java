package com.chinastudyfree.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 年级页：按学科分组列出该年级的全部教材。 */
public class GradeActivity extends Activity {

    private int grade;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        grade = getIntent().getIntExtra("grade", 1);
        try {
            build();
        } catch (Exception e) {
            android.util.Log.e("csf-grade", "build failed", e);
            Ui.error(this, "教材列表加载失败：" + e.getMessage());
        }
    }

    private void build() {
        List<JSONObject> books = Data.booksOfGrade(this, grade);
        String gradeName = "年级 " + grade;
        JSONArray gs = Data.arr(Data.index(this), "grades");
        for (int k = 0; k < gs.length(); k++) {
            JSONObject g = gs.optJSONObject(k);
            if (g != null && Data.i(g, "grade") == grade) gradeName = Data.s(g, "name");
        }

        Ui.Page page = Ui.page(this, gradeName,
                "共 " + books.size() + " 本教材 · 点开即可开始学习", true);
        LinearLayout c = page.content;

        // 按学科保序分组
        LinkedHashMap<String, List<JSONObject>> groups = new LinkedHashMap<>();
        for (JSONObject b : books) {
            String key = Data.s(b, "subjectName");
            if (!groups.containsKey(key)) groups.put(key, new ArrayList<JSONObject>());
            groups.get(key).add(b);
        }

        for (Map.Entry<String, List<JSONObject>> e : groups.entrySet()) {
            c.addView(Ui.section(this, e.getKey() + " · " + e.getValue().size() + " 本"));
            for (JSONObject b : e.getValue()) c.addView(bookCard(b));
        }
    }

    private LinearLayout bookCard(final JSONObject b) {
        LinearLayout card = Ui.card(this, R.color.card, 14);
        card.addView(Ui.tv(this, Data.s(b, "fullName"), 16, R.color.ink, true));
        card.addView(Ui.gap(this, 6));
        card.addView(Ui.tv(this,
                Data.s(b, "publisherName") + " · " + Data.i(b, "unitsCount") + " 单元 · "
                        + Data.i(b, "lessonsCount") + " 节课 · " + Data.i(b, "questionsCount") + " 题",
                13, R.color.inkLight, false));

        // 该教材已完成课程数（只读本地进度，不解析整本书 JSON）
        int done = Progress.doneLessonsForBook(this, Data.s(b, "id"));
        if (done > 0) {
            card.addView(Ui.gap(this, 6));
            card.addView(Ui.tv(this, "✓ 已学 " + done + " / " + Data.i(b, "lessonsCount") + " 节",
                    13, R.color.correct, true));
        }

        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(v -> {
            Intent in = new Intent(this, BookActivity.class);
            in.putExtra("bookId", Data.s(b, "id"));
            startActivity(in);
        });
        return card;
    }
}
