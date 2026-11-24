# Instructor notes

## Before each session

```
python tools/selftest.py
```

It regenerates the mock data and compares it with the committed files, runs both references,
runs every weekly check, round trips a decisions file, runs the starters, and tests the viewer math.
CI runs the same thing on every push.

## Demo order (Java reference build)

Each week, demo the step live in Java while students build in their own language.

| Week | Demo | Show |
|---|---|---|
| 1 | `readCsv`, `toCents`, the gulfview case in `reference/java/Structurer.java` | why `BigDecimal`, why ids come from row numbers |
| 2 | the other four cases in `readBank` | each bank's surprise, and that one shape hides them all |
| 3 | `markTransfers`, `applyRules` | transfers first, first match wins, the review rule |
| 4 | the reference viewer (`python tools/serve.py`) | the three views, then tagging a release |

Run it from the repo root:

```
java reference/java/Structurer.java
python tools/check.py --week 3 --file out/reference-java/transactions.json
```

The Java and Python references produce the same contract fields; `selftest.py` checks that.

## Answers worth knowing

- 286 rows: gulfview 67, coquina 12, harborline 102, palmetto 54, sunpine 51 (plus 7 in the bonus Tarpon file).
- 24 transfer pairs over six months, 4 in March 2027.
- March 2027: income $5,421.50, spending $3,123.96, net +$2,297.54, $1,481.34 of transfers excluded.
- Reference review queue: 9 rows with AI off.
- The starter rule `RENT` also catches PADDLE COVE RENTALS (a kayak rental). It is left in on purpose for week 3.
- Two identical CORNER BEAN CAFE charges on 2027-03-25 are a deliberate duplicate looking pair. They are two real coffees.

## The optional AI step

```
python reference/python/structure.py --ai mock      # fake, free, deterministic
python reference/python/structure.py --ai claude    # real
```

`--ai claude` needs `pip install anthropic` and `ANTHROPIC_API_KEY` in the environment.
It sends only the leftover descriptions (no amounts, dates or accounts) in one request, caches answers in
`out/ai_cache.json` so a second run is free, and every suggestion still lands in the review queue.

Default model: `claude-opus-5-5`. For a cheaper classroom run set `BANKVIEW_AI_MODEL=claude-haiku-5-5`.
Decide before week 3 who provides the key; students do not need one.

## Regenerating the data

```
python data/generate_mock_data.py
python reference/python/structure.py --ai mock --out data/sample_output/transactions.json
python tools/selftest.py
```

The generator is seeded (2027), so it writes the same files every time unless you change it.

## CI and CD

Students ask their agent for `.github/workflows/checks.yml` in weeks 1 to 3 and `deploy.yml` in week 4
(prompts and review checklists in each week guide, background in `docs/CI_CD.md`, answers in `reference/ci/`).

- This repo's own `.github/workflows/check.yml` runs `selftest.py` only in this repo, so student copies do not spend minutes on it.
- If Actions will not start (billing notice, no minutes), `python tools/ci_local.py --week N` runs the same steps,
  and `python tools/ci_local.py --week 4` then `python tools/serve.py _site` previews exactly what CD would deploy.
- Pages on a private repo needs a paid plan or the GitHub Student Developer Pack, and the Pages site itself is public.
  Fine here, since all data is fake. Decide before week 4 whether students deploy from private copies.
- Week 4 demo: tag `v1.0.0`, watch the deploy run. A common first failure is the github-pages environment rule that
  blocks tags; add a `v*` tag rule under Settings > Environments.

## Giving students access

The repo is private. Options: add each student as a collaborator, make it a template repo so each student
gets their own copy, or use GitHub Classroom. Students commit to their own copy, not to this one.
