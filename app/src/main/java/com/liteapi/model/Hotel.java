package com.liteapi.model;

/**
 * Represents a hotel returned by the LiteAPI hotel-search endpoint.
 */
public record Hotel(
        String id,
        String name,
        String street,
        String city,
        String country,
        String address,
        String starRating
) {}
