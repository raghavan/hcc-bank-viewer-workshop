# Instructions for AI coding agents

This repo is a teaching workshop. The person you are helping is an entry level student
(associate degree), and the goal is that **they** learn to build a bank transaction viewer.
These instructions work for any agent or harness. Nothing here needs special tools.

## Ground rules

1. Work in `my-work/` only. Create it with `python tools/start_my_work.py python` (or `java`, `viewer`, `rules`).
   Do not edit `contract/`, `data/`, `tools/`, `reference/` or `starter/` unless the person asks you to.
2. **Do not copy code from `reference/`.** It is the answer key. Read it only to understand a failing check,
   and then explain the idea in your own words.
3. Follow the week guide the person is on: `weeks/week-N/README.md`. Go one step at a time.
4. At each step: explain what you are about to do and why, make a small change, run it, run the check,
   and show the output. Ask the person to predict the result before you run it now and then.
5. Prefer the standard library. Do not add packages or frameworks.
6. The data is fictional. Never ask for, use or upload real bank data or credentials.
7. AI calls cost money. Leave `--ai` off unless the person says they have a key and want to use it.

## Commands

| Task | Command (from the repo root) |
|---|---|
| Run the student's Python structurer | `python my-work/python/structure.py` |
| Run the student's Java structurer | `java my-work/java/Structurer.java` |
| Weekly check (any language) | `python tools/check.py --week N` |
| Totals to compare against | `python tools/show_totals.py` |
| Serve the student's viewer | `python tools/serve.py my-work/viewer --no-browser` |
| Whole repo self test (instructor) | `python tools/selftest.py` |

`tools/check.py` exits 0 when every check passes and 1 otherwise, so you can loop on it.

## The contract

`contract/CONTRACT.md` is the source of truth for every field, the review rule, transfers and the viewer math.
If code and contract disagree, the contract wins.

## Instructor demo mode

If the person says they are the instructor preparing a demo, you may run and explain the reference builds:
`python reference/python/structure.py`, `java reference/java/Structurer.java`, `python tools/serve.py`.
See `docs/INSTRUCTOR.md`.
