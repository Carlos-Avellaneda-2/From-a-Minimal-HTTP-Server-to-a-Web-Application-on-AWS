package com.networkinglab.lab2;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end tests that actually start the server on an ephemeral port and
 * talk to it over real HTTP, the same way a browser would.
 */
class ServerIntegrationTest {

    static MiniHttpServer server;
    static int port;
    static final HttpClient client = HttpClient.newHttpClient();

    @BeforeAll
    static void startServer() throws Exception {
        Path tempPublic = Files.createTempDirectory("public-it-test");
        Files.writeString(tempPublic.resolve("index.html"), "<html><body>home</body></html>");
        Files.createDirectories(tempPublic.resolve("images"));
        Files.write(tempPublic.resolve("images").resolve("dummy.png"),
                new byte[] {(byte) 0x89, 'P', 'N', 'G'});

        server = new MiniHttpServer(0, tempPublic); // port 0 -> OS picks a free port
        Thread serverThread = new Thread(() -> {
            try {
                server.start();
            } catch (IOException ignored) {
                // server was stopped on purpose in @AfterAll
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();

        for (int i = 0; i < 100 && server.getPort() == 0; i++) {
            Thread.sleep(20);
        }
        port = server.getPort();
        assertTrue(port > 0, "server did not bind to a port in time");
    }

    @AfterAll
    static void stopServer() {
        server.stop();
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build();
        return client.send(req, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void homePageLoads() throws Exception {
        var res = get("/");
        assertEquals(200, res.statusCode());
        assertTrue(res.body().contains("home"));
        assertTrue(res.headers().firstValue("Content-Type").orElse("").contains("text/html"));
    }

    @Test
    void staticImageLoadsAsBinary() throws Exception {
        var res = get("/images/dummy.png");
        assertEquals(200, res.statusCode());
        assertEquals("image/png", res.headers().firstValue("Content-Type").orElse(""));
    }

    @Test
    void healthServiceReportsUp() throws Exception {
        var res = get("/api/health");
        assertEquals(200, res.statusCode());
        assertTrue(res.body().contains("UP"));
    }

    @Test
    void greetingWithValidNameSucceeds() throws Exception {
        var res = get("/api/greet?name=Ana");
        assertEquals(200, res.statusCode());
        assertTrue(res.body().contains("Ana"));
    }

    @Test
    void greetingWithoutNameFails() throws Exception {
        var res = get("/api/greet");
        assertEquals(400, res.statusCode());
    }

    @Test
    void squareWithValidNumberSucceeds() throws Exception {
        var res = get("/api/square?value=4");
        assertEquals(200, res.statusCode());
        assertTrue(res.body().contains("16"));
    }

    @Test
    void squareWithInvalidNumberFails() throws Exception {
        var res = get("/api/square?value=abc");
        assertEquals(400, res.statusCode());
    }

    @Test
    void serverTimeIsProvidedByServer() throws Exception {
        var res = get("/api/time");
        assertEquals(200, res.statusCode());
        assertTrue(res.body().contains("serverTime"));
    }

    @Test
    void missingStaticFileReturns404() throws Exception {
        var res = get("/nope.html");
        assertEquals(404, res.statusCode());
    }

    @Test
    void unsupportedMethodReturns405() throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/health"))
                .method("POST", HttpRequest.BodyPublishers.noBody())
                .build();
        var res = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(405, res.statusCode());
    }

    @Test
    void pathTraversalAttemptIsRejected() throws Exception {
        var res = get("/%2e%2e/%2e%2e/etc/passwd");
        assertTrue(res.statusCode() == 400 || res.statusCode() == 404);
    }

    @Test
    void tenConsecutiveRequestsSucceedOnOneServerRun() throws Exception {
        for (int i = 0; i < 10; i++) {
            var res = get("/api/health");
            assertEquals(200, res.statusCode());
        }
    }
}
