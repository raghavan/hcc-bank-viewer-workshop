# Week 2 (Mon Feb 8): Categories, the review queue, and your own bank

**The story:** "Where did my money go?" needs categories. Rules handle the easy rows. Money
that only moved between your own accounts is not spending at all. And anything you are not
sure about goes to a review queue for a person, instead of being quietly guessed.

This is also the week you can point the program at **your own bank's export**, if you want to.
It is optional, and the mock data works for everything.

By the end of the week you will have:

1. transfers between your own accounts found and marked
2. a rules file and the code that applies it
3. a review queue that holds only the few rows a person should look at
4. optionally, a profile that reads your own bank's CSV, kept off git
5. a green `./mvnw test -Dweek=2`

## In class (about an hour)

1. **Demo (15 min).** Your instructor runs `./run structure` on the starter rules and shows the
   review queue in the viewer (`./run --reference serve`, then the Review queue tab). Then they
   point at one row that a rule got wrong and ask: which rule did that?
2. **Your turn (45 min).** Make `markTransfers` pass, then start on the rules.

## Steps

### 1. Read the rules of the game (10 min)

In `contract/CONTRACT.md`, read **The review rule**, **Transfers** and **Rules files**.

**Checkpoint:** you can say in one sentence why a 0.6 confidence rule puts a row in the review queue.

### 2. Transfers first (30 min)

Fill in `markTransfers` in `Categorizer.java`. A pair is money out of one account and the same
amount into another, within 3 days, with TRANSFER, PAYMENT or AUTOPAY in the description.
Mark both rows.

Why first? A card payment says PAYMENT, and you do not want a rule to call it spending.

**Checkpoint:** `transfersArePairedLikeTheAnswerKey` passes. There are 24 pairs in six months.

### 3. Rules (40 min, then at home)

Fill in `loadRules` and `applyRules`. Your rules live in `rules.csv` at the top of the repo; it
starts as a copy of `contract/starter_rules.csv`. Then:

```
./mvnw test -Dweek=2
./run structure
./run --reference serve        # the viewer, on YOUR output. Open the Review queue tab.
```

The test `yourRulesPlaceAlmostEveryRowCorrectly` will say the review queue is far too big. Good:
now you know what to do. Find merchants that repeat in the queue and add rules for them.

**Watch for the trap.** One starter rule is too broad and puts rows in the wrong category. The
test lists rows that a rule decided wrongly. Find the rule, and fix the pattern so it only
matches what it means.

**Checkpoint:** at least 95% of decided rows are right, and the queue holds between 1 row and
10% of all rows (aim for about 10).

**Prompt to try:**

> Run ./run structure and tell me which merchants appear most often among the rows that still
> need review. Do not edit rules.csv. Suggest three rules with a pattern, a category and a
> confidence, and tell me why each pattern will not catch anything else.

### 4. Optional: your own bank (30 min)

Most banks let you download your transactions as CSV. Every bank's file looks different, so
instead of a new reader for each, you write `Readers.fromProfile`: one reader that follows a
small settings file.

1. Fill in `fromProfile`. Two tests use a made up bank (`data/private-example/`) to check it.
2. Copy `data/private-example/seagrape.properties` into `data/private/` and rename it.
3. Put your own export next to it, and change the settings to match your file's columns.
4. Run it:

```
./run structure --data private
./mvnw test -Dweek=2 -Ddata=private      # checks your rows follow the contract
./run --reference serve
```

**Your data stays on your computer.** `data/private/` is in `.gitignore`, and
`PrivacyTest` fails if anything in it except its README is ever committed. Two more rules:

- Do not let your AI agent open your export. Anything it reads goes to its company's servers.
  Show it only your header line, or work on the Seagrape example, which has the same problems.
- If you are not comfortable with any of this, use the mock data. You lose nothing.

When you are done, run `./run structure` (mock data again) before you commit, so your normal
output is back to the fake bank.

### 5. Stretch: decisions from the review queue

In the viewer's Review queue, approve or change a few rows, then press **Download decisions.json**.
Feed it back:

```
./run structure --decisions decisions.json
```

The queue shrinks. A person is still in charge, and the program remembers what they decided.

## Pipeline step: CI that gets stricter (10 min)

Change `-Dweek=1` to `-Dweek=2` in your `checks.yml` (ask your agent, then check the diff).
Push, and watch the Actions tab. CI now protects everything you built in weeks 1 and 2.

## Stuck?

| You see | Try |
|---|---|
| Transfers: wrong pairs | Sort the candidates by date, then id, before you match. Each row joins one pair at most. |
| `category "Food" is not on the list` | Use exactly the category names in `contract/CONTRACT.md`. |
| A rule never matches | Patterns are matched against the CLEANED description, so clean the pattern too. |
| Too many rows in the queue | Add rules for merchants that repeat. Put specific patterns above broad ones. |
| Queue is empty | A low confidence rule should still leave its rows for a person. |
| `the setting date_column is missing` | Your profile needs that line. Compare it with the Seagrape example. |
| `DateTimeParseException` on your own data | Your bank's date format differs. Look at one date and change `date_format`. |

Still stuck? Peek at `reference/java/src/main/java/moneywrapped/Categorizer.java`, close it, and write your own.

## What to commit

```
git status                     # make sure nothing from data/private/ is listed
git add src rules.csv .github
git commit -m "Week 2: transfers, rules and the review queue"
git push
```

Do not commit your `.properties` profile if it names your bank or account in a way you would
rather keep private. It is ignored by git like everything else in `data/private/`.

**Next week:** Money Wrapped itself. Facts, AI cards, and a check that catches AI making things up.
