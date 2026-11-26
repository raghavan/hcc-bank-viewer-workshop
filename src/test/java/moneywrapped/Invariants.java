package moneywrapped;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Checks that must hold for ANY bank data, yours included, without an answer key.
 * If your own data breaks one of these, the bug is in the code, not in your bank.
 */
final class Invariants {
    private Invariants() {}

    static final List<String> SOURCES = List.of("rule", "transfer", "ai", "ai_approved", "manual", "none");

    static List<String> check(List<Txn> rows) {
        List<String> problems = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        long transferSum = 0;
        int transfers = 0;
        for (Txn t : rows) {
            if (t.id == null || !t.id.matches("[a-z]+-[1-9][0-9]*")) problems.add("bad id " + t.id + " (want like gulfview-12)");
            else if (!ids.add(t.id)) problems.add("id " + t.id + " appears twice");
            if (t.date == null || !t.date.matches("\\d{4}-\\d{2}-\\d{2}")) problems.add(t.id + ": date " + t.date + " is not YYYY-MM-DD");
            if (t.account == null || t.account.isBlank()) problems.add(t.id + ": no account name");
            if (t.description == null || t.description.isEmpty() || !t.description.equals(Money.clean(t.description))) {
                problems.add(t.id + ": description is empty or not cleaned");
            }
            if (t.category == null) problems.add(t.id + ": no category (rows nobody placed should be Other)");
            else if (!Categories.ALL.contains(t.category)) problems.add(t.id + ": category " + t.category + " is not on the list");
            if (!SOURCES.contains(t.categorySource)) problems.add(t.id + ": category_source " + t.categorySource + " is not allowed");
            if (t.confidence != null && (t.confidence < 0 || t.confidence > 1)) problems.add(t.id + ": confidence out of range");
            double conf = t.confidence == null ? 0 : t.confidence;
            boolean should = t.categorySource.equals("none") || t.categorySource.equals("ai")
                    || (t.categorySource.equals("rule") && conf < Categories.REVIEW_THRESHOLD);
            if (t.needsReview != should) problems.add(t.id + ": needs_review is " + t.needsReview + " but the review rule says " + should);
            if (t.isTransfer) {
                transfers++;
                transferSum += t.amountCents;
                if (!"Transfer".equals(t.category)) problems.add(t.id + ": a transfer must have category Transfer");
            } else if ("Transfer".equals(t.category)) {
                problems.add(t.id + ": category Transfer but is_transfer is false");
            }
        }
        if (transfers % 2 != 0) problems.add(transfers + " rows are marked as transfers; transfers come in pairs");
        if (transferSum != 0) problems.add("transfers should net to $0.00, they net to " + Money.format(transferSum));
        return problems;
    }
}
