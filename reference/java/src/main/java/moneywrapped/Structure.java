package moneywrapped;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The structure step from start to finish: read, find transfers, apply rules, and write
 * out/transactions.json. Done for you: the pieces it calls are yours to build.
 */
public final class Structure {
    private Structure() {}

    public static final Path PRIVATE_DIR = Path.of("data/private");
    public static final Path OUT = Path.of("out/transactions.json");

    /** Where the rows came from. "mock" rows are fake and safe to share; "private" rows are yours. */
    public enum DataSource { MOCK, PRIVATE }

    public static List<Txn> read(DataSource source, boolean includeBonus) throws IOException {
        System.out.println("1. Reading bank files (" + source.name().toLowerCase() + " data)");
        return source == DataSource.MOCK ? Readers.readMock(includeBonus) : Readers.readProfiles(PRIVATE_DIR);
    }

    /** Read and categorize. Returns the rows sorted by date, then id. */
    public static List<Txn> run(DataSource source, Path rulesFile, Path decisions, boolean includeBonus) throws IOException {
        List<Txn> rows = new ArrayList<>(read(source, includeBonus));
        System.out.println("2. Finding transfers: " + Categorizer.markTransfers(rows) + " pairs");
        List<Categorizer.Rule> rules = new ArrayList<>(Categorizer.decisionRules(decisions));
        rules.addAll(Categorizer.loadRules(rulesFile));
        System.out.println("3. Applying rules from " + rulesFile + ": " + Categorizer.applyRules(rows, rules) + " rows placed");
        int overridden = Categorizer.applyOverrides(rows, decisions);
        if (overridden > 0) System.out.println("   " + overridden + " rows set by your decisions file");
        Categorizer.finish(rows);
        rows.sort(Comparator.comparing((Txn t) -> t.date).thenComparing(t -> t.id));
        return rows;
    }

    /** The whole file, as the contract describes it, plus where the rows came from. */
    public static Map<String, Object> document(List<Txn> rows, DataSource source) {
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("schema_version", "1.0");
        doc.put("generated_at", LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString());
        doc.put("generator", "java");
        doc.put("data_source", source.name().toLowerCase());
        doc.put("transactions", rows);
        return doc;
    }

    public static void write(List<Txn> rows, DataSource source, Path out) throws IOException {
        Json.write(out, document(rows, source));
        long review = rows.stream().filter(t -> t.needsReview).count();
        System.out.println("\nWrote " + rows.size() + " transactions to " + out);
        System.out.println(review + " rows are waiting in the review queue");
        if (source == DataSource.PRIVATE) {
            System.out.println("These are YOUR transactions. out/ is ignored by git, so they stay on this computer.");
        }
    }
}
