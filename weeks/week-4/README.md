# Week 4 (Mon Mar 1): The viewer, and your first release

**Goal for tonight:** a web page that shows your structured data, and a tagged release on GitHub.

The viewer contract is three sections, matching the mockups in `docs/mockups/`:

| id | What it shows |
|---|---|
| `summary` | Income, spending and net for a month, with spending by category. Stretch: a six month trend. |
| `transactions` | The ledger, with filters (month, account, category, hide transfers, search). |
| `review-queue` | The rows that need a person, with a way to approve or change each one. |

By the end you will have:

1. a viewer in `my-work/viewer/` that reads `out/transactions.json`
2. a green `python tools/check.py --week 4`
3. a `v1.0.0` release on GitHub

## Catch up lane

Behind on weeks 1 to 3? You can still build the viewer tonight. It works on the sample file
`data/sample_output/transactions.json`, which the starter loads when your own output is missing.
Finish the structurer later; the viewer will pick it up.

## Steps

### 1. Start the viewer (10 min)

```
python tools/start_my_work.py viewer
python tools/serve.py my-work/viewer
```

Your browser opens the page. Why a server? Browsers do not let a page opened straight from disk read other files.

Open the reference too, so you can see where you are heading:

```
python tools/serve.py --port 8001
```

### 2. Tabs and the ledger (40 min)

`showView` is done. Fill in `renderLedger`: one table row per transaction.
Then add the filters. Keep the browser console (F12) open.

**Checkpoint:** you can find the two CORNER BEAN CAFE charges on March 25. Are they a mistake, or two coffees?

### 3. Summary (40 min)

Fill in `monthsIn` and `summarize`. Follow "How the viewer adds things up" in `contract/CONTRACT.md`.

**Checkpoint:** your March 2027 numbers match `python tools/show_totals.py`.
On the reference data that is income $5,421.50, spending $3,123.96 and net +$2,297.54.

### 4. Review queue (30 min)

Fill in `renderReview`: a card per row that needs review, with its suggestion and confidence.

Stretch: approve and change buttons that save to `localStorage`, plus a "Download decisions.json" button.
Then feed it back:

```
python reference/python/structure.py --decisions decisions.json
```

### 5. Ship it (15 min)

```
python tools/check.py --week 4
git add my-work
git commit -m "Week 4: viewer"
git push
git tag v1.0.0
git push --tags
```

On GitHub, open **Releases**, choose **Draft a new release**, pick the `v1.0.0` tag, write two sentences about
what it does, and publish. Add a screenshot of your viewer. That release is something you can show an employer.

## Stuck?

| You see | Try |
|---|---|
| Blank page | Open the console (F12). The first red error usually names the line. |
| `Failed to fetch` | Start the page with `tools/serve.py`, not by double clicking the file. |
| Totals too high | Did you skip transfers? |
| Money shows like `165000` | Use `money(cents)`, already in `app.js`. |
| Check says no `#summary` section | Keep the three section ids from the starter. |

Still stuck? Read `reference/viewer/lib.js` (the math) and `reference/viewer/app.js` (the page).

## What to commit

Your `my-work/viewer/` folder, plus the `v1.0.0` tag and a GitHub release.

**Next week (optional):** open lab.
