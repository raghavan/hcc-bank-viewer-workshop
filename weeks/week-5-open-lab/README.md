# Week 5 (Mon Mar 8): Open lab (optional)

Bring questions, finish anything from weeks 1 to 4, or pick a challenge. Each one is a good
story for an interview: "I added X, here is the test that proves it works."

```
./mvnw test -Dweek=5
```

## Challenges

### A. A sixth bank in a new format

`data/bonus/tarpon_checking.json` is a JSON export, not a CSV. Fill in `Readers.tarpon` and the
week 5 test compares it with its own answer key. Then try `./run structure --include-bonus`.

Question: how much of your program had to change? If the answer is "one small method", your
structure is good.

### B. A new Wrapped fact

Invent a fact nobody else has: your most expensive weekday, your longest streak without eating
out, the month you saved the most. Write the test first, then the code, then a card for it.
Does the grounding check still pass on every card?

### C. Your own data, all the way

If you set up your own bank in week 2, run the whole thing on it and look at your own Wrapped.
What surprised you? Which fact would you change your habits over? (Keep it on your computer.)

### D. A better story

Change the instructions Claude gets in `AiCards.java` (the `SYSTEM` text) and compare the cards
with `--ai mock`, or with `--send` if your class has a key. What makes a card good? Count how
many the grounding check rejects before and after.

### E. Make the viewer yours

A dark mode, a budget per category, a share card for one fact, a phone friendly layout. Then cut `v1.1.0`.

### F. Grow the pipeline

Ask your agent for one of these, review it with the checklist in [docs/CI_CD.md](../../docs/CI_CD.md), and prove it runs:

- A status badge for your README that shows green or red.
- A nightly run with `schedule:`, and an explanation of when that is useful.
- A job that runs only when `data/` changes.

Prompt to try: *"Add X to my workflow. Show me the diff first and tell me what could go wrong."*

### G. The Python lane

Write weeks 1 and 2 again in Python (`python-lane/starter/structure.py`) and check them with
`python python-lane/tools/check.py --week 2`. Same contract, same answers. Which was easier to read?

## What to commit

Whatever you built, with a commit message that says what it does, and a new release if it is worth showing.
