package com.networkinglab.lab2;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonUtilTest {

    @Test
    void escapesQuotesAndBackslashes() {
        assertEquals("a\\\"b\\\\c", JsonUtil.escape("a\"b\\c"));
    }

    @Test
    void escapesNewlinesAndTabs() {
        assertEquals("a\\nb\\tc", JsonUtil.escape("a\nb\tc"));
    }

    @Test
    void nullBecomesEmptyString() {
        assertEquals("", JsonUtil.escape(null));
    }

    @Test
    void plainTextIsUnchanged() {
        assertEquals("Ana", JsonUtil.escape("Ana"));
    }
}
