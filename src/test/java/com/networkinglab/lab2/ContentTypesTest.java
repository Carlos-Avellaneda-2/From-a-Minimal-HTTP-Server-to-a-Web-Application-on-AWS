package com.networkinglab.lab2;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContentTypesTest {

    @Test
    void htmlFilesGetHtmlType() {
        assertEquals("text/html; charset=utf-8", ContentTypes.forPath("index.html"));
    }

    @Test
    void jsFilesGetJavascriptType() {
        assertTrue(ContentTypes.forPath("app.js").contains("javascript"));
    }

    @Test
    void pngFilesGetPngType() {
        assertEquals("image/png", ContentTypes.forPath("logo.png"));
    }

    @Test
    void jpegFilesGetJpegType() {
        assertEquals("image/jpeg", ContentTypes.forPath("banner.jpg"));
        assertEquals("image/jpeg", ContentTypes.forPath("banner.jpeg"));
    }

    @Test
    void unknownExtensionFallsBackToOctetStream() {
        assertEquals("application/octet-stream", ContentTypes.forPath("file.xyz"));
    }

    @Test
    void missingExtensionFallsBackToOctetStream() {
        assertEquals("application/octet-stream", ContentTypes.forPath("README"));
    }
}
