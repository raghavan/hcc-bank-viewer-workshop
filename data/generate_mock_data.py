"""Generate the FAKE bank exports used in this workshop.

Everything here is invented: the banks, the household, the merchants and the
amounts. Nothing is copied from a real statement.

The five files in data/raw/ deliberately disagree with each other:
different column names, date formats, sign conventions, extra header lines and
messy spacing. Turning them into ONE clean list is the whole point of the
"structure" step.

Run it again any time. With the same seed you always get the same files:

    python data/generate_mock_data.py

It also writes data/answer_key/answer_key.json, the truth the checks compare
your output against, data/answer_key/wrapped_key.json (the Money Wrapped facts
for that truth, computed by data/wrapped_facts.py), and a bonus sixth bank for
the open lab night.
"""

import argparse
import calendar
import csv
import datetime as dt
import json
import random
from pathlib import Path

from wrapped_facts import facts as wrapped_facts

HERE = Path(__file__).resolve().parent

MONTHS = [(2026, 10), (2026, 11), (2026, 12), (2027, 1), (2027, 2), (2027, 3)]

ACCOUNTS = {
    "gulfview": "Gulfview CU Checking",
    "coquina": "Coquina Savings",
    "harborline": "Harborline Visa",
    "palmetto": "Palmetto Mastercard",
    "sunpine": "Sunpine Debit",
    "tarpon": "Tarpon Trust Checking",
}

# Harborline exports its own category column. It is NOT our category list,
# and sometimes it is simply wrong. Students should not trust it.
HARBORLINE_BANK_CATEGORY = {
    "Groceries": "Supermarkets",
    "Dining": "Restaurants",
    "Transport": "Travel/ Transportation",
    "Health": "Merchandise",  # wrong on purpose
    "Subscriptions": "Services",
    "Shopping": "Merchandise",
    "Other": "Other",
    "Transfer": "Payment/Credit",
}


def cents(dollars):
    """Turn 12.5 into 1250. We always store money as whole cents."""
    return int(round(dollars * 100))


class Ledger:
    """Collects true transactions for one account, in date order."""

    def __init__(self):
        self.rows = []

    def add(self, date, description, amount_cents, category, is_transfer=False):
        self.rows.append(
            {
                "date": date,
                "description": description,
                "amount_cents": amount_cents,
                "category": category,
                "is_transfer": is_transfer,
            }
        )

    def sorted_rows(self):
        # stable sort keeps same-day rows in the order they were added
        return sorted(self.rows, key=lambda r: r["date"])


def build_ledgers(rng):
    gv, cq, hb, pm, sp = Ledger(), Ledger(), Ledger(), Ledger(), Ledger()
    prev_harbor_charges = cents(1184.52)  # pretend September balance
    prev_palmetto_charges = cents(642.18)

    for (y, m) in MONTHS:
        d = lambda day: dt.date(y, m, day)
        last_day = calendar.monthrange(y, m)[1]

        # ---------- Gulfview CU Checking (the main account) ----------
        gv.add(d(1), "PALM COURT APARTMENTS RENT", -cents(1650.00), "Housing")
        gv.add(d(13), "PAYROLL ACME TECH", cents(2710.00), "Income")
        gv.add(d(27), "PAYROLL ACME TECH", cents(2710.00), "Income")
        if m == 12:
            gv.add(d(20), "PAYROLL ACME TECH BONUS", cents(750.00), "Income")
        gv.add(d(15), "BAYSIDE UTILITIES", -cents(rng.uniform(95, 135)), "Utilities")
        gv.add(d(18), "TIDEWATER INTERNET", -cents(59.99), "Utilities")
        gv.add(d(21), "WAVELINE MOBILE", -cents(45.00), "Utilities")
        gv.add(d(rng.choice([7, 9, 16])), "ATM WITHDRAWAL #0042", -cents(rng.choice([60, 80, 100])), "Other")

        # transfer checking -> savings (same day on both sides)
        gv.add(d(26), "TRANSFER TO SAVINGS", -cents(300.00), "Transfer", True)
        cq.add(d(26), "TRANSFER FROM CHECKING", cents(300.00), "Transfer", True)

        # transfer checking -> Sunpine debit (lands the next day)
        gv.add(d(5), "TRANSFER TO SUNPINE", -cents(400.00), "Transfer", True)
        sp.add(d(6), "TRANSFER FROM GULFVIEW", cents(400.00), "Transfer", True)

        # credit card payments: checking pays last month's card charges
        gv.add(d(22), "HARBORLINE VISA PAYMENT", -prev_harbor_charges, "Transfer", True)
        hb.add(d(24), "PAYMENT - THANK YOU", prev_harbor_charges, "Transfer", True)
        gv.add(d(23), "PALMETTO MC AUTOPAY", -prev_palmetto_charges, "Transfer", True)
        pm.add(d(24), "PAYMENT", prev_palmetto_charges, "Transfer", True)

        # ---------- Coquina Savings ----------
        cq.add(d(last_day), "INTEREST PAID", cents(rng.uniform(1.10, 2.60)), "Income")

        # ---------- Harborline Visa (card) ----------
        hb_start = len(hb.rows)
        for day in (3, 10, 17, 24):
            hb.add(d(day), "SUNFRESH MARKET #214", -cents(rng.uniform(55, 115)), "Groceries")
        for day in rng.sample(range(2, 28), 4):
            hb.add(d(day), "CORNER BEAN CAFE", -cents(6.75), "Dining")
        if (y, m) == (2027, 3):
            # two identical coffees on the same day: BOTH are real purchases
            hb.add(d(25), "CORNER BEAN CAFE", -cents(6.75), "Dining")
            hb.add(d(25), "CORNER BEAN CAFE", -cents(6.75), "Dining")
        for day in rng.sample(range(2, 28), 2):
            hb.add(d(day), "TACO TIDE", -cents(rng.uniform(12, 28)), "Dining")
        hb.add(d(rng.randint(2, 27)), "BLUE HERON DINER", -cents(rng.uniform(22, 48)), "Dining")
        for day in rng.sample(range(2, 28), 2):
            hb.add(d(day), "RIDEHOP TRIP", -cents(rng.uniform(9, 26)), "Transport")
        hb.add(d(2), "PEAKFIT GYM MEMBERSHIP", -cents(39.00), "Health")
        hb.add(d(8), "TUNEWAVE MUSIC", -cents(10.99), "Subscriptions")

        # ---------- Palmetto Mastercard (card) ----------
        pm_start = len(pm.rows)
        for day in rng.sample(range(2, 28), 2):
            pm.add(d(day), "MEGAMART #33", -cents(rng.uniform(25, 120)), "Shopping")
        pm.add(d(rng.randint(2, 27)), "BOOKNOOK ONLINE", -cents(rng.uniform(12, 40)), "Shopping")
        threadline_count = 3 if m == 12 else 1
        for day in rng.sample(range(2, 28), threadline_count):
            pm.add(d(day), "THREADLINE APPAREL", -cents(rng.uniform(30, 90)), "Shopping")
        pm.add(d(12), "STREAMBOX MONTHLY", -cents(15.99), "Subscriptions")
        pm.add(d(3), "CLOUDLOCKER STORAGE", -cents(2.99), "Subscriptions")
        pm.add(d(rng.randint(2, 27)), "PIZZA ORBIT", -cents(rng.uniform(18, 35)), "Dining")
        if m in (11, 3):
            pm.add(d(22), "BOOKNOOK ONLINE REFUND", cents(24.99), "Shopping")

        # ---------- Sunpine Debit ----------
        for day in rng.sample(range(2, 28), 3):
            sp.add(d(day), "SUNCOAST FUEL #12", -cents(rng.uniform(32, 58)), "Transport")
        for day in rng.sample(range(2, 28), 2):
            sp.add(d(day), "GULF COAST GROCERS", -cents(rng.uniform(20, 70)), "Groceries")
        sp.add(d(rng.randint(2, 27)), "CORAL PHARMACY", -cents(rng.uniform(8, 30)), "Health")
        if m in (10, 12, 2):
            sp.add(d(rng.randint(2, 27)), "SHIPNEST POSTAGE", -cents(rng.uniform(7, 15)), "Other")
        if m in (11, 1, 3):
            sp.add(d(rng.randint(2, 27)), "CORNER BEAN CAFE", -cents(6.75), "Dining")

        # ---------- merchants with NO starter rule (they end up in review) ----------
        unknown = {
            10: [(sp, 10, "MARKET STALL 7", 18.00, "Groceries")],
            11: [(hb, 14, "GREENLEAF NURSERY", 41.20, "Shopping")],
            12: [(pm, 19, "CITY PARKING GARAGE 3", 14.00, "Transport")],
            1: [(hb, 9, "LUMA YOGA STUDIO", 45.00, "Health")],
            2: [(sp, 21, "PADDLE COVE RENTALS", 40.00, "Other")],
            3: [
                (sp, 4, "MARKET STALL 7", 18.00, "Groceries"),
                (hb, 9, "GREENLEAF NURSERY", 62.30, "Shopping"),
                (pm, 11, "CITY PARKING GARAGE 3", 14.00, "Transport"),
                (hb, 18, "LUMA YOGA STUDIO", 45.00, "Health"),
            ],
        }
        for ledger, day, desc, amount, cat in unknown[m]:
            ledger.add(d(day), desc, -cents(amount), cat)

        # what the cards owe next month = this month's net charges
        prev_harbor_charges = -sum(r["amount_cents"] for r in hb.rows[hb_start:] if not r["is_transfer"])
        prev_palmetto_charges = -sum(r["amount_cents"] for r in pm.rows[pm_start:] if not r["is_transfer"])

    return {"gulfview": gv, "coquina": cq, "harborline": hb, "palmetto": pm, "sunpine": sp}


# ---------------------------------------------------------------------------
# Messy writers: one per bank. Each returns the rows IN FILE ORDER so the
# answer key can number them exactly like a student's parser will.
# ---------------------------------------------------------------------------

def messy_spaces(rng, text):
    """Sometimes double a space or add trailing spaces, like real exports do."""
    if rng.random() < 0.3 and " " in text:
        text = text.replace(" ", "  ", 1)
    if rng.random() < 0.3:
        text = text + "   "
    return text


def money_with_commas(c):
    return f"{abs(c) / 100:,.2f}"


def write_gulfview(path, rows, rng):
    # Date,Description,Debit,Credit,Balance   dates MM/DD/YYYY, money out and in in separate columns
    balance = cents(4200.00)
    with open(path, "w", newline="") as f:
        w = csv.writer(f)
        w.writerow(["Date", "Description", "Debit", "Credit", "Balance"])
        for r in rows:
            balance += r["amount_cents"]
            debit = money_with_commas(r["amount_cents"]) if r["amount_cents"] < 0 else ""
            credit = money_with_commas(r["amount_cents"]) if r["amount_cents"] > 0 else ""
            w.writerow([r["date"].strftime("%m/%d/%Y"), messy_spaces(rng, r["description"]), debit, credit, money_with_commas(balance)])
    return rows


def write_coquina(path, rows, rng):
    # Transaction Date,Details,Amount (USD)   "Mar 26, 2027", "$300.00" / "-$25.00", NEWEST FIRST
    rows = list(reversed(rows))
    with open(path, "w", newline="") as f:
        w = csv.writer(f)
        w.writerow(["Transaction Date", "Details", "Amount (USD)"])
        for r in rows:
            sign = "-" if r["amount_cents"] < 0 else ""
            date_text = r["date"].strftime("%b ") + str(r["date"].day) + r["date"].strftime(", %Y")
            w.writerow([date_text, r["description"].title(), f"{sign}${abs(r['amount_cents']) / 100:.2f}"])
    return rows


def write_harborline(path, rows, rng):
    # Trans. Date,Post Date,Description,Amount,Category
    # CARD convention: purchases are POSITIVE, payments and refunds NEGATIVE (the opposite of ours)
    with open(path, "w", newline="") as f:
        w = csv.writer(f)
        w.writerow(["Trans. Date", "Post Date", "Description", "Amount", "Category"])
        for r in rows:
            post = r["date"] + dt.timedelta(days=1 if rng.random() < 0.6 else 2)
            if r["description"] == "CORNER BEAN CAFE" and r["date"] == dt.date(2027, 3, 25):
                post = r["date"] + dt.timedelta(days=1)  # keep the duplicate pair truly identical
            bank_cat = HARBORLINE_BANK_CATEGORY[r["category"]]
            w.writerow([r["date"].isoformat(), post.isoformat(), r["description"], f"{-r['amount_cents'] / 100:.2f}", bank_cat])
    return rows


def write_palmetto(path, rows, rng):
    # Two junk lines and a blank line before the header. Date M/D/YY.
    # Amount is ALWAYS positive; the Type column (SALE, RETURN, PAYMENT) tells you the direction.
    with open(path, "w", newline="") as f:
        f.write("Palmetto Card Services - Account Activity (FICTIONAL SAMPLE)\n")
        f.write("Account ending 0000,Exported for workshop use\n")
        f.write("\n")
        w = csv.writer(f)
        w.writerow(["Date", "Merchant", "Type", "Amount"])
        for r in rows:
            if r["is_transfer"]:
                kind = "PAYMENT"
            elif r["amount_cents"] > 0:
                kind = "RETURN"
            else:
                kind = "SALE"
            date_text = f"{r['date'].month}/{r['date'].day}/{r['date'].strftime('%y')}"
            w.writerow([date_text, r["description"].title(), kind, f"{abs(r['amount_cents']) / 100:.2f}"])
    return rows


def write_sunpine(path, rows, rng):
    # TXN_ID,DATE_TIME,DESC,AMT,DR_CR   ISO date and time, lowercase text, DR = money out, CR = money in
    # It also ends with a comment line that is not a transaction.
    with open(path, "w", newline="") as f:
        w = csv.writer(f)
        w.writerow(["TXN_ID", "DATE_TIME", "DESC", "AMT", "DR_CR"])
        for i, r in enumerate(rows, start=1):
            t = dt.datetime.combine(r["date"], dt.time(rng.randint(7, 21), rng.randint(0, 59)))
            w.writerow([f"SP{900000 + i}", t.isoformat(), messy_spaces(rng, r["description"].lower()), f"{abs(r['amount_cents']) / 100:.2f}", "DR" if r["amount_cents"] < 0 else "CR"])
        f.write("# end of export\n")
    return rows


def write_tarpon(path, rng):
    """Bonus bank for week 5: a JSON export instead of CSV, March 2027 only."""
    items = [
        (dt.date(2027, 3, 2), "SUNFRESH MARKET #88", -cents(47.15), "Groceries"),
        (dt.date(2027, 3, 6), "TACO TIDE", -cents(19.40), "Dining"),
        (dt.date(2027, 3, 10), "SUNCOAST FUEL #3", -cents(41.02), "Transport"),
        (dt.date(2027, 3, 14), "FREELANCE INVOICE 1031", cents(480.00), "Income"),
        (dt.date(2027, 3, 19), "STREAMBOX MONTHLY", -cents(15.99), "Subscriptions"),
        (dt.date(2027, 3, 23), "HILLTOP HARDWARE", -cents(33.80), "Shopping"),
        (dt.date(2027, 3, 29), "CORAL PHARMACY", -cents(12.60), "Health"),
    ]
    export = {
        "bank": "Tarpon Trust (FICTIONAL)",
        "currency": "USD",
        "transactions": [
            {"posted": day.strftime("%Y%m%d"), "memo": desc, "amount": f"{c / 100:.2f}"}
            for (day, desc, c, _cat) in items
        ],
    }
    path.write_text(json.dumps(export, indent=2) + "\n")
    return [
        {"date": day, "description": desc, "amount_cents": c, "category": cat, "is_transfer": False}
        for (day, desc, c, cat) in items
    ]


def key_rows(bank, rows):
    return [
        {
            "id": f"{bank}-{n}",
            "date": r["date"].isoformat(),
            "account": ACCOUNTS[bank],
            "description": " ".join(r["description"].upper().split()),
            "amount_cents": r["amount_cents"],
            "category": r["category"],
            "is_transfer": r["is_transfer"],
        }
        for n, r in enumerate(rows, start=1)
    ]


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--seed", type=int, default=2027)
    parser.add_argument("--out", default=str(HERE), help="folder that will hold raw/, answer_key/ and bonus/")
    args = parser.parse_args()

    out = Path(args.out)
    (out / "raw").mkdir(parents=True, exist_ok=True)
    (out / "answer_key").mkdir(parents=True, exist_ok=True)
    (out / "bonus").mkdir(parents=True, exist_ok=True)

    rng = random.Random(args.seed)
    ledgers = build_ledgers(rng)

    writers = [
        ("gulfview", "gulfview_checking.csv", write_gulfview),
        ("coquina", "coquina_savings.csv", write_coquina),
        ("harborline", "harborline_visa.csv", write_harborline),
        ("palmetto", "palmetto_mastercard.csv", write_palmetto),
        ("sunpine", "sunpine_debit.csv", write_sunpine),
    ]
    answer = []
    for bank, filename, writer in writers:
        file_rows = writer(out / "raw" / filename, ledgers[bank].sorted_rows(), rng)
        answer.extend(key_rows(bank, file_rows))

    key = {
        "note": "Fictional data. The truth the checks compare against. Do not read categories from here in your code.",
        "seed": args.seed,
        "rows": answer,
    }
    (out / "answer_key" / "answer_key.json").write_text(json.dumps(key, indent=1) + "\n")
    wrapped_key = {
        "note": "Money Wrapped facts for the true mock rows. Your Wrapped.facts must match these exactly.",
        "facts": wrapped_facts(answer),
    }
    (out / "answer_key" / "wrapped_key.json").write_text(json.dumps(wrapped_key, indent=1) + "\n")

    bonus_rows = write_tarpon(out / "bonus" / "tarpon_checking.json", rng)
    bonus_key = {"note": "Answer key for the bonus bank (week 5).", "rows": key_rows("tarpon", bonus_rows)}
    (out / "bonus" / "answer_key_bonus.json").write_text(json.dumps(bonus_key, indent=1) + "\n")

    print(f"Wrote {len(answer)} transactions from 5 fictional banks to {out / 'raw'}")
    print(f"Wrote the answer key to {out / 'answer_key' / 'answer_key.json'}")
    print(f"Wrote the bonus bank ({len(bonus_rows)} rows) to {out / 'bonus'}")


if __name__ == "__main__":
    main()
