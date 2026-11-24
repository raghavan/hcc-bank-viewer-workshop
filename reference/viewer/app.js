// app.js: turns transactions.json into the three pages of the viewer.
// Plain JavaScript, no framework, no build step. The math lives in lib.js.

const L = BankLib;
const DECISIONS_KEY = "bank-viewer-decisions";
const state = { raw: [], rows: [], answerKey: null, view: "summary", month: null, selected: null, source: "" };

// ------------------------------------------------------------ small helpers

const $ = (id) => document.getElementById(id);

// Never put text from a file straight into innerHTML: escape it first.
function esc(text) {
  return String(text).replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
}

function loadDecisions() {
  try { return JSON.parse(localStorage.getItem(DECISIONS_KEY)) || { overrides: {}, new_rules: [] }; }
  catch { return { overrides: {}, new_rules: [] }; }
}

function saveDecisions(d) {
  try { localStorage.setItem(DECISIONS_KEY, JSON.stringify(d)); } catch { /* private window: keep in memory only */ }
  state.decisions = d;
}

function options(select, items, current) {
  select.innerHTML = items.map(([value, label]) =>
    `<option value="${esc(value)}"${value === current ? " selected" : ""}>${esc(label)}</option>`).join("");
}

// ------------------------------------------------------------ loading data

async function fetchJson(url) {
  const res = await fetch(url, { cache: "no-store" });
  if (!res.ok) throw new Error(`${url}: ${res.status}`);
  return res.json();
}

async function loadData() {
  const asked = new URLSearchParams(location.search).get("data");
  const candidates = asked ? [asked] : [
    "/out/transactions.json",            // your own structurer
    "/out/reference/transactions.json",  // the Python reference
    "/data/sample_output/transactions.json", // committed sample, always there
  ];
  for (const url of candidates) {
    try {
      const data = await fetchJson(url);
      return useData(data, url);
    } catch { /* try the next one */ }
  }
  $("loading-message").innerHTML =
    "Could not load any transactions.json. Start the viewer with <code>python tools/serve.py</code> " +
    "from the repo root, or use <b>Load another file</b> on the left.";
}

function useData(data, source) {
  if (!data || !Array.isArray(data.transactions)) {
    alert("That file has no transactions list. See contract/CONTRACT.md.");
    return;
  }
  state.raw = data.transactions;
  state.source = source;
  state.decisions = loadDecisions();
  refreshRows();
  const months = L.months(state.rows);
  state.month = months[months.length - 1];
  $("data-source").textContent = source;
  setupFilters();
  show(location.hash.slice(1) || "summary");
  fetchJson("/data/answer_key/answer_key.json")
    .then((key) => { state.answerKey = Object.fromEntries(key.rows.map((r) => [r.id, r])); render(); })
    .catch(() => {});
}

function refreshRows() {
  state.rows = L.applyDecisions(state.raw, state.decisions);
  $("review-count").textContent = state.rows.filter((r) => r.needs_review).length;
}

$("file-input").addEventListener("change", (e) => {
  const file = e.target.files[0];
  if (!file) return;
  file.text().then((text) => useData(JSON.parse(text), file.name)).catch((err) => alert("Could not read that file: " + err));
});

// ------------------------------------------------------------ navigation

function show(view) {
  if (!["summary", "transactions", "review-queue"].includes(view)) view = "summary";
  state.view = view;
  document.querySelectorAll(".view").forEach((s) => { s.hidden = s.id !== view; });
  document.querySelectorAll(".nav").forEach((b) => b.classList.toggle("active", b.dataset.view === view));
  history.replaceState(null, "", "#" + view);
  render();
}

document.querySelectorAll(".nav").forEach((b) => b.addEventListener("click", () => show(b.dataset.view)));

function render() {
  if (!state.rows.length) return;
  if (state.view === "summary") renderSummary();
  if (state.view === "transactions") renderLedger();
  if (state.view === "review-queue") renderReview();
}

// ------------------------------------------------------------ page 1: summary

function renderSummary() {
  const months = L.months(state.rows);
  options($("month-select"), months.map((m) => [m, L.monthLabel(m)]), state.month);
  $("summary-title").textContent = L.monthLabel(state.month);

  const s = L.summarize(state.rows, state.month);
  const prevMonth = months[months.indexOf(state.month) - 1];
  let vsPrev = "first month in the data";
  if (prevMonth) {
    const prev = L.summarize(state.rows, prevMonth).spending;
    const pct = prev ? ((s.spending - prev) / prev) * 100 : 0;
    vsPrev = `${pct >= 0 ? "+" : ""}${pct.toFixed(1)}% vs ${L.monthLabel(prevMonth, true)}`;
  }
  const savedPct = s.income ? Math.round((s.net / s.income) * 100) : 0;
  $("cards").innerHTML = [
    ["Income", L.money(s.income), "", `${s.deposits} deposits`],
    ["Spending", L.money(s.spending), "", vsPrev],
    ["Net saved", L.money(s.net, true), s.net >= 0 ? "good" : "", `${savedPct}% of income`],
    ["Transfers excluded", L.money(s.transfersOut), "quiet", `${s.transferPairs} matched pairs`],
  ].map(([label, value, cls, hint]) =>
    `<div class="card"><div class="label">${label}</div><div class="value ${cls}">${value}</div><div class="hint">${hint}</div></div>`).join("");

  const max = Math.max(1, ...s.categories.map((c) => c.cents));
  $("category-bars").innerHTML = s.categories.map((c) => `
    <div class="cat-row">
      <div class="top"><span>${esc(c.name)}</span><b>${L.money(c.cents)}</b></div>
      <div class="track"><div class="fill" style="width:${Math.max(0, (c.cents / max) * 100)}%"></div></div>
    </div>`).join("") || `<p class="empty">No spending this month.</p>`;

  $("trend-chart").innerHTML = trendSvg(L.trend(state.rows, state.month));
}

function trendSvg(points) {
  const W = 560, H = 300, left = 46, bottom = 34, top = 14;
  const peak = Math.max(1, ...points.flatMap((p) => [p.spending, p.income]));
  const step = 100000 * Math.ceil(peak / 100000 / 6); // gridline every N thousand dollars
  const maxY = step * Math.ceil(peak / step);
  const y = (c) => top + (H - top - bottom) * (1 - c / maxY);
  const slot = (W - left) / points.length;
  const x = (i) => left + slot * i + slot / 2;

  let grid = "";
  for (let v = 0; v <= maxY; v += step) {
    grid += `<line x1="${left}" x2="${W}" y1="${y(v)}" y2="${y(v)}" stroke="#e3e6ec"/>` +
      `<text x="${left - 8}" y="${y(v) + 4}" text-anchor="end" font-size="12" fill="#687082">$${v / 100000}k</text>`;
  }
  const bars = points.map((p, i) => {
    const color = p.month === state.month ? "#2563eb" : "#93c5fd";
    const bw = slot * 0.58;
    return `<rect x="${x(i) - bw / 2}" y="${y(p.spending)}" width="${bw}" height="${y(0) - y(p.spending)}" rx="4" fill="${color}"><title>${L.monthLabel(p.month)}: ${L.money(p.spending)} spent</title></rect>` +
      `<text x="${x(i)}" y="${H - 12}" text-anchor="middle" font-size="13" fill="#687082">${L.monthLabel(p.month, true)}</text>`;
  }).join("");
  const line = points.map((p, i) => `${x(i)},${y(p.income)}`).join(" ");
  const dots = points.map((p, i) => `<circle cx="${x(i)}" cy="${y(p.income)}" r="4" fill="#16a34a"><title>${L.money(p.income)} income</title></circle>`).join("");
  return `<svg viewBox="0 0 ${W} ${H}" role="img" aria-label="Six month trend">${grid}${bars}` +
    `<polyline points="${line}" fill="none" stroke="#16a34a" stroke-width="2.5" stroke-dasharray="7 6"/>${dots}</svg>`;
}

$("month-select").addEventListener("change", (e) => { state.month = e.target.value; renderSummary(); });

// ------------------------------------------------------------ page 2: transactions

function setupFilters() {
  if (!state.filtersReady) {
    ["f-month", "f-account", "f-category", "f-hide-transfers", "f-search"].forEach((id) =>
      $(id).addEventListener("input", renderLedger));
    state.filtersReady = true;
  }
  const months = L.months(state.rows);
  options($("f-month"), [["", "All months"], ...months.map((m) => [m, L.monthLabel(m)])], state.month);
  const accounts = [...new Set(state.rows.map((r) => r.account))].sort();
  options($("f-account"), [["", "All accounts"], ...accounts.map((a) => [a, a])], "");
  options($("f-category"), [["", "All categories"], ["__review", "Needs review"], ...L.CATEGORIES.map((c) => [c, c])], "");
}

function decidedBy(r) {
  if (r.needs_review) return `<span class="pill wait">Waiting in queue</span>`;
  switch (r.category_source) {
    case "transfer": return `<span class="pill rule">Transfer pair found</span>`;
    case "rule": return `<span class="pill rule">Rule ${esc(r.rule_id || "")}</span>`;
    case "ai_approved": return `<span class="pill ai">AI, you approved</span>`;
    case "manual": return `<span class="pill you">You, by hand</span>`;
    default: return `<span class="pill you">${esc(r.category_source)}</span>`;
  }
}

function renderLedger() {
  const month = $("f-month").value, account = $("f-account").value, cat = $("f-category").value;
  const hide = $("f-hide-transfers").checked, q = $("f-search").value.trim().toUpperCase();
  const rows = state.rows.filter((r) =>
    (!month || r.date.startsWith(month)) &&
    (!account || r.account === account) &&
    (!cat || (cat === "__review" ? r.needs_review : r.category === cat)) &&
    (!hide || !r.is_transfer) &&
    (!q || r.description.includes(q)))
    .sort((a, b) => b.date.localeCompare(a.date) || a.id.localeCompare(b.id));

  const seen = new Set();
  $("ledger-body").innerHTML = rows.map((r) => {
    const sig = [r.date, r.account, r.description, r.amount_cents].join("|");
    const dup = seen.has(sig);
    seen.add(sig);
    const note = dup ? `<div class="dup-note">Same as the row above, and both are kept: each has its own id (${esc(r.id)}).</div>` : "";
    return `<tr class="${dup ? "dup" : ""}">
      <td>${L.dayLabel(r.date)}</td>
      <td>${esc(r.description)}${note}</td>
      <td>${esc(r.account)}</td>
      <td class="num ${r.amount_cents > 0 ? "in" : ""}">${L.money(r.amount_cents, true)}</td>
      <td>${r.needs_review ? `<span class="needs">Needs review</span>` : esc(r.category)}</td>
      <td>${decidedBy(r)}</td></tr>`;
  }).join("") || `<tr><td colspan="6" class="empty">No rows match these filters.</td></tr>`;
  $("ledger-count").textContent = `${rows.length} rows`;
}

// ------------------------------------------------------------ page 3: review queue

function renderReview() {
  const queue = state.rows.filter((r) => r.needs_review).sort((a, b) => b.date.localeCompare(a.date));
  const placed = state.rows.filter((r) => r.category_source === "rule" || r.category_source === "transfer").length;
  $("review-sub").textContent =
    `Rules and transfer matching placed ${placed} of ${state.rows.length} rows. Nothing below counts until you approve it.`;
  if (!queue.find((r) => r.id === state.selected)) state.selected = queue[0] ? queue[0].id : null;

  $("review-cards").innerHTML = queue.map((r) => {
    const conf = r.confidence == null ? 0 : r.confidence;
    const color = conf >= 0.8 ? "var(--green)" : conf >= 0.6 ? "var(--amber)" : "var(--red)";
    const who = r.category_source === "ai" ? "AI suggests" : r.category_source === "rule" ? `Rule ${esc(r.rule_id)} suggests` : "No suggestion yet";
    return `<div class="rcard ${r.id === state.selected ? "selected" : ""}" data-id="${esc(r.id)}">
      <div class="desc"><span>${esc(r.description)}</span>${conf < 0.5 ? `<span class="low">Low confidence</span>` : ""}</div>
      <div class="meta">${esc(r.account)} · ${L.dayLabel(r.date)} · ${L.money(r.amount_cents)}</div>
      <div class="suggest"><span class="sub" style="margin:0">${who}</span><span class="chip">${esc(r.category)}</span>
        <span class="conf">Confidence <span class="track"><span class="fill" style="display:block;width:${conf * 100}%;background:${color}"></span></span>
        <b style="color:${color}">${Math.round(conf * 100)}%</b></span></div>
      <div class="row-buttons">
        <button class="btn primary" data-act="approve">Approve</button>
        <select data-act="change" aria-label="Change category">${[["", "Change"], ...L.CATEGORIES.filter((c) => c !== "Transfer").map((c) => [c, c])]
          .map(([v, l]) => `<option value="${v}">${l}</option>`).join("")}</select>
        <button class="btn" data-act="reject">Reject</button>
        <label class="check"><input type="checkbox" data-act="rule"> Also create a rule</label>
      </div></div>`;
  }).join("") || `<div class="panel empty">The queue is empty. Every row has a category you trust.</div>`;

  renderWhy(queue.find((r) => r.id === state.selected));

  const d = state.decisions;
  const approved = Object.values(d.overrides).filter((o) => o.source === "ai_approved").length;
  const byHand = Object.values(d.overrides).filter((o) => o.source === "manual").length;
  $("review-stats").innerHTML =
    `<dt>Placed by rules and transfers</dt><dd>${placed}</dd>` +
    `<dt>Approved suggestions</dt><dd>${approved}</dd>` +
    `<dt>Set by hand</dt><dd>${byHand}</dd>` +
    `<dt>New rules</dt><dd>${d.new_rules.length}</dd>` +
    `<dt>Waiting for review</dt><dd>${queue.length}</dd>`;
}

function renderWhy(r) {
  if (!r) { $("why-panel").innerHTML = `<h2>Why this suggestion</h2><p class="sub">Nothing to review.</p>`; return; }
  let why;
  if (r.category_source === "ai") why = esc(r.ai_reason || "The AI gave no reason.");
  else if (r.category_source === "rule") why = `Rule ${esc(r.rule_id)} matched, but it is a broad rule with confidence ${r.confidence}, so a person should confirm.`;
  else why = "No rule matched this description. Pick a category, and tick “Also create a rule” so next month it is placed automatically.";
  const pattern = L.suggestPattern(r.description);
  const acc = state.answerKey ? L.accuracy(state.rows, state.answerKey) : null;
  $("why-panel").innerHTML = `<h2>Why this suggestion</h2>
    <div class="eyebrow">${esc(r.description)}</div><p>${why}</p>
    <h2>New rule preview</h2>
    <div class="rule-box">If description contains <b>${esc(pattern)}</b><br>then category is <b>${esc(r.category)}</b></div>
    <p class="sub">Approving with “Also create a rule” adds this rule, so next run this merchant is placed without asking.</p>
    ${acc == null ? "" : `<div class="accuracy">Accuracy vs answer key: ${Math.round(acc * 100)}%</div>`}`;
}

$("review-cards").addEventListener("click", (e) => {
  const card = e.target.closest(".rcard");
  if (!card) return;
  const act = e.target.dataset.act;
  if (!act || act === "change" || act === "rule") {
    if (state.selected !== card.dataset.id) { state.selected = card.dataset.id; renderReview(); }
    return;
  }
  decide(card, act);
});

$("review-cards").addEventListener("change", (e) => {
  if (e.target.dataset.act === "change" && e.target.value) decide(e.target.closest(".rcard"), "change", e.target.value);
});

function decide(card, act, newCategory) {
  const row = state.rows.find((r) => r.id === card.dataset.id);
  const d = { overrides: { ...state.decisions.overrides }, new_rules: [...state.decisions.new_rules] };
  let category = row.category, source = row.category_source === "ai" ? "ai_approved" : "manual";
  if (act === "change") { category = newCategory; source = "manual"; }
  if (act === "reject") { category = "Other"; source = "manual"; }
  d.overrides[row.id] = { category, source };
  if (card.querySelector('[data-act="rule"]').checked && act !== "reject") {
    d.new_rules.push({ pattern: L.suggestPattern(row.description), category });
  }
  saveDecisions(d);
  refreshRows();
  renderReview();
}

$("download-decisions").addEventListener("click", () => {
  const blob = new Blob([JSON.stringify(state.decisions, null, 2)], { type: "application/json" });
  const a = Object.assign(document.createElement("a"), { href: URL.createObjectURL(blob), download: "decisions.json" });
  a.click();
  URL.revokeObjectURL(a.href);
});

$("clear-decisions").addEventListener("click", () => {
  if (!confirm("Clear every decision saved in this browser?")) return;
  saveDecisions({ overrides: {}, new_rules: [] });
  refreshRows();
  renderReview();
});

loadData();
