# Week 1 (Mon Feb 1): Five messy banks, one clean shape

**The story:** every bank exports your money differently. Before anything can tell you where
your money went, a program has to read all of them and agree on one clean shape. That is
"structuring" data, and it is most of real data work.

**The loop you practice:** read a failing test, plan a small step, make it (with your agent or
by hand), run `./mvnw test -Dweek=1`, read the result, repeat.

By the end of the week you will have:

1. `Money.clean` and `Money.toCents`, the two helpers every reader uses
2. a reader for each of the five banks in `Readers.java`
3. a green `./mvnw test -Dweek=1` (11 tests)
4. your first CI pipeline, written by your agent and reviewed by you

## In class (about an hour)

1. **Demo (15 min).** Your instructor opens `data/raw/`, then runs `./mvnw test -Dweek=1` on the
   starter and reads one red test out loud. Then they ask an agent to make `Money.clean` pass,
   and read its change line by line before running the tests again.
2. **Your turn (45 min).** Get set up, run the tests, and make `Money.clean` green yourself.
   Leave with a plan for the other TODOs.

## Steps

### 1. Setup and the first run (10 min)

Not set up yet? Follow [docs/SETUP.md](../../docs/SETUP.md). Then:

```
./mvnw test -Dweek=1
```

Every test fails with a message like `TODO week 1: write Money.clean`. That is your to do list.

### 2. Look at the data before you code (15 min)

Open the five files in `data/raw/` in your editor. For each one, write down:

- How are dates written?
- Where is the amount, and how do you tell money in from money out?
- What would trip up a program? (junk lines, odd spacing, `$` signs, a `#` line at the end)

Then compare your notes with the table in `contract/CONTRACT.md`.

**Checkpoint:** you can explain why `"1,650.00"` in a Debit column becomes `-165000`.

### 3. The two helpers (30 min)

Open `src/main/java/moneywrapped/Money.java`. Fill in `clean` and `toCents`. Read the hints in
the TODO comments first, and the test `Week1Test.toCentsReadsMoneyTextExactly` to see exactly
what goes in and what must come out.

Ask yourself: what should `toCents("")` return? Why is `BigDecimal` safer than `double` for money?

**Prompt to try** (paste it into your agent):

> I am learning Java. Open src/main/java/moneywrapped/Money.java and the test Week1Test.java.
> Do not write the code yet. Explain what Money.toCents must do, using the test cases, and
> ask me how I would handle "-$25.00". Then let me try first.

### 4. One bank at a time (the rest of the week)

Fill in the readers in `Readers.java` in this order: `gulfview`, `coquina`, `harborline`,
`palmetto`, `sunpine`. After each one, run the tests. Each bank has its own test, so you see
one go green at a time:

```
./mvnw test -Dweek=1
./run structure          # writes out/transactions.json; open it and look
```

Each row needs an id like `gulfview-1` (data rows only, from 1), an ISO date, the account name,
a cleaned description, and one signed amount in cents. `Txn`'s constructor does the id and the
cleaning for you.

**Checkpoint:** `./run structure` prints all five banks: 67, 12, 102, 54 and 51 rows (286 in all).

## Pipeline step: your first CI (20 min)

You will not type YAML by hand. You ask your agent, then you check its work.
Background and words: [docs/CI_CD.md](../../docs/CI_CD.md).

**Prompt to try:**

> Create a GitHub Actions workflow at .github/workflows/checks.yml for this repo. On every push to
> main and every pull request, it should check out the code, set up Java 21 (temurin) with the
> maven cache, and run `./mvnw -B test -Dweek=1`. Give it read only permissions and a 10 minute
> timeout. Do not add any secrets. Then explain every line to me like I am new to this.

**What to verify yourself:**

- [ ] The file is in `.github/workflows/` and the trigger says `push`.
- [ ] `permissions` is `contents: read` and nothing more.
- [ ] Every `uses:` line is from `actions/` with a version like `@v4`.
- [ ] The command is exactly the one you run at home.
- [ ] You can say what each step does in your own words.

**Check it works:** commit, push, and open the **Actions** tab on GitHub. A green check next to
your commit means CI passed. Red is fine this week: it shows CI is doing its job while your
TODOs are open. If the run never starts, see "When GitHub Actions does not start" in
[docs/CI_CD.md](../../docs/CI_CD.md) and keep using `./mvnw test` on your computer.

## Stuck?

| You see | Try |
|---|---|
| `TODO week 1: ...` | That method is still a TODO. That is the next thing to write. |
| `gulfview-3: amount_cents is ..., expected ...` | Read the first problem: it names the row, what you made, what was expected, and a hint. |
| Amounts off by a factor of 100 | You stored dollars, not cents. |
| Amounts have the wrong sign | Negative means money left the account. A credit card purchase is money out. |
| `DateTimeParseException` | Print the text you are parsing and compare it with your pattern. |
| `NumberFormatException` in toCents | Remove `$` and `,` before you make the `BigDecimal`. |
| The agent wants to copy `reference/` | Say no, and ask it to explain the idea instead. See `AGENTS.md`. |

Still stuck? Peek at `reference/java/src/main/java/moneywrapped/Readers.java`, close it, and write your own.

## What to commit

```
git add src rules.csv .github
git commit -m "Week 1: read all five banks, add CI"
git push
```

`out/` and `target/` are rebuilt every run, and `.gitignore` skips them.

**Next week:** categories, a review queue, and (if you want) your own bank's export.
