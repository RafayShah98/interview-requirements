package com.liteapi;

import com.liteapi.config.AppConfig;
import com.liteapi.exception.ApiException;
import com.liteapi.http.LiteApiClient;
import com.liteapi.model.Hotel;
import com.liteapi.model.HotelRate;
import com.liteapi.model.Occupancy;
import com.liteapi.model.RateRequest;
import com.liteapi.parser.HotelResponseParser;
import com.liteapi.parser.RateResponseParser;
import com.liteapi.presenter.Presenter;

import java.util.List;
import java.util.Scanner;

/**
 * Entry point for the LiteAPI Hotel Search CLI application.
 *
 * <p>Executes three flows in sequence:
 * <ol>
 *   <li>Search hotels by city / country code</li>
 *   <li>Fetch room rates for a selected hotel</li>
 *   <li>All error conditions are handled with clean messages — no raw stack traces</li>
 * </ol>
 *
 * <p>The city name can be supplied as the first CLI argument, e.g.:
 * <pre>
 *   ./gradlew run --args="New York"
 * </pre>
 */
public class Main {

    // Default stay dates — edit these or make them interactive as needed
    private static final String DEFAULT_CHECKIN  = "2026-05-02";
    private static final String DEFAULT_CHECKOUT = "2026-05-04";
    private static final String DEFAULT_CURRENCY = "USD";
    private static final String DEFAULT_NATIONALITY = "US";
    private static final int    DEFAULT_ADULTS   = 2;

    public static void main(String[] args) {
        Presenter presenter = new Presenter();

        // ── Load configuration ────────────────────────────────────────────────
        AppConfig config;
        try {
            config = new AppConfig();
        } catch (ApiException e) {
            presenter.printError(e.getMessage());
            System.exit(1);
            return;
        }

        LiteApiClient    client       = new LiteApiClient(config.getApiKey());
        HotelResponseParser hotelParser = new HotelResponseParser();
        RateResponseParser  rateParser  = new RateResponseParser();

        Scanner scanner = new Scanner(System.in);

        // ── Flow 1 — Search Hotels ────────────────────────────────────────────
        presenter.printBanner("FLOW 1 — HOTEL SEARCH");

        String[] locationInput = resolveLocation(args, scanner, presenter);
        String countryCode = locationInput[0];
        String cityName    = locationInput[1];

        presenter.printInfo(String.format("Searching hotels in %s%s ...",
                cityName.isBlank() ? "" : cityName + ", ", countryCode));

        List<Hotel> hotels;
        try {
            String json = client.searchHotels(countryCode, cityName);
            hotels = hotelParser.parse(json);
        } catch (ApiException e) {
            presenter.printError(e.getMessage());
            System.exit(1);
            return;
        }

        presenter.printHotels(hotels);

        if (hotels.isEmpty()) {
            System.exit(0);
        }

        // ── Flow 2 — Get Hotel Rates ──────────────────────────────────────────
        presenter.printBanner("FLOW 2 — HOTEL RATES");

        String hotelId = resolveHotelId(hotels, scanner, presenter);

        System.out.print("  Enter check-in date [YYYY-MM-DD] (default " + DEFAULT_CHECKIN + "): ");
        String checkin = scanner.nextLine().trim();
        if (checkin.isBlank()) checkin = DEFAULT_CHECKIN;

        System.out.print("  Enter check-out date [YYYY-MM-DD] (default " + DEFAULT_CHECKOUT + "): ");
        String checkout = scanner.nextLine().trim();
        if (checkout.isBlank()) checkout = DEFAULT_CHECKOUT;

        System.out.print("  Enter currency code (default " + DEFAULT_CURRENCY + "): ");
        String currency = scanner.nextLine().trim();
        if (currency.isBlank()) currency = DEFAULT_CURRENCY;

        System.out.print("  Enter guest nationality (default " + DEFAULT_NATIONALITY + "): ");
        String nationality = scanner.nextLine().trim();
        if (nationality.isBlank()) nationality = DEFAULT_NATIONALITY;

        presenter.printInfo(String.format(
                "Fetching rates for hotel %s  (check-in: %s, check-out: %s) ...",
                hotelId, checkin, checkout));

        RateRequest rateRequest = new RateRequest(
                List.of(hotelId),
                checkin,
                checkout,
                currency,
                nationality,
                List.of(new Occupancy(DEFAULT_ADULTS, 0, List.of()))
        );

        List<HotelRate> rates;
        try {
            String json = client.getHotelRates(rateRequest);
            rates = rateParser.parse(json);
        } catch (ApiException e) {
            presenter.printError(e.getMessage());
            System.exit(1);
            return;
        }

        presenter.printRates(rates);
        System.out.println("Done.");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Input helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns {@code {countryCode, cityName}} either from CLI args or by
     * prompting the user interactively.
     *
     * <p>Accepts inputs such as:
     * <ul>
     *   <li>{@code "New York, US"} — parsed into city + country</li>
     *   <li>{@code "US"}           — treated as country-only search</li>
     *   <li>{@code "New York"}     — treated as city name; country defaults to US</li>
     * </ul>
     */
    private static String[] resolveLocation(String[] args, Scanner scanner, Presenter presenter) {
        String rawInput;
        if (args.length > 0) {
            rawInput = String.join(" ", args);
            presenter.printInfo("Using location from command-line argument: " + rawInput);
        } else {
            System.out.print("  Enter city name or country code (e.g. 'New York, US' or 'US'): ");
            rawInput = scanner.nextLine().trim();
        }
        return parseLocation(rawInput);
    }

    /**
     * Parses a raw location string into a {@code [countryCode, cityName]} pair.
     */
    static String[] parseLocation(String rawInput) {
        if (rawInput == null || rawInput.isBlank()) {
            return new String[]{"US", ""};
        }

        // "New York, US"  — last comma-separated segment is the country code
        int lastComma = rawInput.lastIndexOf(',');
        if (lastComma >= 0) {
            String country = rawInput.substring(lastComma + 1).trim();
            String city    = rawInput.substring(0, lastComma).trim();
            if (country.length() == 2 && country.chars().allMatch(Character::isLetter)) {
                return new String[]{country.toUpperCase(), city};
            }
        }

        // Two-letter code alone — treat as country
        if (rawInput.length() == 2 && rawInput.chars().allMatch(Character::isLetter)) {
            return new String[]{rawInput.toUpperCase(), ""};
        }

        // Default: treat the whole string as a city name with fallback country US
        return new String[]{"US", rawInput};
    }

    /**
     * Asks the user to pick a hotel by number or to type a Hotel ID directly.
     */
    private static String resolveHotelId(List<Hotel> hotels, Scanner scanner, Presenter presenter) {
        while (true) {
            System.out.print("  Enter hotel number (1-" + hotels.size() + ") or paste a Hotel ID: ");
            String input = scanner.nextLine().trim();

            // Try to parse as a list index first
            try {
                int idx = Integer.parseInt(input);
                if (idx >= 1 && idx <= hotels.size()) {
                    return hotels.get(idx - 1).id();
                }
                presenter.printError("Please enter a number between 1 and " + hotels.size() + ".");
            } catch (NumberFormatException e) {
                // Not a number — use as a literal hotel ID
                if (!input.isBlank()) {
                    return input;
                }
                presenter.printError("Input cannot be empty.");
            }
        }
    }
}
