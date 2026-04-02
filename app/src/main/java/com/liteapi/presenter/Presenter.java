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

    // ── ANSI colour / style codes ────────────────────────────────────────────
    private static final String RESET   = "\u001B[0m";
    private static final String BOLD    = "\u001B[1m";
    private static final String DIM     = "\u001B[2m";
    private static final String CYAN    = "\u001B[36m";
    private static final String YELLOW  = "\u001B[33m";
    private static final String GREEN   = "\u001B[32m";
    private static final String RED     = "\u001B[31m";
    private static final String MAGENTA = "\u001B[35m";
    private static final String BLUE    = "\u001B[34m";
    private static final String WHITE   = "\u001B[97m";

    // ── Box-drawing characters ────────────────────────────────────────────────
    private static final int    BOX_WIDTH     = 70;
    private static final String H_LINE        = "─".repeat(BOX_WIDTH);
    private static final String H_LINE_THIN   = "╌".repeat(BOX_WIDTH);
    private static final String TOP_BORDER    = "╔" + "═".repeat(BOX_WIDTH) + "╗";
    private static final String MID_BORDER    = "╠" + "═".repeat(BOX_WIDTH) + "╣";
    private static final String BOT_BORDER    = "╚" + "═".repeat(BOX_WIDTH) + "╝";
    private static final String TOP_LIGHT     = "┌" + H_LINE + "┐";
    private static final String MID_LIGHT     = "├" + H_LINE + "┤";
    private static final String BOT_LIGHT     = "└" + H_LINE + "┘";
    private static final String SEP_LIGHT     = "├" + H_LINE_THIN + "┤";

    // ── Column widths for hotels table ────────────────────────────────────────
    private static final int COL_NUM    = 4;
    private static final int COL_NAME   = 32;
    private static final int COL_ID     = 14;
    private static final int COL_STARS  = 6;

    // -----------------------------------------------------------------------
    // Hotels
    // -----------------------------------------------------------------------

    /**
     * Prints a numbered list of hotels.
     */
    public void printHotels(List<Hotel> hotels) {
        System.out.println();
        if (hotels.isEmpty()) {
            System.out.println(TOP_LIGHT);
            System.out.println(pad("│  " + YELLOW + "⚠  No hotels found for the given location." + RESET));
            System.out.println(BOT_LIGHT);
            System.out.println();
            return;
        }

        // Header
        System.out.println(CYAN + TOP_LIGHT + RESET);
        System.out.println(CYAN + "│" + RESET
                + BOLD + WHITE
                + String.format("  %-" + COL_NUM + "s  %-" + COL_NAME + "s  %-" + COL_ID + "s  %s",
                        "#", "Hotel Name", "Hotel ID", "Stars")
                + RESET
                + padRight("", BOX_WIDTH - 4 - COL_NUM - COL_NAME - COL_ID - 8)
                + CYAN + "│" + RESET);
        System.out.println(CYAN + MID_LIGHT + RESET);

        for (int i = 0; i < hotels.size(); i++) {
            Hotel h = hotels.get(i);
            String displayAddress = !"N/A".equals(h.address()) ? h.address() : buildAddress(h);

            String numStr   = String.format("%-" + COL_NUM + "d", i + 1);
            String nameStr  = truncate(h.name(), COL_NAME);
            String idStr    = truncate(h.id(), COL_ID);
            String starStr  = starsDisplay(h.starRating());

            String row = String.format("  %s%s%s  %-" + COL_NAME + "s  %s%-" + COL_ID + "s%s  %s",
                    BOLD + YELLOW, numStr, RESET,
                    nameStr,
                    DIM, idStr, RESET,
                    starStr);

            System.out.println(CYAN + "│" + RESET + padRawLine(row, BOX_WIDTH) + CYAN + "│" + RESET);

            // Address line
            String addrLine = "      " + DIM + truncate(displayAddress, BOX_WIDTH - 8) + RESET;
            System.out.println(CYAN + "│" + RESET + padRawLine(addrLine, BOX_WIDTH) + CYAN + "│" + RESET);

            if (i < hotels.size() - 1) {
                System.out.println(CYAN + SEP_LIGHT + RESET);
            }
        }

        System.out.println(CYAN + MID_LIGHT + RESET);
        System.out.println(CYAN + "│" + RESET
                + GREEN + BOLD + String.format("  ✔  %d hotel(s) found", hotels.size()) + RESET
                + padRight("", BOX_WIDTH - visibleLength(String.format("  ✔  %d hotel(s) found", hotels.size())))
                + CYAN + "│" + RESET);
        System.out.println(CYAN + BOT_LIGHT + RESET);
        System.out.println();
    }

    // -----------------------------------------------------------------------
    // Rates
    // -----------------------------------------------------------------------

    /**
     * Prints the available rates for a hotel.
     */
    public void printRates(List<HotelRate> rates) {
        System.out.println();
        if (rates.isEmpty()) {
            System.out.println(TOP_LIGHT);
            System.out.println(pad("│  " + YELLOW + "⚠  No rates available for this hotel and the selected dates." + RESET));
            System.out.println(pad("│  " + DIM    + "   Tip: Try different check-in / check-out dates." + RESET));
            System.out.println(BOT_LIGHT);
            System.out.println();
            return;
        }

        System.out.println(MAGENTA + TOP_LIGHT + RESET);
        System.out.println(MAGENTA + "│" + RESET
                + BOLD + WHITE + "  AVAILABLE RATES" + RESET
                + padRight("", BOX_WIDTH - 18)
                + MAGENTA + "│" + RESET);
        System.out.println(MAGENTA + MID_LIGHT + RESET);

        for (int i = 0; i < rates.size(); i++) {
            HotelRate r = rates.get(i);

            // Room name row
            String roomLine = String.format("  %s[%d]%s %s%s%s",
                    BOLD + YELLOW, i + 1, RESET, BOLD + WHITE, r.roomName(), RESET);
            System.out.println(MAGENTA + "│" + RESET + padRawLine(roomLine, BOX_WIDTH) + MAGENTA + "│" + RESET);

            // Rate row
            String rateLine = String.format("      %sRate   :%s  %s%s %s%s",
                    DIM, RESET, GREEN + BOLD, r.amount(), r.currency(), RESET);
            System.out.println(MAGENTA + "│" + RESET + padRawLine(rateLine, BOX_WIDTH) + MAGENTA + "│" + RESET);

            // Board row
            String boardLine = String.format("      %sBoard  :%s  %s", DIM, RESET, r.boardType());
            System.out.println(MAGENTA + "│" + RESET + padRawLine(boardLine, BOX_WIDTH) + MAGENTA + "│" + RESET);

            // Cancellation
            printCancellationPolicies(r.cancellationPolicies(), MAGENTA);

            if (i < rates.size() - 1) {
                System.out.println(MAGENTA + SEP_LIGHT + RESET);
            }
        }

        System.out.println(MAGENTA + MID_LIGHT + RESET);
        System.out.println(MAGENTA + "│" + RESET
                + GREEN + BOLD + String.format("  ✔  %d rate(s) found", rates.size()) + RESET
                + padRight("", BOX_WIDTH - visibleLength(String.format("  ✔  %d rate(s) found", rates.size())))
                + MAGENTA + "│" + RESET);
        System.out.println(MAGENTA + BOT_LIGHT + RESET);
        System.out.println();
    }

    // -----------------------------------------------------------------------
    // Errors / Info / Banner
    // -----------------------------------------------------------------------

    /**
     * Prints a clean, user-friendly error message. No stack traces.
     */
    public void printError(String message) {
        System.out.println();
        System.out.println("  " + RED + BOLD + "✖  ERROR" + RESET + "  " + message);
        System.out.println();
    }

    /**
     * Prints an informational / section heading.
     */
    public void printInfo(String message) {
        System.out.println("  " + CYAN + "▶" + RESET + "  " + message);
    }

    /**
     * Prints a prominent section banner.
     */
    public void printBanner(String title) {
        int innerWidth = BOX_WIDTH;
        String paddedTitle = "  " + title;
        System.out.println();
        System.out.println(BLUE + BOLD + TOP_BORDER + RESET);
        System.out.println(BLUE + BOLD + "║" + RESET
                + BOLD + WHITE + padRight(paddedTitle, innerWidth) + RESET
                + BLUE + BOLD + "║" + RESET);
        System.out.println(BLUE + BOLD + BOT_BORDER + RESET);
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private void printCancellationPolicies(List<CancellationPolicy> policies, String borderColor) {
        if (policies.isEmpty()) {
            String line = String.format("      %sCancel :%s  %sNo cancellation policy info%s",
                    DIM, RESET, DIM, RESET);
            System.out.println(borderColor + "│" + RESET + padRawLine(line, BOX_WIDTH) + borderColor + "│" + RESET);
            return;
        }
        String headerLine = String.format("      %sCancel :%s", DIM, RESET);
        System.out.println(borderColor + "│" + RESET + padRawLine(headerLine, BOX_WIDTH) + borderColor + "│" + RESET);
        for (CancellationPolicy p : policies) {
            String pLine = String.format("               %s%s%s — %s%s %s%s (%s)",
                    DIM, p.cancelTime(), RESET,
                    YELLOW, p.amount(), p.currency(), RESET,
                    p.type());
            System.out.println(borderColor + "│" + RESET + padRawLine(pLine, BOX_WIDTH) + borderColor + "│" + RESET);
        }
    }

    /** Builds an address string from individual fields. */
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

    /** Truncates a string to {@code maxLen} visible characters, appending "…" if cut. */
    private String truncate(String value, int maxLen) {
        if (value == null || value.isBlank()) return "N/A";
        return value.length() <= maxLen ? value : value.substring(0, maxLen - 1) + "…";
    }

    /** Pads a plain (no ANSI) string on the right to exactly {@code width} chars. */
    private String padRight(String s, int width) {
        if (s == null) s = "";
        int pad = width - s.length();
        return pad > 0 ? s + " ".repeat(pad) : s;
    }

    /**
     * Pads a line that may contain ANSI escape sequences so that the visible
     * content occupies exactly {@code width} characters.
     */
    private String padRawLine(String line, int width) {
        int visible = visibleLength(line);
        int pad = width - visible;
        return line + (pad > 0 ? " ".repeat(pad) : "");
    }

    /** Returns the number of visible (non-ANSI) characters in a string. */
    private int visibleLength(String s) {
        // Strip ANSI escape sequences: ESC [ ... m
        return s.replaceAll("\u001B\\[[;\\d]*m", "").length();
    }

    /** Pads a simple "│ content" line to the full box width (no inner border char). */
    private String pad(String lineWithBorder) {
        return lineWithBorder; // used for simple single-line boxes only
    }

    /** Renders a star-rating string as coloured star characters. */
    private String starsDisplay(String rating) {
        try {
            int stars = (int) Double.parseDouble(rating);
            String filled = YELLOW + "★".repeat(Math.min(stars, 5)) + RESET;
            String empty  = DIM    + "★".repeat(Math.max(0, 5 - stars)) + RESET;
            return filled + empty;
        } catch (NumberFormatException e) {
            return DIM + rating + RESET;
        }
    }
}
