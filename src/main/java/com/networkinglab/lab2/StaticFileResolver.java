package com.networkinglab.lab2;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Resolves a request path against the public-resources directory and
 * guarantees the result cannot escape that directory (path traversal).
 */
public final class StaticFileResolver {

    private final Path baseDir;

    public StaticFileResolver(Path baseDir) {
        this.baseDir = baseDir.toAbsolutePath().normalize();
    }

    public Path baseDir() {
        return baseDir;
    }

    /**
     * Resolves a request path (e.g. "/", "/app.js", "/images/logo.png") to a
     * file inside the public-resources directory.
     *
     * @return the resolved, normalized path, or {@code null} if the request
     *         path attempts to escape the base directory.
     */
    public Path resolve(String requestPath) {
        String cleaned = requestPath == null ? "/" : requestPath;
        if (cleaned.equals("/") || cleaned.isEmpty()) {
            cleaned = "/index.html";
        }
        while (cleaned.startsWith("/")) {
            cleaned = cleaned.substring(1);
        }

        Path resolved = baseDir.resolve(cleaned).normalize();
        if (!resolved.startsWith(baseDir)) {
            return null; // traversal attempt, e.g. ../../etc/passwd
        }
        return resolved;
    }

    public boolean existsAsFile(Path resolved) {
        return resolved != null && Files.isRegularFile(resolved) && Files.isReadable(resolved);
    }
}
