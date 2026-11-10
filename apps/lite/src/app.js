/* 聪聪学堂 · 全新静态前端 v2 —— 渐进增强脚本
 * 严格 ES5，零依赖。所有内容已写在 HTML 里，本脚本只负责：
 *   答题判分 / 进度记录 / 继续学习 / 课时完成标记。
 * 任何异常都被吞掉，绝不影响页面可读性（老 WebView 白屏防线）。
 */
(function () {
  "use strict";

  var KEY = "csf-lite-v2";

  function readProgress() {
    try {
      var raw = localStorage.getItem(KEY);
      if (!raw) return { v: 2, lessons: {}, last: null };
      var p = JSON.parse(raw);
      if (!p || typeof p !== "object") return { v: 2, lessons: {}, last: null };
      if (!p.lessons || typeof p.lessons !== "object") p.lessons = {};
      return p;
    } catch (e) { return { v: 2, lessons: {}, last: null }; }
  }
  function writeProgress(p) {
    try { localStorage.setItem(KEY, JSON.stringify(p)); } catch (e) {}
  }
  function esc(s) {
    return String(s == null ? "" : s)
      .replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;");
  }
  function norm(s) {
    return String(s == null ? "" : s)
      .replace(/\s+/g, "")
      .replace(/[，]/g, ",")
      .replace(/[（）]/g, function (c) { return c === "（" ? "(" : ")"; })
      .replace(/[。．]$/g, "")
      .toLowerCase();
  }
  function byClass(root, cls) {
    return root.querySelectorAll ? root.querySelectorAll("." + cls) : [];
  }
  function one(root, cls) {
    var l = byClass(root, cls); return l.length ? l[0] : null;
  }
  function addClass(el, c) { if (el && (" " + el.className + " ").indexOf(" " + c + " ") < 0) el.className += " " + c; }
  function removeClass(el, c) {
    if (!el) return;
    el.className = (" " + el.className + " ").replace(" " + c + " ", " ").replace(/^\s+|\s+$/g, "");
  }
  function pathBookId() {
    try {
      var m = location.pathname.match(/\/book\/([^/]+)\//);
      if (m) return decodeURIComponent(m[1]);
    } catch (e) {}
    return "";
  }

  // ── 课程页：答题 ────────────────────────────────────────────────────
  function initLesson() {
    var list = document.querySelector(".q-list");
    if (!list) return;
    var book = list.getAttribute("data-book") || "";
    var lesson = list.getAttribute("data-lesson") || "";
    var total = parseInt(list.getAttribute("data-total"), 10) || 0;
    var qs = byClass(list, "q");
    var answered = 0, correct = 0;

    // 进入课程即记录「继续学习」
    try {
      var p0 = readProgress();
      p0.last = {
        book: book, lesson: lesson,
        title: document.title.replace(/ · 聪聪学堂$/, ""),
        href: location.pathname,
        at: Date.now()
      };
      writeProgress(p0);
    } catch (e) {}

    function feedback(q, ok, msg) {
      var fb = one(q, "q-feedback");
      if (!fb) return;
      fb.innerHTML = msg;
      fb.setAttribute("class", "q-feedback " + (ok ? "ok" : "no"));
      fb.hidden = false;
    }
    function updateBar() {
      var label = document.getElementById("lp-label");
      var fill = document.getElementById("lp-fill");
      if (label) label.textContent = "已完成 " + answered + " / " + total;
      if (fill) fill.style.width = (total ? Math.round(answered / total * 100) : 0) + "%";
    }
    function maybeDone() {
      if (!total || answered < total) return;
      var box = document.getElementById("lesson-done");
      if (box) {
        var sum = document.getElementById("done-summary");
        if (sum) sum.innerHTML = "答对 <b>" + correct + "</b> / " + total + " 题";
        box.hidden = false;
      }
      try {
        var p = readProgress();
        p.lessons[book + "/" + lesson] = { c: correct, t: total, at: Date.now() };
        writeProgress(p);
      } catch (e) {}
    }
    function settle(q, ok, msg) {
      if (q.getAttribute("data-done") === "1") return;
      q.setAttribute("data-done", "1");
      addClass(q, ok ? "correct" : "wrong");
      answered++;
      if (ok) correct++;
      feedback(q, ok, msg);
      var opts = byClass(q, "opt");
      for (var i = 0; i < opts.length; i++) opts[i].disabled = true;
      updateBar();
      maybeDone();
    }
    function revealRight(q) {
      var ans = q.getAttribute("data-answer") || "";
      var opts = byClass(q, "opt");
      for (var i = 0; i < opts.length; i++) {
        if ((opts[i].getAttribute("data-val") || "") === ans) addClass(opts[i], "right");
      }
    }

    for (var i = 0; i < qs.length; i++) {
      (function (q) {
        var mode = q.getAttribute("data-mode");
        var ans = q.getAttribute("data-answer") || "";

        if (mode === "click") {
          var opts = byClass(q, "opt");
          for (var j = 0; j < opts.length; j++) {
            (function (btn) {
              btn.onclick = function () {
                if (q.getAttribute("data-done") === "1") return;
                var val = btn.getAttribute("data-val") || "";
                var ok = val === ans;
                addClass(btn, ok ? "right" : "bad");
                if (!ok) revealRight(q);
                settle(q, ok, ok
                  ? "✓ 答对了！"
                  : "✗ 正确答案：<b>" + esc(ans) + "</b>");
              };
            })(opts[j]);
          }
        } else if (mode === "input") {
          var input = one(q, "fill-input");
          var submit = one(q, "fill-submit");
          function check() {
            if (q.getAttribute("data-done") === "1" || !input) return;
            var val = norm(input.value);
            if (!val) { feedback(q, false, "请先填写答案"); var fb = one(q, "q-feedback"); if (fb) fb.hidden = false; return; }
            var ok = val === norm(ans);
            input.disabled = true;
            if (submit) submit.disabled = true;
            settle(q, ok, ok ? "✓ 答对了！" : "✗ 正确答案：<b>" + esc(ans) + "</b>");
          }
          if (submit) submit.onclick = check;
          if (input) {
            input.onkeydown = function (ev) {
              var k = ev && (ev.keyCode || ev.which);
              if (k === 13) { check(); }
            };
          }
        } else {
          // 自测题（连线 / 连词成句）：点选高亮，不自动判分
          var sOpts = byClass(q, "opt");
          for (var k = 0; k < sOpts.length; k++) {
            (function (btn) {
              btn.onclick = function () {
                if (q.getAttribute("data-done") === "1") return;
                if ((" " + btn.className + " ").indexOf(" picked ") >= 0) removeClass(btn, "picked");
                else addClass(btn, "picked");
              };
            })(sOpts[k]);
          }
          var mark = document.createElement("button");
          mark.type = "button";
          mark.className = "btn btn-ghost self-finish";
          mark.style.marginTop = "10px";
          mark.textContent = "核对答案，完成本题";
          mark.onclick = function () {
            if (q.getAttribute("data-done") === "1") return;
            mark.parentNode.removeChild(mark);
            settle(q, false, "参考答案：<b>" + esc(ans) + "</b>（自测题，请自行核对）");
          };
          q.appendChild(mark);
        }
      })(qs[i]);
    }
    updateBar();
  }

  // ── 教材页：标记已完成的课时 ─────────────────────────────────────────
  function initBook() {
    var rows = document.querySelectorAll(".lesson-row");
    if (!rows || !rows.length) return;
    var book = pathBookId();
    if (!book) return;
    var p = readProgress();
    for (var i = 0; i < rows.length; i++) {
      var key = book + "/" + (rows[i].getAttribute("data-lesson") || "");
      var rec = p.lessons[key];
      var state = one(rows[i], "lesson-state");
      if (rec && state) state.textContent = "✓";
    }
  }

  // ── 首页：继续学习 ───────────────────────────────────────────────────
  function initHome() {
    var slot = document.getElementById("resume-slot");
    if (!slot) return;
    var p = readProgress();
    var last = p.last;
    if (!last || !last.href) return;
    var count = 0, key;
    for (key in p.lessons) { if (Object.prototype.hasOwnProperty.call(p.lessons, key)) count++; }
    var html = '<a class="resume" href="' + esc(last.href) + '">' +
      '<span class="resume-icon">▶</span><span class="resume-txt">' +
      '<b>继续学习</b><span>' + esc(last.title || "上次的小课") +
      (count ? ' · 已完成 ' + count + ' 课' : '') + '</span></span><span>›</span></a>';
    slot.innerHTML = html;
  }

  function boot() {
    try { initHome(); } catch (e) {}
    try { initBook(); } catch (e) {}
    try { initLesson(); } catch (e) {}
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", boot);
  } else {
    boot();
  }
})();
