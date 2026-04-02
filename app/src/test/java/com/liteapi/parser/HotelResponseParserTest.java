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
                      "address": "123 Main St, New York, US"
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
        assertEquals("N/A",               h.street());
        assertEquals("N/A",               h.city());
        assertEquals("N/A",               h.country());
        assertEquals("123 Main St, New York, US", h.address());
    }

    @Test
    void parsesMultipleHotels() {
        String json = """
                {
                  "data": [
                    {"id": "h1", "name": "Hotel A", "starRating": "3", "address": "1 Road, NYC, US"},
                    {"id": "h2", "name": "Hotel B", "starRating": "5", "address": "2 Ave, NYC, US"}
                  ]
                }
                """;

        List<Hotel> hotels = parser.parse(json);
        assertEquals(2, hotels.size());
        assertEquals("h1", hotels.get(0).id());
        assertEquals("h2", hotels.get(1).id());
        assertEquals("1 Road, NYC, US", hotels.get(0).address());
        assertEquals("2 Ave, NYC, US", hotels.get(1).address());
    }

    @Test
    void parsesHotelWithNestedAddressObject() {
        String json = """
                {
                  "data": [
                    {
                      "id": "lp42fec",
                      "name": "Hotel Jadran",
                      "starRating": "3",
                      "address": "Obala dr. Franje Tudmana 52",
                      "city": "Sibenik",
                      "country": "HR"
                    }
                  ]
                }
                """;

        List<Hotel> hotels = parser.parse(json);
        assertEquals(1, hotels.size());
        Hotel h = hotels.getFirst();
        assertEquals("lp42fec", h.id());
        assertEquals("Hotel Jadran", h.name());
        assertEquals("Obala dr. Franje Tudmana 52", h.address());
        assertEquals("Sibenik", h.city());
        assertEquals("HR", h.country());
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
        assertEquals("N/A", hotels.getFirst().address());
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
