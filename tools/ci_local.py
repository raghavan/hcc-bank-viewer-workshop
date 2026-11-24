"""Run your CI pipeline on your own computer, step by step, like GitHub Actions would.

    python tools/ci_local.py --week 1
    python tools/ci_local.py --week 4          # also builds the site a deploy would publish

Use it:
  * before you push, so you never break the build on GitHub, and
  * whenever GitHub Actions does not start (no minutes, a billing notice, or no internet).

It runs the same three steps your workflow file should run:
  1. run your structurer        (my-work/python, my-work/java, or --run "your command")
  2. run the weekly checks      (python tools/check.py --week N)
  3. week 4 and later: build _site/ with tools/build_site.py and check it

A pipeline stops at the first red step, so this does too. Exit code 0 means green.
"""

import argparse
import shlex
import shutil
import subprocess
import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PY = sys.executable


def find_structurer():
    if (ROOT / "my-work" / "python" / "structure.py").exists():
        return [PY, "my-work/python/structure.py"]
    if (ROOT / "my-work" / "java" / "Structurer.java").exists():
        if shutil.which("java") is None:
            raise SystemExit("Found my-work/java but Java is not installed (see docs/SETUP.md).")
        return ["java", "my-work/java/Structurer.java"]
    raise SystemExit('No structurer found in my-work/. Run `python tools/start_my_work.py python`, '
                     'or pass your own command with --run "...".')


def step(n, name, cmd):
    print(f"\n=== step {n}: {name}")
    print("$ " + " ".join(cmd))
    start = time.time()
    code = subprocess.run(cmd, cwd=ROOT).returncode
    took = time.time() - start
    if code != 0:
        print(f"=== step {n} FAILED after {took:.1f}s (exit code {code}). The pipeline stops here.")
        sys.exit(code)
    print(f"=== step {n} passed in {took:.1f}s")


def main():
    p = argparse.ArgumentParser(description="Run the CI steps locally.")
    p.add_argument("--week", type=int, required=True, choices=[1, 2, 3, 4, 5])
    p.add_argument("--run", help='your structurer command if it is not in my-work/python or my-work/java, '
                                 'for example --run "node my-work/js/structure.js"')
    p.add_argument("--viewer", default="my-work/viewer", help="viewer folder for week 4 and later")
    args = p.parse_args()

    run_cmd = shlex.split(args.run) if args.run else find_structurer()
    step(1, "run the structurer", run_cmd)
    step(2, f"weekly checks for week {args.week}",
         [PY, "tools/check.py", "--week", str(args.week), "--viewer", f"{args.viewer}/index.html"])
    if args.week >= 4:
        step(3, "build the site a deploy would publish", [PY, "tools/build_site.py", "--viewer", args.viewer])
        step(4, "check the built site", [PY, "tools/check.py", "--week", "4", "--file", "_site/transactions.json",
                                         "--viewer", "_site/index.html"])
    print("\nPipeline green. Safe to push.")


if __name__ == "__main__":
    main()
