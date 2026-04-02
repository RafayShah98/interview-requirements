package com.liteapi.model;

import java.util.List;

/**
 * Represents a single rate option for a hotel room returned by the rates endpoint.
 */
public record HotelRate(
        String hotelId,
        String roomName,
        String amount,
        String currency,
        String boardType,
        List<CancellationPolicy> cancellationPolicies
) {}
