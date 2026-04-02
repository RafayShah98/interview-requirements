package com.liteapi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Request body for the POST /hotels/rates endpoint.
 * Field names match the LiteAPI JSON contract exactly (used by Jackson).
 */
public record RateRequest(
        List<String> hotelIds,

        @JsonProperty("checkIn")
        String checkin,

        @JsonProperty("checkOut")
        String checkout,

        String currency,

        @JsonProperty("nationality")
        String guestNationality,

        List<Occupancy> occupancies
) {
    /**
     * Represents room occupancy details for a rate request.
     */
    public record Occupancy(int adults) {}
}
