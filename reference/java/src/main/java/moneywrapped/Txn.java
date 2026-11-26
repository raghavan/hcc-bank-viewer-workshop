package moneywrapped;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * One clean transaction, in the shape every program in this workshop agrees on
 * (contract/CONTRACT.md). Done for you.
 *
 * The fields are public and mutable on purpose: reading a bank file fills in the
 * first five, and categorizing fills in the rest. Jackson turns amountCents into
 * "amount_cents" in the JSON because of the snake case naming rule below.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonPropertyOrder({"id", "date", "account", "description", "amount_cents", "category",
        "category_source", "rule_id", "confidence", "needs_review", "is_transfer"})
public class Txn {
    public String id;            // "gulfview-12": bank id, a dash, the data row number
    public String date;          // "2027-03-05"
    public String account;       // "Gulfview CU Checking"
    public String description;   // cleaned: uppercase, single spaces
    public long amountCents;     // negative means money left the account
    public String category;      // one of Categories.ALL, or null before week 2
    public String categorySource = "none";
    public String ruleId;
    public Double confidence;
    public boolean needsReview = true;
    public boolean isTransfer = false;

    public Txn() {
        // Jackson uses this when it reads a transactions.json file back in.
    }

    /** Build a row the way every reader does. rowNumber starts at 1. */
    public Txn(String bank, int rowNumber, String account, String date, String description, long amountCents) {
        this.id = bank + "-" + rowNumber;
        this.account = account;
        this.date = date;
        this.description = Money.clean(description);
        this.amountCents = amountCents;
    }

    /** "2027-03-05" becomes "2027-03". */
    public String month() {
        return date.substring(0, 7);
    }

    @Override
    public String toString() {
        return id + " " + date + " " + description + " " + Money.format(amountCents) + " " + category;
    }
}
