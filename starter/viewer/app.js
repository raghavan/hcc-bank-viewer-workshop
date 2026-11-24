// My viewer. Plain JavaScript, no frameworks, no build step.
//
// Run:   python tools/serve.py my-work/viewer
// Then open the address it prints. Press F12 and look at the Console tab
// whenever something does not show up: errors land there.
//
// The math rules ("How the viewer adds things up") are in contract/CONTRACT.md.

let rows = [];

// Try a copy next to the page first (that is how the deployed site works, week 4 CD),
// then your own output, then the sample file that ships with the repo.
const SOURCES = ["transactions.json", "/out/transactions.json", "/data/sample_output/transactions.json"];

async function load() {
  for (const url of SOURCES) {
    const res = await fetch(url).catch(() => null);
    if (res && res.ok) {
      rows = (await res.json()).transactions;
      document.getElementById("status").textContent = `Loaded ${rows.length} rows from ${url}`;
      return;
    }
  }
  document.getElementById("status").textContent =
    "No transactions.json found. Run your structurer first (or check the server is running from the repo root).";
}

// Done for you: 165000 -> "$1,650.00", -675 -> "-$6.75"
function money(cents) {
  const text = "$" + (Math.abs(cents) / 100).toLocaleString("en-US", { minimumFractionDigits: 2 });
  return cents < 0 ? "-" + text : text;
}

// ---------------------------------------------------------------- step 1: tabs
function showView(name) {
  for (const section of document.querySelectorAll(".view")) {
    section.hidden = section.id !== name;
  }
}

// ---------------------------------------------------------------- step 2: ledger
function renderLedger() {
  const body = document.getElementById("ledger-body");
  // TODO: make one <tr> per row with date, description, account, money(amount_cents),
  // category and category_source. Start with only the first 20 rows to keep it simple.
  // Then add filters: month, account, category, "hide transfers", a search box.
  body.innerHTML = `<tr><td colspan="6">TODO: ${rows.length} rows are waiting to be shown</td></tr>`;
}

// ---------------------------------------------------------------- step 3: summary
function monthsIn(data) {
  // TODO: the list of "YYYY-MM" values, sorted. Hint: row.date.slice(0, 7) and new Set(...)
  return [];
}

function summarize(data, month) {
  // TODO: for rows in that month, skip transfers (is_transfer), then:
  //   income   = sum of amount_cents where category is "Income"
  //   spending = minus the sum of amount_cents for every other category
  //   net      = income - spending
  // Check your numbers against: python tools/show_totals.py out/transactions.json
  return { income: 0, spending: 0, net: 0, byCategory: {} };
}

function renderSummary() {
  const select = document.getElementById("month-select");
  const months = monthsIn(rows);
  select.innerHTML = months.map((m) => `<option>${m}</option>`).join("");
  const s = summarize(rows, select.value || months[months.length - 1]);
  document.getElementById("cards").innerHTML =
    `<p>Income ${money(s.income)} | Spending ${money(s.spending)} | Net ${money(s.net)}</p>`;
  // TODO: one bar per category in #category-bars, biggest first.
  // Stretch: a six month trend chart (an <svg> with one <rect> per month).
}

// ---------------------------------------------------------------- step 4: review queue
function renderReview() {
  const queue = rows.filter((r) => r.needs_review);
  document.getElementById("review-count").textContent = queue.length;
  // TODO: one card per row with its description, amount, suggested category,
  // confidence, and buttons to approve or change it.
  // Stretch: remember choices with localStorage and offer a decisions.json download
  // (format in contract/CONTRACT.md) so the structurer can learn from you.
  document.getElementById("review-cards").textContent = `TODO: ${queue.length} rows wait for a person`;
}

// ---------------------------------------------------------------- start
document.querySelectorAll("[data-view]").forEach((b) =>
  b.addEventListener("click", () => showView(b.dataset.view)));
document.getElementById("month-select").addEventListener("change", renderSummary);

load().then(() => {
  renderSummary();
  renderLedger();
  renderReview();
});
