"""Reference structurer (Python, standard library only).

Reads the five messy bank exports, turns them into one clean list that follows
contract/CONTRACT.md, categorizes it, and writes JSON for the viewer.

Run from the repo root:

    python reference/python/structure.py
    python reference/python/structure.py --ai mock          # fake AI, no key needed
    python reference/python/structure.py --ai claude        # real AI, needs a key
    python reference/python/structure.py --include-bonus    # also read the week 5 bank

Students: try the weekly steps yourself first. Peek here when you are stuck.
"""

import argparse
import datetime as dt
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

import ai_assist  # noqa: E402
import categorize  # noqa: E402
import readers  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]


def main():
    p = argparse.ArgumentParser(description="Structure the fake bank exports into one clean JSON file.")
    p.add_argument("--raw", default=str(ROOT / "data" / "raw"), help="folder with the bank CSV files")
    p.add_argument("--accounts", default=str(ROOT / "contract" / "accounts.csv"))
    p.add_argument("--rules", default=str(ROOT / "reference" / "rules.csv"))
    p.add_argument("--out", default=str(ROOT / "out" / "reference" / "transactions.json"))
    p.add_argument("--ai", default="off", choices=["off", "mock", "claude"], help="AI help for leftovers (default off)")
    p.add_argument("--decisions", default=None, help="decisions.json saved from the viewer's review queue")
    p.add_argument("--include-bonus", action="store_true", help="also read data/bonus/tarpon_checking.json")
    args = p.parse_args()

    print("1. Reading bank files")
    extras = []
    if args.include_bonus:
        extras.append(("tarpon", ROOT / "data" / "bonus" / "tarpon_checking.json", "Tarpon Trust Checking"))
    rows = readers.read_all(args.raw, args.accounts, extras)
    for r in rows:
        categorize.blank_decision(r)

    print("2. Finding transfers between your own accounts")
    pairs = categorize.mark_transfers(rows)
    print(f"  {pairs} transfer pairs")

    print("3. Applying rules")
    rules = categorize.load_rules(args.rules)
    decisions = {}
    if args.decisions:
        decisions = json.loads(Path(args.decisions).read_text(encoding="utf-8"))
        rules = categorize.rules_from_decisions(decisions) + rules
    placed = categorize.apply_rules(rows, rules)
    print(f"  rules placed {placed} rows")

    print(f"4. AI suggestions for leftovers (mode: {args.ai})")
    suggested = ai_assist.suggest(rows, args.ai, Path(args.out).parent / "ai_cache.json")
    print(f"  {suggested} suggestions, all waiting for review")

    # Rows nobody could place: keep them visible as Other, waiting for a person.
    for r in rows:
        if r["category_source"] == "none":
            r["category"] = "Other"

    if decisions:
        changed = categorize.apply_decisions(rows, decisions)
        print(f"5. Applied {changed} decisions from {args.decisions}")

    out = {
        "schema_version": "1.0",
        "generated_at": dt.datetime.now().replace(microsecond=0).isoformat(),
        "generator": "reference/python",
        "transactions": sorted(rows, key=lambda r: (r["date"], r["id"])),
    }
    out_path = Path(args.out)
    out_path.parent.mkdir(parents=True, exist_ok=True)
    out_path.write_text(json.dumps(out, indent=1) + "\n", encoding="utf-8")

    review = sum(1 for r in rows if r["needs_review"])
    print(f"\nWrote {len(rows)} transactions to {out_path}")
    print(f"{review} rows are waiting in the review queue")


if __name__ == "__main__":
    main()
