package com.liteapi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the static {@link Main#parseLocation(String)} helper.
 */
class MainTest {

    @Test
    void parsesCityAndCountry() {
        String[] result = Main.parseLocation("New York, US");
        assertEquals("US",       result[0]);
        assertEquals("New York", result[1]);
    }

    @Test
    void parsesCountryCodeAlone() {
        String[] result = Main.parseLocation("GB");
        assertEquals("GB", result[0]);
        assertEquals("",   result[1]);
    }

    @Test
    void parsesCityNameAlone() {
        String[] result = Main.parseLocation("Paris");
        assertEquals("US",    result[0]); // default fallback
        assertEquals("Paris", result[1]);
    }

    @Test
    void handlesNullInput() {
        String[] result = Main.parseLocation(null);
        assertEquals("US", result[0]);
        assertEquals("",   result[1]);
    }

    @Test
    void handlesBlankInput() {
        String[] result = Main.parseLocation("  ");
        assertEquals("US", result[0]);
        assertEquals("",   result[1]);
    }

    @Test
    void normalisesCountryCodeToUpperCase() {
        String[] result = Main.parseLocation("us");
        assertEquals("US", result[0]);
    }
}
