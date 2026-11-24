"""My structurer: turns five messy bank files into one clean transactions.json.

This is YOUR file. It already runs (try it), but most functions are empty.
Each week you fill in a few TODOs, run the program, then run the checker:

    python my-work/python/structure.py
    python tools/check.py --week 1

Week 1: clean_description, to_cents, read_gulfview
Week 2: read_coquina, read_harborline, read_palmetto, read_sunpine
Week 3: mark_transfers, load_rules, apply_rules (and later, optional AI help)

The rules for every field live in contract/CONTRACT.md. Read that first.
Standard library only, so there is nothing to install.
"""

import csv
import datetime as dt
import json
from decimal import Decimal
from pathlib import Path

# This file lives in my-work/python/, so the repo root is two folders up.
ROOT = Path(__file__).resolve().parents[2]
RAW = ROOT / "data" / "raw"
OUT = ROOT / "out" / "transactions.json"


# ===================================================================== week 1
# Small helpers first. Test each one in the Python prompt before moving on:
#   >>> from structure import clean_description
#   >>> clean_description("  payroll   acme tech ")
#   'PAYROLL ACME TECH'

def clean_description(text):
    """Uppercase, trim both ends, and squeeze runs of spaces down to one space."""
    # TODO (week 1): look up str.upper() and str.split() with no arguments.
    # Hint: " ".join(...) puts words back together with single spaces.
    return text


def to_cents(text):
    """Turn money text like '1,650.00' or '$300.00' into whole cents (165000).

    An empty cell means 0.
    """
    # TODO (week 1): remove "$" and "," first, then convert.
    # Why Decimal and not float? Try 0.1 + 0.2 in Python and see what happens.
    # Decimal("1650.00") * 100 is exact; int(...) of that gives whole cents.
    return 0


def parse_date(text, fmt):
    """'03/05/2027' with fmt '%m/%d/%Y' -> '2027-03-05'. Already done for you."""
    return dt.datetime.strptime(text.strip(), fmt).date().isoformat()


def make_row(bank, n, account, date, description, amount_cents):
    """Build one row in the shared shape. n is the data row number, starting at 1."""
    return {
        "id": f"{bank}-{n}",
        "date": date,
        "account": account,
        "description": clean_description(description),
        "amount_cents": amount_cents,
    }


def read_gulfview(path, account):
    """Gulfview CU Checking: Date,Description,Debit,Credit,Balance (MM/DD/YYYY).

    Money out is in Debit, money in is in Credit. Our contract wants ONE signed
    number: negative when money leaves the account.
    """
    rows = []
    # TODO (week 1):
    #   1. open the file (newline="" and encoding="utf-8")
    #   2. loop over csv.DictReader(f) with enumerate(..., start=1) to get n
    #   3. amount = credit cents minus debit cents
    #   4. rows.append(make_row("gulfview", n, account, date, description, amount))
    return rows


# ===================================================================== week 2
# Same idea, four new formats. Open each file in data/raw/ and look before you code.
# The table in contract/CONTRACT.md lists the surprise in each one.

def read_coquina(path, account):
    # TODO (week 2): dates look like "Mar 26, 2027". Which strptime code is "Mar"?
    return []


def read_harborline(path, account):
    # TODO (week 2): a credit card. A purchase is POSITIVE here but money OUT for us.
    # Use "Trans. Date", not "Post Date".
    return []


def read_palmetto(path, account):
    # TODO (week 2): three junk lines come before the header. Amounts are always
    # positive; the Type column (SALE, RETURN, PAYMENT) tells you the sign.
    return []


def read_sunpine(path, account):
    # TODO (week 2): lowercase text, DR means money out, the date has a time on it,
    # and the last line is a comment that starts with #. Skip it.
    return []


READERS = {
    "gulfview": read_gulfview,
    "coquina": read_coquina,
    "harborline": read_harborline,
    "palmetto": read_palmetto,
    "sunpine": read_sunpine,
}


def read_accounts():
    """contract/accounts.csv says which file belongs to which bank. Done for you."""
    with open(ROOT / "contract" / "accounts.csv", newline="", encoding="utf-8") as f:
        return list(csv.DictReader(f))


# ===================================================================== week 3

def blank_decision(row):
    """Every row starts undecided. Done for you, so week 1 and 2 output is complete."""
    row.update({"category": None, "category_source": "none", "rule_id": None,
                "confidence": None, "needs_review": True, "is_transfer": False})


def mark_transfers(rows):
    """Find money moving between YOUR OWN accounts, so it is not counted twice.

    A pair is: a description with TRANSFER, PAYMENT or AUTOPAY in it, two
    different accounts, amounts that are opposite (-40000 and +40000), and dates
    no more than 3 days apart. Mark BOTH rows. Return how many pairs you found.
    """
    # TODO (week 3): for each money-out row, look for a matching money-in row.
    # Each row can be in one pair only. Set is_transfer True, category "Transfer",
    # category_source "transfer", confidence 1.0, needs_review False.
    return 0


def load_rules(path):
    """Read a rules CSV: rule_id,pattern,category,confidence."""
    # TODO (week 3): return a list of dicts. Turn confidence into a float.
    return []


def apply_rules(rows, rules):
    """Give each row the category of the FIRST rule whose pattern is in its description.

    Skip rows that are already transfers. Return how many rows a rule placed.
    needs_review is True when confidence is below 0.8 (see the review rule).
    """
    # TODO (week 3). Then look at what your rules got wrong: is RENT catching
    # something that is not rent?
    return 0


# ===================================================================== main

def main():
    rows = []
    for acct in read_accounts():
        reader = READERS[acct["bank_id"]]
        found = reader(RAW / acct["file"], acct["account_name"])
        print(f"  {acct['bank_id']:<11} {len(found):>4} rows")
        rows.extend(found)

    for r in rows:
        blank_decision(r)

    pairs = mark_transfers(rows)
    print(f"  transfers   {pairs:>4} pairs")

    rules_file = ROOT / "my-work" / "rules.csv"
    if not rules_file.exists():
        rules_file = ROOT / "contract" / "starter_rules.csv"
    placed = apply_rules(rows, load_rules(rules_file))
    print(f"  rules       {placed:>4} rows placed ({rules_file.name})")

    # Week 3: rows nobody could place stay visible as "Other", waiting for a person.
    for r in rows:
        if r["category"] is None:
            r["category"] = "Other"

    OUT.parent.mkdir(parents=True, exist_ok=True)
    data = {
        "schema_version": "1.0",
        "generated_at": dt.datetime.now().replace(microsecond=0).isoformat(),
        "generator": "my-work/python",
        "transactions": sorted(rows, key=lambda r: (r["date"], r["id"])),
    }
    OUT.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")
    print(f"Wrote {len(rows)} rows to {OUT.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
