"""Build the folder that gets deployed (the "CD" half of CI/CD).

    python tools/build_site.py                          # my-work/viewer + out/transactions.json
    python tools/build_site.py --viewer reference/viewer
    python tools/build_site.py --sample                 # use the committed sample data instead

It makes _site/ with your viewer's files and a copy of transactions.json next to
index.html. That folder is exactly what a GitHub Pages deploy uploads.
Preview it the same way a visitor will see it:

    python tools/serve.py _site

Standard library only. _site/ is rebuilt every time and is not committed.
"""

import argparse
import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def build(viewer, data, site):
    viewer, data, site = Path(viewer), Path(data), Path(site)
    if not (viewer / "index.html").exists():
        raise SystemExit(f"No index.html in {viewer}. Run `python tools/start_my_work.py viewer` first.")
    if not data.exists():
        raise SystemExit(f"No {data}. Run your structurer first, or pass --sample.")
    if site.exists():
        shutil.rmtree(site)
    shutil.copytree(viewer, site, ignore=shutil.ignore_patterns("*.md", ".*"))
    shutil.copy2(data, site / "transactions.json")
    (site / ".nojekyll").write_text("")  # tells GitHub Pages to serve files as they are
    files = sorted(p.relative_to(site).as_posix() for p in site.rglob("*") if p.is_file())
    return files


def main():
    p = argparse.ArgumentParser(description="Build _site/ for deploying the viewer.")
    p.add_argument("--viewer", default=str(ROOT / "my-work" / "viewer"))
    p.add_argument("--data", default=str(ROOT / "out" / "transactions.json"))
    p.add_argument("--sample", action="store_true", help="use data/sample_output/transactions.json")
    p.add_argument("--site", default=str(ROOT / "_site"))
    args = p.parse_args()
    data = ROOT / "data" / "sample_output" / "transactions.json" if args.sample else args.data
    files = build(args.viewer, data, args.site)
    print(f"Built {Path(args.site).name}/ with {len(files)} files:")
    for f in files:
        print("  " + f)
    print("\nPreview it: python tools/serve.py _site")


if __name__ == "__main__":
    sys.exit(main())
