package com.liteapi.presenter;

import com.liteapi.model.CancellationPolicy;
import com.liteapi.model.Hotel;
import com.liteapi.model.HotelRate;

import java.util.List;

/**
 * Presentation layer responsible for formatting and printing results to the console.
 * No business logic lives here — only display formatting.
 */
public class Presenter {

    private static final String DIVIDER = "-".repeat(60);
    private static final String THIN_DIVIDER = ".".repeat(60);

    // -----------------------------------------------------------------------
    // Hotels
    // -----------------------------------------------------------------------

    /**
     * Prints a numbered list of hotels.
     */
    public void printHotels(List<Hotel> hotels) {
        if (hotels.isEmpty()) {
            System.out.println("  No hotels found for the given location.");
            return;
        }
        System.out.println();
        System.out.println(DIVIDER);
        System.out.printf("  %-4s %-30s %-15s %s%n", "#", "Hotel Name", "Hotel ID", "Stars");
        System.out.println(DIVIDER);

        for (int i = 0; i < hotels.size(); i++) {
            Hotel h = hotels.get(i);
            String displayAddress = !"N/A".equals(h.address()) ? h.address() : buildAddress(h);
            System.out.printf("  %-4d %-30s %-15s %-5s%n",
                    i + 1,
                    truncate(h.name(), 29),
                    truncate(h.id(), 14),
                    h.starRating()
            );
            System.out.printf("          %s%n", displayAddress);
        }
        System.out.println(DIVIDER);
        System.out.printf("  Total: %d hotel(s)%n", hotels.size());
        System.out.println();
    }

    // -----------------------------------------------------------------------
    // Rates
    // -----------------------------------------------------------------------

    /**
     * Prints the available rates for a hotel.
     */
    public void printRates(List<HotelRate> rates) {
        if (rates.isEmpty()) {
            System.out.println("  No rates available for this hotel and the selected dates.");
            System.out.println("  Tip: Try different check-in / check-out dates.");
            return;
        }
        System.out.println();
        System.out.println(DIVIDER);
        System.out.println("  AVAILABLE RATES");
        System.out.println(DIVIDER);

        for (int i = 0; i < rates.size(); i++) {
            HotelRate r = rates.get(i);
            System.out.printf("  [%d] %s%n", i + 1, r.roomName());
            System.out.printf("      Rate    : %s %s%n", r.amount(), r.currency());
            System.out.printf("      Board   : %s%n", r.boardType());
            printCancellationPolicies(r.cancellationPolicies());
            if (i < rates.size() - 1) {
                System.out.println("  " + THIN_DIVIDER);
            }
        }
        System.out.println(DIVIDER);
        System.out.printf("  Total: %d rate(s)%n", rates.size());
        System.out.println();
    }

    // -----------------------------------------------------------------------
    // Errors
    // -----------------------------------------------------------------------

    /**
     * Prints a clean, user-friendly error message. No stack traces.
     */
    public void printError(String message) {
        System.out.println();
        System.out.println("  [ERROR] " + message);
        System.out.println();
    }

    /**
     * Prints an informational / section heading.
     */
    public void printInfo(String message) {
        System.out.println("  -> " + message);
    }

    /**
     * Prints a prominent section banner.
     */
    public void printBanner(String title) {
        System.out.println();
        System.out.println(DIVIDER);
        System.out.println("  " + title);
        System.out.println(DIVIDER);
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private void printCancellationPolicies(List<CancellationPolicy> policies) {
        if (policies.isEmpty()) {
            System.out.println("      Cancel  : No cancellation policy information available");
            return;
        }
        System.out.println("      Cancel  :");
        for (CancellationPolicy p : policies) {
            System.out.printf("                %s - %s %s (%s)%n",
                    p.cancelTime(), p.amount(), p.currency(), p.type());
        }
    }

    private String buildAddress(Hotel h) {
        StringBuilder sb = new StringBuilder();
        if (!"N/A".equals(h.street()))  sb.append(h.street());
        if (!"N/A".equals(h.city())) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(h.city());
        }
        if (!"N/A".equals(h.country())) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(h.country());
        }
        return sb.isEmpty() ? "N/A" : sb.toString();
    }

    private String truncate(String value, int maxLen) {
        if (value == null) return "N/A";
        return value.length() <= maxLen ? value : value.substring(0, maxLen - 3) + "...";
    }
}
