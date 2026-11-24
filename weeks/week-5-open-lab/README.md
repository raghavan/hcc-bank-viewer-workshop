# Week 5 (Mon Mar 8): Open lab (optional)

Bring questions, finish anything from weeks 1 to 4, or pick a challenge. Each one is a good story for an interview.

```
python tools/check.py --week 5
```

## Challenges

### A. A sixth bank in a new format

`data/bonus/tarpon_checking.json` is a JSON export, not a CSV. Dates look like `20270305` and the amounts are already signed our way.
Add a reader for it (bank id `tarpon`, account name "Tarpon Trust Checking") and the week 5 check compares it with its own answer key.

Question: how much of your program had to change? If the answer is "one small function", your structure is good.

### B. The decisions loop

Make the review queue remember choices and download `decisions.json`. Teach your structurer to read it
(format in `contract/CONTRACT.md`). Run it again: the queue should shrink. That loop, person corrects, program learns, is how real tools improve.

### C. Real AI for the leftovers

Try `--ai claude` in the reference with a key from your instructor, then add it to your own structurer.
Rules: never let the AI decide alone (`needs_review` stays true), cache answers so you never pay twice, and keep it working with no key at all.

### D. Port it

Rewrite your structurer in another language (Java starter in `starter/java/`, or anything else).
Same contract, same checker. Compare the two: which was easier to read?

### E. Make the viewer yours

A dark mode, a chart library, a budget per category, an export to CSV, a phone friendly layout. Then cut `v1.1.0`.

## What to commit

Whatever you built, with a commit message that says what it does, and a new release if it is worth showing.
