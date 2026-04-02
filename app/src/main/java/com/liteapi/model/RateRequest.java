package com.liteapi.model;

import java.util.List;

/**
 * Request body for the POST /hotels/rates endpoint.
 * Field names match the LiteAPI JSON contract exactly (used by Jackson).
 */
public record RateRequest(
        List<String> hotelIds,
        String checkin,
        String checkout,
        String currency,
        String guestNationality,
        List<Occupancy> occupancies
) {}
