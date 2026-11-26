# Instructor notes

## Before each session

```
python tools/selftest.py
```

It regenerates the mock data and both answer keys and compares them with the committed files,
checks that the starter and reference share every file without TODOs, runs the reference tests
for all five weeks, checks that the starter compiles and fails week 1, runs the reference end
to end (structure, wrapped with the fake AI, site), proves the site refuses private data and
that git ignores `data/private/`, runs the Python lane against the same contract, parses the
workflow files and tests the viewer math. CI runs the same thing on every push to this repo.

## How the repo is built

- `src/main/java/moneywrapped/` is the **starter** (what students edit). Methods to write throw
  `UnsupportedOperationException("TODO week N: ...")`, with hints in comments above them.
- `reference/java/src/main/java/moneywrapped/` is the **reference**: the same files, finished.
  The Maven profile `-Preference` builds it instead (`./run --reference ...`,
  `./mvnw -Preference test -Dweek=5`). Its output goes to `target/reference/`, so the two never mix.
- `src/test/java/moneywrapped/` holds the tests. `@Week(n)` skips a test until `-Dweek=n` or later.
- `data/wrapped_facts.py` is an independent Python version of the twelve facts. The generator
  uses it to write `data/answer_key/wrapped_key.json`, so the Java facts are checked against
  code written separately, not against themselves.

## Session plan (about an hour each)

Each night is a short live demo, then students start the loop in class and finish at home.

| Week | Date | Demo (reference build) | Students start |
|---|---|---|---|
| 1 | Feb 1 | the five raw files; `./mvnw test -Dweek=1` red on the starter; an agent makes `Money.clean` pass while you read its diff out loud | `Money.clean`, then plan the readers |
| 2 | Feb 8 | `./run structure` with the starter rules; the Review queue tab; find the rule that put a kayak rental in Housing | `markTransfers`, then rules |
| 3 | Feb 22 | `./run --reference wrapped --ai mock`: the Wrapped tab and the crossed out taco card; then `--ai claude` without `--send` and read `out/ai_request_preview.json` together | `Grounding.check` |
| 4 | Mar 1 | the four screens; `/../pom.xml` against a careless server; ask an agent for `deploy.yml` and review it with the checklist; tag a release | `Server.resolve`, then CI and CD |
| 5 | Mar 8 | open lab | challenges in `weeks/week-5-open-lab/` |

## Answers worth knowing

- 286 rows: gulfview 67, coquina 12, harborline 102, palmetto 54, sunpine 51 (plus 7 in the bonus Tarpon file).
- 24 transfer pairs over six months, 4 in March 2027.
- March 2027: income $5,421.50, spending $3,123.96, net +$2,297.54.
- Reference rules: 231 rows placed by rules, a review queue of 9.
- The starter rule `RENT` also catches PADDLE COVE RENTALS (a kayak rental). It is left in on purpose for week 2.
- Two identical CORNER BEAN CAFE charges on 2027-03-25 are a deliberate duplicate looking pair. They are two real coffees.

The twelve facts on the mock data:

| Fact | Answer |
|---|---|
| period | 6 months, October 2026 to March 2027 |
| money_in / money_out | $33,279.64 / $18,249.82 |
| savings_rate | 45% ($15,029.82 kept) |
| top_category | Groceries, $2,535.22 (14% of money out) |
| top_merchant | SUNFRESH MARKET, $1,956.42 over 24 visits |
| most_visited | CORNER BEAN CAFE, 29 visits, $195.75 |
| biggest_month | December 2026, $3,264.30 |
| weekend_share | 20% ($3,624.37) |
| biggest_purchase | MEGAMART #33, $116.02 on February 11, 2027 |
| recurring | 7 charges, $1,823.96 a month |
| quiet_subscription | CLOUDLOCKER STORAGE, $2.99 a month, $35.88 a year |

## The AI step

```
./run --reference wrapped --ai mock              # fake, free, offline; one card always lies
./run --reference wrapped --ai claude            # writes out/ai_request_preview.json, sends nothing
./run --reference wrapped --ai claude --send     # real call, needs ANTHROPIC_API_KEY
```

- Only the facts go out (id, label, text, values). No rows, account names or row ids; a test checks that.
- The answer comes back as structured output and every card still goes through the grounding check.
- Default model `claude-opus-5-5`. Set `MONEY_WRAPPED_MODEL` to use another model.
- One `--send` is one API request, billed to the key's owner. Decide before week 3 who provides a
  key, or whether you demo the real call yourself and students stay on `--ai mock`.
- The `--send` path is compiled and its request is previewed in the tests, but the tests never call the API.

## Students' own bank data

Optional, from week 2. The rules students and their agents follow are in `data/private/README.md`,
`weeks/week-2/README.md` and `AGENTS.md`. Worth saying out loud in class: an AI coding agent that
opens a bank export sends it to that agent's company, so agents are told never to open files in
`data/private/`. Students who would rather not use their own data lose nothing.

## Regenerating the data

```
python data/generate_mock_data.py
./run --reference structure --out data/sample_output/transactions.json
./run --reference wrapped --ai mock --in data/sample_output/transactions.json --out data/sample_output/wrapped.json
python tools/selftest.py
```

The generator is seeded (2027), so it writes the same files every time unless you change it.

## CI and CD

Students ask their agent for `.github/workflows/checks.yml` in week 1 (and move it up a week
each session) and `deploy.yml` in week 4. Prompts and review checklists are in each week guide,
background in `docs/CI_CD.md`, answers in `reference/ci/`.

- This repo's own `.github/workflows/check.yml` runs `selftest.py` only in this repo, so student copies do not spend minutes on it.
- If Actions will not start (billing notice, no minutes), the same commands run locally:
  `./mvnw -B test -Dweek=N`, and for CD `./run structure`, `./run wrapped --ai mock`, `./run site`.
- Pages on a private repo needs a paid plan or the GitHub Student Developer Pack, and the Pages site itself is public.
  Fine here, since only mock data can be published. Decide before week 4 whether students deploy from private copies.
- A common first deploy failure is the github-pages environment rule that blocks tags; add a `v*` tag rule under Settings > Environments.

## The optional Python lane

`python-lane/` has a Python starter and reference for weeks 1 and 2, and a checker that reads
only output files, so it works for any language (`python python-lane/tools/check.py --week N`).
The Python reference and the Java reference produce the same fields; `selftest.py` checks that.

## Giving students access

The repo is private. Options: add each student as a collaborator, make it a template repo so each student
gets their own copy, or use GitHub Classroom. Students commit to their own copy, not to this one.
