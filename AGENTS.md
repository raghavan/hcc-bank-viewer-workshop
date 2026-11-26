# Instructions for AI coding agents

This repo is a teaching workshop called **Money Wrapped**: a "Spotify Wrapped" for a bank
account. The person you are helping is an entry level student (associate degree). The goal
is that **they** learn the agentic development loop in Java: plan a small step, make it, run
the tests, read the result, repeat. These instructions work for any agent or harness.

## Ground rules

1. The student's code lives in `src/main/java/moneywrapped/` and their rules in `rules.csv`.
   Change those. Do not edit `src/test/`, `contract/`, `data/` (apart from the student's own
   `data/private/` profile), `reference/`, `tools/` or `pom.xml` unless the person asks you to.
2. **Do not copy code from `reference/`.** It is the answer key. Read it only to understand a
   failing test, and then explain the idea in your own words.
3. Follow the week guide the person is on: `weeks/week-N/README.md`. Go one TODO at a time.
4. At each step: say what you are about to do and why, make a small change, run the tests,
   and show the output. Now and then, ask the person to predict the result before you run it.
5. Teach, do not just finish. When a test fails, point at the line in the failure message that
   matters and ask the student what they think it means before you fix it.
6. Use the Java standard library and the libraries already in `pom.xml`. Add no dependencies.

## Their own bank data stays private

Students may put their own bank export in `data/private/`. Git ignores that folder, and the
tests check that nothing in it is ever committed. You must also keep it private:

- **Do not open, read, print or summarize any file in `data/private/`** apart from its
  `README.md` and the `.properties` profile the student writes. Anything you read is sent to
  your model provider.
- When a profile does not work, ask the student for the header line only, or work on the
  fake example in `data/private-example/`, which has the same kinds of problems.
- Never commit, upload or paste real transactions anywhere. `./run site` refuses to publish
  anything that is not mock data; never work around that.

## AI calls cost money

`./run wrapped --ai claude` only writes `out/ai_request_preview.json` and sends nothing. Read
that preview with the student so they see exactly what would leave their computer. Add
`--send` only when the person says they have an API key and want to spend money on a call.
`--ai mock` is free and offline, and it is enough for every test.

## Commands

| Task | Command (from the repo root; on Windows use `run.cmd` and `mvnw.cmd`) |
|---|---|
| The week's tests (cumulative) | `./mvnw test -Dweek=N` |
| Structure the mock data | `./run structure` |
| Structure the student's own data | `./run structure --data private` |
| Wrapped facts and cards | `./run wrapped` or `./run wrapped --ai mock` |
| The viewer | `./run serve`, then http://localhost:8000 |
| Build the site a deploy would publish | `./run site` |
| The finished instructor build, for comparison | put `--reference` first, like `./run --reference structure` |
| Whole repo self test (instructor) | `python tools/selftest.py` |

`./mvnw test` exits 0 when every test for that week passes, so you can loop on it.

## CI and CD (GitHub Actions)

Week 4 has a pipeline step where the student asks you for workflows. When you write one:

1. Put it in `.github/workflows/` of the student's repo (`checks.yml` for CI, `deploy.yml` for CD).
2. Make it run the same commands the student runs by hand: `./mvnw -B test -Dweek=N`, and for
   the deploy `./run structure`, `./run wrapped --ai mock` and `./run site`.
3. Use only official `actions/*` actions pinned to a major version, `contents: read` by default,
   a `timeout-minutes`, and `pages: write` and `id-token: write` only in the deploy workflow.
   Add no secrets: CI never calls a paid API.
4. Show the diff, explain the file line by line in plain words, and walk the student through the
   "What to verify yourself" list in the week guide. Do not copy `reference/ci/`; it is the answer key.
5. Run the same commands locally before suggesting a push. If Actions does not start on GitHub,
   say so plainly and use the local run. Do not change account or billing settings.

## The contract

`contract/CONTRACT.md` is the source of truth for every field, the review rule, transfers, the
Money Wrapped facts and the viewer math. If code and contract disagree, the contract wins.

## The optional Python lane

`python-lane/` holds a Python version of weeks 1 and 2 and a language neutral checker
(`python python-lane/tools/check.py --week N`). Use it only when the person chooses Python.

## Instructor demo mode

If the person says they are the instructor preparing a demo, you may run and explain the
reference build with `./run --reference ...` and `./mvnw -Preference test -Dweek=5`.
See `docs/INSTRUCTOR.md`.
