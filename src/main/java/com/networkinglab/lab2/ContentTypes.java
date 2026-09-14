package com.networkinglab.lab2;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps file extensions to HTTP Content-Type values.
 * Deliberately a plain lookup table (no framework), matching the lab's
 * requirement to keep the mechanism visible and explicit.
 */
public final class ContentTypes {

    private static final Map<String, String> TYPES = new HashMap<>();

    static {
        TYPES.put("html", "text/html; charset=utf-8");
        TYPES.put("htm", "text/html; charset=utf-8");
        TYPES.put("js", "application/javascript; charset=utf-8");
        TYPES.put("css", "text/css; charset=utf-8");
        TYPES.put("png", "image/png");
        TYPES.put("jpg", "image/jpeg");
        TYPES.put("jpeg", "image/jpeg");
        TYPES.put("txt", "text/plain; charset=utf-8");
        TYPES.put("json", "application/json; charset=utf-8");
    }

    private ContentTypes() {
    }

    /** Returns the content type for a file name/path based on its extension. */
    public static String forPath(String path) {
        if (path == null) {
            return "application/octet-stream";
        }
        int dot = path.lastIndexOf('.');
        if (dot < 0 || dot == path.length() - 1) {
            return "application/octet-stream";
        }
        String ext = path.substring(dot + 1).toLowerCase();
        return TYPES.getOrDefault(ext, "application/octet-stream");
    }
}
