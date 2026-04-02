package com.liteapi.model;

/**
 * Represents one cancellation-policy entry returned inside a rate.
 */
public record CancellationPolicy(
        String cancelTime,
        String amount,
        String currency,
        String type
) {}
