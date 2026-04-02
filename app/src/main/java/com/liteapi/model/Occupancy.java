package com.liteapi.model;

import java.util.List;

/**
 * Represents the occupancy configuration for a single room in a rate request.
 * Field names match the LiteAPI JSON contract exactly (used by Jackson).
 */
public record Occupancy(
        int adults,
        int children,
        List<Integer> childAges
) {}
