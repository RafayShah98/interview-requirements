package com.liteapi.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.liteapi.exception.ApiException;
import com.liteapi.model.RateRequest;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Low-level HTTP layer responsible for all communication with the LiteAPI.
 *
 * <ul>
 *   <li>Attaches the {@code X-API-Key} header to every request.</li>
 *   <li>Translates HTTP error status codes into typed {@link ApiException}s.</li>
 *   <li>Implements exponential-backoff retry for HTTP 429 (rate-limit).</li>
 * </ul>
 */
public class LiteApiClient {

    private static final Logger LOG = Logger.getLogger(LiteApiClient.class.getName());

    private static final String BASE_URL = "https://api.liteapi.travel/v3.0";
    private static final String API_KEY_HEADER = "X-API-Key";
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 2_000;
    private static final int MAX_LOG_BODY_LENGTH = 500;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    public LiteApiClient(String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Searches hotels by country code and optional city name.
     *
     * @param countryCode ISO-2 country code (e.g. {@code "US"})
     * @param cityName    optional city name (e.g. {@code "New York"}); pass {@code null} to omit
     * @return raw JSON response body string
     */
    public String searchHotels(String countryCode, String cityName) {
        StringBuilder url = new StringBuilder(BASE_URL)
                .append("/data/hotels?countryCode=")
                .append(URLEncoder.encode(countryCode.trim(), StandardCharsets.UTF_8));

        if (cityName != null && !cityName.isBlank()) {
            url.append("&cityName=")
               .append(URLEncoder.encode(cityName.trim(), StandardCharsets.UTF_8));
        }

        LOG.info("GET " + url);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url.toString()))
                .header(API_KEY_HEADER, apiKey)
                .header("Accept", "application/json")
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();

        return executeWithRetry(request);
    }

    /**
     * Fetches room rates for the given hotel and stay details.
     *
     * @param rateRequest fully populated request body
     * @return raw JSON response body string
     */
    public String getHotelRates(RateRequest rateRequest) {
        String body;
        try {
            body = objectMapper.writeValueAsString(rateRequest);
        } catch (IOException e) {
            throw new ApiException("Failed to serialise rate request: " + e.getMessage(), e);
        }

        String ratesUrl = BASE_URL + "/hotels/rates";
        LOG.info("POST " + ratesUrl);
        LOG.fine("Request body: " + body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ratesUrl))
                .header(API_KEY_HEADER, apiKey)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(30))
                .build();

        return executeWithRetry(request);
    }

    /**
     * Executes the request, retrying on HTTP 429 with exponential back-off.
     */
    private String executeWithRetry(HttpRequest request) {
        long backoffMs = INITIAL_BACKOFF_MS;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            LOG.fine("Attempt " + attempt + "/" + MAX_RETRIES + " — " + request.method() + " " + request.uri());
            HttpResponse<String> response = send(request);
            int status = response.statusCode();
            LOG.fine("HTTP " + status + " received");

            if (status == 200 || status == 201) {
                LOG.info("HTTP " + status + " OK — " + request.method() + " " + request.uri());
                LOG.fine("Response body (" + response.body().length() + " bytes): "
                        + truncateBody(response.body()));
                return response.body();
            }

            if (status == 429) {
                if (attempt == MAX_RETRIES) {
                    LOG.warning("HTTP 429 — rate limit exceeded after " + attempt + " attempt(s)");
                    throw new ApiException(429,
                            "Rate limit exceeded. Please wait and try again later.");
                }
                LOG.warning("HTTP 429 — rate limit hit, retrying in " + (backoffMs / 1000)
                        + "s (attempt " + attempt + "/" + MAX_RETRIES + ")");
                System.out.printf(
                        "  [Rate limit] Too many requests. Retrying in %d second(s)... (attempt %d/%d)%n",
                        backoffMs / 1000, attempt, MAX_RETRIES);
                sleep(backoffMs);
                backoffMs *= 2; // exponential back-off
                continue;
            }

            LOG.warning("HTTP " + status + " error — " + request.method() + " " + request.uri()
                    + " — body: " + truncateBody(response.body()));
            handleErrorStatus(status, request.uri().toString(), response.body());
        }

        // Unreachable, but satisfies the compiler
        throw new ApiException("Unexpected error executing request.");
    }

    /**
     * Maps HTTP error status codes to meaningful {@link ApiException}s.
     */
    private void handleErrorStatus(int status, String url, String body) {
        String apiMessage = extractMessage(body);
        switch (status) {
            case 400 -> throw new ApiException(400,
                    "Bad request — missing or invalid parameters."
                    + (apiMessage.isBlank() ? "" : " API says: " + apiMessage));
            case 401 -> throw new ApiException(401,
                    "Authentication failed — check that your API key is correct.");
            case 403 -> throw new ApiException(403,
                    "Access forbidden — your API key does not have permission for this resource.");
            case 404 -> throw new ApiException(404,
                    "Resource not found — the endpoint does not exist: " + url);
            case 500 -> throw new ApiException(500,
                    "Server error — LiteAPI is experiencing issues. Please try again later."
                    + (apiMessage.isBlank() ? "" : " API says: " + apiMessage));
            default  -> throw new ApiException(status,
                    "Unexpected response from LiteAPI (HTTP " + status + ") for " + url + "."
                    + (apiMessage.isBlank() ? "" : " API says: " + apiMessage));
        }
    }

    /**
     * Attempts to pull a human-readable message from an error response body.
     */
    private String extractMessage(String body) {
        if (body == null || body.isBlank()) return "";
        try {
            var node = objectMapper.readTree(body);
            for (String field : new String[]{"message", "error", "detail"}) {
                var msgNode = node.path(field);
                if (!msgNode.isMissingNode() && !msgNode.isNull()) {
                    return msgNode.asText();
                }
            }
        } catch (IOException ignored) {
            // body is not JSON — return raw (trimmed) text
        }
        int limit = Math.min(body.length(), 200);
        return body.substring(0, limit);
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Network error sending " + request.method() + " " + request.uri(), e);
            throw new ApiException("Network error: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.log(Level.SEVERE, "Request interrupted: " + request.method() + " " + request.uri(), e);
            throw new ApiException("Request interrupted.", e);
        }
    }

    private String truncateBody(String body) {
        if (body == null) return "(null)";
        int limit = Math.min(body.length(), MAX_LOG_BODY_LENGTH);
        return body.substring(0, limit) + (body.length() > MAX_LOG_BODY_LENGTH ? "... [truncated]" : "");
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
