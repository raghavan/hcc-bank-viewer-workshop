# Week 1 (Mon Feb 1): Meet the data, read one bank

**Goal for tonight:** your program reads ONE messy bank file and writes clean rows
that follow the contract. That is the whole idea of "structuring" data, on a small scale.

By the end you will have:

1. a working setup (Python, git, an editor)
2. read the contract, the one page every structurer in this room agrees on
3. a structurer that turns `data/raw/gulfview_checking.csv` into clean JSON
4. a green `python tools/check.py --week 1`
5. your first CI pipeline, written by your AI agent and reviewed by you

> You can work in any language. The checker only reads your output file.
> The steps below use the Python starter; a Java starter is in `starter/java/`.

## Steps

### 1. Setup (15 min)

Missed setup night? Follow [docs/SETUP.md](../../docs/SETUP.md), then:

```
git clone <your repo link>
cd hcc-bank-viewer-workshop
python tools/check.py --week 1
```

The first three lines should say PASS (Python, git, inside a repo). The rest fail for now. That is expected.

### 2. Look at the raw data (20 min)

Open the five files in `data/raw/` in your editor. Do not code yet. For each one, write down:

- How are dates written?
- Where is the amount, and how do you tell money in from money out?
- What would trip up a program? (junk lines, odd spacing, `$` signs)

Then open `contract/CONTRACT.md` and compare your notes with its table.

**Checkpoint:** you can explain why `"1,650.00"` in a Debit column becomes `-165000`.

### 3. Get your own copy of the starter (5 min)

```
python tools/start_my_work.py python
python my-work/python/structure.py
```

It runs and writes `out/transactions.json` with 0 rows. Your job is to fill in the TODOs.

### 4. Two helpers (30 min)

Fill in `clean_description` and `to_cents`. Test each one by hand in the Python prompt:

```
cd my-work/python
python
>>> from structure import clean_description, to_cents
>>> clean_description("  payroll   acme tech ")
>>> to_cents("1,650.00")
```

Ask yourself: what should `to_cents("")` return? Why is `Decimal` safer than `float` for money?

### 5. Read Gulfview (45 min)

Fill in `read_gulfview`. Use `csv.DictReader` so you can write `row["Debit"]` instead of counting columns.

Each row needs an id like `gulfview-1` (data rows only, counting from 1), an ISO date,
the account name from `contract/accounts.csv`, a cleaned description, and one signed amount in cents.

```
python my-work/python/structure.py
python tools/check.py --week 1
```

**Checkpoint:** `Gulfview: all 67 expected rows are there` and every field matches.

## Pipeline step: your first CI (20 min)

You will not type YAML by hand. You ask your AI agent, then you check its work.
Background and words: [docs/CI_CD.md](../../docs/CI_CD.md).

**Prompt to try** (paste it into your agent):

> Create a GitHub Actions workflow at .github/workflows/checks.yml for this repo. On every push to main,
> it should check out the code, set up Python 3.12, and run `python tools/ci_local.py --week 1`.
> Give it read only permissions and a 10 minute timeout. Do not add any secrets.
> Then explain every line to me like I am new to this.

**What to verify yourself:**

- [ ] The file is in `.github/workflows/` and the trigger says `push`.
- [ ] `permissions` is `contents: read` and nothing more.
- [ ] Every `uses:` line is from `actions/` with a version like `@v4`.
- [ ] You can say what each step does in your own words.
- [ ] `python tools/ci_local.py --week 1` passes on your computer first.

**Check it works:** commit, push, and open the **Actions** tab on GitHub. A green check mark next to your commit means CI passed.
If the run never starts, see "When GitHub Actions does not start" in [docs/CI_CD.md](../../docs/CI_CD.md) and use `python tools/ci_local.py --week 1` instead.

## Stuck?

| You see | Try |
|---|---|
| `FileNotFoundError` | Run commands from the repo root folder, not from inside `my-work/`. |
| Amounts off by a factor of 100 | You stored dollars, not cents. Multiply by 100 before `int()`. |
| Amounts have the wrong sign | Credit is money in (positive), Debit is money out (negative). |
| `ValueError` on a date | Gulfview dates are `%m/%d/%Y`. Print the text you are parsing. |
| Descriptions do not match | Two spaces inside `PAYROLL  ACME TECH` must become one. |
| Using an AI helper | Ask it to explain, not to write the function. Paste the FAIL line and ask "what does this mean?" |

Still stuck? Peek at `reference/python/readers.py`, close it, and write your own version.

## What to commit

```
git add my-work .github
git commit -m "Week 1: read Gulfview, add CI"
git push
```

Do not commit `out/`. It is rebuilt every time you run your program (and `.gitignore` skips it).

**Next week:** the other four banks, each with its own surprise.
