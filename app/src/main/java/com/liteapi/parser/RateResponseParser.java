package com.liteapi.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liteapi.exception.ApiException;
import com.liteapi.model.CancellationPolicy;
import com.liteapi.model.HotelRate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Parses the JSON response from the LiteAPI {@code POST /hotels/rates} endpoint
 * into a flat list of {@link HotelRate} objects, one per room × rate combination.
 */
public class RateResponseParser {

    private static final Logger LOG = Logger.getLogger(RateResponseParser.class.getName());

    private final ObjectMapper objectMapper;

    public RateResponseParser() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Parses the raw JSON string returned by {@code POST /hotels/rates}.
     *
     * @param json raw response body
     * @return flat list of {@link HotelRate} objects (never {@code null})
     */
    public List<HotelRate> parse(String json) {
        LOG.fine("Parsing rates response (" + (json != null ? json.length() : 0) + " bytes)");
        JsonNode root;
        try {
            root = objectMapper.readTree(json);
        } catch (IOException e) {
            LOG.severe("Failed to parse rates response: " + e.getMessage());
            throw new ApiException("Failed to parse rates response: " + e.getMessage(), e);
        }

        JsonNode data = root.path("data");
        if (data.isMissingNode() || data.isNull()) {
            LOG.warning("Rates response has no 'data' field — response structure may have changed. "
                    + "Root keys: " + root.fieldNames());
            return List.of();
        }

        // data may be a single object or an array of hotel results
        List<HotelRate> rates = new ArrayList<>();
        if (data.isArray()) {
            for (JsonNode hotelNode : data) {
                rates.addAll(parseHotelNode(hotelNode));
            }
        } else {
            rates.addAll(parseHotelNode(data));
        }
        LOG.fine("Parsed " + rates.size() + " rate(s) from response");
        return rates;
    }

    private List<HotelRate> parseHotelNode(JsonNode hotelNode) {
        String hotelId = hotelNode.path("hotelId").asText("N/A");
        List<HotelRate> result = new ArrayList<>();

        JsonNode rooms = hotelNode.path("rooms");
        if (rooms.isArray()) {
            for (JsonNode room : rooms) {
                result.addAll(parseRoom(hotelId, room));
            }
        }
        return result;
    }

    private List<HotelRate> parseRoom(String hotelId, JsonNode room) {
        String roomName = room.path("name").asText("N/A");
        List<HotelRate> result = new ArrayList<>();

        JsonNode rates = room.path("rates");
        if (rates.isArray()) {
            for (JsonNode rateNode : rates) {
                result.add(parseRate(hotelId, roomName, rateNode));
            }
        }
        return result;
    }

    private HotelRate parseRate(String hotelId, String roomName, JsonNode rateNode) {
        // Amount: retailRate.total[0].amount or retailRate.total.amount
        String amount = extractAmount(rateNode);
        String currency = extractCurrency(rateNode);
        String boardType = rateNode.path("boardType").asText("N/A");

        List<CancellationPolicy> policies = parseCancellationPolicies(rateNode);

        return new HotelRate(hotelId, roomName, amount, currency, boardType, policies);
    }

    private String extractAmount(JsonNode rateNode) {
        JsonNode retailRate = rateNode.path("retailRate");
        if (!retailRate.isMissingNode()) {
            JsonNode total = retailRate.path("total");
            if (total.isArray() && total.size() > 0) {
                return total.get(0).path("amount").asText("N/A");
            }
            if (total.isObject()) {
                return total.path("amount").asText("N/A");
            }
        }
        // fallback
        return rateNode.path("price").asText("N/A");
    }

    private String extractCurrency(JsonNode rateNode) {
        JsonNode retailRate = rateNode.path("retailRate");
        if (!retailRate.isMissingNode()) {
            JsonNode total = retailRate.path("total");
            if (total.isArray() && total.size() > 0) {
                return total.get(0).path("currency").asText("N/A");
            }
            if (total.isObject()) {
                return total.path("currency").asText("N/A");
            }
        }
        return rateNode.path("currency").asText("N/A");
    }

    private List<CancellationPolicy> parseCancellationPolicies(JsonNode rateNode) {
        List<CancellationPolicy> policies = new ArrayList<>();
        JsonNode cancelNode = rateNode.path("cancellationPolicies");
        if (cancelNode.isMissingNode() || cancelNode.isNull()) {
            return policies;
        }

        JsonNode infos = cancelNode.path("cancelPolicyInfos");
        if (infos.isArray()) {
            for (JsonNode info : infos) {
                policies.add(new CancellationPolicy(
                        info.path("cancelTime").asText("N/A"),
                        info.path("amount").asText("N/A"),
                        info.path("currency").asText("N/A"),
                        info.path("type").asText("N/A")
                ));
            }
        }
        return policies;
    }
}
