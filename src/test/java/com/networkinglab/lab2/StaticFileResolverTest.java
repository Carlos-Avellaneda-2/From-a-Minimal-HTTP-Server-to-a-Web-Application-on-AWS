package com.networkinglab.lab2;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class StaticFileResolverTest {

    private Path base;
    private StaticFileResolver resolver;

    @BeforeEach
    void setUp() throws IOException {
        base = Files.createTempDirectory("public-resources-test");
        Files.writeString(base.resolve("index.html"), "<html></html>");
        resolver = new StaticFileResolver(base);
    }

    @Test
    void rootPathMapsToIndexHtml() {
        Path resolved = resolver.resolve("/");
        assertNotNull(resolved);
        assertEquals("index.html", resolved.getFileName().toString());
    }

    @Test
    void normalPathStaysInsideBaseDir() {
        Path resolved = resolver.resolve("/app.js");
        assertNotNull(resolved);
        assertEquals(base.toAbsolutePath().normalize(), resolved.getParent());
    }

    @Test
    void traversalAttemptIsRejected() {
        assertNull(resolver.resolve("/../../etc/passwd"));
    }

    @Test
    void deepTraversalAttemptIsRejected() {
        assertNull(resolver.resolve("/images/../../../etc/shadow"));
    }

    @Test
    void nonExistentFileIsRecognizedAsMissing() {
        Path resolved = resolver.resolve("/does-not-exist.html");
        assertNotNull(resolved); // path is valid/inside base...
        assertEquals(false, resolver.existsAsFile(resolved)); // ...but file is missing
    }
}
