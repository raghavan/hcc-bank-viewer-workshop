# The contract

This page is the one thing every student shares. The main track is Java, but the
contract is about files, not code: any program that writes files that follow it can be
checked by `python python-lane/tools/check.py` and shown by the viewer in `viewer/`.

Think of it like an electrical outlet: the shape of the plug is fixed, and you can build
any appliance behind it.

## What your structurer reads

The five files in `data/raw/`. Each one comes from a different (fictional) bank, and
each one formats the same idea differently.

| Bank id | File | Account name | What is odd about it |
|---|---|---|---|
| `gulfview` | `gulfview_checking.csv` | Gulfview CU Checking | Money out and money in are in two columns (`Debit`, `Credit`). Amounts have thousands commas. Dates are `MM/DD/YYYY`. Extra spaces in descriptions. |
| `coquina` | `coquina_savings.csv` | Coquina Savings | Dates look like `Mar 26, 2027`. Amounts look like `$300.00` or `-$25.00`. Newest row first. |
| `harborline` | `harborline_visa.csv` | Harborline Visa | A credit card: purchases are **positive** and payments are **negative**, the opposite of our rule. Has its own `Category` column that does not match ours (and is sometimes wrong). Use `Trans. Date`, not `Post Date`. |
| `palmetto` | `palmetto_mastercard.csv` | Palmetto Mastercard | Three junk lines before the header. Dates are `M/D/YY`. Amounts are always positive; `Type` says `SALE`, `RETURN` or `PAYMENT`. |
| `sunpine` | `sunpine_debit.csv` | Sunpine Debit | Date and time together (`2027-03-04T14:22:00`). Lowercase text. `DR` means money out, `CR` means money in. Ends with a `#` comment line. |

The same table lives in `contract/accounts.csv` so your code can read it.

### Your own bank (week 2)

Instead of the five fake banks, `./run structure --data private` reads every profile in
`data/private/`. A profile is a small `.properties` file that says where your export is and
which columns hold the date, the description and the money. Every setting is explained in
`data/private-example/seagrape.properties`. Your rows get the profile's `bank_id` in their
ids (`mybank-1`, `mybank-2`, ...) and the profile's `account_name`. Git ignores
`data/private/`, so your files stay on your computer.

## What your structurer writes

One JSON file. By default: `out/transactions.json`.

```json
{
  "schema_version": "1.0",
  "data_source": "mock",
  "transactions": [
    {
      "id": "gulfview-1",
      "date": "2026-10-01",
      "account": "Gulfview CU Checking",
      "description": "PALM COURT APARTMENTS RENT",
      "amount_cents": -165000,
      "category": "Housing",
      "category_source": "rule",
      "rule_id": "H1",
      "confidence": 1.0,
      "needs_review": false,
      "is_transfer": false
    }
  ]
}
```

The machine readable version is `contract/transaction.schema.json`.

`data_source` is `mock` for the fake banks and `private` for your own data. Tools use it
to keep your data safe: `./run site` refuses to publish anything that is not `mock`.

### The fields, one by one

| Field | Type | Rule |
|---|---|---|
| `id` | text | `<bank id>-<row number>`. Row number counts only real transaction rows in the file, starting at 1, in the order they appear in the file. Skip the header, junk lines, blank lines and `#` lines. Two identical coffees on the same day get different ids, so both are kept. |
| `date` | text | `YYYY-MM-DD`. For Harborline use `Trans. Date`. For Sunpine drop the time. |
| `account` | text | The account name from `contract/accounts.csv`. |
| `description` | text | Uppercase, spaces trimmed from both ends, and runs of spaces squeezed to one. `"gulf  coast grocers  "` becomes `"GULF COAST GROCERS"`. |
| `amount_cents` | whole number | Money in **cents**, from the household's point of view. **Negative = money left the account. Positive = money came in.** `$1,650.00` paid out is `-165000`. |
| `category` | text or `null` | One of the categories below. `null` is allowed only in week 1. |
| `category_source` | text | Who decided the category: `rule`, `transfer`, `ai`, `ai_approved`, `manual` or `none`. |
| `rule_id` | text or `null` | Which rule matched, when `category_source` is `rule` or `transfer`. |
| `confidence` | number or `null` | From 0 to 1. Rules usually say 1.0. Broad rules and AI guesses say less. |
| `needs_review` | true or false | See "The review rule" below. |
| `is_transfer` | true or false | True when money only moved between the household's own accounts (savings transfers and credit card payments). Both sides of the move are marked. |

### Why cents?

Computers store most decimals approximately. In many languages `0.1 + 0.2` is not exactly
`0.3`. Money must add up to the penny, so we store whole cents and only add the dot when
we show the number to a person.

### The categories (fixed list)

`Income`, `Housing`, `Groceries`, `Dining`, `Shopping`, `Transport`, `Utilities`,
`Health`, `Subscriptions`, `Other`, `Transfer`

Use exactly these spellings. An AI suggestion that is not on this list is thrown away.

### The review rule

A row needs a human (`needs_review: true`) when any of these is true:

- nothing decided its category (`category_source` is `none`)
- an AI suggested it and nobody approved it yet (`category_source` is `ai`)
- a rule matched but its confidence is below `0.8`

Everything else has `needs_review: false`.

### Transfers

A transfer is a pair: money leaves one of the household's accounts and the same amount
arrives in another one within 3 days. Credit card payments count, because paying your
own card is not spending. Mark **both** rows with `is_transfer: true`, category
`Transfer`, and leave them out of income and spending totals.

## How the viewer adds things up

Every viewer must get the same numbers, so the math is part of the contract.

1. Ignore rows where `is_transfer` is true.
2. **Income** for a month = the sum of `amount_cents` of rows in category `Income`.
3. **Spending in a category** = minus the sum of `amount_cents` of that category's rows.
   (A refund is positive, so it lowers spending, which is what you want.)
4. **Spending** = the sum over every category except `Income`.
5. **Net saved** = income minus spending.

To see the numbers your viewer should show, run:

```
python python-lane/tools/show_totals.py out/transactions.json
```

## Rules files

Rules are plain CSV so any language can read them:

```
rule_id,pattern,category,confidence
G1,SUNFRESH MARKET,Groceries,1.0
G9,MARKET,Groceries,0.6
```

A rule matches when its `pattern` appears anywhere inside the cleaned description.
**The first matching rule wins**, so put specific patterns above broad ones.

- `contract/starter_rules.csv`: a small set to start from.
- `reference/rules.csv`: the full set the reference build uses.

## Money Wrapped facts (week 3)

`./run wrapped` reads `out/transactions.json` and writes `out/wrapped.json`: a list of
facts, then cards that tell the story. Java computes every fact in whole cents. The
expected facts for the mock data are in `data/answer_key/wrapped_key.json`.

Words used below:

- **Spending rows**: rows that are not transfers and not `Income`. Refunds count, and lower spending.
- **Everyday spending**: spending rows without `Housing`, so rent does not win every time.
- **Merchant**: the description without trailing words that hold a digit or start with `#`,
  then without a trailing `REFUND`. `SUNFRESH MARKET #214` and `SUNFRESH MARKET #88` are both `SUNFRESH MARKET`.
- **Percent**: a whole number, rounded half up, computed with whole numbers only:
  `pct(num, den) = floor((200 * num + den) / (2 * den))`.
- **Ties** go to the name that comes first in alphabetical order, unless a fact says otherwise.

| Fact id | What it is |
|---|---|
| `period` | How many months the data covers, first month to last month. |
| `money_in` | The sum of `Income` rows. |
| `money_out` | Minus the sum of spending rows. |
| `savings_rate` | `money_in - money_out` as a percent of `money_in`, and the amount kept. |
| `top_category` | The category with the most everyday spending, its amount, and its percent of `money_out`. |
| `top_merchant` | The merchant with the most everyday spending, its amount, and its visits (money out rows). |
| `most_visited` | The merchant with the most visits. Ties: more money first, then the name. |
| `biggest_month` | The month with the most spending, rent included. Ties: the earlier month. |
| `weekend_share` | Spending on Saturdays and Sundays, and its percent of `money_out`. |
| `biggest_purchase` | The single money out row with the largest amount, not `Housing` and not `Utilities`. Ties: the earlier date, then the id. |
| `recurring` | Merchants with money out rows in at least 3 different months, exactly one charge in each of those months, and every charge within 10% of the median charge (for an even count, the lower middle one). The monthly amount is that median. Biggest first, then by name. |
| `quiet_subscription` | The smallest `recurring` charge, and 12 times it for a year. |

Each fact has an `id`, a `label`, a `text` sentence with the numbers in it, a list of
`values` (the numbers as a person reads them, like `$1,956.42` or `45%`) and `data` (the
same numbers in cents, for code).

### Cards and the grounding check

A card has a `title`, a `caption` and `fact_ids`: the facts it is built on. Cards can come
from a template (`--ai off`), a free fake AI (`--ai mock`) or Claude (`--ai claude`).

A card is **grounded** when it cites at least one fact, every cited fact exists, and every
number in its title and caption (money, percents and counts) appears in the text or the
values of a fact it cites. `$1,650` and `$1,650.00` are the same number. Cards that are not
grounded go to `rejected` with a `reason`, and the viewer shows them crossed out.

Only facts go to an AI: their `id`, `label`, `text` and `values`. Never rows, account names
or row ids. `--ai claude` writes exactly what it would send to `out/ai_request_preview.json`
and sends nothing until you add `--send`.

## Decisions file (optional, week 2)

The viewer's review queue can save your choices as `decisions.json`. A
structurer that supports it reads that file on the next run:

```json
{
  "overrides": {"harborline-88": {"category": "Health", "source": "ai_approved"}},
  "new_rules": [{"pattern": "LUMA YOGA", "category": "Health"}]
}
```
