package com.chinastudyfree.app;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 从 APK 内置 assets 读取教材数据；纯 org.json，零第三方解析库。 */
public final class Data {
    private Data() {}

    private static JSONObject INDEX;
    private static final Map<String, JSONObject> BOOK_CACHE = new HashMap<>();

    public static synchronized JSONObject index(Context c) {
        if (INDEX == null) INDEX = read(c, "data/index.json");
        return INDEX;
    }

    public static synchronized JSONObject book(Context c, String id) {
        JSONObject b = BOOK_CACHE.get(id);
        if (b == null) {
            b = read(c, "data/books/" + id + ".json");
            BOOK_CACHE.put(id, b);
        }
        return b;
    }

    public static JSONArray arr(JSONObject o, String key) {
        JSONArray a = (o == null) ? null : o.optJSONArray(key);
        return (a == null) ? new JSONArray() : a;
    }

    public static String s(JSONObject o, String key) {
        return (o == null) ? "" : o.optString(key, "");
    }

    public static int i(JSONObject o, String key) {
        return (o == null) ? 0 : o.optInt(key, 0);
    }

    /** 按 id 找教材摘要（index.json 里的那一条） */
    public static JSONObject findBookMeta(Context c, String id) {
        JSONArray books = arr(index(c), "books");
        for (int k = 0; k < books.length(); k++) {
            JSONObject b = books.optJSONObject(k);
            if (b != null && id.equals(s(b, "id"))) return b;
        }
        return null;
    }

    /** 该年级的教材列表 */
    public static List<JSONObject> booksOfGrade(Context c, int grade) {
        List<JSONObject> out = new ArrayList<>();
        JSONArray books = arr(index(c), "books");
        for (int k = 0; k < books.length(); k++) {
            JSONObject b = books.optJSONObject(k);
            if (b != null && i(b, "grade") == grade) out.add(b);
        }
        return out;
    }

    /** 一本书内所有课程的扁平有序列表 */
    public static List<JSONObject> flatLessons(Context c, String bookId) {
        List<JSONObject> out = new ArrayList<>();
        JSONArray units = arr(book(c, bookId), "units");
        for (int u = 0; u < units.length(); u++) {
            JSONArray lessons = arr(units.optJSONObject(u), "lessons");
            for (int k = 0; k < lessons.length(); k++) {
                JSONObject L = lessons.optJSONObject(k);
                if (L != null) out.add(L);
            }
        }
        return out;
    }

    public static JSONObject findLesson(Context c, String bookId, String lessonId) {
        for (JSONObject L : flatLessons(c, bookId)) {
            if (lessonId.equals(s(L, "id"))) return L;
        }
        return null;
    }

    public static int questionCount(JSONObject lesson) {
        return lesson == null ? 0 : arr(lesson, "questions").length();
    }

    private static JSONObject read(Context c, String path) {
        InputStream in = null;
        try {
            in = c.getAssets().open(path);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[16384];
            int n;
            while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
            return new JSONObject(new String(bos.toByteArray(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            android.util.Log.e("csf-data", "read failed: " + path, e);
            return new JSONObject();
        } finally {
            if (in != null) try { in.close(); } catch (Exception ignored) {}
        }
    }
}
