# Week 3 (Mon Feb 22): Categories, transfers, and a review queue

**Goal for tonight:** give every row a category, and be honest about the ones you are not sure of.
Rules handle the easy rows. Anything uncertain goes to a review queue for a person (week 4 builds the screen for it).

By the end you will have:

1. transfers between your own accounts found and marked, so they are not counted as spending
2. a rules file and code that applies it
3. a review queue that holds the few rows a person should look at
4. a green `python tools/check.py --week 3`

## Steps

### 1. Read the rules of the game (10 min)

In `contract/CONTRACT.md`, read **The review rule**, **Transfers** and **Rules files**.

**Checkpoint:** you can say in one sentence why a 0.6 confidence rule puts a row in the review queue.

### 2. Find transfers first (40 min)

Fill in `mark_transfers`. A pair is money out of one of your accounts and the same amount into another,
within 3 days, with TRANSFER, PAYMENT or AUTOPAY in the description. Mark both rows.

Why first? A card payment says PAYMENT, and you do not want a rule to call it spending.

**Checkpoint:** the checker says `every transfer pair is found`. There are 24 pairs across six months.

### 3. Rules (40 min)

```
python tools/start_my_work.py rules
```

That gives you `my-work/rules.csv` with seven starter rules. Fill in `load_rules` and `apply_rules`
(the first rule whose pattern appears in the description wins), then run everything.

The checker will say the review queue is far too big. Good: now you know what to do.
Look at the rows still waiting, find merchants that repeat, and add rules for them.

```
python tools/show_totals.py
```

**Watch for the trap.** One starter rule is too broad. The checker prints a NOTE when a decided row has the wrong category.
Find it, and fix the pattern so it only matches what it means.

**Checkpoint:** accuracy at least 95% and a review queue of no more than 10% of rows (aim for about 10 rows).

### 4. Optional: AI help for the leftovers (20 min)

Some merchants show up once (a nursery, a yoga studio). A rule for each is silly. An AI can suggest a category,
but a suggestion is never final: it always lands in the review queue.

The reference shows both modes. Try them and compare:

```
python reference/python/structure.py --ai mock      # a fake AI, free, no key
python tools/check.py --week 3 --file out/reference/transactions.json
```

`--ai claude` calls a real model. It needs `pip install anthropic` and an API key from your instructor; see `docs/INSTRUCTOR.md`.
Nothing in this course requires it.

Talk about it: what should a program do when it is only 48% sure?

## Pipeline step: CI on pull requests (20 min)

Real teams never push straight to main. They open a pull request (PR), and CI runs on it before anyone merges.

**Prompt to try:**

> Update .github/workflows/checks.yml to run on pull requests as well as pushes to main, use week 3 checks,
> and upload out/transactions.json as an artifact named transactions even when a step fails.
> Then walk me through making a branch called rules, committing my rule changes, and opening a pull request.

**What to verify yourself:**

- [ ] `on:` lists both `push` and `pull_request`.
- [ ] The upload step has `if: always()`, so you get the file even from a red run. Ask the agent why that matters.
- [ ] Permissions are still read only.

**Check it works:** your pull request shows the `checks` job with a green check, and the run page has a
**transactions** artifact you can download. Merge only when it is green.
No Actions? Run `python tools/ci_local.py --week 3`, and merge locally with `git merge rules`.

## Stuck?

| You see | Try |
|---|---|
| `0 transfer pairs` | Print the rows whose description has TRANSFER, PAYMENT or AUTOPAY. Are the amounts opposite? |
| Too many transfers | Each row can be in one pair only. Also check the two rows are in different accounts. |
| Review queue too big | Add rules for merchants that appear more than twice. `show_totals` helps you spot them. |
| Review queue is 0 | Rows nobody placed must stay `needs_review: true` with category "Other". |
| Accuracy below 95% | Read the examples under the FAIL line. Which rule id caught them? |

Still stuck? Peek at `reference/python/categorize.py` and `reference/rules.csv`.

## What to commit

```
git add my-work .github
git commit -m "Week 3: categories, transfers and review queue"
git push
```

**Next week:** the viewer, and your first release.
