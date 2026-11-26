# Week 3 (Mon Feb 22): Money Wrapped

**The story:** this is the payoff. Your program now knows where every dollar went. Tonight it
finds the surprising things in that data and tells them as a story, like Spotify Wrapped:

> **Your coffee habit, by the numbers.** CORNER BEAN CAFE was your most visited place: 29 visits and $195.75.

There is one big rule, and it is the most important idea in this workshop:

**Java does the math. The AI only tells the story. And a check you write proves the AI did not
make a number up.**

An AI is great with words and sometimes invents numbers. So the facts come from your code, in
whole cents, and every card the AI writes goes through a **grounding check** before a person sees it.

By the end of the week you will have:

1. `Grounding.check`, which throws out any card with a number that is not in the facts
2. nine more facts in `Wrapped.facts`, matching the answer key
3. `Wrapped.recurring`, which finds the charges that quietly repeat every month
4. cards from the free fake AI, and (if your class uses it) from Claude
5. a green `./mvnw test -Dweek=3`

## In class (about an hour)

1. **Demo (20 min).** Your instructor runs `./run --reference wrapped --ai mock` and opens the
   Wrapped tab. One card says something about tacos. It is crossed out. Why? Then they run
   `--ai claude` without `--send`, open `out/ai_request_preview.json`, and ask the room: is
   there anything in here you would not want to send?
2. **Your turn (40 min).** Write the grounding check and watch it catch the taco card.

## Steps

### 1. Look at the finished thing (5 min)

```
./run structure
./run --reference wrapped --ai mock
./run --reference serve        # open the Wrapped tab, then press Ctrl+C to stop
```

Read the facts on the left and the rejected card on the right. Tonight you build the check
that rejected it, and this week you build the facts.

### 2. The grounding check (30 min, in class)

Fill in `Grounding.check`. `numbersIn` is done: it turns "$1,956.42 over 24 visits" into the
numbers 1956.42 and 24. A card passes when it cites at least one real fact and every number in
its title and caption is in a fact it cites. The test
`groundingKeepsTrueCardsAndRejectsMadeUpNumbers` has seven cards to try.

```
./mvnw test -Dweek=3
./run wrapped --ai mock
```

The fake AI always writes one card that claims $9,999.00 on tacos. Your check catches it, and
the viewer shows it crossed out under "Caught by the grounding check". (It writes a card for
each fact it knows, so you get more cards as you add facts: six once all twelve are done.)

**Prompt to try:**

> Read Grounding.java and the grounding test in Week3Test.java. Do not write check() for me.
> Walk me through the seven test cards one by one and ask me whether each should pass and why.

### 3. The facts, one at a time (at home)

Your build has three facts so far: `period`, `money_in` and `money_out`. Read how `money_in`
is made in `Wrapped.java`. Every other fact has the same shape. Then open
`data/answer_key/wrapped_key.json` and read the sentence for each fact: those are the answers
for the mock data. The rule for each one is in `contract/CONTRACT.md`, under
**Money Wrapped facts**.

Add the facts in the order the TODO lists them. After each one:

```
./mvnw test -Dweek=3
./run wrapped && ./run --reference serve
```

`everyFactMatchesTheAnswerKey` tells you which fact is next and what it should say.

**Prompt to try:**

> I am adding the top_merchant fact in Wrapped.java. Read its rule in contract/CONTRACT.md.
> Do not write it for me. Ask me three questions that lead me to the right grouping, and tell
> me which existing helper in Money.java I should use.

Two facts need more thinking:

- **recurring**: a merchant counts when it charges about the same amount once a month, for at
  least 3 months. Write `recurring()` first, then the fact that uses it.
- **quiet_subscription**: the smallest repeat charge. $2.99 does not feel like much. Times 12?

### 4. Real AI, safely (20 min)

```
./run wrapped --ai claude
```

This sends **nothing**. It writes `out/ai_request_preview.json`, exactly what would go to the
Claude API. Open it. You will find the facts, and no rows, no account names and no row ids.
The test `theAiSeesFactsButNeverRowsOrAccounts` keeps it that way.

To really call Claude you need an API key, and each call costs money, billed to whoever owns
the key. Your instructor will tell you whether your class uses one. If it does:

```
export ANTHROPIC_API_KEY=...          # Windows PowerShell: $env:ANTHROPIC_API_KEY="..."
./run wrapped --ai claude --send
```

Never commit a key, and never paste it into a chat with an agent. The cards come back, go
through your grounding check, and show up on the Wrapped tab.

On your own data, the same rule holds: only the facts (totals and merchant names) are sent,
never your rows. Read the preview first, every time, and decide for yourself.

## Pipeline step: CI for week 3 (5 min)

Move your `checks.yml` to `-Dweek=3`. CI never calls the real AI: the tests use the fake one,
so CI needs no key and costs nothing.

## Stuck?

| You see | Try |
|---|---|
| `savings_rate is missing` | That is the next fact to add. The test prints the sentence it expects. |
| Off by a few cents or one percent | Use `pct()` for percents and keep everything in cents until you format it. |
| Top category is Housing | Use everyday spending (no Housing), as the contract says. |
| Visits are too high | A refund is not a visit. Count money out rows only. |
| recurring finds too many | One charge per month, at least 3 months, every charge within 10% of the median. |
| A true card is rejected | Collect numbers from the fact's `text` and its `values`. |
| `ANTHROPIC_API_KEY` errors | Without a key, use `--ai mock`. It is enough for every test. |

Still stuck? Peek at `reference/java/src/main/java/moneywrapped/Wrapped.java`, close it, and write your own.

## What to commit

```
git add src .github
git commit -m "Week 3: Money Wrapped facts and the grounding check"
git push
```

`out/` (with `wrapped.json` and the AI preview) is ignored by git.

**Next week:** your own server, the viewer, CI and CD, and a release you can show an employer.
