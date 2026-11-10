package com.chinastudyfree.app;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/** 学习进度：SharedPreferences 本地保存，完全离线，不碰旧 WebView 版本的存档。 */
public final class Progress {
    private Progress() {}

    private static final String PREF = "csf-native-v3";

    private static SharedPreferences p(Context c) {
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public static int answers(Context c) { return p(c).getInt("answers", 0); }
    public static int correct(Context c) { return p(c).getInt("correct", 0); }

    public static void record(Context c, boolean ok) {
        SharedPreferences.Editor e = p(c).edit();
        e.putInt("answers", answers(c) + 1);
        if (ok) e.putInt("correct", correct(c) + 1);
        e.apply();
    }

    /** getStringSet 返回的是内部集合，必须复制后再改 */
    private static Set<String> doneSet(Context c) {
        Set<String> raw = p(c).getStringSet("doneLessons", null);
        return raw == null ? new HashSet<String>() : new HashSet<String>(raw);
    }

    public static int doneLessons(Context c) { return doneSet(c).size(); }
    public static boolean isDone(Context c, String lessonId) { return doneSet(c).contains(lessonId); }

    /** 某本教材已完成的小课数：只用 id 前缀判断，无需解析整本书 JSON */
    public static int doneLessonsForBook(Context c, String bookId) {
        String prefix = bookId + "-";
        int n = 0;
        for (String id : doneSet(c)) {
            if (id.startsWith(prefix)) n++;
        }
        return n;
    }

    public static void markDone(Context c, String lessonId) {
        Set<String> s = doneSet(c);
        s.add(lessonId);
        p(c).edit().putStringSet("doneLessons", s).apply();
    }

    public static String lastBook(Context c) { return p(c).getString("lastBook", ""); }
    public static String lastLesson(Context c) { return p(c).getString("lastLesson", ""); }
    public static String lastTitle(Context c) { return p(c).getString("lastTitle", ""); }

    public static void touch(Context c, String bookId, String lessonId, String title) {
        p(c).edit()
                .putString("lastBook", bookId)
                .putString("lastLesson", lessonId)
                .putString("lastTitle", title)
                .apply();
    }
}
