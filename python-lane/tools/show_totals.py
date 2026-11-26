"""Print the numbers a correct viewer should show, using the math in CONTRACT.md.

    python python-lane/tools/show_totals.py                       # reads out/transactions.json
    python python-lane/tools/show_totals.py out/python/transactions.json
    python python-lane/tools/show_totals.py --month 2027-02

Use it to check your viewer by eye: the cards and bars should match these numbers.
"""

import argparse
import json
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


def money(c):
    sign = "-" if c < 0 else ""
    return f"{sign}${abs(c) / 100:,.2f}"


def totals(rows):
    months = defaultdict(lambda: {"income": 0, "by_category": defaultdict(int), "transfers": 0})
    for r in rows:
        m = months[r["date"][:7]]
        if r["is_transfer"]:
            if r["amount_cents"] < 0:
                m["transfers"] += -r["amount_cents"]
            continue
        cat = r["category"] or "Other"
        if cat == "Income":
            m["income"] += r["amount_cents"]
        else:
            m["by_category"][cat] += -r["amount_cents"]
    return months


def main():
    p = argparse.ArgumentParser()
    p.add_argument("file", nargs="?", default=str(ROOT / "out" / "transactions.json"))
    p.add_argument("--month", help="YYYY-MM (default: the latest month in the file)")
    args = p.parse_args()

    rows = json.loads(Path(args.file).read_text())["transactions"]
    months = totals(rows)
    month = args.month or max(months)
    m = months[month]
    spending = sum(m["by_category"].values())

    print(f"Month {month}")
    print(f"  Income             {money(m['income']):>12}")
    print(f"  Spending           {money(spending):>12}")
    print(f"  Net saved          {money(m['income'] - spending):>12}")
    print(f"  Transfers excluded {money(m['transfers']):>12}")
    print("Spending by category")
    for cat, c in sorted(m["by_category"].items(), key=lambda kv: -kv[1]):
        print(f"  {cat:<18} {money(c):>12}")
    print("Six month trend (spending / income)")
    for key in sorted(months)[-6:]:
        mm = months[key]
        print(f"  {key}  {money(sum(mm['by_category'].values())):>12} / {money(mm['income']):>12}")


if __name__ == "__main__":
    main()
