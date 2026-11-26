# Week 4 (Mon Mar 1): Your server, CI and CD, and your first release

**The story:** Money Wrapped works on your computer. Tonight it becomes a real piece of
software: your own Java web server shows it in a browser, a robot tests every change you push
(CI), and a tagged release publishes it to the web by itself (CD), with mock data only.

The viewer in `viewer/` is done for you. It has four screens:

| id | What it shows |
|---|---|
| `wrapped` | The Money Wrapped cards, the facts behind them, and the cards the grounding check caught |
| `summary` | Income, spending and net for a month, with spending by category and a six month trend |
| `transactions` | The ledger, with filters (month, account, category, hide transfers, search) |
| `review-queue` | The rows that need a person, with approve and change buttons |

By the end of the week you will have:

1. `Server.resolve`, so `./run serve` (your server, not the reference) shows the viewer
2. a green `./mvnw test -Dweek=4`
3. a CI workflow on week 4, and a deploy workflow (CD) that publishes on every release
4. a `v1.0.0` release on GitHub you can show an employer

## Catch up lane

Behind on weeks 1 to 3? You can still do tonight. `./run --reference structure` and
`./run --reference wrapped --ai mock` fill `out/` with the instructor's output, so your server
and the viewer have something to show. Finish your own code later; nothing else changes.

## In class (about an hour)

1. **Demo (20 min).** Your instructor shows the four screens, then asks: what could go wrong
   if a web server hands out any file someone asks for? They try `/../pom.xml` against a
   careless server. Then they ask an agent for the deploy workflow and review it with the
   checklist below, out loud.
2. **Your turn (40 min).** Write `Server.resolve` and see your own server work.

## Steps

### 1. Your server (30 min)

Open `Server.java`. `start` and `handle` are done. Fill in `resolve`: given a request path like
`/app.js`, return the file that answers it, or `null` for "404 Not Found".

```
./mvnw test -Dweek=4
./run serve                   # then open http://localhost:8000
```

The server listens on `127.0.0.1` only, so nobody else on the Wi-Fi can see your money.

**Checkpoint:** `theServerNeverServesFilesOutsideTheViewer` passes. Ask yourself why
`/../pom.xml` is dangerous, and what your code does to stop it.

**Prompt to try:**

> Explain path traversal to me with the /../pom.xml example in Week4Test. Then review my
> Server.resolve and tell me if there is any request path that still escapes the viewer folder.
> Do not rewrite it; point at the line.

### 2. Look around your own app (10 min)

Open each screen. On the Summary screen, March 2027 should show income $5,421.50, spending
$3,123.96 and net +$2,297.54 on the mock data. Find the two CORNER BEAN CAFE charges on
March 25 in the ledger: a mistake, or two coffees?

### 3. Pipeline step: CI on week 4 (5 min)

Move your `checks.yml` to `-Dweek=4`, push, and check the Actions tab.

### 4. Pipeline step: CD, deploy on every release (30 min)

Continuous delivery: when you tag a release, a robot tests it, builds the site and publishes
it. Nobody copies files by hand. First see what would be published, on your own computer:

```
./run structure
./run wrapped --ai mock
./run site                    # makes _site/: the viewer and the two JSON files
```

`./run site` **refuses** to publish anything that did not come from the mock data. Try it after
`./run structure --data private` if you set up your own bank in week 2: it stops. Your own
money never goes on a website.

**Prompt to try:**

> Create .github/workflows/deploy.yml that runs when I push a tag starting with v, and also has
> a manual Run workflow button. It should set up Java 21 (temurin, maven cache), run
> `./mvnw -B test -Dweek=4` first, then `./run structure`, `./run wrapped --ai mock` and
> `./run site`, then upload the _site folder with actions/upload-pages-artifact and deploy it
> with actions/deploy-pages. Give only the deploy the permissions it needs, and explain why it
> needs pages write and id-token write.

Then on GitHub: **Settings > Pages > Source: GitHub Actions.**

**What to verify yourself:**

- [ ] The trigger is tags `v*` (and `workflow_dispatch`), not every push.
- [ ] The tests run **before** the upload. A red build must never be deployed.
- [ ] It runs `./run structure` with no `--data private`, so only mock data is published.
- [ ] Only `pages: write` and `id-token: write` are added, and you can say why.
- [ ] It uses `--ai mock`, so the deploy needs no key and costs nothing.

**Check it works:** push your `v1.0.0` tag (step 5). The deploy run turns green and shows a
link. Open it: your Money Wrapped, live on the web.

If Pages is not offered for a private repo, or Actions does not start, your CD still works
locally: `./run site` builds exactly what would be deployed. See [docs/CI_CD.md](../../docs/CI_CD.md).

### 5. Ship it (15 min)

```
./mvnw test -Dweek=4
git status                    # nothing from data/private/ listed
git add src rules.csv .github
git commit -m "Week 4: server, CI and CD"
git push
git tag v1.0.0
git push --tags
```

On GitHub, open **Releases**, choose **Draft a new release**, pick the `v1.0.0` tag, write two
sentences about what it does, and publish. Add a screenshot of your Wrapped screen. That
release is something you can show an employer.

## Stuck?

| You see | Try |
|---|---|
| `Not found. Did you run ./run structure ...` | Run `./run structure` and `./run wrapped` first, so `out/` has both files. |
| Port 8000 is busy | `./run serve --port 8001` |
| Blank page | Open the browser console (F12). The first red error usually names the line. |
| The Wrapped tab says "No wrapped.json yet" | Run `./run wrapped --ai mock`, then reload. |
| `Refusing to publish` | That is the safety check. Run `./run structure` (mock data) first. |
| The deploy is red at the test step | Good: CD refused to ship a broken build. Fix the test, then tag `v1.0.1`. |

Still stuck? Peek at `reference/java/src/main/java/moneywrapped/Server.java` and `reference/ci/`.

## What to commit

Your code, `.github/workflows/checks.yml` and `deploy.yml`, the `v1.0.0` tag and a GitHub release.

**Next week (optional):** open lab.
