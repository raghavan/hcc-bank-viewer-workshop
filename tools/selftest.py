"""Instructor self test: proves the whole repo still works, end to end.

    python tools/selftest.py

It runs on a fresh clone with nothing installed beyond Python 3.9+.
Java and Node are used when present and skipped (with a note) when not.
CI runs this on every push (.github/workflows/check.yml).

Students never need this. Your check is tools/check.py.
"""

import filecmp
import json
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PY = sys.executable
results = []


def run(label, cmd, expect_ok=True, cwd=ROOT):
    proc = subprocess.run(cmd, cwd=cwd, capture_output=True, text=True)
    ok = (proc.returncode == 0) == expect_ok
    results.append((ok, label))
    print(("ok    " if ok else "FAIL  ") + label)
    if not ok:
        print((proc.stdout + proc.stderr)[-2500:])
    return proc


def same_tree(a, b):
    cmp = filecmp.dircmp(a, b)
    if cmp.left_only or cmp.right_only or cmp.diff_files or cmp.funny_files:
        return False
    return all(same_tree(Path(a) / d, Path(b) / d) for d in cmp.common_dirs)


def without_timestamp(path):
    data = json.loads(Path(path).read_text(encoding="utf-8"))
    data.pop("generated_at", None)
    return data


def record(ok, label):
    results.append((ok, label))
    print(("ok    " if ok else "FAIL  ") + label)


def main():
    tmp = Path(tempfile.mkdtemp(prefix="bankview-selftest-"))
    try:
        # 1. The mock data is reproducible from the seed.
        run("generator runs", [PY, "data/generate_mock_data.py", "--out", str(tmp / "data")])
        for sub in ("raw", "answer_key", "bonus"):
            record(same_tree(ROOT / "data" / sub, tmp / "data" / sub), f"generated data/{sub} matches the committed files")

        # 2. The Python reference, with AI off and with the fake AI.
        ref = tmp / "ref" / "transactions.json"
        mock = tmp / "mock" / "transactions.json"
        bonus = tmp / "bonus" / "transactions.json"
        run("reference structurer (AI off)", [PY, "reference/python/structure.py", "--out", str(ref)])
        run("reference structurer (AI mock)", [PY, "reference/python/structure.py", "--ai", "mock", "--out", str(mock)])
        run("reference structurer (bonus bank)", [PY, "reference/python/structure.py", "--include-bonus", "--out", str(bonus)])
        record(without_timestamp(mock) == without_timestamp(ROOT / "data" / "sample_output" / "transactions.json"),
               "data/sample_output matches a fresh --ai mock run")

        # 3. The weekly checker passes on the reference output.
        for week in (1, 2, 3):
            run(f"check week {week} on the reference", [PY, "tools/check.py", "--week", str(week), "--file", str(ref)])
        run("check week 3 on the mock AI output", [PY, "tools/check.py", "--week", "3", "--file", str(mock)])
        viewer = "reference/viewer/index.html"
        run("check week 4 on the reference viewer", [PY, "tools/check.py", "--week", "4", "--file", str(ref), "--viewer", viewer])
        run("check week 5 with the bonus bank", [PY, "tools/check.py", "--week", "5", "--file", str(bonus), "--viewer", viewer])
        run("show_totals", [PY, "tools/show_totals.py", str(ref)])

        # 4. A decisions.json round trip: approving a row removes it from the queue.
        decisions = tmp / "decisions.json"
        decisions.write_text(json.dumps({"overrides": {"harborline-94": {"category": "Health", "source": "ai_approved"}},
                                         "new_rules": [{"pattern": "LUMA YOGA", "category": "Health"}]}))
        dec_out = tmp / "dec" / "transactions.json"
        run("reference structurer with --decisions", [PY, "reference/python/structure.py", "--ai", "mock",
                                                      "--decisions", str(decisions), "--out", str(dec_out)])
        before = sum(r["needs_review"] for r in without_timestamp(mock)["transactions"])
        after = sum(r["needs_review"] for r in without_timestamp(dec_out)["transactions"])
        record(after < before, f"decisions shrink the review queue ({before} to {after})")

        # 5. The checker really fails on broken output.
        broken = tmp / "broken.json"
        rows = without_timestamp(ref)
        rows["transactions"][0]["amount_cents"] = 1
        rows["transactions"] = rows["transactions"][1:] + rows["transactions"][:1]
        broken.write_text(json.dumps(rows))
        run("checker catches a wrong amount", [PY, "tools/check.py", "--week", "2", "--file", str(broken)], expect_ok=False)

        # 6. Starters run without crashing on a fresh copy (in a scratch clone of the repo).
        clone = tmp / "clone"
        shutil.copytree(ROOT, clone, ignore=shutil.ignore_patterns(".git", "out", "my-work", "_site", "__pycache__"))
        if shutil.which("git"):
            subprocess.run(["git", "init", "-q"], cwd=clone)  # a student's clone is a git repo
        for what in ("python", "java", "viewer", "rules"):
            run(f"start_my_work {what}", [PY, "tools/start_my_work.py", what], cwd=clone)
        run("starter python runs", [PY, "my-work/python/structure.py"], cwd=clone)
        run("starter output fails week 1 until its TODOs are done",
            [PY, "tools/check.py", "--week", "1"], expect_ok=False, cwd=clone)
        run("starter viewer has the three sections", [PY, "tools/check.py", "--week", "4",
                                                      "--file", str(ref)], cwd=clone)

        # 6b. The CI and CD helpers.
        run("ci_local fails on the starter (TODOs not done yet)", [PY, "tools/ci_local.py", "--week", "1"],
            expect_ok=False, cwd=clone)
        ref_cmd = f'"{PY}" reference/python/structure.py --out out/transactions.json'
        for week in (1, 3):
            run(f"ci_local week {week} passes with the reference structurer",
                [PY, "tools/ci_local.py", "--week", str(week), "--run", ref_cmd], cwd=clone)
        run("ci_local week 4 builds and checks the site (reference viewer)",
            [PY, "tools/ci_local.py", "--week", "4", "--run", ref_cmd, "--viewer", "reference/viewer"], cwd=clone)
        site = clone / "_site"
        record((site / "index.html").exists() and (site / "transactions.json").exists() and (site / "lib.js").exists(),
               "_site has the viewer and its transactions.json")
        run("build_site works on the starter viewer with sample data", [PY, "tools/build_site.py", "--viewer",
            "my-work/viewer", "--sample", "--site", str(tmp / "site2")], cwd=clone)
        try:
            import yaml  # optional: only used to parse the workflow files when PyYAML is around
            for wf in [ROOT / ".github" / "workflows" / "check.yml", *sorted((ROOT / "reference" / "ci").glob("*.yml"))]:
                data = yaml.safe_load(wf.read_text())
                record(isinstance(data, dict) and "jobs" in data, f"{wf.relative_to(ROOT)} is valid YAML with jobs")
        except ImportError:
            print("skip  PyYAML not installed (workflow syntax check)")

        # 7. Java reference (and starter), when Java 17+ is installed.
        if shutil.which("java"):
            java_out = tmp / "java" / "transactions.json"
            run("Java reference runs", ["java", "reference/java/Structurer.java", "data/raw", str(java_out)])
            run("check week 3 on the Java reference", [PY, "tools/check.py", "--week", "3", "--file", str(java_out)])
            fields = ["id", "date", "account", "description", "amount_cents", "category", "category_source",
                      "rule_id", "confidence", "needs_review", "is_transfer"]
            pick = lambda p: [[r[f] for f in fields] for r in json.loads(Path(p).read_text())["transactions"]]
            record(pick(java_out) == pick(ref), "Java and Python references agree on every field")
            run("Java starter runs", ["java", "my-work/java/Structurer.java"], cwd=clone)
        else:
            print("skip  Java not installed")

        # 8. The viewer math, when Node is installed.
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
            run("viewer math (lib.js) gives the March 2027 totals",
                ["node", "-e", js, str(ROOT / "reference" / "viewer" / "lib.js"), str(ref)])
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
