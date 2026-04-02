package com.liteapi.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liteapi.exception.ApiException;
import com.liteapi.model.Hotel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses the JSON response from the LiteAPI hotel-search endpoint into
 * a list of {@link Hotel} domain objects.
 *
 * <p>Null-safe: missing or empty fields are replaced with sensible defaults
 * rather than throwing.
 */
public class HotelResponseParser {

    private final ObjectMapper objectMapper;

    public HotelResponseParser() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Parses the raw JSON string returned by {@code GET /data/hotels}.
     *
     * @param json raw response body
     * @return list of parsed {@link Hotel} objects (never {@code null})
     */
    public List<Hotel> parse(String json) {
        JsonNode root;
        try {
            root = objectMapper.readTree(json);
        } catch (IOException e) {
            throw new ApiException("Failed to parse hotel search response: " + e.getMessage(), e);
        }

        JsonNode data = root.path("data");
        if (data.isMissingNode() || data.isNull() || !data.isArray()) {
            return List.of();
        }

        List<Hotel> hotels = new ArrayList<>();
        for (JsonNode hotelNode : data) {
            hotels.add(parseHotel(hotelNode));
        }
        return hotels;
    }

    private Hotel parseHotel(JsonNode node) {
        String id          = text(node, "id");
        String name        = text(node, "name");
        String starRating  = starRating(node);

        JsonNode address   = node.path("address");
        String street      = addressField(address, "street1", "street");
        String city        = addressField(address, "city", "cityName");
        String country     = addressField(address, "country", "countryCode");

        return new Hotel(id, name, street, city, country, starRating);
    }

    private String text(JsonNode node, String field) {
        JsonNode n = node.path(field);
        return (n.isMissingNode() || n.isNull()) ? "N/A" : n.asText("N/A");
    }

    private String addressField(JsonNode address, String... candidates) {
        if (address.isMissingNode() || address.isNull()) return "N/A";
        for (String candidate : candidates) {
            JsonNode n = address.path(candidate);
            if (!n.isMissingNode() && !n.isNull() && !n.asText().isBlank()) {
                return n.asText();
            }
        }
        return "N/A";
    }

    private String starRating(JsonNode node) {
        for (String field : new String[]{"starRating", "stars", "rating"}) {
            JsonNode n = node.path(field);
            if (!n.isMissingNode() && !n.isNull()) {
                String val = n.asText();
                if (!val.isBlank() && !val.equals("0")) return val;
            }
        }
        return "N/A";
    }
}
