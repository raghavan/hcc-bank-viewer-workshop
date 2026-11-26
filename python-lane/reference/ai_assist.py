"""Optional AI suggestions for rows no rule could place.

Three modes, picked with --ai on the command line:

  off     (default) no AI at all. Leftover rows go to the review queue as "Other".
  mock    a FAKE, offline stand in that pretends to be an AI. Free, needs no key,
          and lets you build and test the review queue in class.
  claude  calls Claude through the official Anthropic Python SDK. Needs
          `pip install anthropic` and an API key in ANTHROPIC_API_KEY.
          Costs real money (very little for this data), so it is never the default.

An AI suggestion NEVER counts on its own: every suggested row keeps
needs_review = true until a person approves it in the viewer.

Answers are cached in out/ai_cache.json, keyed by description, so running the
structurer again does not pay for the same question twice.
"""

import json
import os
from pathlib import Path

from categorize import CATEGORIES

DEFAULT_MODEL = os.environ.get("BANKVIEW_AI_MODEL", "claude-opus-5-5")

# A tiny fake "model" for mock mode. It is obviously not smart; that is the point.
MOCK_GUESSES = [
    ("YOGA", "Health", 0.86, "The description names a yoga studio, which fits Health."),
    ("PARKING", "Transport", 0.71, "Parking garages usually belong with Transport."),
    ("NURSERY", "Shopping", 0.48, "A plant nursery is probably a store, but it is a guess."),
    ("STALL", "Groceries", 0.62, "A market stall often sells food."),
    ("HARDWARE", "Shopping", 0.80, "A hardware store is shopping."),
    ("INVOICE", "Income", 0.70, "An invoice payment coming in looks like income."),
]


def mock_suggest(description):
    for word, category, confidence, reason in MOCK_GUESSES:
        if word in description:
            return {"category": category, "confidence": confidence, "reason": reason}
    return {"category": "Other", "confidence": 0.30, "reason": "No clue in the description."}


def claude_suggest_batch(descriptions, model):
    """Ask Claude about many descriptions in ONE request and get JSON back."""
    try:
        import anthropic
    except ImportError:
        raise SystemExit("The claude mode needs the SDK: pip install anthropic")

    client = anthropic.Anthropic()  # reads ANTHROPIC_API_KEY from the environment
    schema = {
        "type": "object",
        "properties": {
            "answers": {
                "type": "array",
                "items": {
                    "type": "object",
                    "properties": {
                        "description": {"type": "string"},
                        "category": {"type": "string", "enum": CATEGORIES[:-1]},
                        "confidence": {"type": "number"},
                        "reason": {"type": "string"},
                    },
                    "required": ["description", "category", "confidence", "reason"],
                    "additionalProperties": False,
                },
            }
        },
        "required": ["answers"],
        "additionalProperties": False,
    }
    prompt = (
        "You categorize bank transactions for a household budget app.\n"
        "Allowed categories: " + ", ".join(CATEGORIES[:-1]) + ".\n"
        "For each description below, pick one category, give a confidence from 0 to 1 "
        "(be honest: use low numbers when the description is vague), and one short reason.\n\n"
        + "\n".join(f"- {d}" for d in descriptions)
    )
    response = client.beta.messages.create(
        model=model,
        max_tokens=4000,
        betas=["server-side-fallback-2026-07-01"],
        fallbacks="default",
        output_config={"effort": "low", "format": {"type": "json_schema", "schema": schema}},
        messages=[{"role": "user", "content": prompt}],
    )
    if response.stop_reason == "refusal":
        raise SystemExit("The AI declined this request. Leaving rows for manual review.")
    text = next(b.text for b in response.content if b.type == "text")
    answers = json.loads(text)["answers"]
    return {a["description"]: a for a in answers}


def suggest(rows, mode, cache_path):
    """Fill in AI suggestions for rows that still need a category."""
    leftovers = [r for r in rows if r["category_source"] == "none"]
    if mode == "off" or not leftovers:
        return 0

    cache_path = Path(cache_path)
    cache = json.loads(cache_path.read_text()) if cache_path.exists() else {}
    key = lambda d: f"{mode}:{d}"

    missing = sorted({r["description"] for r in leftovers if key(r["description"]) not in cache})
    if missing:
        if mode == "mock":
            for d in missing:
                cache[key(d)] = mock_suggest(d)
        elif mode == "claude":
            print(f"  asking Claude ({DEFAULT_MODEL}) about {len(missing)} descriptions in one batch")
            answers = claude_suggest_batch(missing, DEFAULT_MODEL)
            for d in missing:
                a = answers.get(d)
                if a and a["category"] in CATEGORIES:
                    conf = max(0.0, min(1.0, float(a["confidence"])))
                    cache[key(d)] = {"category": a["category"], "confidence": conf, "reason": a["reason"]}
        else:
            raise SystemExit(f"Unknown --ai mode {mode!r}. Use off, mock or claude.")
        cache_path.parent.mkdir(parents=True, exist_ok=True)
        cache_path.write_text(json.dumps(cache, indent=1, sort_keys=True) + "\n")

    count = 0
    for r in leftovers:
        s = cache.get(key(r["description"]))
        if not s:
            continue
        r.update({
            "category": s["category"],
            "category_source": "ai",
            "confidence": round(s["confidence"], 2),
            "needs_review": True,
            "ai_reason": s["reason"],
        })
        count += 1
    return count
