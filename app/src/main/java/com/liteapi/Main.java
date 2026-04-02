package com.liteapi;

import com.liteapi.config.AppConfig;
import com.liteapi.exception.ApiException;
import com.liteapi.http.LiteApiClient;
import com.liteapi.model.Hotel;
import com.liteapi.model.HotelRate;
import com.liteapi.model.RateRequest;
import com.liteapi.parser.HotelResponseParser;
import com.liteapi.parser.RateResponseParser;
import com.liteapi.presenter.Presenter;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
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

    private static final int DEFAULT_CHECKIN_OFFSET_DAYS = 30;
    private static final int DEFAULT_STAY_NIGHTS = 2;
    private static final String DEFAULT_CURRENCY = "USD";
    private static final String DEFAULT_NATIONALITY = "US";
    private static final int DEFAULT_ADULTS = 2;

    public static void main(String[] args) {
        Presenter presenter = new Presenter();

        try {
            AppConfig config = new AppConfig();
            LiteApiClient client = new LiteApiClient(config.getApiKey());
            HotelResponseParser hotelParser = new HotelResponseParser();
            RateResponseParser rateParser = new RateResponseParser();
            Scanner scanner = new Scanner(System.in);

            presenter.printBanner("FLOW 1 — HOTEL SEARCH");
            String[] locationInput = resolveLocation(args, scanner, presenter);
            String countryCode = locationInput[0];
            String cityName = locationInput[1];

            presenter.printInfo(String.format("Searching hotels in %s%s ...",
                    cityName.isBlank() ? "" : cityName + ", ", countryCode));

            String hotelsJson = client.searchHotels(countryCode, cityName);
            List<Hotel> hotels = hotelParser.parse(hotelsJson);
            presenter.printHotels(hotels);

            if (hotels.isEmpty()) {
                System.exit(0);
            }

            presenter.printBanner("FLOW 2 — HOTEL RATES");
            String hotelId = resolveHotelId(hotels, scanner, presenter);
            RateRequest rateRequest = resolveRateRequest(hotelId, scanner, presenter);

            presenter.printInfo(String.format(
                    "Fetching rates for hotel %s (check-in: %s, check-out: %s) ...",
                    hotelId,
                    rateRequest.checkin(),
                    rateRequest.checkout()));

            String ratesJson = client.getHotelRates(rateRequest);
            List<HotelRate> rates = rateParser.parse(ratesJson);
            presenter.printRates(rates);
            System.out.println("Done.");
        } catch (ApiException e) {
            presenter.printError(e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            presenter.printError("Unexpected application error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
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

    private static RateRequest resolveRateRequest(String hotelId, Scanner scanner, Presenter presenter) {
        LocalDate defaultCheckinDate = LocalDate.now().plusDays(DEFAULT_CHECKIN_OFFSET_DAYS);
        LocalDate defaultCheckoutDate = defaultCheckinDate.plusDays(DEFAULT_STAY_NIGHTS);

        String checkin = resolveDate(
                scanner,
                presenter,
                "  Enter check-in date [YYYY-MM-DD] (default " + defaultCheckinDate + "): ",
                defaultCheckinDate
        );
        String checkout = resolveCheckoutDate(scanner, presenter, checkin, defaultCheckoutDate);
        String currency = resolveThreeLetterCode(
                scanner,
                presenter,
                "  Enter currency code (default " + DEFAULT_CURRENCY + "): ",
                DEFAULT_CURRENCY,
                "Currency"
        );
        String nationality = resolveTwoLetterCode(
                scanner,
                presenter,
                "  Enter guest nationality (default " + DEFAULT_NATIONALITY + "): ",
                DEFAULT_NATIONALITY,
                "Guest nationality"
        );
        int adults = resolveAdults(scanner, presenter);

        return new RateRequest(
                List.of(hotelId),
                checkin,
                checkout,
                currency,
                nationality,
                List.of(new RateRequest.Occupancy(adults))
        );
    }

    private static String resolveDate(Scanner scanner, Presenter presenter, String prompt, LocalDate defaultDate) {
        while (true) {
            System.out.print(prompt);
            if (!scanner.hasNextLine()) {
                return defaultDate.toString();
            }
            String input = scanner.nextLine().trim();
            if (input.isBlank()) {
                return defaultDate.toString();
            }
            try {
                return LocalDate.parse(input).toString();
            } catch (DateTimeParseException ignored) {
                presenter.printError("Please enter a valid date in YYYY-MM-DD format.");
            }
        }
    }

    private static String resolveCheckoutDate(
            Scanner scanner,
            Presenter presenter,
            String checkin,
            LocalDate defaultCheckoutDate
    ) {
        while (true) {
            String checkout = resolveDate(
                    scanner,
                    presenter,
                    "  Enter check-out date [YYYY-MM-DD] (default " + defaultCheckoutDate + "): ",
                    defaultCheckoutDate
            );

            if (LocalDate.parse(checkout).isAfter(LocalDate.parse(checkin))) {
                return checkout;
            }
            presenter.printError("Check-out date must be after check-in date.");
        }
    }

    private static String resolveTwoLetterCode(
            Scanner scanner,
            Presenter presenter,
            String prompt,
            String defaultValue,
            String fieldName
    ) {
        while (true) {
            System.out.print(prompt);
            if (!scanner.hasNextLine()) {
                return defaultValue;
            }
            String input = scanner.nextLine().trim();
            String value = input.isBlank() ? defaultValue : input.toUpperCase();
            if (value.length() == 2 && value.chars().allMatch(Character::isLetter)) {
                return value;
            }
            presenter.printError(fieldName + " must be a 2-letter code.");
        }
    }

    private static String resolveThreeLetterCode(
            Scanner scanner,
            Presenter presenter,
            String prompt,
            String defaultValue,
            String fieldName
    ) {
        while (true) {
            System.out.print(prompt);
            if (!scanner.hasNextLine()) {
                return defaultValue;
            }
            String input = scanner.nextLine().trim();
            String value = input.isBlank() ? defaultValue : input.toUpperCase();
            if (value.length() == 3 && value.chars().allMatch(Character::isLetter)) {
                return value;
            }
            presenter.printError(fieldName + " must be a 3-letter code.");
        }
    }

    private static int resolveAdults(Scanner scanner, Presenter presenter) {
        while (true) {
            System.out.print("  Enter adults count (default " + DEFAULT_ADULTS + "): ");
            if (!scanner.hasNextLine()) {
                return DEFAULT_ADULTS;
            }
            String input = scanner.nextLine().trim();
            if (input.isBlank()) {
                return DEFAULT_ADULTS;
            }
            try {
                int adults = Integer.parseInt(input);
                if (adults >= 1 && adults <= 8) {
                    return adults;
                }
            } catch (NumberFormatException ignored) {
                // Unified error message below.
            }
            presenter.printError("Adults must be a number between 1 and 8.");
        }
    }
}
