package moneywrapped;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Week 4: a tiny web server for the viewer, built into Java (no framework).
 *
 *   GET /                    the viewer page (viewer/index.html)
 *   GET /app.js and friends  the other files in viewer/
 *   GET /transactions.json   your out/transactions.json
 *   GET /wrapped.json        your out/wrapped.json
 *
 * It listens on 127.0.0.1 only, so nobody else on the Wi-Fi can see your money.
 */
public final class Server {
    private Server() {}

    static final Map<String, String> TYPES = Map.of(
            "html", "text/html; charset=utf-8",
            "js", "text/javascript; charset=utf-8",
            "css", "text/css; charset=utf-8",
            "json", "application/json; charset=utf-8",
            "png", "image/png",
            "svg", "image/svg+xml");

    public static String contentType(Path file) {
        String name = file.getFileName().toString();
        String ext = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1) : "";
        return TYPES.getOrDefault(ext, "application/octet-stream");
    }

    /**
     * Which file answers a request path, or null for 404. The two JSON files come from
     * outDir; everything else comes from viewerDir, and never from outside it.
     */
    public static Path resolve(String requestPath, Path viewerDir, Path outDir) {
        // TODO week 4: decide which file answers a request
        // Hint: /transactions.json and /wrapped.json come from outDir. / means index.html.
        // Hint: everything else comes from viewerDir. Normalize the path and make sure it still
        // starts with viewerDir, or someone could ask for /../pom.xml. Return null for a 404.
        throw new UnsupportedOperationException("TODO week 4: decide which file answers a request");
    }

    static void handle(HttpExchange ex, Path viewerDir, Path outDir) throws IOException {
        try (ex) {
            Path file = "GET".equals(ex.getRequestMethod()) ? resolve(ex.getRequestURI().getPath(), viewerDir, outDir) : null;
            byte[] body;
            int status;
            if (file == null) {
                status = 404;
                body = "Not found. Did you run ./run structure and ./run wrapped first?".getBytes();
                ex.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
            } else {
                status = 200;
                body = Files.readAllBytes(file);
                ex.getResponseHeaders().set("Content-Type", contentType(file));
                ex.getResponseHeaders().set("Cache-Control", "no-store");
            }
            ex.sendResponseHeaders(status, body.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(body);
            }
        }
    }

    /** Start the server. Port 0 picks any free port (the tests use that). */
    public static HttpServer start(int port, Path viewerDir, Path outDir) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 0);
        server.createContext("/", ex -> handle(ex, viewerDir, outDir));
        server.start();
        return server;
    }
}
