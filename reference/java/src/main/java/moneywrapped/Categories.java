package moneywrapped;

import java.util.List;

/** The fixed category list from contract/CONTRACT.md, and the review rule's cut off. Done for you. */
public final class Categories {
    private Categories() {}

    public static final List<String> ALL = List.of("Income", "Housing", "Groceries", "Dining", "Shopping",
            "Transport", "Utilities", "Health", "Subscriptions", "Other", "Transfer");

    /** A rule below this confidence still sends its row to a person. */
    public static final double REVIEW_THRESHOLD = 0.8;
}
