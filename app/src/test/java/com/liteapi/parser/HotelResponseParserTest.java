package com.liteapi.parser;

import com.liteapi.exception.ApiException;
import com.liteapi.model.Hotel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link HotelResponseParser}.
 * All tests use inline JSON strings — no network calls are made.
 */
class HotelResponseParserTest {

    private final HotelResponseParser parser = new HotelResponseParser();

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    void parsesFullHotelRecord() {
        String json = """
                {
                  "data": [
                    {
                      "id": "lp24373",
                      "name": "Grand Central Hotel",
                      "starRating": "4",
                      "address": {
                        "street1": "123 Main St",
                        "city": "New York",
                        "country": "US"
                      }
                    }
                  ]
                }
                """;

        List<Hotel> hotels = parser.parse(json);

        assertEquals(1, hotels.size());
        Hotel h = hotels.getFirst();
        assertEquals("lp24373",           h.id());
        assertEquals("Grand Central Hotel", h.name());
        assertEquals("4",                 h.starRating());
        assertEquals("123 Main St",       h.street());
        assertEquals("New York",          h.city());
        assertEquals("US",                h.country());
    }

    @Test
    void parsesMultipleHotels() {
        String json = """
                {
                  "data": [
                    {"id": "h1", "name": "Hotel A", "starRating": "3",
                     "address": {"street1": "1 Road", "city": "NYC", "country": "US"}},
                    {"id": "h2", "name": "Hotel B", "starRating": "5",
                     "address": {"street1": "2 Ave",  "city": "NYC", "country": "US"}}
                  ]
                }
                """;

        List<Hotel> hotels = parser.parse(json);
        assertEquals(2, hotels.size());
        assertEquals("h1", hotels.get(0).id());
        assertEquals("h2", hotels.get(1).id());
    }

    // ── Missing / null fields ────────────────────────────────────────────────

    @Test
    void handlesMissingAddressGracefully() {
        String json = """
                {
                  "data": [
                    {"id": "lp001", "name": "No Address Hotel"}
                  ]
                }
                """;

        List<Hotel> hotels = parser.parse(json);
        assertEquals(1, hotels.size());
        assertEquals("N/A", hotels.getFirst().street());
        assertEquals("N/A", hotels.getFirst().city());
        assertEquals("N/A", hotels.getFirst().country());
    }

    @Test
    void handlesMissingStarRating() {
        String json = """
                {
                  "data": [
                    {"id": "lp002", "name": "Unrated Hotel",
                     "address": {"city": "NY", "country": "US"}}
                  ]
                }
                """;

        List<Hotel> hotels = parser.parse(json);
        assertEquals("N/A", hotels.getFirst().starRating());
    }

    @Test
    void returnsEmptyListWhenDataIsEmpty() {
        String json = "{\"data\": []}";
        List<Hotel> hotels = parser.parse(json);
        assertTrue(hotels.isEmpty());
    }

    @Test
    void returnsEmptyListWhenDataIsNull() {
        String json = "{\"data\": null}";
        List<Hotel> hotels = parser.parse(json);
        assertTrue(hotels.isEmpty());
    }

    @Test
    void returnsEmptyListWhenDataKeyAbsent() {
        String json = "{\"status\": \"ok\"}";
        List<Hotel> hotels = parser.parse(json);
        assertTrue(hotels.isEmpty());
    }

    // ── Error cases ───────────────────────────────────────────────────────────

    @Test
    void throwsApiExceptionOnInvalidJson() {
        assertThrows(ApiException.class, () -> parser.parse("not-valid-json"));
    }

    @Test
    void throwsApiExceptionOnNullInput() {
        assertThrows(Exception.class, () -> parser.parse(null));
    }
}
