package server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Static file server for the client/ folder plus /metrics JSON (tasks 1.6, 1.8).
 * Uses the JDK's built-in com.sun.net.httpserver.
 */
public class WebFileServer {
    private static final Map<String, String> TYPES = Map.of(
            "html", "text/html; charset=utf-8",
            "js", "application/javascript; charset=utf-8",
            "css", "text/css; charset=utf-8",
            "json", "application/json",
            "png", "image/png",
            "ico", "image/x-icon");

    private final HttpServer server;
    private final Path root;

    public WebFileServer(int port, Path root, Metrics metrics) throws IOException {
        this.root = root.toAbsolutePath().normalize();
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newFixedThreadPool(8));

        server.createContext("/api/metrics", ex -> {
            byte[] body = metrics.toJson().getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.getResponseHeaders().add("Cache-Control", "no-store");
            ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            send(ex, 200, body);
        });

        server.createContext("/", this::serveFile);
    }

    private void serveFile(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        if (path.equals("/")) path = "/index.html";
        Path file = root.resolve(path.substring(1)).normalize();

        // block ../ path traversal
        if (!file.startsWith(root) || !Files.isRegularFile(file)) {
            send(ex, 404, "404 Not Found".getBytes(StandardCharsets.UTF_8));
            return;
        }
        String name = file.getFileName().toString();
        String ext = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1) : "";
        ex.getResponseHeaders().add("Content-Type",
                TYPES.getOrDefault(ext, "application/octet-stream"));
        send(ex, 200, Files.readAllBytes(file));
    }

    private static void send(HttpExchange ex, int status, byte[] body) throws IOException {
        ex.sendResponseHeaders(status, body.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(body);
        }
    }

    public void start() {
        server.start();
        System.out.println("[http] serving " + root + " on port " + server.getAddress().getPort());
    }
}
