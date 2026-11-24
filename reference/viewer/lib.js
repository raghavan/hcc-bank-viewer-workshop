// lib.js: the math behind the viewer, with no HTML in it.
// Keeping the math separate means we can test it without a browser
// (see tools/selftest.py, which runs it with Node if Node is installed).
//
// The rules come from "How the viewer adds things up" in contract/CONTRACT.md.

const BankLib = (() => {
  const CATEGORIES = ["Income", "Housing", "Groceries", "Dining", "Shopping", "Transport",
    "Utilities", "Health", "Subscriptions", "Other", "Transfer"];

  // 165000 -> "$1,650.00" ; -675 -> "-$6.75" ; with sign=true, 271000 -> "+$2,710.00"
  function money(cents, sign = false) {
    const abs = Math.abs(cents) / 100;
    const text = "$" + abs.toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    if (cents < 0) return "-" + text;
    return sign && cents > 0 ? "+" + text : text;
  }

  const monthOf = (row) => row.date.slice(0, 7);

  function months(rows) {
    return [...new Set(rows.map(monthOf))].sort();
  }

  function monthLabel(key, short = false) {
    const [y, m] = key.split("-").map(Number);
    const d = new Date(Date.UTC(y, m - 1, 1));
    return d.toLocaleString("en-US", { month: short ? "short" : "long", year: short ? undefined : "numeric", timeZone: "UTC" });
  }

  function dayLabel(date) {
    const [y, m, d] = date.split("-").map(Number);
    return new Date(Date.UTC(y, m - 1, d)).toLocaleString("en-US", { month: "short", day: "2-digit", timeZone: "UTC" });
  }

  // Everything the summary page needs for one month.
  function summarize(rows, month) {
    const inMonth = rows.filter((r) => monthOf(r) === month);
    const byCategory = {};
    let income = 0, deposits = 0, transfersOut = 0, transferRows = 0;
    for (const r of inMonth) {
      if (r.is_transfer) {
        transferRows += 1;
        if (r.amount_cents < 0) transfersOut += -r.amount_cents;
        continue;
      }
      const cat = r.category || "Other";
      if (cat === "Income") {
        income += r.amount_cents;
        if (r.amount_cents >= 100000) deposits += 1; // paychecks, not pennies of interest
      } else {
        byCategory[cat] = (byCategory[cat] || 0) - r.amount_cents;
      }
    }
    const spending = Object.values(byCategory).reduce((a, b) => a + b, 0);
    const categories = Object.entries(byCategory)
      .map(([name, cents]) => ({ name, cents }))
      .sort((a, b) => b.cents - a.cents);
    return { month, income, deposits, spending, net: income - spending, transfersOut, transferPairs: transferRows / 2, categories };
  }

  function trend(rows, lastMonth, count = 6) {
    const all = months(rows).filter((m) => m <= lastMonth).slice(-count);
    return all.map((m) => {
      const s = summarize(rows, m);
      return { month: m, spending: s.spending, income: s.income };
    });
  }

  // "LUMA YOGA STUDIO" -> "LUMA YOGA" ; "CITY PARKING GARAGE 3" -> "CITY PARKING"
  function suggestPattern(description) {
    const words = description.split(" ").filter((w) => !/\d/.test(w) && !w.startsWith("#"));
    return words.slice(0, 2).join(" ") || description;
  }

  // Apply the choices a person made in the review queue (kept in the browser).
  function applyDecisions(rows, decisions) {
    const overrides = (decisions && decisions.overrides) || {};
    return rows.map((r) => {
      const d = overrides[r.id];
      if (!d) return r;
      return { ...r, category: d.category, category_source: d.source, confidence: 1, needs_review: false, rule_id: null };
    });
  }

  function accuracy(rows, answerKey) {
    const decided = rows.filter((r) => !r.needs_review && answerKey[r.id]);
    if (!decided.length) return null;
    const right = decided.filter((r) => r.category === answerKey[r.id].category).length;
    return right / decided.length;
  }

  return { CATEGORIES, money, months, monthLabel, dayLabel, summarize, trend, suggestPattern, applyDecisions, accuracy };
})();

if (typeof module !== "undefined") module.exports = BankLib;
