package com.networkinglab.lab2;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents a parsed HTTP request line: method, decoded path and decoded
 * query parameters. Only what this lab needs - no headers, no body.
 */
public final class ParsedRequest {

    public final String method;
    public final String path;            // decoded, without the query string
    public final Map<String, String> query;
    public final String rawTarget;       // original request-target, for logging

    private ParsedRequest(String method, String path, Map<String, String> query, String rawTarget) {
        this.method = method;
        this.path = path;
        this.query = query;
        this.rawTarget = rawTarget;
    }

    /** Parses a request line such as "GET /api/square?value=4 HTTP/1.1". Returns null if malformed. */
    public static ParsedRequest parseRequestLine(String line) {
        if (line == null || line.isBlank()) {
            return null;
        }
        String[] parts = line.trim().split(" +");
        if (parts.length != 3) {
            return null;
        }
        String method = parts[0];
        String target = parts[1];
        String protocol = parts[2];
        if (!protocol.startsWith("HTTP/")) {
            return null;
        }

        String rawPath = target;
        String rawQuery = "";
        int qIdx = target.indexOf('?');
        if (qIdx >= 0) {
            rawPath = target.substring(0, qIdx);
            rawQuery = target.substring(qIdx + 1);
        }

        String decodedPath;
        try {
            decodedPath = URLDecoder.decode(rawPath, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }

        return new ParsedRequest(method, decodedPath, parseQuery(rawQuery), target);
    }

    private static Map<String, String> parseQuery(String raw) {
        Map<String, String> map = new LinkedHashMap<>();
        if (raw == null || raw.isEmpty()) {
            return map;
        }
        for (String pair : raw.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            int eq = pair.indexOf('=');
            String k;
            String v;
            if (eq >= 0) {
                k = pair.substring(0, eq);
                v = pair.substring(eq + 1);
            } else {
                k = pair;
                v = "";
            }
            try {
                k = URLDecoder.decode(k, StandardCharsets.UTF_8);
                v = URLDecoder.decode(v, StandardCharsets.UTF_8);
            } catch (Exception ignored) {
                // keep raw values if decoding fails
            }
            map.put(k, v);
        }
        return map;
    }
}
