"""Instructor self test: proves the whole repo still works, end to end.

    python tools/selftest.py

Needs Python 3.9+ and Java 21 (the Maven wrapper downloads Maven itself).
Node is used when present and skipped (with a note) when not.
CI runs this on every push to the workshop repo (.github/workflows/check.yml).

Students never need this. Their check is ./mvnw test -Dweek=N
(or python python-lane/tools/check.py --week N in the optional Python lane).
"""

import filecmp
import json
import os
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PY = sys.executable
MVNW_NAME = "mvnw.cmd" if os.name == "nt" else "mvnw"
MVNW = str(ROOT / MVNW_NAME)
results = []

# Files with no TODOs in them must be the same in the starter and the reference,
# so a fix in one never gets forgotten in the other.
SHARED = ["AiCards", "Card", "Categories", "Csv", "Fact", "Json", "Main", "Site", "Structure", "Txn", "WrappedRun"]
STARTER = ROOT / "src" / "main" / "java" / "moneywrapped"
REFERENCE = ROOT / "reference" / "java" / "src" / "main" / "java" / "moneywrapped"


def run(label, cmd, expect_ok=True, cwd=ROOT, show=False):
    proc = subprocess.run(cmd, cwd=cwd, capture_output=True, text=True)
    ok = (proc.returncode == 0) == expect_ok
    results.append((ok, label))
    print(("ok    " if ok else "FAIL  ") + label)
    if not ok or show:
        print((proc.stdout + proc.stderr)[-3000:])
    return proc


def record(ok, label):
    results.append((ok, label))
    print(("ok    " if ok else "FAIL  ") + label)


def same_tree(a, b):
    cmp = filecmp.dircmp(a, b)
    if cmp.left_only or cmp.right_only or cmp.diff_files or cmp.funny_files:
        return False
    return all(same_tree(Path(a) / d, Path(b) / d) for d in cmp.common_dirs)


def without_timestamp(path):
    data = json.loads(Path(path).read_text(encoding="utf-8"))
    data.pop("generated_at", None)
    return data


def java(args, folder=ROOT):
    """The same as ./run --reference ARGS, started in folder (the repo, or a scratch copy of it)."""
    return [str(Path(folder) / MVNW_NAME), "-q", "-B", "-Preference", "compile", "exec:java", "-Dexec.args=" + " ".join(args)]


def main():
    tmp = Path(tempfile.mkdtemp(prefix="moneywrapped-selftest-"))
    try:
        # 1. The mock data and both answer keys are reproducible from the seed.
        run("generator runs", [PY, "data/generate_mock_data.py", "--out", str(tmp / "data")])
        for sub in ("raw", "answer_key", "bonus"):
            record(same_tree(ROOT / "data" / sub, tmp / "data" / sub), f"generated data/{sub} matches the committed files")

        # 2. Starter and reference share every file that has no TODOs.
        for name in SHARED:
            a, b = STARTER / f"{name}.java", REFERENCE / f"{name}.java"
            record(a.exists() and b.exists() and a.read_bytes() == b.read_bytes(), f"{name}.java is the same in starter and reference")
        record((ROOT / "rules.csv").read_bytes() == (ROOT / "contract" / "starter_rules.csv").read_bytes(),
               "rules.csv starts as a copy of contract/starter_rules.csv")

        # 3. The Java tests: the reference passes every week, the starter fails week 1.
        run("reference passes ./mvnw test -Dweek=5", [MVNW, "-q", "-B", "-Preference", "test", "-Dweek=5"])
        starter = run("starter compiles", [MVNW, "-q", "-B", "compile"])
        if starter.returncode == 0:
            run("starter fails week 1 until its TODOs are done", [MVNW, "-q", "-B", "test", "-Dweek=1"], expect_ok=False)

        # 4. The reference, end to end, on the mock data.
        tx = tmp / "java" / "transactions.json"
        wrapped = tmp / "java" / "wrapped.json"
        run("./run --reference structure", java(["structure", "--out", str(tx)]))
        run("./run --reference wrapped --ai mock", java(["wrapped", "--ai", "mock", "--in", str(tx), "--out", str(wrapped)]))
        if wrapped.exists():
            w = json.loads(wrapped.read_text(encoding="utf-8"))
            record(len(w["facts"]) == 12 and len(w["rejected"]) == 1, "wrapped.json has 12 facts and catches the one made up card")
            key = json.loads((ROOT / "data" / "answer_key" / "wrapped_key.json").read_text(encoding="utf-8"))
            record([f["data"] for f in w["facts"]] == [f["data"] for f in key["facts"]],
                   "the Java facts on the reference output equal the Python oracle's")
        for fresh in (tx, wrapped):
            sample = ROOT / "data" / "sample_output" / fresh.name
            record(fresh.exists() and without_timestamp(fresh) == without_timestamp(sample),
                   f"data/sample_output/{fresh.name} matches a fresh run")

        # 5. The site: builds from mock data, refuses anything else.
        clone = tmp / "clone"
        shutil.copytree(ROOT, clone, ignore=shutil.ignore_patterns(".git", "out", "target", "_site", "__pycache__"))
        (clone / "out").mkdir()
        shutil.copy(tx, clone / "out" / "transactions.json")
        shutil.copy(wrapped, clone / "out" / "wrapped.json")
        run("./run --reference site builds _site from mock data", java(["site"], clone), cwd=clone)
        site = clone / "_site"
        record(all((site / f).exists() for f in ("index.html", "app.js", "lib.js", "transactions.json", "wrapped.json")),
               "_site has the viewer and both JSON files")
        mine = json.loads((clone / "out" / "transactions.json").read_text(encoding="utf-8"))
        mine["data_source"] = "private"
        (clone / "out" / "transactions.json").write_text(json.dumps(mine), encoding="utf-8")
        shutil.rmtree(site)
        run("site refuses private data", java(["site"], clone), expect_ok=False, cwd=clone)
        record(not site.exists(), "nothing was published from private data")

        # 6. Own bank data: the example profile reads, and stays out of git.
        priv = clone / "data" / "private"
        for f in (ROOT / "data" / "private-example").iterdir():
            shutil.copy(f, priv / f.name)
        ptx = tmp / "private" / "transactions.json"
        run("structure --data private reads the example profile", java(["structure", "--data", "private", "--out", str(ptx)], clone), cwd=clone)
        if ptx.exists():
            p = json.loads(ptx.read_text(encoding="utf-8"))
            record(p["data_source"] == "private" and len(p["transactions"]) == 12, "private output is marked private (12 rows)")
        ignored = subprocess.run(["git", "check-ignore", "-q", "data/private/seagrape_export.csv"], cwd=ROOT).returncode == 0
        record(ignored, "git ignores a bank file dropped into data/private/")

        # 7. The optional Python lane still works against the same contract.
        pyref = tmp / "python" / "transactions.json"
        pybonus = tmp / "python-bonus" / "transactions.json"
        run("python lane reference structurer", [PY, "python-lane/reference/structure.py", "--out", str(pyref)])
        run("python lane reference with the bonus bank", [PY, "python-lane/reference/structure.py", "--include-bonus", "--out", str(pybonus)])
        fields = ["id", "date", "account", "description", "amount_cents", "category", "category_source",
                  "rule_id", "confidence", "needs_review", "is_transfer"]
        pick = lambda f: [[r[k] for k in fields] for r in json.loads(Path(f).read_text(encoding="utf-8"))["transactions"]]
        if pyref.exists() and tx.exists():
            record(pick(pyref) == pick(tx), "Java and Python references agree on every field")
        for week in (1, 2):
            run(f"python checker week {week} on the Java output", [PY, "python-lane/tools/check.py", "--week", str(week), "--file", str(tx)])
        run("python checker week 4 (facts, viewer)", [PY, "python-lane/tools/check.py", "--week", "4", "--file", str(tx),
                                                     "--wrapped", str(wrapped)])
        run("python checker week 5 with the bonus bank", [PY, "python-lane/tools/check.py", "--week", "5", "--file", str(pybonus),
                                                         "--wrapped", str(wrapped)])
        broken = tmp / "broken.json"
        rows = without_timestamp(tx)
        rows["transactions"][0]["amount_cents"] = 1
        broken.write_text(json.dumps(rows), encoding="utf-8")
        run("python checker catches a wrong amount", [PY, "python-lane/tools/check.py", "--week", "1", "--file", str(broken)],
            expect_ok=False)
        run("python lane starter runs", [PY, "python-lane/starter/structure.py"], cwd=clone)
        run("python lane starter fails week 1 until its TODOs are done", [PY, "python-lane/tools/check.py", "--week", "1"],
            expect_ok=False, cwd=clone)
        run("show_totals", [PY, "python-lane/tools/show_totals.py", str(tx)])

        # 8. The workflow files parse.
        try:
            import yaml  # optional
            for wf in [ROOT / ".github" / "workflows" / "check.yml", *sorted((ROOT / "reference" / "ci").glob("*.yml"))]:
                data = yaml.safe_load(wf.read_text(encoding="utf-8"))
                record(isinstance(data, dict) and "jobs" in data, f"{wf.relative_to(ROOT)} is valid YAML with jobs")
        except ImportError:
            print("skip  PyYAML not installed (workflow syntax check)")

        # 9. The viewer math, when Node is installed.
        if shutil.which("node"):
            js = (
                "const L=require(process.argv[1]);const fs=require('fs');"
                "const rows=JSON.parse(fs.readFileSync(process.argv[2],'utf8')).transactions;"
                "const s=L.summarize(rows,'2027-03');"
                "const ok=s.income===542150&&s.spending===312396&&s.net===229754&&s.transferPairs===4"
                "&&L.trend(rows,'2027-03').length===6&&L.money(-675)==='-$6.75'"
                "&&L.suggestPattern('CITY PARKING GARAGE 3')==='CITY PARKING';"
                "console.log(JSON.stringify(s));process.exit(ok?0:1);"
            )
            run("viewer math (lib.js) gives the March 2027 totals", ["node", "-e", js, str(ROOT / "viewer" / "lib.js"), str(tx)])
            run("viewer app.js parses", ["node", "--check", str(ROOT / "viewer" / "app.js")])
        else:
            print("skip  Node not installed (viewer math test)")
    finally:
        shutil.rmtree(tmp, ignore_errors=True)

    failed = [label for ok, label in results if not ok]
    print()
    if failed:
        print(f"{len(failed)} of {len(results)} failed: " + "; ".join(failed))
        sys.exit(1)
    print(f"All {len(results)} self tests pass.")


if __name__ == "__main__":
    main()
