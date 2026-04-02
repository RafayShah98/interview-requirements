package com.liteapi.parser;

import com.liteapi.exception.ApiException;
import com.liteapi.model.HotelRate;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link RateResponseParser}.
 * All tests use inline JSON strings — no network calls are made.
 */
class RateResponseParserTest {

    private final RateResponseParser parser = new RateResponseParser();

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    void parsesFullRateRecord() {
        String json = """
                {
                  "data": [
                    {
                      "hotelId": "lp24373",
                      "rooms": [
                        {
                          "name": "Deluxe King Room",
                          "rates": [
                            {
                              "boardType": "RO",
                              "retailRate": {
                                "total": [
                                  {"amount": 250.00, "currency": "USD"}
                                ]
                              },
                              "cancellationPolicies": {
                                "cancelPolicyInfos": [
                                  {
                                    "cancelTime": "2025-11-08T00:00:00",
                                    "amount": "100",
                                    "currency": "USD",
                                    "type": "percentage"
                                  }
                                ]
                              }
                            }
                          ]
                        }
                      ]
                    }
                  ]
                }
                """;

        List<HotelRate> rates = parser.parse(json);

        assertEquals(1, rates.size());
        HotelRate r = rates.getFirst();
        assertEquals("lp24373",        r.hotelId());
        assertEquals("Deluxe King Room", r.roomName());
        assertEquals("250.0",          r.amount());
        assertEquals("USD",            r.currency());
        assertEquals("RO",             r.boardType());
        assertEquals(1, r.cancellationPolicies().size());
        assertEquals("percentage",     r.cancellationPolicies().getFirst().type());
    }

    @Test
    void parsesMultipleRooms() {
        String json = """
                {
                  "data": [
                    {
                      "hotelId": "h1",
                      "rooms": [
                        {
                          "name": "Single Room",
                          "rates": [
                            {
                              "boardType": "BB",
                              "retailRate": {"total": [{"amount": 100, "currency": "USD"}]},
                              "cancellationPolicies": {}
                            }
                          ]
                        },
                        {
                          "name": "Double Room",
                          "rates": [
                            {
                              "boardType": "HB",
                              "retailRate": {"total": [{"amount": 200, "currency": "USD"}]},
                              "cancellationPolicies": {}
                            }
                          ]
                        }
                      ]
                    }
                  ]
                }
                """;

        List<HotelRate> rates = parser.parse(json);
        assertEquals(2, rates.size());
        assertEquals("Single Room", rates.get(0).roomName());
        assertEquals("Double Room", rates.get(1).roomName());
    }

    // ── Empty / null ─────────────────────────────────────────────────────────

    @Test
    void returnsEmptyListWhenDataIsEmptyArray() {
        String json = "{\"data\": []}";
        assertTrue(parser.parse(json).isEmpty());
    }

    @Test
    void returnsEmptyListWhenDataIsNull() {
        String json = "{\"data\": null}";
        assertTrue(parser.parse(json).isEmpty());
    }

    @Test
    void returnsEmptyListWhenNoRooms() {
        String json = """
                {
                  "data": [
                    {"hotelId": "h1", "rooms": []}
                  ]
                }
                """;
        assertTrue(parser.parse(json).isEmpty());
    }

    @Test
    void handlesNoCancellationPolicies() {
        String json = """
                {
                  "data": [
                    {
                      "hotelId": "h1",
                      "rooms": [
                        {
                          "name": "Room",
                          "rates": [
                            {
                              "boardType": "RO",
                              "retailRate": {"total": [{"amount": 99, "currency": "USD"}]}
                            }
                          ]
                        }
                      ]
                    }
                  ]
                }
                """;

        List<HotelRate> rates = parser.parse(json);
        assertEquals(1, rates.size());
        assertTrue(rates.getFirst().cancellationPolicies().isEmpty());
    }

    // ── Error cases ───────────────────────────────────────────────────────────

    @Test
    void throwsApiExceptionOnInvalidJson() {
        assertThrows(ApiException.class, () -> parser.parse("{bad json}"));
    }
}
