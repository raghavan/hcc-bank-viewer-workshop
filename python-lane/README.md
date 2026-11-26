# The optional Python lane

The main track is Java. This folder is for students who would rather write weeks 1 and 2 in
Python, or who want to compare two languages on the same problem. Standard library only.

| Path | What it is |
|---|---|
| `starter/structure.py` | Your file: readers, transfers and rules, with TODOs |
| `reference/` | A finished Python version. Peek when stuck, then write your own |
| `tools/check.py` | The weekly checker. It reads only output files, so it works for any language |
| `tools/show_totals.py` | The numbers the viewer's summary should show |
| `tools/serve.py` | A tiny web server for the viewer, if you are not using `./run serve` |

```
python python-lane/starter/structure.py          # writes out/transactions.json
python python-lane/tools/check.py --week 1
python python-lane/tools/check.py --week 2
```

The output follows the same contract (`contract/CONTRACT.md`), so the Java Wrapped step and the
viewer work on it too: `./run --reference wrapped --ai mock`, then `./run --reference serve`.
On Windows, if `python` is not found, try `py`.
