"""Readers: one small function per bank format.

Every reader returns a list of plain dictionaries with the SAME shape:

    {"id": "gulfview-1", "date": "2026-10-01", "account": "...",
     "description": "PALM COURT APARTMENTS RENT", "amount_cents": -165000}

That shared shape is the heart of the "structure" step. Once every bank looks the
same, the rest of the program never has to care which bank a row came from.
"""

import csv
import datetime as dt
import json
from decimal import Decimal
from pathlib import Path


# ---------------------------------------------------------------- helpers

def clean_description(text):
    """Uppercase, trim the ends, squeeze runs of spaces to one."""
    return " ".join(text.upper().split())


def to_cents(text):
    """'$1,650.00' -> 165000 and '-$25.00' -> -2500.

    Decimal (not float) so that the cents are always exact.
    """
    text = text.strip().replace("$", "").replace(",", "")
    if text == "":
        return 0
    return int((Decimal(text) * 100).to_integral_value())


def parse_date(text, fmt):
    return dt.datetime.strptime(text.strip(), fmt).date().isoformat()


def make_row(bank, n, account, date, description, amount_cents):
    return {
        "id": f"{bank}-{n}",
        "date": date,
        "account": account,
        "description": clean_description(description),
        "amount_cents": amount_cents,
    }


def data_lines(path, skip_lines=0):
    """Open a CSV and drop junk: leading lines, blank lines and # comments."""
    with open(path, newline="", encoding="utf-8") as f:
        lines = f.read().splitlines()[skip_lines:]
    return [line for line in lines if line.strip() and not line.lstrip().startswith("#")]


# ---------------------------------------------------------------- one reader per bank

def read_gulfview(path, account):
    # Date,Description,Debit,Credit,Balance
    rows = []
    for n, r in enumerate(csv.DictReader(data_lines(path)), start=1):
        amount = to_cents(r["Credit"]) - to_cents(r["Debit"])
        rows.append(make_row("gulfview", n, account, parse_date(r["Date"], "%m/%d/%Y"), r["Description"], amount))
    return rows


def read_coquina(path, account):
    # Transaction Date,Details,Amount (USD)   e.g. "Mar 26, 2027", "-$25.00"
    rows = []
    for n, r in enumerate(csv.DictReader(data_lines(path)), start=1):
        rows.append(make_row("coquina", n, account, parse_date(r["Transaction Date"], "%b %d, %Y"), r["Details"], to_cents(r["Amount (USD)"])))
    return rows


def read_harborline(path, account):
    # Trans. Date,Post Date,Description,Amount,Category
    # Card convention is the opposite of ours, so flip the sign.
    rows = []
    for n, r in enumerate(csv.DictReader(data_lines(path)), start=1):
        rows.append(make_row("harborline", n, account, parse_date(r["Trans. Date"], "%Y-%m-%d"), r["Description"], -to_cents(r["Amount"])))
    return rows


def read_palmetto(path, account):
    # 3 junk lines, then Date,Merchant,Type,Amount. Amount is always positive.
    rows = []
    for n, r in enumerate(csv.DictReader(data_lines(path, skip_lines=3)), start=1):
        amount = to_cents(r["Amount"])
        if r["Type"].strip().upper() == "SALE":
            amount = -amount  # a purchase is money out
        rows.append(make_row("palmetto", n, account, parse_date(r["Date"], "%m/%d/%y"), r["Merchant"], amount))
    return rows


def read_sunpine(path, account):
    # TXN_ID,DATE_TIME,DESC,AMT,DR_CR
    rows = []
    for n, r in enumerate(csv.DictReader(data_lines(path)), start=1):
        amount = to_cents(r["AMT"])
        if r["DR_CR"].strip().upper() == "DR":
            amount = -amount
        date = r["DATE_TIME"].strip()[:10]  # keep YYYY-MM-DD, drop the time
        rows.append(make_row("sunpine", n, account, date, r["DESC"], amount))
    return rows


def read_tarpon(path, account):
    # Bonus bank (week 5): JSON, dates like 20270305, amounts already signed our way.
    data = json.loads(Path(path).read_text(encoding="utf-8"))
    rows = []
    for n, t in enumerate(data["transactions"], start=1):
        rows.append(make_row("tarpon", n, account, parse_date(t["posted"], "%Y%m%d"), t["memo"], to_cents(t["amount"])))
    return rows


READERS = {
    "gulfview": read_gulfview,
    "coquina": read_coquina,
    "harborline": read_harborline,
    "palmetto": read_palmetto,
    "sunpine": read_sunpine,
    "tarpon": read_tarpon,
}


def read_accounts(accounts_csv):
    with open(accounts_csv, newline="", encoding="utf-8") as f:
        return list(csv.DictReader(f))


def read_all(raw_dir, accounts_csv, extra_files=()):
    """Read every bank listed in accounts.csv, plus optional (bank, path, account) extras."""
    rows = []
    for acct in read_accounts(accounts_csv):
        path = Path(raw_dir) / acct["file"]
        if not path.exists():
            print(f"  skipping {acct['file']} (not found)")
            continue
        found = READERS[acct["bank_id"]](path, acct["account_name"])
        print(f"  {acct['file']:<26} {len(found):>4} rows")
        rows.extend(found)
    for bank, path, account in extra_files:
        found = READERS[bank](path, account)
        print(f"  {Path(path).name:<26} {len(found):>4} rows (bonus)")
        rows.extend(found)
    return rows
