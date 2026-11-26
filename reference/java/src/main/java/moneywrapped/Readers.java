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
        List<Txn> rows = new ArrayList<>();
        int n = 0;
        for (Map<String, String> r : Csv.read(file, 0)) {
            n++;
            long amount = Money.toCents(r.get("Credit")) - Money.toCents(r.get("Debit"));
            rows.add(new Txn("gulfview", n, account, isoDate(r.get("Date"), "MM/dd/yyyy"), r.get("Description"), amount));
        }
        return rows;
    }

    /** Coquina Savings: "Mar 26, 2027" dates and "$300.00" or "-$25.00" amounts. */
    public static List<Txn> coquina(Path file, String account) throws IOException {
        List<Txn> rows = new ArrayList<>();
        int n = 0;
        for (Map<String, String> r : Csv.read(file, 0)) {
            n++;
            rows.add(new Txn("coquina", n, account, isoDate(r.get("Transaction Date"), "MMM d, yyyy"),
                    r.get("Details"), Money.toCents(r.get("Amount (USD)"))));
        }
        return rows;
    }

    /** Harborline Visa: a credit card, so a purchase is POSITIVE in the file but money out for us. */
    public static List<Txn> harborline(Path file, String account) throws IOException {
        List<Txn> rows = new ArrayList<>();
        int n = 0;
        for (Map<String, String> r : Csv.read(file, 0)) {
            n++;
            rows.add(new Txn("harborline", n, account, r.get("Trans. Date").trim(),
                    r.get("Description"), -Money.toCents(r.get("Amount"))));
        }
        return rows;
    }

    /** Palmetto Mastercard: three junk lines, M/d/yy dates, and a Type column that gives the sign. */
    public static List<Txn> palmetto(Path file, String account) throws IOException {
        List<Txn> rows = new ArrayList<>();
        int n = 0;
        for (Map<String, String> r : Csv.read(file, 3)) {
            n++;
            long amount = Money.toCents(r.get("Amount"));
            if (r.get("Type").trim().equalsIgnoreCase("SALE")) amount = -amount;
            rows.add(new Txn("palmetto", n, account, isoDate(r.get("Date"), "M/d/yy"), r.get("Merchant"), amount));
        }
        return rows;
    }

    /** Sunpine Debit: lowercase, DR means money out, a time on every date, a # line at the end. */
    public static List<Txn> sunpine(Path file, String account) throws IOException {
        List<Txn> rows = new ArrayList<>();
        int n = 0;
        for (Map<String, String> r : Csv.read(file, 0)) {
            n++;
            long amount = Money.toCents(r.get("AMT"));
            if (r.get("DR_CR").trim().equalsIgnoreCase("DR")) amount = -amount;
            rows.add(new Txn("sunpine", n, account, r.get("DATE_TIME").trim().substring(0, 10), r.get("DESC"), amount));
        }
        return rows;
    }

    // ================================================================= week 2: your own bank

    /**
     * Read any bank's CSV export, described by a profile (a .properties file). See
     * data/private-example/seagrape.properties for every setting with a comment.
     */
    public static List<Txn> fromProfile(Path profileFile) throws IOException {
        Properties p = new Properties();
        try (Reader in = Files.newBufferedReader(profileFile, StandardCharsets.UTF_8)) {
            p.load(in);
        }
        String bank = required(p, "bank_id", profileFile);
        if (!bank.matches("[a-z]+")) {
            throw new IllegalArgumentException(profileFile + ": bank_id must be lowercase letters only, like \"mybank\"");
        }
        String account = required(p, "account_name", profileFile);
        Path csv = profileFile.resolveSibling(required(p, "file", profileFile));
        int skip = Integer.parseInt(p.getProperty("skip_lines", "0").trim());
        String dateColumn = required(p, "date_column", profileFile);
        String dateFormat = required(p, "date_format", profileFile);
        String descColumn = required(p, "description_column", profileFile);
        String amountColumn = p.getProperty("amount_column", "").trim();
        String debitColumn = p.getProperty("debit_column", "").trim();
        String creditColumn = p.getProperty("credit_column", "").trim();
        boolean flipped = p.getProperty("sign", "normal").trim().equalsIgnoreCase("flipped");
        if (amountColumn.isEmpty() == (debitColumn.isEmpty() || creditColumn.isEmpty())) {
            throw new IllegalArgumentException(profileFile + ": set amount_column, or both debit_column and credit_column");
        }

        List<Txn> rows = new ArrayList<>();
        int n = 0;
        for (Map<String, String> r : Csv.read(csv, skip)) {
            n++;
            long amount = amountColumn.isEmpty()
                    ? Money.toCents(Csv.cell(r, creditColumn)) - Money.toCents(Csv.cell(r, debitColumn))
                    : Money.toCents(Csv.cell(r, amountColumn));
            if (flipped) amount = -amount;
            rows.add(new Txn(bank, n, account, isoDate(Csv.cell(r, dateColumn), dateFormat),
                    Csv.cell(r, descColumn), amount));
        }
        return rows;
    }

    private static String required(Properties p, String key, Path file) {
        String v = p.getProperty(key, "").trim();
        if (v.isEmpty()) throw new IllegalArgumentException(file + ": the setting " + key + " is missing");
        return v;
    }

    // ================================================================= week 5 (bonus)

    /** Tarpon Trust Checking: a JSON export. "posted" is yyyyMMdd, "amount" is already signed. */
    public static List<Txn> tarpon(Path file, String account) throws IOException {
        JsonNode root = Json.MAPPER.readTree(file.toFile());
        List<Txn> rows = new ArrayList<>();
        int n = 0;
        for (JsonNode t : root.get("transactions")) {
            n++;
            rows.add(new Txn("tarpon", n, account, isoDate(t.get("posted").asText(), "yyyyMMdd"),
                    t.get("memo").asText(), Money.toCents(t.get("amount").asText())));
        }
        return rows;
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
