"""Weekly checks for the optional Python lane. Works for ANY language, because it only reads files.

The Java track has its own checks: ./mvnw test -Dweek=N. This script is for students who
write their structurer in Python (or anything else) and still want a green light.

    python python-lane/tools/check.py --week 1     all five banks read correctly
    python python-lane/tools/check.py --week 2     transfers, rules and the review queue
    python python-lane/tools/check.py --week 3     Money Wrapped facts in out/wrapped.json
    python python-lane/tools/check.py --week 4     the viewer and your release tag
    python python-lane/tools/check.py --week 5     the bonus bank too

By default it reads out/transactions.json. Point it somewhere else with --file:

    python python-lane/tools/check.py --week 2 --file out/python/transactions.json

Green PASS lines mean you are done for the week. Each FAIL line says what is
wrong and gives a hint. Standard library only, Python 3.9 or newer.
"""

import argparse
import json
import re
import shutil
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
CATEGORIES = ["Income", "Housing", "Groceries", "Dining", "Shopping", "Transport",
              "Utilities", "Health", "Subscriptions", "Other", "Transfer"]
SOURCES = ["rule", "transfer", "ai", "ai_approved", "manual", "none"]
FIELDS = ["id", "date", "account", "description", "amount_cents", "category",
          "category_source", "rule_id", "confidence", "needs_review", "is_transfer"]
BANKS = ["gulfview", "coquina", "harborline", "palmetto", "sunpine"]

USE_COLOR = sys.stdout.isatty()


def paint(text, code):
    return f"\033[{code}m{text}\033[0m" if USE_COLOR else text


class Report:
    def __init__(self):
        self.failed = 0
        self.passed = 0

    def ok(self, msg):
        self.passed += 1
        print(paint("PASS ", "32") + msg)

    def fail(self, msg, hint=None, examples=()):
        self.failed += 1
        print(paint("FAIL ", "31") + msg)
        for e in list(examples)[:5]:
            print("       e.g. " + str(e))
        if hint:
            print(paint("       hint: ", "33") + hint)

    def note(self, msg):
        print(paint("NOTE ", "36") + msg)

    def check(self, condition, good, bad, hint=None, examples=()):
        if condition:
            self.ok(good)
        else:
            self.fail(bad, hint, examples)
        return condition


def load_key(path):
    return {r["id"]: r for r in json.loads(Path(path).read_text())["rows"]}


def clean(text):
    return " ".join(str(text).upper().split())


# ------------------------------------------------------------------ checks

def check_setup(rep):
    rep.check(sys.version_info >= (3, 9), f"Python {sys.version.split()[0]}",
              f"Python {sys.version.split()[0]} is too old", "Install Python 3.10 or newer (see docs/SETUP.md).")
    has_git = shutil.which("git") is not None
    rep.check(has_git, "git is installed", "git is not installed", "Install git (see docs/SETUP.md).")
    if has_git:
        inside = subprocess.run(["git", "rev-parse", "--is-inside-work-tree"], cwd=ROOT,
                                capture_output=True, text=True).returncode == 0
        rep.check(inside, "this folder is a git repository", "this folder is not a git repository",
                  "Get the repo with `git clone` instead of downloading a zip.")


def load_output(rep, path):
    path = Path(path)
    if not rep.check(path.exists(), f"found {path}", f"{path} does not exist",
                     "Run your structurer first. It should write out/transactions.json."):
        return None
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as e:
        rep.fail(f"{path} is not valid JSON ({e})", "Write it with your language's JSON library, not by hand.")
        return None
    rep.ok("the file is valid JSON")
    if not isinstance(data, dict) or not isinstance(data.get("transactions"), list):
        rep.fail('the top level must be an object with a "transactions" list',
                 'See the example at the top of contract/CONTRACT.md.')
        return None
    rep.check(data.get("schema_version") == "1.0", 'schema_version is "1.0"',
              'schema_version must be "1.0"', 'Add "schema_version": "1.0" next to "transactions".')
    return data["transactions"]


def check_shape(rep, rows, week):
    problems = []
    for r in rows:
        if not isinstance(r, dict):
            problems.append(f"not an object: {r!r}")
            continue
        missing = [f for f in FIELDS if f not in r]
        if missing:
            problems.append(f"{r.get('id', '?')} is missing {', '.join(missing)}")
            continue
        if not isinstance(r["id"], str) or not re.fullmatch(r"[a-z]+-[1-9][0-9]*", r["id"]):
            problems.append(f"bad id {r['id']!r} (want like gulfview-12)")
        if not isinstance(r["date"], str) or not re.fullmatch(r"\d{4}-\d{2}-\d{2}", r["date"]):
            problems.append(f"{r['id']}: date {r['date']!r} is not YYYY-MM-DD")
        if not isinstance(r["amount_cents"], int) or isinstance(r["amount_cents"], bool):
            problems.append(f"{r['id']}: amount_cents {r['amount_cents']!r} is not a whole number")
        if r["category_source"] not in SOURCES:
            problems.append(f"{r['id']}: category_source {r['category_source']!r} is not one of {SOURCES}")
        if r["category"] is not None and r["category"] not in CATEGORIES:
            problems.append(f"{r['id']}: category {r['category']!r} is not on the list")
        if r["confidence"] is not None and not (isinstance(r["confidence"], (int, float)) and 0 <= r["confidence"] <= 1):
            problems.append(f"{r['id']}: confidence {r['confidence']!r} must be between 0 and 1 or null")
        for flag in ("needs_review", "is_transfer"):
            if not isinstance(r[flag], bool):
                problems.append(f"{r['id']}: {flag} must be true or false")
        if r["description"] != clean(r["description"]):
            problems.append(f"{r['id']}: description {r['description']!r} is not cleaned")
    rep.check(not problems, f"all {len(rows)} rows follow the contract",
              f"{len(problems)} rows break the contract",
              "Compare one of these rows with the field table in contract/CONTRACT.md.", problems)
    ids = [r.get("id") for r in rows if isinstance(r, dict)]
    dupes = sorted({i for i in ids if ids.count(i) > 1})
    rep.check(not dupes, "every id is unique", f"{len(dupes)} ids appear more than once",
              "Ids are <bank>-<row number in the file>. Two identical coffees still have different row numbers.", dupes)
    return not problems and not dupes


def compare_with_key(rep, rows, key, banks, label):
    by_id = {r["id"]: r for r in rows if isinstance(r, dict) and "id" in r}
    expected = [k for k in key.values() if k["id"].split("-")[0] in banks]
    missing = [k["id"] for k in expected if k["id"] not in by_id]
    rep.check(not missing, f"{label}: all {len(expected)} expected rows are there",
              f"{label}: {len(missing)} of {len(expected)} rows are missing",
              "Count data rows only, starting at 1. Skip headers, junk lines, blank lines and # lines.", missing)
    for field, hint in [
        ("date", "Check the date format for that bank (see the table in CONTRACT.md)."),
        ("amount_cents", "Check the sign: negative means money left the account. Cards flip it."),
        ("description", "Uppercase it, trim it, squeeze double spaces."),
        ("account", "Use the account_name column from contract/accounts.csv."),
    ]:
        wrong = []
        for k in expected:
            r = by_id.get(k["id"])
            if r is None:
                continue
            got = clean(r.get(field)) if field == "description" else r.get(field)
            if got != k[field]:
                wrong.append(f"{k['id']}: {field} is {r.get(field)!r}, expected {k[field]!r}")
        rep.check(not wrong, f"{label}: every {field} matches",
                  f"{label}: {len(wrong)} rows have the wrong {field}", hint, wrong)
    return not missing


def check_categories(rep, rows, key):
    by_id = {r["id"]: r for r in rows}
    nulls = [r["id"] for r in rows if r["category"] is None]
    rep.check(not nulls, "every row has a category", f"{len(nulls)} rows have no category",
              'Rows nobody could place should be "Other" with needs_review true.', nulls)

    bad_review = []
    for r in rows:
        conf = r["confidence"] if r["confidence"] is not None else 0
        should = r["category_source"] in ("none", "ai") or (r["category_source"] == "rule" and conf < 0.8)
        if r["needs_review"] != should:
            bad_review.append(f"{r['id']}: source {r['category_source']}, confidence {r['confidence']}, needs_review {r['needs_review']}")
    rep.check(not bad_review, "needs_review follows the review rule",
              f"{len(bad_review)} rows break the review rule",
              "See 'The review rule' in contract/CONTRACT.md.", bad_review)

    t_wrong = [f"{i}: is_transfer {by_id[i]['is_transfer']}, expected {k['is_transfer']} ({k['description']})"
               for i, k in key.items() if i in by_id and by_id[i]["is_transfer"] != k["is_transfer"]]
    rep.check(not t_wrong, "every transfer pair is found (and nothing else is called a transfer)",
              f"{len(t_wrong)} rows have the wrong is_transfer",
              "A transfer is money out of one account and the same amount into another within 3 days.", t_wrong)
    t_cat = [r["id"] for r in rows if r["is_transfer"] and r["category"] != "Transfer"]
    rep.check(not t_cat, 'transfers have category "Transfer"', f"{len(t_cat)} transfers have another category",
              None, t_cat)

    decided = [r for r in rows if not r["needs_review"] and r["id"] in key]
    right = [r for r in decided if r["category"] == key[r["id"]]["category"]]
    wrong = [f"{r['id']} {r['description']}: {r['category']}, expected {key[r['id']]['category']} (rule {r['rule_id']})"
             for r in decided if r["category"] != key[r["id"]]["category"]]
    accuracy = len(right) / len(decided) if decided else 0
    rep.check(accuracy >= 0.95, f"accuracy on decided rows is {accuracy:.0%} (need 95%)",
              f"accuracy on decided rows is {accuracy:.0%}, need 95%",
              "Look for a rule pattern that is too broad. Is RENT matching something that is not rent?", wrong)
    if wrong and accuracy >= 0.95:
        rep.note(f"{len(wrong)} decided rows still have the wrong category. Worth a look:")
        for w in wrong[:5]:
            print("       " + w)

    queue = [r for r in rows if r["needs_review"]]
    share = len(queue) / len(rows) if rows else 1
    rep.check(0 < len(queue) and share <= 0.10,
              f"review queue has {len(queue)} rows ({share:.0%} of all, limit 10%)",
              f"review queue has {len(queue)} rows ({share:.0%} of all); it should be between 1 row and 10%",
              "Too many: add rules for merchants that repeat. Zero: low confidence rows should wait for a person.")


def check_viewer(rep, viewer):
    viewer = Path(viewer)
    if not rep.check(viewer.exists(), f"found your viewer at {viewer}", f"no viewer at {viewer}",
                     "The viewer lives in viewer/index.html. Pass --viewer PATH if yours is somewhere else."):
        return
    html = viewer.read_text(encoding="utf-8", errors="replace")
    sibling_js = "".join(p.read_text(encoding="utf-8", errors="replace") for p in viewer.parent.glob("*.js"))
    for part in ("wrapped", "summary", "transactions", "review-queue"):
        rep.check(f'id="{part}"' in html, f'the viewer has a #{part} section',
                  f'the viewer has no element with id="{part}"',
                  "The four screens are part of the viewer contract (see weeks/week-4/README.md).")
    rep.check("transactions.json" in html + sibling_js, "the viewer loads transactions.json",
              "the viewer never mentions transactions.json", "Load the structurer's output with fetch().")


def check_wrapped(rep, path, key_path):
    path = Path(path)
    if not rep.check(path.exists(), f"found {path}", f"{path} does not exist",
                     "Compute the facts and write them to out/wrapped.json (Java: ./run wrapped)."):
        return
    try:
        doc = json.loads(path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as e:
        rep.fail(f"{path} is not valid JSON ({e})")
        return
    facts = {f.get("id"): f for f in doc.get("facts", []) if isinstance(f, dict)}
    key = json.loads(Path(key_path).read_text(encoding="utf-8"))["facts"]
    for k in key:
        got = facts.get(k["id"])
        if got is None:
            rep.fail(f"fact {k['id']} is missing", f"Expected: {k['text']}. Rules in contract/CONTRACT.md.")
        else:
            rep.check(got.get("data") == k["data"], f"fact {k['id']} matches",
                      f"fact {k['id']} has the wrong numbers",
                      f"Expected: {k['text']}. Rules in contract/CONTRACT.md under Money Wrapped facts.",
                      [f"data is {got.get('data')}, expected {k['data']}"])
    for c in doc.get("cards", []):
        if not c.get("fact_ids"):
            rep.fail(f"card {c.get('title')!r} cites no facts", "Every card must name the facts it is built on.")


def check_release(rep):
    if shutil.which("git") is None:
        return
    tags = subprocess.run(["git", "tag", "--list", "v1*"], cwd=ROOT, capture_output=True, text=True).stdout.split()
    if tags:
        rep.ok(f"release tag found: {', '.join(tags)}")
    else:
        rep.note("no v1 tag yet. Last step of week 4: git tag v1.0.0 && git push --tags, then make a GitHub release.")


def main():
    p = argparse.ArgumentParser(description="Weekly checks for Money Wrapped (optional Python lane).")
    p.add_argument("--week", type=int, required=True, choices=[1, 2, 3, 4, 5])
    p.add_argument("--file", default=str(ROOT / "out" / "transactions.json"))
    p.add_argument("--wrapped", default=str(ROOT / "out" / "wrapped.json"))
    p.add_argument("--viewer", default=str(ROOT / "viewer" / "index.html"))
    args = p.parse_args()

    rep = Report()
    print(paint(f"Week {args.week} check  ({args.file})", "1"))
    key = load_key(ROOT / "data" / "answer_key" / "answer_key.json")
    bonus_key = load_key(ROOT / "data" / "bonus" / "answer_key_bonus.json")

    if args.week == 1:
        check_setup(rep)
    rows = load_output(rep, args.file)
    if rows is not None:
        shape_ok = check_shape(rep, rows, args.week)
        for bank in BANKS:
            compare_with_key(rep, rows, key, [bank], bank.capitalize())
        known = set(key) | set(bonus_key)
        extra = [r["id"] for r in rows if isinstance(r, dict) and r.get("id") not in known]
        rep.check(not extra, "no unexpected rows", f"{len(extra)} rows are not in any bank file",
                  "Did you read a file twice, or count a header as a row?", extra)
        if args.week >= 2 and shape_ok:
            check_categories(rep, rows, {**key, **bonus_key})
        if args.week == 5:
            tarpon = [r for r in rows if isinstance(r, dict) and str(r.get("id", "")).startswith("tarpon-")]
            if tarpon:
                compare_with_key(rep, rows, bonus_key, ["tarpon"], "Tarpon (bonus)")
            else:
                rep.note("stretch goal open: read data/bonus/tarpon_checking.json (a JSON export).")
    if args.week >= 3:
        check_wrapped(rep, args.wrapped, ROOT / "data" / "answer_key" / "wrapped_key.json")
    if args.week >= 4:
        check_viewer(rep, args.viewer)
        check_release(rep)

    print()
    if rep.failed:
        print(paint(f"{rep.failed} to fix, {rep.passed} passing. You've got this: fix the first FAIL, then run again.", "31"))
        sys.exit(1)
    print(paint(f"All {rep.passed} checks pass. Week {args.week} done!", "32"))


if __name__ == "__main__":
    main()
