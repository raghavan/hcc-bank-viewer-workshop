package moneywrapped;

import com.sun.net.httpserver.HttpServer;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The commands. Done for you. Run them with ./run (or run.cmd on Windows):
 *
 *   ./run structure                     five mock banks to out/transactions.json
 *   ./run structure --data private      YOUR bank files in data/private/ instead
 *   ./run wrapped                       out/wrapped.json, cards without AI
 *   ./run wrapped --ai mock             cards from the fake AI (free, offline)
 *   ./run wrapped --ai claude           writes the request preview, sends nothing
 *   ./run wrapped --ai claude --send    really asks Claude (needs ANTHROPIC_API_KEY)
 *   ./run serve                         the viewer at http://localhost:8000
 *   ./run site                          _site/ for GitHub Pages (mock data only)
 */
public final class Main {
    private Main() {}

    public static void main(String[] args) throws Exception {
        List<String> a = new ArrayList<>(List.of(args));
        String command = a.isEmpty() ? "help" : a.remove(0);
        try {
            switch (command) {
                case "structure" -> structure(a);
                case "wrapped" -> wrapped(a);
                case "serve" -> serve(a);
                case "site" -> Site.build(Path.of(option(a, "--viewer", "viewer")), Path.of("out"),
                        Path.of(option(a, "--out", "_site")));
                default -> help();
            }
        } catch (UnsupportedOperationException e) {
            System.err.println("\nNot written yet: " + e.getMessage() + ". Find that TODO, then run ./mvnw test to check it.");
            System.exit(1);
        } catch (IllegalArgumentException | IllegalStateException | java.io.IOException e) {
            System.err.println("\nStopped: " + e.getMessage());
            System.exit(1);
        }
    }

    static void structure(List<String> a) throws Exception {
        String data = option(a, "--data", "mock");
        Structure.DataSource source = switch (data) {
            case "mock" -> Structure.DataSource.MOCK;
            case "private" -> Structure.DataSource.PRIVATE;
            default -> throw new IllegalArgumentException("--data must be mock or private");
        };
        Path rules = Path.of(option(a, "--rules", Categorizer.DEFAULT_RULES.toString()));
        String decisions = option(a, "--decisions", null);
        boolean bonus = a.contains("--include-bonus");
        Path out = Path.of(option(a, "--out", Structure.OUT.toString()));
        List<Txn> rows = Structure.run(source, rules, decisions == null ? null : Path.of(decisions), bonus);
        Structure.write(rows, source, out);
    }

    static void wrapped(List<String> a) throws Exception {
        Path in = Path.of(option(a, "--in", Structure.OUT.toString()));
        String ai = option(a, "--ai", "off");
        boolean send = a.contains("--send");
        Path out = Path.of(option(a, "--out", WrappedRun.OUT.toString()));
        if (!in.toFile().exists()) throw new IllegalArgumentException("No " + in + " yet. Run ./run structure first.");
        Json.write(out, WrappedRun.run(in, ai, send));
        System.out.println("\nWrote " + out + ". See it with ./run serve, on the Wrapped tab.");
    }

    static void serve(List<String> a) throws Exception {
        int port = Integer.parseInt(option(a, "--port", "8000"));
        HttpServer server = Server.start(port, Path.of(option(a, "--viewer", "viewer")), Path.of("out"));
        System.out.println("Viewer running at http://localhost:" + server.getAddress().getPort() + "  (Ctrl+C to stop)");
        Thread.currentThread().join();
    }

    static String option(List<String> a, String name, String fallback) {
        int i = a.indexOf(name);
        if (i < 0) return fallback;
        if (i + 1 >= a.size()) throw new IllegalArgumentException(name + " needs a value");
        return a.get(i + 1);
    }

    static void help() {
        System.out.println("""
                Money Wrapped commands:
                  ./run structure [--data mock|private] [--rules FILE] [--decisions FILE] [--include-bonus]
                  ./run wrapped   [--ai off|mock|claude] [--send] [--in out/transactions.json]
                  ./run serve     [--port 8000]
                  ./run site      [--out _site]""");
    }
}
