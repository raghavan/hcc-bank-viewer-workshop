"""Categorize: transfers first, then rules, then (optionally) AI for leftovers.

Order matters:
  1. find transfer pairs (money moving between the household's own accounts)
  2. apply rules from a CSV file; the first matching rule wins
  3. whatever is left goes to the review queue, optionally with an AI guess
"""

import csv
import datetime as dt

CATEGORIES = [
    "Income", "Housing", "Groceries", "Dining", "Shopping", "Transport",
    "Utilities", "Health", "Subscriptions", "Other", "Transfer",
]
REVIEW_THRESHOLD = 0.8
TRANSFER_WORDS = ("TRANSFER", "PAYMENT", "AUTOPAY")
TRANSFER_WINDOW_DAYS = 3


def load_rules(path):
    with open(path, newline="", encoding="utf-8") as f:
        rules = []
        for r in csv.DictReader(f):
            if r["category"] not in CATEGORIES:
                raise ValueError(f"Rule {r['rule_id']} uses unknown category {r['category']!r}")
            rules.append({
                "rule_id": r["rule_id"],
                "pattern": r["pattern"].upper(),
                "category": r["category"],
                "confidence": float(r["confidence"]),
            })
        return rules


def blank_decision(row):
    row.update({
        "category": None,
        "category_source": "none",
        "rule_id": None,
        "confidence": None,
        "needs_review": True,
        "is_transfer": False,
    })


def mark_transfers(rows):
    """Pair up money that left one account and arrived in another.

    Both rows must mention a transfer word, live in different accounts, have
    opposite amounts, and be at most 3 days apart.
    """
    def day(r):
        return dt.date.fromisoformat(r["date"])

    candidates = [r for r in rows if any(w in r["description"] for w in TRANSFER_WORDS)]
    outgoing = [r for r in candidates if r["amount_cents"] < 0]
    incoming = [r for r in candidates if r["amount_cents"] > 0]
    pairs = 0
    for out in sorted(outgoing, key=lambda r: r["date"]):
        for inc in sorted(incoming, key=lambda r: r["date"]):
            if inc["is_transfer"] or inc["account"] == out["account"]:
                continue
            if inc["amount_cents"] != -out["amount_cents"]:
                continue
            if abs((day(inc) - day(out)).days) > TRANSFER_WINDOW_DAYS:
                continue
            for r in (out, inc):
                r.update({
                    "category": "Transfer",
                    "category_source": "transfer",
                    "rule_id": "TRANSFER_PAIR",
                    "confidence": 1.0,
                    "needs_review": False,
                    "is_transfer": True,
                })
            pairs += 1
            break
    return pairs


def apply_rules(rows, rules):
    placed = 0
    for row in rows:
        if row["is_transfer"]:
            continue
        for rule in rules:
            if rule["pattern"] in row["description"]:
                row.update({
                    "category": rule["category"],
                    "category_source": "rule",
                    "rule_id": rule["rule_id"],
                    "confidence": rule["confidence"],
                    "needs_review": rule["confidence"] < REVIEW_THRESHOLD,
                })
                placed += 1
                break
    return placed


def apply_decisions(rows, decisions):
    """Apply choices saved from the viewer's review queue (decisions.json)."""
    overrides = decisions.get("overrides", {})
    changed = 0
    for row in rows:
        choice = overrides.get(row["id"])
        if not choice or choice.get("category") not in CATEGORIES:
            continue
        row.update({
            "category": choice["category"],
            "category_source": choice.get("source", "manual"),
            "rule_id": None,
            "confidence": 1.0,
            "needs_review": False,
        })
        changed += 1
    return changed


def rules_from_decisions(decisions):
    extra = []
    for n, r in enumerate(decisions.get("new_rules", []), start=1):
        if r.get("category") in CATEGORIES and r.get("pattern"):
            extra.append({"rule_id": f"YOU{n}", "pattern": r["pattern"].upper(), "category": r["category"], "confidence": 1.0})
    return extra
