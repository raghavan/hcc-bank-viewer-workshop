# Week 2 (Mon Feb 8): Five banks, one shape

**Goal for tonight:** read all five bank exports into the same clean shape.
Once every bank looks the same, the rest of the program never has to care which bank a row came from.

By the end you will have:

1. readers for Coquina, Harborline, Palmetto and Sunpine
2. all 286 rows in `out/transactions.json`
3. a green `python tools/check.py --week 2`

## Steps

### 1. Warm up (10 min)

```
git pull
python my-work/python/structure.py
python tools/check.py --week 1
```

Week 1 should still pass. If it does not, fix that first.

### 2. One bank at a time (25 min each)

Do them in this order. After each one, run your program and `python tools/check.py --week 2`.
The checker reports each bank separately, so you can watch the list turn green.

| Bank | The surprise | Question to ask yourself |
|---|---|---|
| Coquina Savings | `"Mar 26, 2027"` and `$300.00`, newest first | Which `strptime` code means "Mar"? Does the order of rows change the ids? |
| Harborline Visa | A purchase is **positive** | On a card statement, whose point of view is the sign from? |
| Palmetto Mastercard | 3 junk lines before the header; amounts are always positive | How do you skip lines before handing the file to `csv`? What does Type tell you? |
| Sunpine Debit | lowercase, `DR`/`CR`, a time in the date, a `# end of export` line | Which part of `2026-10-05T21:54:00` do you keep? |

**Checkpoint:** `no unexpected rows` and every bank shows `all expected rows are there`.

### 3. Look at what you built (10 min)

```
python tools/show_totals.py
```

Every row is still "Other" because nothing is categorized yet. That is next week.
Notice the transfers (money moving between your own accounts). They make spending look bigger than it is.

### 4. Stretch

- Make your program print how many rows each bank gave.
- Write a tiny test: `assert to_cents("$1,234.56") == 123456`.
- Harborline has its own Category column. Compare it with what you think each row is. Is the bank always right?

## Pipeline step: watch CI catch a mistake (15 min)

**Prompt to try:**

> Update .github/workflows/checks.yml so it runs the week 2 checks instead of week 1.
> Show me the diff before you change anything.

Now break something on purpose: flip the sign in one reader, commit and push.

**What to verify yourself:**

- [ ] The agent changed only the week number (read the diff).
- [ ] The broken push turns **red**, and the run log names the bank that failed.
- [ ] After you fix it and push again, it turns green.

**Check it works:** your commit list on GitHub shows one red X followed by a green check.
No Actions? Run `python tools/ci_local.py --week 2` before and after the fix.

Talk about it: why is it good that the robot is strict?

## Stuck?

| You see | Try |
|---|---|
| `KeyError: 'Date'` on Palmetto | The header is not the first line. Print the first 5 lines and look. |
| Sunpine has one extra row | The last line starts with `#`. Skip comment and blank lines. |
| Harborline amounts have the wrong sign | Flip them: a positive card charge is money out for us. |
| Coquina ids are in the wrong order | Ids follow the row order in the file, even if the file is newest first. |

Still stuck? Peek at `reference/python/readers.py`.

## What to commit

```
git add my-work .github
git commit -m "Week 2: read all five banks"
git push
```

**Next week (Feb 22, no session Feb 15):** categories, transfers and a review queue.
