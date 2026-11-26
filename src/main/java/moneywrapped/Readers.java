package moneywrapped;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;

/**
 * One reader per bank. Each turns one messy export into clean Txn rows, in file order.
 *
 * Week 1: gulfview, then coquina, harborline, palmetto and sunpine.
 * Week 2: fromProfile, so the same program can read YOUR bank's export.
 * Week 5 (bonus): tarpon, a JSON export instead of CSV.
 *
 * The surprise in each file is listed in the table in contract/CONTRACT.md.
 */
public final class Readers {
    private Readers() {}

    /** "03/05/2027" with pattern "MM/dd/yyyy" becomes "2027-03-05". Done for you. */
    public static String isoDate(String text, String pattern) {
        DateTimeFormatter f = DateTimeFormatter.ofPattern(pattern, Locale.US);
        return LocalDate.from(f.parse(text.trim())).toString();
    }

    // ================================================================= week 1

    /** Gulfview CU Checking: Date,Description,Debit,Credit,Balance. Two money columns. */
    public static List<Txn> gulfview(Path file, String account) throws IOException {
        // TODO week 1: read the Gulfview CU file
        // Hint: Csv.read(file, 0) gives one Map per row, keyed by the header names.
        // Hint: amount = Credit minus Debit, in cents. Dates look like 03/05/2027.
        // Hint: new Txn("gulfview", rowNumber, account, isoDate(...), description, amountCents), rowNumber starts at 1.
        throw new UnsupportedOperationException("TODO week 1: read the Gulfview CU file");
    }

    /** Coquina Savings: "Mar 26, 2027" dates and "$300.00" or "-$25.00" amounts. */
    public static List<Txn> coquina(Path file, String account) throws IOException {
        // TODO week 1: read the Coquina Savings file
        // Hint: open data/raw/coquina_savings.csv and look at the dates and the amounts first.
        // Hint: the DateTimeFormatter pattern for "Mar 26, 2027" is "MMM d, yyyy".
        throw new UnsupportedOperationException("TODO week 1: read the Coquina Savings file");
    }

    /** Harborline Visa: a credit card, so a purchase is POSITIVE in the file but money out for us. */
    public static List<Txn> harborline(Path file, String account) throws IOException {
        // TODO week 1: read the Harborline Visa file
        // Hint: on a credit card a purchase is POSITIVE in the file, but it is money out for you.
        throw new UnsupportedOperationException("TODO week 1: read the Harborline Visa file");
    }

    /** Palmetto Mastercard: three junk lines, M/d/yy dates, and a Type column that gives the sign. */
    public static List<Txn> palmetto(Path file, String account) throws IOException {
        // TODO week 1: read the Palmetto Mastercard file
        // Hint: the first three lines are junk, so skip them: Csv.read(file, 3).
        // Hint: the Type column decides the sign. Dates look like 3/5/27.
        throw new UnsupportedOperationException("TODO week 1: read the Palmetto Mastercard file");
    }

    /** Sunpine Debit: lowercase, DR means money out, a time on every date, a # line at the end. */
    public static List<Txn> sunpine(Path file, String account) throws IOException {
        // TODO week 1: read the Sunpine Debit file
        // Hint: DR means money out. Every date has a time after it. Csv.read already skips lines that start with #.
        throw new UnsupportedOperationException("TODO week 1: read the Sunpine Debit file");
    }

    // ================================================================= week 2: your own bank

    /**
     * Read any bank's CSV export, described by a profile (a .properties file). See
     * data/private-example/seagrape.properties for every setting with a comment.
     */
    public static List<Txn> fromProfile(Path profileFile) throws IOException {
        // TODO week 2: read any bank from a profile
        // Hint: load the .properties file with java.util.Properties. Every setting is explained in
        // data/private-example/seagrape.properties. The profile's file setting is next to the profile itself.
        // Hint: use Csv.cell(row, column) so a typo in a column name gives a clear message.
        // Hint: amount_column alone, OR debit_column plus credit_column. sign=flipped turns the sign around.
        throw new UnsupportedOperationException("TODO week 2: read any bank from a profile");
    }

    private static String required(Properties p, String key, Path file) {
        String v = p.getProperty(key, "").trim();
        if (v.isEmpty()) throw new IllegalArgumentException(file + ": the setting " + key + " is missing");
        return v;
    }

    // ================================================================= week 5 (bonus)

    /** Tarpon Trust Checking: a JSON export. "posted" is yyyyMMdd, "amount" is already signed. */
    public static List<Txn> tarpon(Path file, String account) throws IOException {
        // TODO week 5: read the Tarpon Trust JSON export (bonus)
        // Hint: Json.MAPPER.readTree(file.toFile()) and then the "transactions" array.
        // Hint: posted looks like 20270305, so the pattern is "yyyyMMdd".
        throw new UnsupportedOperationException("TODO week 5: read the Tarpon Trust JSON export (bonus)");
    }

    // ================================================================= putting it together (done for you)

    /** Read the five mock banks listed in contract/accounts.csv (and the bonus bank if asked). */
    public static List<Txn> readMock(boolean includeBonus) throws IOException {
        List<Txn> all = new ArrayList<>();
        for (Map<String, String> acct : Csv.read(Path.of("contract/accounts.csv"), 0)) {
            Path file = Path.of("data/raw").resolve(acct.get("file"));
            String account = acct.get("account_name");
            List<Txn> rows = switch (acct.get("bank_id")) {
                case "gulfview" -> gulfview(file, account);
                case "coquina" -> coquina(file, account);
                case "harborline" -> harborline(file, account);
                case "palmetto" -> palmetto(file, account);
                case "sunpine" -> sunpine(file, account);
                default -> throw new IllegalArgumentException("No reader for bank " + acct.get("bank_id"));
            };
            System.out.printf("  %-12s %4d rows%n", acct.get("bank_id"), rows.size());
            all.addAll(rows);
        }
        if (includeBonus) {
            List<Txn> rows = tarpon(Path.of("data/bonus/tarpon_checking.json"), "Tarpon Trust Checking");
            System.out.printf("  %-12s %4d rows (bonus)%n", "tarpon", rows.size());
            all.addAll(rows);
        }
        return all;
    }

    /** Every profile in a folder (data/private by default). Your files never leave your computer. */
    public static List<Path> profiles(Path folder) throws IOException {
        if (!Files.isDirectory(folder)) return List.of();
        try (Stream<Path> s = Files.list(folder)) {
            return s.filter(f -> f.getFileName().toString().endsWith(".properties")).sorted().toList();
        }
    }

    public static List<Txn> readProfiles(Path folder) throws IOException {
        List<Path> found = profiles(folder);
        if (found.isEmpty()) {
            throw new IOException("No bank profile in " + folder + ". Copy data/private-example/seagrape.properties "
                    + "there, rename it, and point it at your export (see weeks/week-2/README.md).");
        }
        List<Txn> all = new ArrayList<>();
        for (Path profile : found) {
            List<Txn> rows = fromProfile(profile);
            System.out.printf("  %-24s %4d rows%n", profile.getFileName(), rows.size());
            all.addAll(rows);
        }
        return all;
    }
}
