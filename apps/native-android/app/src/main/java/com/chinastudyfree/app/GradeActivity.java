package com.chinastudyfree.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 年级页：按学科分组列出该年级的全部教材。web 风格（返回头 + 学科卡片）。 */
public class GradeActivity extends BaseActivity {

    private int grade;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        grade = getIntent().getIntExtra("grade", 1);
    }

    @Override
    protected boolean useBack() { return true; }
    @Override
    protected boolean useBottomNav() { return false; }

    @Override
    protected String titleText() {
        JSONArray gs = Data.arr(Data.index(this), "grades");
        for (int k = 0; k < gs.length(); k++) {
            JSONObject g = gs.optJSONObject(k);
            if (g != null && Data.i(g, "grade") == grade) return Data.s(g, "name");
        }
        return "年级 " + grade;
    }

    @Override
    protected void buildContent(LinearLayout c) {
        List<JSONObject> books = Data.booksOfGrade(this, grade);
        LinearLayout info = Ui.card(this, R.color.secondary, 14);
        info.setBackground(Ui.roundStroke(Ui.color(this, R.color.bluePale),
                Ui.color(this, R.color.secondary), Ui.dp(this, 2), Ui.dp(this, 14)));
        info.addView(Ui.tv(this, "共 " + books.size() + " 本教材", 15, R.color.ink, true));
        info.addView(Ui.gap(this, 4));
        info.addView(Ui.tv(this, "点开教材即可开始学习", 13, R.color.inkLight, false));
        c.addView(info);

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
        final String id = Data.s(b, "id");
        int done = 0;
        for (JSONObject L : Data.flatLessons(this, id)) {
            if (P.isLessonDone(Data.s(L, "id"))) done++;
        }
        int total = Data.i(b, "lessonsCount");
        float frac = total > 0 ? (float) done / total : 0f;

        LinearLayout card = Ui.card(this, R.color.card, 14);
        card.setBackground(Ui.roundStroke(Ui.color(this, R.color.card),
                Ui.color(this, R.color.line), Ui.dp(this, 2), Ui.dp(this, 14)));
        card.addView(Ui.tv(this, Data.s(b, "fullName"), 16, R.color.ink, true));
        card.addView(Ui.gap(this, 6));
        card.addView(Ui.tv(this,
                Data.s(b, "publisherName") + " · " + Data.i(b, "unitsCount") + " 单元 · "
                        + Data.i(b, "questionsCount") + " 题",
                13, R.color.inkLight, false));
        card.addView(Ui.gap(this, 8));

        LinearLayout track = makeBarTrack();
        View fill = makeBarFill(R.color.primary);
        View spacer = makeBarSpacer();
        ((LinearLayout.LayoutParams) fill.getLayoutParams()).weight = frac;
        ((LinearLayout.LayoutParams) spacer.getLayoutParams()).weight = 1f - frac;
        track.addView(fill); track.addView(spacer);
        card.addView(track);
        card.addView(Ui.gap(this, 6));

        TextView prog = Ui.tv(this,
                done > 0 ? ("✓ 已学 " + done + " / " + total + " 节") : ("0 / " + total + " 节"),
                13, done > 0 ? R.color.correct : R.color.inkLight, true);
        card.addView(prog);

        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(v -> {
            Intent in = new Intent(this, BookActivity.class);
            in.putExtra("bookId", id);
            startActivity(in);
        });
        return card;
    }

    protected LinearLayout makeBarTrack() {
        LinearLayout track = new LinearLayout(this);
        track.setOrientation(LinearLayout.HORIZONTAL);
        track.setBackgroundColor(Ui.color(this, R.color.swan));
        int h = Ui.dp(this, 12);
        track.setLayoutParams(Ui.lpMatch());
        track.getLayoutParams().height = h;
        ((android.graphics.drawable.GradientDrawable) track.getBackground()).setCornerRadius(Ui.dp(this, 6));
        return track;
    }
    protected View makeBarFill(int colorRes) {
        View f = new View(this);
        f.setBackgroundColor(Ui.color(this, colorRes));
        f.setLayoutParams(new LinearLayout.LayoutParams(0, android.view.ViewGroup.LayoutParams.MATCH_PARENT, 0f));
        return f;
    }
    protected View makeBarSpacer() {
        View s = new View(this);
        s.setLayoutParams(new LinearLayout.LayoutParams(0, android.view.ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        return s;
    }
}
