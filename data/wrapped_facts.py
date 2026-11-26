"""Money Wrapped facts, computed in plain Python as an independent answer key.

The Java build (src/main/java/moneywrapped/Wrapped.java) must get exactly these
numbers. data/generate_mock_data.py calls facts() on the true rows and writes
data/answer_key/wrapped_key.json. The rules for every fact are in contract/CONTRACT.md.
"""

import calendar
import datetime as dt
import re


def pct(num, den):
    """Whole percent, rounded half up: (200*num + den) // (2*den)."""
    return 0 if den == 0 else (200 * num + den) // (2 * den)


def money(cents):
    text = f"${abs(cents) // 100:,}.{abs(cents) % 100:02d}"
    return "-" + text if cents < 0 else text


def merchant(description):
    words = " ".join(description.upper().split()).split(" ")
    while words and (words[-1].startswith("#") or re.search(r"\d", words[-1])):
        words.pop()
    if len(words) > 1 and words[-1] == "REFUND":
        words.pop()
    return " ".join(words) if words else description


def month_name(ym):
    y, m = ym.split("-")
    return f"{calendar.month_name[int(m)]} {y}"


def day_name(iso):
    d = dt.date.fromisoformat(iso)
    return f"{calendar.month_name[d.month]} {d.day}, {d.year}"


def fact(fid, label, text, values, data):
    return {"id": fid, "label": label, "text": text, "values": values, "data": data}


def facts(rows):
    spending = [r for r in rows if not r["is_transfer"] and r["category"] != "Income"]
    money_out_rows = [r for r in spending if r["amount_cents"] < 0]
    everyday = [r for r in spending if r["category"] != "Housing"]
    months = sorted({r["date"][:7] for r in rows})
    out = []

    out.append(fact("period", "The period",
                    f"{len(months)} months of money, {month_name(months[0])} to {month_name(months[-1])}",
                    [str(len(months)), month_name(months[0]), month_name(months[-1])],
                    {"first_month": months[0], "last_month": months[-1], "months": len(months)}))

    money_in = sum(r["amount_cents"] for r in rows if not r["is_transfer"] and r["category"] == "Income")
    money_out = -sum(r["amount_cents"] for r in spending)
    out.append(fact("money_in", "Money in", f"Money in: {money(money_in)}", [money(money_in)], {"cents": money_in}))
    out.append(fact("money_out", "Money out", f"Money out, not counting transfers: {money(money_out)}",
                    [money(money_out)], {"cents": money_out}))
    saved = money_in - money_out
    rate = pct(saved, money_in)
    out.append(fact("savings_rate", "Savings rate", f"You kept {rate}% of what came in: {money(saved)}",
                    [f"{rate}%", money(saved)], {"pct": rate, "saved_cents": saved}))

    by_cat = {}
    for r in everyday:
        by_cat[r["category"]] = by_cat.get(r["category"], 0) - r["amount_cents"]
    if by_cat:
        name, cents = sorted(by_cat.items(), key=lambda kv: (-kv[1], kv[0]))[0]
        share = pct(cents, money_out)
        out.append(fact("top_category", "Top everyday category",
                        f"Your top everyday category: {name}, {money(cents)} ({share}% of money out)",
                        [money(cents), f"{share}%"], {"name": name, "cents": cents, "pct_of_out": share}))

    by_merchant = {}
    for r in everyday:
        m = by_merchant.setdefault(merchant(r["description"]), {"cents": 0, "visits": 0})
        m["cents"] -= r["amount_cents"]
        if r["amount_cents"] < 0:
            m["visits"] += 1
    if by_merchant:
        name, m = sorted(by_merchant.items(), key=lambda kv: (-kv[1]["cents"], kv[0]))[0]
        out.append(fact("top_merchant", "Top merchant",
                        f"Where most of your everyday money went: {name}, {money(m['cents'])} over {m['visits']} visits",
                        [money(m["cents"]), str(m["visits"])], {"name": name, "cents": m["cents"], "visits": m["visits"]}))
        name, m = sorted(by_merchant.items(), key=lambda kv: (-kv[1]["visits"], -kv[1]["cents"], kv[0]))[0]
        out.append(fact("most_visited", "Most visited",
                        f"Your most visited place: {name}, {m['visits']} visits and {money(m['cents'])}",
                        [str(m["visits"]), money(m["cents"])], {"name": name, "visits": m["visits"], "cents": m["cents"]}))

    by_month = {}
    for r in spending:
        by_month[r["date"][:7]] = by_month.get(r["date"][:7], 0) - r["amount_cents"]
    if by_month:
        month, cents = sorted(by_month.items(), key=lambda kv: (-kv[1], kv[0]))[0]
        out.append(fact("biggest_month", "Biggest month",
                        f"Your biggest spending month: {month_name(month)}, {money(cents)}",
                        [month_name(month), money(cents)], {"month": month, "cents": cents}))

    weekend = -sum(r["amount_cents"] for r in spending if dt.date.fromisoformat(r["date"]).weekday() >= 5)
    share = pct(weekend, money_out)
    out.append(fact("weekend_share", "Weekend spending",
                    f"{share}% of your money out went on Saturdays and Sundays: {money(weekend)}",
                    [f"{share}%", money(weekend)], {"pct": share, "cents": weekend}))

    purchases = [r for r in money_out_rows if r["category"] not in ("Housing", "Utilities")]
    if purchases:
        r = sorted(purchases, key=lambda r: (r["amount_cents"], r["date"], r["id"]))[0]
        out.append(fact("biggest_purchase", "Biggest single purchase",
                        f"Your biggest single purchase: {r['description']}, {money(-r['amount_cents'])} on {day_name(r['date'])}",
                        [money(-r["amount_cents"]), day_name(r["date"])],
                        {"description": r["description"], "date": r["date"], "cents": -r["amount_cents"]}))

    groups = {}
    for r in money_out_rows:
        groups.setdefault(merchant(r["description"]), []).append(r)
    items = []
    for name, rs in groups.items():
        charges = sorted(-r["amount_cents"] for r in rs)
        months_seen = {r["date"][:7] for r in rs}
        median = charges[(len(charges) - 1) // 2]
        if len(months_seen) >= 3 and len(rs) == len(months_seen) and all(abs(c - median) * 10 <= median for c in charges):
            items.append({"merchant": name, "monthly_cents": median, "months": len(months_seen)})
    items.sort(key=lambda i: (-i["monthly_cents"], i["merchant"]))
    if items:
        total = sum(i["monthly_cents"] for i in items)
        listing = ", ".join(f"{i['merchant']} {money(i['monthly_cents'])}" for i in items)
        out.append(fact("recurring", "Repeat charges",
                        f"{len(items)} charges repeat every month, {money(total)} a month in total: {listing}",
                        [str(len(items)), money(total)] + [money(i["monthly_cents"]) for i in items],
                        {"count": len(items), "total_monthly_cents": total, "items": items}))
        q = items[-1]
        out.append(fact("quiet_subscription", "Quietest repeat charge",
                        f"Your smallest repeat charge: {q['merchant']} at {money(q['monthly_cents'])} a month, "
                        f"{money(12 * q['monthly_cents'])} a year",
                        [money(q["monthly_cents"]), money(12 * q["monthly_cents"])],
                        {"merchant": q["merchant"], "monthly_cents": q["monthly_cents"],
                         "yearly_cents": 12 * q["monthly_cents"]}))
    return out
