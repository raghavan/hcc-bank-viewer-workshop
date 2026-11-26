# Money Wrapped

**Your bank account's year in review, built in Java with an AI agent at your side.**

You know Spotify Wrapped: "You listened to 4,200 minutes of music. Your top artist was...".
In this workshop you build the same thing for money. It reads messy exports from five banks,
cleans them into one shape, finds the patterns, and tells the story:

> **Your coffee habit, by the numbers.** CORNER BEAN CAFE was your most visited place: 29 visits and $195.75.
>
> **The quiet one.** CLOUDLOCKER STORAGE costs $2.99 a month, which is $35.88 a year. Still using it?

Java does every bit of the math, in whole cents. An AI writes the story around the numbers,
and a check you write yourself throws out any card where the AI made a number up.

Use the fake data that comes with the repo, or your own bank's export. Your own data never
leaves your computer: git ignores it, the tests check that, and the website step refuses it.

A workshop for Hillsborough College students. Everything in `data/` is fictional: the banks,
the Rivera family and every number were made up by `data/generate_mock_data.py`.

![Monthly summary mockup](docs/mockups/1-monthly-summary.png)

## What you really learn: the agentic loop

Every step of every week is the same loop, and by week 4 it is a habit:

```
  plan a small step  ──►  ask your agent (or write it)  ──►  ./mvnw test  ──►  read the result
         ▲                                                                          │
         └──────────────────────────────  fix, or take the next step  ◄─────────────┘
```

The tests are already written. You make them pass, one at a time, and you explain each change.

## The schedule

Each session is about an hour: a short demo, then you start the loop and finish it at home.

| Week | Date | You build | Your check |
|---|---|---|---|
| 0 | before Feb 1 | [Setup](docs/SETUP.md): Java 21, git, this repo | `./mvnw test -Dweek=1` runs |
| 1 | Mon Feb 1 | [Five messy banks, one clean shape](weeks/week-1/README.md) | `./mvnw test -Dweek=1` |
| 2 | Mon Feb 8 | [Categories, the review queue, and your own bank](weeks/week-2/README.md) | `./mvnw test -Dweek=2` |
| 3 | Mon Feb 22 | [Money Wrapped: facts, AI cards and the grounding check](weeks/week-3/README.md) | `./mvnw test -Dweek=3` |
| 4 | Mon Mar 1 | [The viewer, CI and CD, and your first release](weeks/week-4/README.md) | `./mvnw test -Dweek=4` |
| 5 | Mon Mar 8 | [Open lab (optional)](weeks/week-5-open-lab/README.md) | `./mvnw test -Dweek=5` |

## How it fits together

```
data/raw/*.csv        ──►  ./run structure  ──►  out/transactions.json  ──►  ./run wrapped  ──►  out/wrapped.json
(or data/private/)         read, clean,          one clean shape,           facts in Java,        facts and cards
                           categorize            see contract/              cards from AI,
                                                                            grounding check
                                                         └──────────────────┬──────────────────┘
                                                                     ./run serve: the viewer
```

## Quick start

You need Java 21 and git ([docs/SETUP.md](docs/SETUP.md)). Maven comes with the repo (`./mvnw`).

```
git clone <the repo link from your instructor>
cd hcc-bank-viewer-workshop
./mvnw test -Dweek=1            # the week 1 tests: they fail until you write the code. That is the point.
```

Then open [weeks/week-1/README.md](weeks/week-1/README.md) and go step by step.

Want to see the finished thing first? Run the instructor's build:

```
./run --reference structure
./run --reference wrapped --ai mock
./run --reference serve          # then open http://localhost:8000
```

On Windows use `mvnw.cmd` and `run.cmd` instead of `./mvnw` and `./run`.

## Why this stack

- **Java 21 with Maven.** You know Java from class, and Maven is what real Java teams use. The
  wrapper (`./mvnw`) downloads the right Maven for you, so there is nothing extra to install.
- **Tests first.** JUnit tests for each week are already in `src/test/`. Green means done.
- **A plain HTML, CSS and JavaScript viewer.** No framework and no build step. Java serves it.
- **AI is optional and always checked.** `--ai mock` is a free, offline fake AI that is enough for
  every test. The real Claude call shows you exactly what it would send before anything leaves.

## Map of the repo

| Folder | What is in it |
|---|---|
| `weeks/` | One guide per session: goal, steps, prompts for your agent, a "Stuck?" table |
| `src/main/java/moneywrapped/` | **Your code.** Starter files with TODOs |
| `rules.csv` | **Your category rules.** Starts as a copy of `contract/starter_rules.csv` |
| `src/test/java/moneywrapped/` | The weekly tests. Read them; do not change them |
| `contract/` | The data contract, the Money Wrapped fact rules, the schema, the account list |
| `data/` | The fake bank exports, answer keys, a bonus bank, a sample output, and `private/` for yours |
| `viewer/` | The browser app: Wrapped cards, summary, ledger and review queue |
| `reference/` | The finished Java build and the CI and CD workflows. Peek when stuck, then write your own |
| `python-lane/` | Optional: weeks 1 and 2 in Python, and a checker that works for any language |
| `docs/` | Setup, CI and CD guide, instructor notes, mockups |
| `tools/selftest.py` | The instructor's whole repo check |

## Using an AI coding agent?

Please do. That is the skill this workshop is about. Point your agent at [AGENTS.md](AGENTS.md).
It tells any agent to work in your files, explain each step, run the tests, keep your own bank
data private, and never copy the reference. The goal is code you can explain yourself.
