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

    @Test
    void parsesRoomTypesShape() {
        String json = """
                {
                  "data": [
                    {
                      "hotelId": "h2",
                      "roomTypes": [
                        {
                          "roomType": "Queen Studio",
                          "rates": [
                            {
                              "boardType": "RO",
                              "retailRate": {"total": [{"amount": 120, "currency": "USD"}]}
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
        assertEquals("Queen Studio", rates.getFirst().roomName());
    }

    @Test
    void parsesRoomNameFromRateObject() {
        // This test covers the new documentation format where the name is in the rate object
        String json = """
                {
                  "data": [
                    {
                      "hotelId": "lp1897",
                      "roomTypes": [
                        {
                          "roomTypeId": "room123",
                          "rates": [
                            {
                              "name": "Standard King Room",
                              "boardType": "RO",
                              "boardName": "Room Only",
                              "retailRate": {"total": [{"amount": 163.66, "currency": "USD"}]},
                              "cancellationPolicies": {
                                "cancelPolicyInfos": [
                                  {
                                    "cancelTime": "2026-07-30 02:00:00",
                                    "amount": "163.66",
                                    "currency": "USD",
                                    "type": "amount"
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
        assertEquals("lp1897", r.hotelId());
        assertEquals("Standard King Room", r.roomName());
        assertEquals("163.66", r.amount());
        assertEquals("USD", r.currency());
        assertEquals("RO", r.boardType());
        assertEquals(1, r.cancellationPolicies().size());
        assertEquals("amount", r.cancellationPolicies().getFirst().type());
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

    @Test
    void throwsApiExceptionWhenErrorObjectIsPresent() {
        String json = """
                {
                  "error": {
                    "code": 401,
                    "message": "unauthorized"
                  }
                }
                """;

        ApiException ex = assertThrows(ApiException.class, () -> parser.parse(json));
        assertEquals(401, ex.getStatusCode());
        assertTrue(ex.getMessage().contains("unauthorized"));
    }
}
