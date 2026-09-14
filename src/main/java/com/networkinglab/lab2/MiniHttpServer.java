package com.networkinglab.lab2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * A deliberately sequential HTTP server: it accepts one TCP connection,
 * handles it completely (static resource or hardcoded JSON service), and
 * only then accepts the next one. No threads, no thread pool, no queue.
 *
 * This is the whole point of the lab: understand the baseline before
 * distributing work in later assignments.
 */
public class MiniHttpServer {

    private final int requestedPort;
    private final StaticFileResolver resolver;
    private volatile boolean running = true;
    private ServerSocket serverSocket;

    public MiniHttpServer(int port, Path publicDir) {
        this.requestedPort = port;
        this.resolver = new StaticFileResolver(publicDir);
    }

    /** Actual bound port (useful when constructed with port 0 for tests). */
    public int getPort() {
        return serverSocket != null ? serverSocket.getLocalPort() : 0;
    }

    /**
     * Starts the accept loop. Blocks the calling thread until {@link #stop()}
     * is called. Binding with no explicit address makes the socket listen on
     * all network interfaces (0.0.0.0), which is what allows remote clients
     * (e.g. from EC2's public IP) to connect - not just localhost.
     */
    public void start() throws IOException {
        serverSocket = new ServerSocket(requestedPort);
        System.out.println("[MiniHttpServer] listening on port " + serverSocket.getLocalPort()
                + ", serving from " + resolver.baseDir());

        while (running) {
            Socket client;
            try {
                client = serverSocket.accept();
            } catch (SocketException e) {
                if (!running) {
                    break; // stop() closed the socket on purpose
                }
                throw e;
            }
            // Handled fully, synchronously, before the loop accepts again.
            handleClient(client);
        }
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException ignored) {
            // shutting down anyway
        }
    }

    private void handleClient(Socket client) {
        try (client;
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(client.getInputStream(), StandardCharsets.ISO_8859_1));
             OutputStream out = client.getOutputStream()) {

            String requestLine = in.readLine();

            // Consume (and discard) headers so the stream is left in a clean
            // state; this lab's services take no request body.
            String header;
            while ((header = in.readLine()) != null && !header.isEmpty()) {
                // intentionally discarded
            }

            ParsedRequest req = ParsedRequest.parseRequestLine(requestLine);
            if (req == null) {
                writeResponse(out, 400, "text/plain; charset=utf-8",
                        "400 Bad Request".getBytes(StandardCharsets.UTF_8));
                return;
            }

            if (!"GET".equalsIgnoreCase(req.method)) {
                writeResponse(out, 405, "text/plain; charset=utf-8",
                        "405 Method Not Allowed".getBytes(StandardCharsets.UTF_8));
                return;
            }

            if (req.path.startsWith("/api/")) {
                handleService(req, out);
            } else {
                handleStatic(req, out);
            }
        } catch (IOException e) {
            // A single malformed/broken request must never take the whole
            // server down: log and let the accept loop continue.
            System.err.println("[MiniHttpServer] error handling request: " + e.getMessage());
        }
    }

    private void handleStatic(ParsedRequest req, OutputStream out) throws IOException {
        Path resolved = resolver.resolve(req.path);
        if (resolved == null) {
            writeResponse(out, 400, "text/plain; charset=utf-8",
                    "400 Bad Request: invalid path".getBytes(StandardCharsets.UTF_8));
            return;
        }
        if (!resolver.existsAsFile(resolved)) {
            writeResponse(out, 404, "text/html; charset=utf-8",
                    "<h1>404 Not Found</h1>".getBytes(StandardCharsets.UTF_8));
            return;
        }
        byte[] body = Files.readAllBytes(resolved); // bytes, not chars: works for HTML and images alike
        String contentType = ContentTypes.forPath(resolved.getFileName().toString());
        writeResponse(out, 200, contentType, body);
    }

    private void handleService(ParsedRequest req, OutputStream out) throws IOException {
        switch (req.path) {
            case "/api/greet" -> handleGreet(req, out);
            case "/api/square" -> handleSquare(req, out);
            case "/api/time" -> handleTime(out);
            case "/api/health" -> writeJson(out, 200, "{\"status\":\"UP\"}");
            default -> writeJson(out, 404, "{\"error\":\"unknown service\"}");
        }
    }

    private void handleGreet(ParsedRequest req, OutputStream out) throws IOException {
        String name = req.query.get("name");
        if (name == null || name.isBlank()) {
            writeJson(out, 400, "{\"error\":\"missing or empty 'name' parameter\"}");
            return;
        }
        String json = "{\"greeting\":\"Hello, " + JsonUtil.escape(name.trim()) + "!\"}";
        writeJson(out, 200, json);
    }

    private void handleSquare(ParsedRequest req, OutputStream out) throws IOException {
        Double value = parseDouble(req.query.get("value"));
        if (value == null) {
            writeJson(out, 400, "{\"error\":\"missing or invalid numeric 'value' parameter\"}");
            return;
        }

        // Optional, test-only parameter: lets you simulate a slow request to
        // observe the sequential limitation described in section 6.2 of the
        // lab guide, e.g. GET /api/square?value=5&delayMs=5000
        Long delayMs = parseLong(req.query.get("delayMs"));
        if (delayMs != null && delayMs > 0) {
            try {
                Thread.sleep(Math.min(delayMs, 30_000));
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }

        double square = value * value;
        String json = "{\"input\":" + formatNumber(value) + ",\"square\":" + formatNumber(square) + "}";
        writeJson(out, 200, json);
    }

    private void handleTime(OutputStream out) throws IOException {
        String now = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        writeJson(out, 200, "{\"serverTime\":\"" + JsonUtil.escape(now) + "\"}");
    }

    private static Double parseDouble(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Long parseLong(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String formatNumber(double d) {
        if (d == Math.floor(d) && !Double.isInfinite(d)) {
            return String.valueOf((long) d);
        }
        return String.valueOf(d);
    }

    private void writeJson(OutputStream out, int status, String json) throws IOException {
        writeResponse(out, status, "application/json; charset=utf-8", json.getBytes(StandardCharsets.UTF_8));
    }

    private void writeResponse(OutputStream out, int status, String contentType, byte[] body) throws IOException {
        StringBuilder headers = new StringBuilder();
        headers.append("HTTP/1.1 ").append(status).append(' ').append(statusText(status)).append("\r\n");
        headers.append("Content-Type: ").append(contentType).append("\r\n");
        headers.append("Content-Length: ").append(body.length).append("\r\n");
        headers.append("Connection: close\r\n");
        headers.append("\r\n");
        out.write(headers.toString().getBytes(StandardCharsets.US_ASCII));
        out.write(body);
        out.flush();
    }

    private String statusText(int status) {
        return switch (status) {
            case 200 -> "OK";
            case 400 -> "Bad Request";
            case 404 -> "Not Found";
            case 405 -> "Method Not Allowed";
            default -> "Error";
        };
    }

    public static void main(String[] args) throws IOException {
        int port = 8080;
        String portEnv = System.getenv("PORT");
        if (portEnv != null) {
            try {
                port = Integer.parseInt(portEnv);
            } catch (NumberFormatException ignored) {
                // keep default
            }
        }
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {
                // keep default/env value
            }
        }

        String publicDirName = System.getenv().getOrDefault("PUBLIC_DIR", "public");
        Path publicDir = Paths.get(publicDirName);
        if (!Files.isDirectory(publicDir)) {
            System.err.println("Public resources directory not found: " + publicDir.toAbsolutePath());
            System.err.println("Run the server from the project/deployment root, or set PUBLIC_DIR.");
            System.exit(1);
        }

        new MiniHttpServer(port, publicDir).start();
    }
}
