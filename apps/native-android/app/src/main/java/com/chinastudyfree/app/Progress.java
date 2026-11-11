package com.chinastudyfree.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 全局进度状态模型（复刻 web 的 store/progress.ts + @cstf/core 经济算法）。
 * 单例，SharedPreferences 持久化（key "csf-native-v3"，独立于旧 web 存档）。
 *
 * 数值与 web / iOS 双端一致（XP/宝石/红心/连胜/每日任务/联赛/宝箱/SRS/成就）。
 */
public class Progress {
    private static final String TAG = "CSF-Progress";
    private static final String PREFS = "csf-native-v3";
    private static final String KEY = "state";

    // ===== 经济常量（与 @cstf/core/economy.ts 一致） =====
    public static final int MAX_HEARTS = 5;
    public static final int HEART_REGEN_SECONDS = 300;
    public static final long HEART_RECHARGE_MS = HEART_REGEN_SECONDS * 1000L;
    public static final int HEART_REFILL_COST = 350;
    public static final int REVIEW_HEART_REWARD = 1;
    public static final int REVIEW_HEART_MIN_CORRECT = 5;
    public static final int FREEZE_COST = 200;
    public static final int MAX_FREEZES = 2;
    public static final int INITIAL_FREEZES = 2;
    public static final int STREAK_MAKEUP_COST = 50;
    public static final double THREE_STAR = 0.95;
    public static final double TWO_STAR = 0.80;
    public static final int XP_PER_CORRECT = 10;
    public static final int PERFECT_XP_BONUS = 5;
    public static final int FIRST_PERFECT_XP_BONUS = 5;
    public static final int LESSON_GEM_BASE = 3;
    public static final int LESSON_GEM_TWO_STAR = 5;
    public static final int LESSON_GEM_THREE_STAR = 10;
    public static final int FIRST_PERFECT_GEM = 15;
    public static final int DAILY_GOAL_BONUS = 20;
    public static final int WEEKEND_XP_MULTIPLIER = 2;
    public static final int EXAM_XP_MULTIPLIER = 2;
    public static final int[] DAILY_GOAL_OPTIONS = {20, 50, 100,200};
    public static final int DEFAULT_DAILY_GOAL = 50;
    public static final int CHEST_EVERY_N_LESSONS = 5;
    public static final int UNLOCK_LESSONS = 10;
    public static final int LEAGUE_BOT_COUNT = 15;
    public static final int LEAGUE_GROUP_SIZE = 16;
    public static final int PROMOTE_ZONE = 5;
    public static final int DEMOTE_ZONE = 5;
    public static final int PROMOTION_BONUS_GEMS = 20;

    public static final int[] STREAK_MILESTONE = {3,7,14,30,60,100};
    public static final int[] STREAK_MILESTONE_GEMS = {30,80,150,300,500,800};
    public static final int[] DAILY_REWARD_TABLE = {5,5,8,12,15,20,25,30};

    public static final String[] LEAGUE_TIERS = {"bronze","silver","gold","sapphire","ruby","diamond"};
    public static final String[] LEAGUE_TIER_NAMES = {"青铜联赛","白银联赛","黄金联赛","蓝宝石联赛","红宝石联赛","钻石联赛"};
    public static final int[][] TIER_RANGES = {
        {40,260},{80,420},{120,600},{160,800},{200,1000},{240,1200}
    };
    public static final int[] RANK_GEM_REWARDS = {0,100,80,60,40,40,0,0,0,0,0,0,0,0,0,0};

    public static final String[] BOT_NAMES = {
        "小猴淘淘","兔子朵朵","熊猫团团","小鹿灵灵","狐狸悠悠","小象壮壮",
        "企鹅冰冰","小猫咪咪","小狗旺旺","松鼠果果","小鸟啾啾","海豚蓝蓝",
        "小马奔奔","刺猬球球","考拉抱抱","小龙腾腾","河马呼呼","小羊咩咩",
        "青蛙呱呱","蜜蜂嗡嗡","猫头鹰慧慧","小熊憨憨","仓鼠豆豆","鹦鹉花花",
        "小鲸鱼泡泡","蜗牛慢慢","小老虎威威","浣熊乐乐","长颈鹿高高","小恐龙吼吼",
        "雪人白白","星星闪闪","月亮弯弯","太阳暖暖","彩虹七七","云朵飘飘"
    };

    public static final int[][] QUEST_POOL = {
        {0,30,10},{0,60,15},{0,100,25},
        {1,1,10},{1,2,20},{1,3,30},
        {2,1,10},{2,3,20},
        {3,1,15},{3,2,25},
    };

    public static final String[][] ACHIEVEMENTS = {
        {"first-lesson","完成第一节课", "20"},
        {"ten-lessons","完成 10 节课", "50"},
        {"fifty-lessons","完成 50 节课", "100"},
        {"xp-100","累计 100 经验", "20"},
        {"xp-1000","累计 1000 经验", "50"},
        {"xp-5000","累计 5000 经验", "100"},
        {"streak-3","连续学习 3 天", "20"},
        {"streak-7","连续学习 7 天", "50"},
        {"streak-30","连续学习 30 天", "100"},
        {"streak-100","连续学习 100 天", "100"},
        {"perfect-1","首次获得 3 星", "20"},
        {"perfect-10","10 次 3 星通关", "50"},
        {"perfect-50","50 次 3 星通关", "100"},
        {"first-cosmetic","首次装扮", "20"},
        {"gem-collector","累计收集 500 宝石", "50"},
        {"first-review","首次复习错题", "20"},
    };

    private static Progress sInstance;
    private final SharedPreferences sp;
    private JSONObject m;

    public static synchronized Progress get(Context ctx) {
        if (sInstance == null) sInstance = new Progress(ctx.getApplicationContext());
        return sInstance;
    }

    private Progress(Context ctx) {
        sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        load();
    }

    private void load() {
        String raw = sp.getString(KEY, null);
        if (raw == null || raw.isEmpty()) {
            m = new JSONObject();
            applyDefaults();
            save();
        } else {
            try { m = new JSONObject(raw); }
            catch (JSONException e) { m = new JSONObject(); applyDefaults(); }
        }
        ensureDateRoll();
    }

    private void applyDefaults() {
        safePut("xp", 0); safePut("streak", 0); safePut("lastActiveDate", "");
        safePut("hearts", MAX_HEARTS); safePut("nextHeartAt", 0L); safePut("lastReviewHeartDate", "");
        safePut("dailyGoal", DEFAULT_DAILY_GOAL); safePut("todayXp", 0); safePut("lastXpDate", "");
        safePut("streakFreezes", INITIAL_FREEZES); safePut("freezesMigrated", true);
        safePut("gems", 0); safePut("lifetimeGems", 0);
        safePut("dailyQuestDate", ""); safePut("dailyLessons", 0); safePut("dailyReviews", 0); safePut("dailyReadings", 0);
        safePut("claimedQuests", new JSONObject()); safePut("lastTimeDate", "");
        safePut("activeBookId", ""); safePut("selectedGrade", 0); safePut("selectedStage", ""); safePut("lastStage", "");
        safePut("leagueSalt", ""); safePut("leagueTier", "bronze"); safePut("leagueWeekKey", "");
        safePut("lastDailyRewardDate", ""); safePut("muted", false); safePut("autoNarrate", true);
        safePut("reviewedEver", false); safePut("purchases", 0);
        safePut("completedLessons", new JSONObject());
        safePut("mistakesBank", new JSONArray());
        safePut("xpHistory", new JSONObject());
        safePut("lessonHistory", new JSONObject());
        safePut("claimedChests", new JSONObject());
        safePut("perfectedLessons", new JSONObject());
        safePut("completedReadings", new JSONObject());
        safePut("unlockedAchievements", new JSONObject());
        safePut("claimedStreakRewards", new JSONObject());
        safePut("subjectVersions", new JSONObject());
        safePut("pendingStreakMilestone", JSONObject.NULL);
        safePut("pendingLeagueResult", JSONObject.NULL);
    }

    private void safePut(String k, Object v) { try { m.put(k, v); } catch (JSONException ignore) {} }

    public synchronized void save() { sp.edit().putString(KEY, m.toString()).apply(); }

    private int gi(String k, int def) { return m.optInt(k, def); }
    private long gl(String k, long def) { return m.optLong(k, def); }
    private String gs(String k, String def) { return m.optString(k, def); }
    private boolean gb(String k, boolean def) { return m.optBoolean(k, def); }
    private JSONObject gj(String k) { JSONObject o = m.optJSONObject(k); return o == null ? new JSONObject() : o; }
    private JSONArray ga(String k) { JSONArray a = m.optJSONArray(k); return a == null ? new JSONArray() : a; }

    public static String dateKey(Date d) {
        Calendar c = Calendar.getInstance();
        if (d != null) c.setTime(d);
        return String.format("%04d-%02d-%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH)+1, c.get(Calendar.DAY_OF_MONTH));
    }
    public String todayStr() { return dateKey(new Date()); }
    public static int daysBetween(String a, String b) {
        if (a == null || a.isEmpty() || b == null || b.isEmpty()) return 1000;
        try {
            String[] pa = a.split("-"), pb = b.split("-");
            long ta = new GregorianCalendar(Integer.parseInt(pa[0]),Integer.parseInt(pa[1])-1,Integer.parseInt(pa[2])).getTimeInMillis();
            long tb = new GregorianCalendar(Integer.parseInt(pb[0]),Integer.parseInt(pb[1])-1,Integer.parseInt(pb[2])).getTimeInMillis();
            return (int) Math.round((tb-ta)/86400000.0);
        } catch (Exception e) { return 1000; }
    }
    private static Date addDays(int n) { Calendar c = Calendar.getInstance(); c.add(Calendar.DAY_OF_MONTH, n); return c.getTime(); }

    private static String nowIso() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ROOT).format(new Date());
    }

    public void ensureDateRoll() {
        String today = todayStr();
        if (!gs("dailyQuestDate","").equals(today)) {
            safePut("dailyQuestDate", today);
            safePut("dailyLessons", 0); safePut("dailyReviews", 0); safePut("dailyReadings", 0);
        }
        refreshHearts();
        settleLeagueIfNeeded();
        save();
    }

    // ===== 红心 =====
    public int getHearts() { return gi("hearts", MAX_HEARTS); }
    public long getNextHeartAt() { return gl("nextHeartAt", 0); }

    public void loseHeart() {
        int h = getHearts(); if (h <= 0) return;
        int nh = h - 1;
        long na = gl("nextHeartAt", 0);
        if (na == 0) na = System.currentTimeMillis() + HEART_RECHARGE_MS;
        safePut("hearts", nh); safePut("nextHeartAt", na); save();
    }
    public void refreshHearts() {
        int h = getHearts(); long na = gl("nextHeartAt", 0);
        if (h >= MAX_HEARTS) { if (na != 0) { safePut("nextHeartAt", 0); save(); } return; }
        if (na == 0) { safePut("nextHeartAt", System.currentTimeMillis() + HEART_RECHARGE_MS); save(); return; }
        long now = System.currentTimeMillis(); boolean changed = false;
        while (na != 0 && now >= na && h < MAX_HEARTS) { h++; changed = true; na = (h < MAX_HEARTS) ? na + HEART_RECHARGE_MS : 0; }
        if (changed) { safePut("hearts", h); safePut("nextHeartAt", na); save(); }
    }
    public void refillHeartsFull() { safePut("hearts", MAX_HEARTS); safePut("nextHeartAt", 0); save(); }
    public void addHeart(int n) {
        if (n <= 0) return; refreshHearts();
        int h = getHearts(); if (h >= MAX_HEARTS) return;
        int next = Math.min(MAX_HEARTS, h + n);
        safePut("hearts", next); if (next >= MAX_HEARTS) safePut("nextHeartAt", 0);
        save();
    }
    public int awardReviewHeart(int correctCount) {
        if (gs("lastReviewHeartDate","").equals(todayStr())) return 0;
        refreshHearts(); if (correctCount < REVIEW_HEART_MIN_CORRECT) return 0;
        int h = getHearts(); if (h >= MAX_HEARTS) return 0;
        int granted = Math.min(REVIEW_HEART_REWARD, MAX_HEARTS - h);
        if (granted <= 0) return 0;
        addHeart(granted); safePut("lastReviewHeartDate", todayStr()); save();
        return granted;
    }
    public boolean buyHeartRefill() {
        refreshHearts(); int h = getHearts(), g = gi("gems",0);
        if (h >= MAX_HEARTS || g < HEART_REFILL_COST) return false;
        safePut("gems", g - HEART_REFILL_COST); safePut("hearts", MAX_HEARTS); safePut("nextHeartAt", 0);
        safePut("purchases", gi("purchases",0)+1); save(); return true;
    }

    // ===== 宝石 =====
    public int getGems() { return gi("gems", 0); }
    public int getLifetimeGems() { return gi("lifetimeGems", 0); }
    public void addGems(int n) { if (n <= 0) return; safePut("gems", gi("gems",0)+n); safePut("lifetimeGems", gi("lifetimeGems",0)+n); save(); }
    public boolean spendGems(int n) { if (n <= 0) return true; if (gi("gems",0) < n) return false; safePut("gems", gi("gems",0)-n); save(); return true; }

    // ===== 护盾 / 连胜 =====
    public int getStreakFreezes() { return gi("streakFreezes", 0); }
    public boolean buyStreakFreeze() {
        int f = getStreakFreezes(), g = gi("gems",0);
        if (f >= MAX_FREEZES || g < FREEZE_COST) return false;
        safePut("gems", g - FREEZE_COST); safePut("streakFreezes", f+1); safePut("purchases", gi("purchases",0)+1); save(); return true;
    }
    public int getStreak() { return gi("streak", 0); }
    public int getXp() { return gi("xp", 0); }

    public int bumpStreakIfNeeded() {
        String today = todayStr(); String last = gs("lastActiveDate","");
        if (last.equals(today)) return gi("streak",0);
        int streak = gi("streak",0), freezes = gi("streakFreezes",0);
        if (last.isEmpty()) { streak = 1; }
        else {
            int gap = daysBetween(last, today);
            if (gap == 1) { streak = streak + 1; if (new Date().getDay() == 1 && freezes < MAX_FREEZES) freezes++; }
            else if (gap > 1) { int missed = gap - 1; if (freezes >= missed) { streak = streak + 1; freezes -= missed; } else streak = 1; }
        }
        safePut("streak", streak); safePut("lastActiveDate", today); safePut("streakFreezes", freezes);
        maybeMilestone(streak); save(); return streak;
    }
    private void maybeMilestone(int streak) {
        for (int i = 0; i < STREAK_MILESTONE.length; i++) {
            if (STREAK_MILESTONE[i] == streak) {
                JSONObject cr = gj("claimedStreakRewards");
                if (!cr.optBoolean(String.valueOf(streak), false)) {
                    int gem = STREAK_MILESTONE_GEMS[i];
                    safePut("gems", gi("gems",0)+gem); safePut("lifetimeGems", gi("lifetimeGems",0)+gem);
                    try { cr.put(String.valueOf(streak), true); } catch (JSONException ignore) {}
                    safePut("claimedStreakRewards", cr);
                    JSONObject pending = new JSONObject();
                    try { pending.put("streak", streak); pending.put("gems", gem); } catch (JSONException ignore) {}
                    safePut("pendingStreakMilestone", pending);
                }
                break;
            }
        }
    }
    public boolean makeUpYesterdayStreak() {
        int g = gi("gems",0);
        if (g < STREAK_MAKEUP_COST) return false;
        String today = todayStr(), last = gs("lastActiveDate","");
        if (last.equals(today)) return false;
        if (daysBetween(last, today) < 2) return false;
        Calendar c = Calendar.getInstance(); c.add(Calendar.DAY_OF_MONTH, -1);
        safePut("gems", g - STREAK_MAKEUP_COST); safePut("lastActiveDate", dateKey(c.getTime())); save(); return true;
    }

    // ===== 每日目标 =====
    public int getDailyGoal() { return gi("dailyGoal", DEFAULT_DAILY_GOAL); }
    public void setDailyGoal(int g) { safePut("dailyGoal", Math.max(10, Math.min(500, g))); save(); }
    public int getTodayXp() { return gs("lastXpDate","").equals(todayStr()) ? gi("todayXp",0) : 0; }

    private void applyXpGain(int xp) {
        String today = todayStr();
        boolean sameDay = gs("lastXpDate","").equals(today);
        int prevToday = sameDay ? gi("todayXp",0) : 0;
        int newToday = prevToday + xp;
        try { JSONObject hist = gj("xpHistory"); hist.put(today, hist.optInt(today,0) + xp); safePut("xpHistory", hist); } catch (JSONException ignore) {}
        safePut("todayXp", newToday); safePut("lastXpDate", today); safePut("xp", gi("xp",0)+xp);
        if (prevToday < getDailyGoal() && newToday >= getDailyGoal() && getDailyGoal() > 0) addGems(DAILY_GOAL_BONUS);
    }

    // ===== 课程结算 =====
    public static class LessonOutcome {
        public int xpGained, gemsGained, stars, streakAfter;
        public boolean dailyGoalReachedNow, isFirstPerfect;
    }

    public LessonOutcome completeLesson(String lessonId, String lessonTitle, int correct, int total, boolean isExam) {
        double accuracy = total > 0 ? (double) correct / total : 0;
        boolean perfect = (correct == total && total > 0);
        int stars = accuracy >= THREE_STAR ? 3 : (accuracy >= TWO_STAR ? 2 : 1);
        boolean isFirstPerfect = (stars == 3) && !gj("perfectedLessons").optBoolean(lessonId, false);
        boolean isWeekend = (new Date().getDay() == 0 || new Date().getDay() == 6);
        int xp = correct * XP_PER_CORRECT;
        if (perfect) xp += PERFECT_XP_BONUS;
        if (isFirstPerfect) xp += FIRST_PERFECT_XP_BONUS;
        if (isExam) xp *= EXAM_XP_MULTIPLIER;
        if (isWeekend) xp *= WEEKEND_XP_MULTIPLIER;

        LessonOutcome o = new LessonOutcome();
        o.xpGained = xp; o.stars = stars; o.isFirstPerfect = isFirstPerfect;

        ensureDateRoll();
        safePut("dailyLessons", gi("dailyLessons",0)+1);
        int prevToday = gs("lastXpDate","").equals(todayStr()) ? gi("todayXp",0) : 0;
        applyXpGain(xp);
        o.dailyGoalReachedNow = (prevToday < getDailyGoal() && getTodayXp() >= getDailyGoal());

        int gems = LESSON_GEM_BASE;
        if (stars == 3) gems += LESSON_GEM_THREE_STAR; else if (stars == 2) gems += LESSON_GEM_TWO_STAR;
        if (isFirstPerfect) gems += FIRST_PERFECT_GEM;
        if (o.dailyGoalReachedNow) gems += DAILY_GOAL_BONUS;
        o.gemsGained = gems;

        bumpStreakIfNeeded();
        o.streakAfter = gi("streak",0);

        JSONObject cl = gj("completedLessons");
        JSONObject prior = cl.optJSONObject(lessonId);
        boolean better = (prior == null) || (stars >= prior.optInt("stars",0));
        if (better) {
            JSONObject r = new JSONObject();
            try { r.put("stars", stars); r.put("accuracy", Math.max(accuracy, prior==null?0:prior.optDouble("accuracy",0))); r.put("completedAt", nowIso()); } catch (JSONException ignore) {}
            try { cl.put(lessonId, r); } catch (JSONException ignore) {}
        }
        safePut("completedLessons", cl);
        if (isFirstPerfect) { JSONObject pf = gj("perfectedLessons"); try { pf.put(lessonId, true); } catch (JSONException ignore) {} safePut("perfectedLessons", pf); }
        try { JSONObject lh = gj("lessonHistory"); lh.put(todayStr(), lh.optInt(todayStr(),0)+1); safePut("lessonHistory", lh); } catch (JSONException ignore) {}
        if (accuracy >= 0.999) clearMistakesForLesson(lessonId);
        save();
        return o;
    }

    public int getStars(String lessonId) { JSONObject r = gj("completedLessons").optJSONObject(lessonId); return r == null ? 0 : r.optInt("stars",0); }
    public boolean isLessonDone(String lessonId) { return gj("completedLessons").has(lessonId); }

    // ===== 错题本 / SRS =====
    public void addMistake(String lessonId, String lessonTitle, JSONObject question) {
        JSONArray arr = ga("mistakesBank"); JSONArray out = new JSONArray();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject e = arr.optJSONObject(i);
            if (e != null && e.optString("lessonId").equals(lessonId)
                    && e.optJSONObject("question") != null
                    && e.optJSONObject("question").optInt("id",-1) == question.optInt("id",-2)) continue;
            out.put(e);
        }
        JSONObject entry = new JSONObject();
        try { entry.put("lessonId", lessonId); entry.put("lessonTitle", lessonTitle); entry.put("question", question);
            entry.put("addedAt", nowIso()); entry.put("box", 1); entry.put("correctCount", 0); entry.put("nextReviewDate", todayStr()); } catch (JSONException ignore) {}
        out.put(entry); safePut("mistakesBank", out); save();
    }
    public void clearMistakesForLesson(String lessonId) {
        JSONArray arr = ga("mistakesBank"), out = new JSONArray();
        for (int i = 0; i < arr.length(); i++) { JSONObject e = arr.optJSONObject(i); if (e == null || !e.optString("lessonId").equals(lessonId)) out.put(e); }
        safePut("mistakesBank", out); save();
    }
    public int getMistakeCount() { return ga("mistakesBank").length(); }

    public boolean reviewMistake(String lessonId, int questionId, boolean correct) {
        JSONArray arr = ga("mistakesBank"), out = new JSONArray(); boolean newlyGrad = false;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject e = arr.optJSONObject(i);
            if (e == null) { out.put(e); continue; }
            if (!e.optString("lessonId").equals(lessonId)) { out.put(e); continue; }
            JSONObject q = e.optJSONObject("question");
            if (q == null || q.optInt("id",-1) != questionId) { out.put(e); continue; }
            boolean wasGrad = e.optBoolean("graduated", false);
            try {
                e.put("lastReviewedAt", nowIso());
                if (!correct) { e.put("box",1); e.put("correctCount",0); e.put("nextReviewDate", todayStr()); e.put("graduated", false); }
                else {
                    int cc = e.optInt("correctCount",0)+1; int box = e.optInt("box",1);
                    if (box == 1) { e.put("box",2); e.put("nextReviewDate", dateKey(addDays(1))); }
                    else if (box == 2) { e.put("box",3); e.put("nextReviewDate", dateKey(addDays(3))); }
                    else { e.put("box",3); e.put("nextReviewDate", dateKey(addDays(7))); }
                    e.put("correctCount", cc);
                    boolean grad = (e.optInt("box",3) >= 3 && cc >= 2); e.put("graduated", grad);
                    if (grad && !wasGrad) newlyGrad = true;
                }
            } catch (JSONException ignore) {}
            out.put(e);
        }
        safePut("mistakesBank", out); save(); return newlyGrad;
    }

    public int awardReviewXP(int correctCount, int reviewedCount) {
        if (correctCount <= 0 && reviewedCount <= 0) return 0;
        ensureDateRoll();
        int xp = Math.max(0, correctCount) * 5;
        safePut("dailyReviews", gi("dailyReviews",0)+Math.max(0,reviewedCount));
        if (!gb("reviewedEver",false)) safePut("reviewedEver", true);
        applyXpGain(xp); save(); bumpStreakIfNeeded(); return xp;
    }

    public List<JSONObject> getDueMistakes() {
        List<JSONObject> due = new ArrayList<>(); String today = todayStr();
        JSONArray arr = ga("mistakesBank");
        for (int i = 0; i < arr.length(); i++) {
            JSONObject e = arr.optJSONObject(i); if (e == null) continue;
            if (isGraduated(e)) continue;
            String nd = e.optString("nextReviewDate","");
            if (nd.isEmpty() || nd.compareTo(today) <= 0) due.add(e);
        }
        Collections.sort(due, new java.util.Comparator<JSONObject>() {
            public int compare(JSONObject a, JSONObject b) { return Integer.compare(a.optInt("box",1), b.optInt("box",1)); }
        });
        return due;
    }
    private boolean isGraduated(JSONObject e) {
        if (e.optBoolean("graduated", false)) return true;
        return e.optInt("box",1) >= 3 && e.optInt("correctCount",0) >= 2;
    }

    // ===== 每日任务 =====
    public static class Quest { public int kind; public int target; public int reward; public String id; public String title; }
    public static final int KIND_EARN_XP=0, KIND_FINISH=1, KIND_REVIEW=2, KIND_READ=3;
    public static String questTitle(int kind, int target) {
        switch (kind) {
            case KIND_EARN_XP: return "获得 " + target + " 点经验";
            case KIND_FINISH: return "完成 " + target + " 节小课";
            case KIND_REVIEW: return "复习 " + target + " 道错题";
            default: return "读完 " + target + " 篇课文或故事";
        }
    }
    public List<Quest> todayQuests() { return dailyQuests(todayStr()); }
    private static int qIdx(BigInteger hash, int count, long salt, BigInteger U64) {
        if (count <= 0) return 0;
        return mix(hash.add(BigInteger.valueOf(salt)).and(U64)).mod(BigInteger.valueOf(count)).intValue();
    }
    private static Quest qPick(List<Quest> pool, BigInteger hash, int kind, long salt, BigInteger U64) {
        List<Quest> opts = new ArrayList<>();
        for (Quest q : pool) if (q.kind == kind) opts.add(q);
        return opts.get(qIdx(hash, opts.size(), salt, U64));
    }
    public static List<Quest> dailyQuests(String dateStr) {
        BigInteger U64 = BigInteger.ONE.shiftLeft(64).subtract(BigInteger.ONE);
        BigInteger hash = mix(djb2Hash(dateStr));
        List<Quest> pool = new ArrayList<>();
        for (int[] q : QUEST_POOL) { Quest x = new Quest(); x.kind=q[0]; x.target=q[1]; x.reward=q[2]; pool.add(x); }
        List<Integer> rest = new ArrayList<>(); rest.add(1); rest.add(2); rest.add(3);
        int fi = qIdx(hash, rest.size(), 0x11L, U64);
        int firstKind = rest.remove(fi);
        int secondKind = rest.get(qIdx(hash, rest.size(), 0x22L, U64));
        Quest q0 = qPick(pool, hash, 0, 0x01L, U64);
        Quest q1 = qPick(pool, hash, firstKind, 0x33L, U64);
        Quest q2 = qPick(pool, hash, secondKind, 0x44L, U64);
        for (Quest q : new Quest[]{q0,q1,q2}) q.id = q.kind+"-"+q.target;
        for (Quest q : new Quest[]{q0,q1,q2}) q.title = questTitle(q.kind, q.target);
        List<Quest> res = new ArrayList<>(); res.add(q0); res.add(q1); res.add(q2);
        return res;
    }
    public int questProgress(int kind) {
        String today = todayStr();
        if (kind == KIND_EARN_XP) return gs("lastXpDate","").equals(today) ? gi("todayXp",0) : 0;
        if (!gs("dailyQuestDate","").equals(today)) return 0;
        switch (kind) { case KIND_FINISH: return gi("dailyLessons",0); case KIND_REVIEW: return gi("dailyReviews",0); default: return gi("dailyReadings",0); }
    }
    public boolean questClaimed(String key) { return gj("claimedQuests").optBoolean(key, false); }
    public boolean claimQuest(Quest q) {
        String today = todayStr(); String key = today + ":" + q.id;
        JSONObject cq = gj("claimedQuests");
        if (cq.optBoolean(key, false)) return false;
        if (questProgress(q.kind) < q.target) return false;
        try { cq.put(key, true); } catch (JSONException ignore) {}
        safePut("claimedQuests", cq); addGems(q.reward); save(); return true;
    }
    public int claimableQuestCount() {
        int n = 0;
        for (Quest q : todayQuests()) if (questProgress(q.kind) >= q.target && !gj("claimedQuests").optBoolean(todayStr()+":"+q.id, false)) n++;
        return n;
    }

    // ===== 每日登录奖励 =====
    public int effectiveStreak() {
        int s = gi("streak",0); String last = gs("lastActiveDate",""); int f = gi("streakFreezes",0);
        if (s <= 0 || last.isEmpty()) return 0;
        int gap = daysBetween(last, todayStr());
        return (gap <= 1 || gap-1 <= f) ? s : 0;
    }
    public int claimDailyReward() {
        String today = todayStr();
        if (gs("lastDailyRewardDate","").equals(today)) return 0;
        int eff = effectiveStreak();
        int idx = Math.min(Math.max(0, eff), DAILY_REWARD_TABLE.length-1);
        int reward = DAILY_REWARD_TABLE[idx];
        safePut("gems", gi("gems",0)+reward); safePut("lifetimeGems", gi("lifetimeGems",0)+reward);
        safePut("lastDailyRewardDate", today); save(); return reward;
    }
    public boolean dailyRewardClaimedToday() { return gs("lastDailyRewardDate","").equals(todayStr()); }

    // ===== 宝箱 =====
    public boolean claimChest(String chestId) {
        JSONObject cc = gj("claimedChests");
        if (cc.optBoolean(chestId, false)) return false;
        try { cc.put(chestId, true); } catch (JSONException ignore) {}
        safePut("claimedChests", cc); save(); return true;
    }
    public boolean chestClaimed(String chestId) { return gj("claimedChests").optBoolean(chestId, false); }
    /** 三档随机宝箱奖励（与 web chestLogic.rollChestReward 同概率 85/10/5） */
    public static class ChestReward { public int gems; public String tier; }
    public static ChestReward rollChestReward(java.util.Random rnd) {
        double roll = rnd.nextDouble(); ChestReward r = new ChestReward();
        if (roll < 0.85) { r.gems = 10 + rnd.nextInt(11); r.tier = "common"; }
        else if (roll < 0.95) { r.gems = 21 + rnd.nextInt(10); r.tier = "rare"; }
        else { r.gems = 40 + rnd.nextInt(11); r.tier = "epic"; }
        return r;
    }

    // ===== 成就 =====
    public Set<String> computeUnlockedAchievements() {
        Set<String> s = new HashSet<>();
        int lessons = gj("completedLessons").length();
        int perfected = gj("perfectedLessons").length();
        int xp = gi("xp",0); int streak = gi("streak",0); int lgem = gi("lifetimeGems",0);
        boolean purchased = gi("purchases",0) > 0; boolean reviewed = gb("reviewedEver",false);
        for (String[] a : ACHIEVEMENTS) {
            String id = a[0]; boolean ok = false;
            switch (id) {
                case "first-lesson": ok = lessons >= 1; break;
                case "ten-lessons": ok = lessons >= 10; break;
                case "fifty-lessons": ok = lessons >= 50; break;
                case "xp-100": ok = xp >= 100; break;
                case "xp-1000": ok = xp >= 1000; break;
                case "xp-5000": ok = xp >= 5000; break;
                case "streak-3": ok = streak >= 3; break;
                case "streak-7": ok = streak >= 7; break;
                case "streak-30": ok = streak >= 30; break;
                case "streak-100": ok = streak >= 100; break;
                case "perfect-1": ok = perfected >= 1; break;
                case "perfect-10": ok = perfected >= 10; break;
                case "perfect-50": ok = perfected >= 50; break;
                case "first-cosmetic": ok = purchased; break;
                case "gem-collector": ok = lgem >= 500; break;
                case "first-review": ok = reviewed; break;
            }
            if (ok) s.add(id);
        }
        return s;
    }
    public List<String> checkAndUnlockAchievements() {
        Set<String> unlockedNow = computeUnlockedAchievements();
        JSONObject ua = gj("unlockedAchievements"); List<String> newly = new ArrayList<>();
        for (String id : unlockedNow) {
            if (!ua.optBoolean(id, false)) {
                try { ua.put(id, true); } catch (JSONException ignore) {}
                for (String[] a : ACHIEVEMENTS) if (a[0].equals(id)) { addGems(Integer.parseInt(a[2])); newly.add(a[1]); }
            }
        }
        safePut("unlockedAchievements", ua);
        if (!newly.isEmpty()) save();
        return newly;
    }
    public int getUnlockedAchievementCount() { return gj("unlockedAchievements").length(); }
    public JSONObject getUnlockedAchievementsObj() { return gj("unlockedAchievements"); }

    // ===== 联赛（确定性） =====
    public boolean leagueUnlocked() { return gj("completedLessons").length() >= UNLOCK_LESSONS; }
    private String leagueSeedStr() {
        String salt = gs("leagueSalt","");
        if (salt.isEmpty()) { salt = Long.toHexString(System.currentTimeMillis()) + "-" + Long.toHexString(new java.util.Random().nextLong()); safePut("leagueSalt", salt); save(); }
        return salt;
    }
    public String getLeagueTier() { return gs("leagueTier", "bronze"); }
    public void setLeagueTier(String t) { safePut("leagueTier", t); save(); }
    private static String weekStartKey(Date d) {
        Calendar c = Calendar.getInstance(); if (d != null) c.setTime(d);
        int dow = c.get(Calendar.DAY_OF_WEEK);
        int back = (dow == Calendar.SUNDAY) ? 6 : (dow - Calendar.MONDAY);
        c.add(Calendar.DAY_OF_MONTH, -back); return dateKey(c.getTime());
    }
    public static String weekKeyFor() { return weekStartKey(new Date()); }
    public static class LeagueRow { public boolean isUser; public String name; public int weekXp; }
    public List<LeagueRow> leagueStandings() {
        String weekKey = gs("leagueWeekKey", weekKeyFor());
        String tier = getLeagueTier(); int tierIdx = 0;
        for (int i = 0; i < LEAGUE_TIERS.length; i++) if (LEAGUE_TIERS[i].equals(tier)) tierIdx = i;
        String salt = leagueSeedStr();
        BigInteger seed = mix(djb2Hash(weekKey + "#" + tier + "#" + salt));
        BigInteger U64 = BigInteger.ONE.shiftLeft(64).subtract(BigInteger.ONE);
        List<String> pool = new ArrayList<>(java.util.Arrays.asList(BOT_NAMES));
        List<LeagueRow> rows = new ArrayList<>();
        java.util.Random rnd = new java.util.Random(seed.longValue() & Long.MAX_VALUE);
        for (int i = 0; i < LEAGUE_BOT_COUNT; i++) {
            int bi = rnd.nextInt(pool.size()); String name = pool.remove(bi);
            int rmin = TIER_RANGES[tierIdx][0], rmax = TIER_RANGES[tierIdx][1];
            int goal = rmin + rnd.nextInt(rmax - rmin + 1);
            LeagueRow r = new LeagueRow(); r.isUser = false; r.name = name; r.weekXp = goal; rows.add(r);
        }
        int userXp = weekXpFromHistory(weekKey);
        LeagueRow me = new LeagueRow(); me.isUser = true; me.name = "你"; me.weekXp = userXp; rows.add(me);
        Collections.sort(rows, new java.util.Comparator<LeagueRow>() {
            public int compare(LeagueRow a, LeagueRow b) { return Integer.compare(b.weekXp, a.weekXp); }
        });
        return rows;
    }
    public int weekXpFromHistory(String weekKey) {
        JSONObject hist = gj("xpHistory"); int sum = 0;
        try {
            java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("yyyy-MM-dd");
            Calendar c = Calendar.getInstance(); c.setTime(fmt.parse(weekKey));
            for (int i = 0; i < 7; i++) { sum += hist.optInt(dateKey(c.getTime()), 0); c.add(Calendar.DAY_OF_MONTH, 1); }
        } catch (Exception e) { return 0; }
        return sum;
    }
    public void settleLeagueIfNeeded() {
        String currentWeek = weekKeyFor(); String wk = gs("leagueWeekKey","");
        if (wk.isEmpty()) { safePut("leagueWeekKey", currentWeek); return; }
        if (wk.equals(currentWeek)) return;
        if (!leagueUnlocked()) { safePut("leagueWeekKey", currentWeek); return; }
        String tier = getLeagueTier();
        List<LeagueRow> rows = leagueStandings();
        int userXp = weekXpFromHistory(wk);
        int rank = 1; for (LeagueRow r : rows) if (r.weekXp > userXp) rank++;
        int rgem = (rank < RANK_GEM_REWARDS.length) ? RANK_GEM_REWARDS[rank] : 0;
        boolean promoted = rank <= PROMOTE_ZONE && !tier.equals("diamond");
        boolean demoted = rank >= LEAGUE_GROUP_SIZE - DEMOTE_ZONE + 1 && !tier.equals("bronze");
        int gems = rgem + (promoted ? PROMOTION_BONUS_GEMS : 0);
        int tierIdx = 0; for (int i=0;i<LEAGUE_TIERS.length;i++) if (LEAGUE_TIERS[i].equals(tier)) tierIdx=i;
        String tierAfter = tier;
        if (promoted && tierIdx < LEAGUE_TIERS.length-1) tierAfter = LEAGUE_TIERS[tierIdx+1];
        else if (demoted && tierIdx > 0) tierAfter = LEAGUE_TIERS[tierIdx-1];
        if (gems > 0) { safePut("gems", gi("gems",0)+gems); safePut("lifetimeGems", gi("lifetimeGems",0)+gems); }
        safePut("leagueWeekKey", currentWeek); safePut("leagueTier", tierAfter);
        JSONObject pr = new JSONObject();
        try { pr.put("weekKey", wk); pr.put("rank", rank); pr.put("tierBefore", tier); pr.put("tierAfter", tierAfter);
            pr.put("promoted", promoted); pr.put("demoted", demoted); pr.put("gems", gems); } catch (JSONException ignore) {}
        safePut("pendingLeagueResult", pr);
    }
    public JSONObject getPendingLeagueResult() { Object o = m.opt("pendingLeagueResult"); return (o instanceof JSONObject && o != JSONObject.NULL) ? (JSONObject) o : null; }
    public void clearPendingLeagueResult() { safePut("pendingLeagueResult", JSONObject.NULL); save(); }
    public JSONObject getPendingStreakMilestone() { Object o = m.opt("pendingStreakMilestone"); return (o instanceof JSONObject && o != JSONObject.NULL) ? (JSONObject) o : null; }
    public void clearPendingStreakMilestone() { safePut("pendingStreakMilestone", JSONObject.NULL); save(); }

    // ===== 选择与偏好 =====
    public int getSelectedGrade() { return gi("selectedGrade",0); }
    public void setSelectedGrade(int g) { safePut("selectedGrade", g); save(); }
    public String getSelectedStage() { return gs("selectedStage",""); }
    public void setSelectedStage(String s) {
        if (s == null || s.isEmpty()) { safePut("selectedStage",""); safePut("lastStage",""); }
        else { safePut("selectedStage", s); safePut("lastStage", s); }
        save();
    }
    public String getLastStage() { String s = gs("lastStage",""); return s.isEmpty() ? gs("selectedStage","") : s; }
    public String getActiveBookId() { return gs("activeBookId",""); }
    public void setActiveBookId(String id) { safePut("activeBookId", id); save(); }
    public boolean isMuted() { return gb("muted", false); }
    public void setMuted(boolean v) { safePut("muted", v); save(); }
    public boolean isAutoNarrate() { return gb("autoNarrate", true); }
    public void setAutoNarrate(boolean v) { safePut("autoNarrate", v); save(); }

    /** 就地重置当前单例。不能把 sInstance 置空，否则已存在的 Activity 仍持有旧实例，
     *  而新页面 get() 会创建新实例，两个实例各自 save() 会互相覆盖导致进度分裂。 */
    public void resetAll() { sp.edit().remove(KEY).apply(); load(); }

    // ===== 确定性哈希（与 web rng.ts 逐位一致） =====
    private static BigInteger djb2Hash(String s) {
        BigInteger hash = BigInteger.valueOf(5381);
        byte[] bytes = s.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        for (byte b : bytes) {
            BigInteger bb = BigInteger.valueOf(b & 0xFF);
            hash = hash.multiply(BigInteger.valueOf(33)).add(bb).and(BigInteger.ONE.shiftLeft(64).subtract(BigInteger.ONE));
        }
        return hash;
    }
    private static BigInteger mix(BigInteger value) {
        BigInteger U64 = BigInteger.ONE.shiftLeft(64).subtract(BigInteger.ONE);
        BigInteger x = value.add(new BigInteger("9e3779b97f4a7c15",16)).and(U64);
        x = x.xor(x.shiftRight(30)).multiply(new BigInteger("bf58476d1ce4e5b9",16)).and(U64);
        x = x.xor(x.shiftRight(27)).multiply(new BigInteger("94d049bb133111eb",16)).and(U64);
        return x.xor(x.shiftRight(31));
    }

    // ===== 派生 / 便捷查询 =====
    public int getCompletedLessonCount() { return gj("completedLessons").length(); }
    public void setActiveLesson(String id, String title) {
        safePut("activeLessonId", id == null ? "" : id);
        safePut("activeLessonTitle", title == null ? "" : title);
        save();
    }
    public String getActiveLessonId() { return gs("activeLessonId", ""); }
    public String getActiveLessonTitle() { return gs("activeLessonTitle", ""); }
    /** 红心回满还需秒数；已满返回 0 */
    public int heartRegenRemaining() {
        if (getHearts() >= MAX_HEARTS) return 0;
        long na = gl("nextHeartAt", 0);
        if (na == 0) return 0;
        long s = (na - System.currentTimeMillis()) / 1000;
        return s > 0 ? (int) s : 0;
    }
    /** 本节课是否为 3 星满分（用于继续学习标记） */
    public boolean isPerfect(String lessonId) { return getStars(lessonId) >= 3; }

    // 兼容性别名（旧版 LessonActivity 调用）
    public int answers() { return gi("answers",0); }
    public int correct() { return gi("correct",0); }
    public void record(boolean ok) { safePut("answers", gi("answers",0)+1); if (ok) safePut("correct", gi("correct",0)+1); save(); }
    public boolean isDone(String lessonId) { return isLessonDone(lessonId); }
    public void markDone(String lessonId) { /* 由 completeLesson 处理，保留用于兼容 */ }
}
