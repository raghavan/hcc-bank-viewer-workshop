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
4. a deploy pipeline (CD) that publishes your viewer when you tag a release

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

### 5. Pipeline step: CD, deploy on every release (30 min)

Continuous delivery: when you tag a release, a robot tests it, builds the site and publishes it. Nobody copies files by hand.

First, see what will be deployed, on your own computer:

```
python tools/ci_local.py --week 4
python tools/serve.py _site
```

`tools/build_site.py` (step 3 of that pipeline) puts your viewer and your `transactions.json` together in `_site/`.

**Prompt to try:**

> Create .github/workflows/deploy.yml that runs when I push a tag starting with v, and also has a manual
> Run workflow button. It should run `python tools/ci_local.py --week 4` first, then upload the _site folder
> with actions/upload-pages-artifact and deploy it with actions/deploy-pages. Give only the deploy the
> permissions it needs. Explain why it needs pages write and id-token write.

Then on GitHub: **Settings > Pages > Source: GitHub Actions.**

**What to verify yourself:**

- [ ] The trigger is tags `v*` (and `workflow_dispatch`), not every push.
- [ ] The tests run **before** the upload. A red build must never be deployed.
- [ ] Only `pages: write` and `id-token: write` are added, and you can say why.
- [ ] The uploaded folder is `_site`, and it holds fake data only.

**Check it works:** push your `v1.0.0` tag (step 6). The **deploy viewer** run turns green and shows a link.
Open it: your viewer, live on the web.

If Pages is not offered for a private repo, or Actions does not start, your CD still works locally:
`python tools/ci_local.py --week 4` builds exactly what would be deployed. See [docs/CI_CD.md](../../docs/CI_CD.md).

### 6. Ship it (15 min)

```
python tools/check.py --week 4
python tools/ci_local.py --week 4
git add my-work .github
git commit -m "Week 4: viewer and deploy"
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

Your `my-work/viewer/` folder, `.github/workflows/deploy.yml`, the `v1.0.0` tag and a GitHub release.

**Next week (optional):** open lab.
