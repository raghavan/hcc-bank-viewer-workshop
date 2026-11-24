# The contract

This page is the one thing every student shares. You may write your structurer in
Python, Java, JavaScript, C#, Go or anything else. As long as your program writes a file
that follows this contract, every check in `tools/check.py` and every viewer in this repo
can read it.

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

## What your structurer writes

One JSON file. By default: `out/transactions.json`.

```json
{
  "schema_version": "1.0",
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

### The fields, one by one

| Field | Type | Rule |
|---|---|---|
| `id` | text | `<bank id>-<row number>`. Row number counts only real transaction rows in the file, starting at 1, in the order they appear in the file. Skip the header, junk lines, blank lines and `#` lines. Two identical coffees on the same day get different ids, so both are kept. |
| `date` | text | `YYYY-MM-DD`. For Harborline use `Trans. Date`. For Sunpine drop the time. |
| `account` | text | The account name from `contract/accounts.csv`. |
| `description` | text | Uppercase, spaces trimmed from both ends, and runs of spaces squeezed to one. `"gulf  coast grocers  "` becomes `"GULF COAST GROCERS"`. |
| `amount_cents` | whole number | Money in **cents**, from the household's point of view. **Negative = money left the account. Positive = money came in.** `$1,650.00` paid out is `-165000`. |
| `category` | text or `null` | One of the categories below. `null` is allowed only until week 3. |
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
python tools/show_totals.py out/transactions.json
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

## Decisions file (optional, week 4)

The reference viewer's review queue can save your choices as `decisions.json`. A
structurer that supports it reads that file on the next run:

```json
{
  "overrides": {"harborline-88": {"category": "Health", "source": "ai_approved"}},
  "new_rules": [{"pattern": "LUMA YOGA", "category": "Health"}]
}
```
