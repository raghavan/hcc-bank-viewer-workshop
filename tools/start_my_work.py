"""Copy a starter into my-work/, the folder that is yours to change.

    python tools/start_my_work.py python     # week 1 to 3 in Python
    python tools/start_my_work.py java       # week 1 to 3 in Java
    python tools/start_my_work.py viewer     # week 4
    python tools/start_my_work.py rules      # your own copy of the starter rules (week 3)

Using another language? Make my-work/<language>/ yourself. The checker only
reads out/transactions.json, so any language works.

It never overwrites files you already have.
"""

import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
STARTERS = {
    "python": (ROOT / "starter" / "python", ROOT / "my-work" / "python"),
    "java": (ROOT / "starter" / "java", ROOT / "my-work" / "java"),
    "viewer": (ROOT / "starter" / "viewer", ROOT / "my-work" / "viewer"),
}
NEXT = {
    "python": "python my-work/python/structure.py",
    "java": "java my-work/java/Structurer.java",
    "viewer": "python tools/serve.py my-work/viewer",
    "rules": "edit my-work/rules.csv, then run your structurer again",
}


def copy_file(src, dst):
    if dst.exists():
        print(f"  kept   {dst.relative_to(ROOT)} (already there)")
        return
    dst.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(src, dst)
    print(f"  copied {dst.relative_to(ROOT)}")


def main():
    if len(sys.argv) != 2 or sys.argv[1] not in [*STARTERS, "rules"]:
        print(__doc__)
        sys.exit(1)
    what = sys.argv[1]
    if what == "rules":
        copy_file(ROOT / "contract" / "starter_rules.csv", ROOT / "my-work" / "rules.csv")
    else:
        src, dst = STARTERS[what]
        for f in sorted(src.rglob("*")):
            if f.is_file():
                copy_file(f, dst / f.relative_to(src))
    print(f"\nNext: {NEXT[what]}")


if __name__ == "__main__":
    main()
