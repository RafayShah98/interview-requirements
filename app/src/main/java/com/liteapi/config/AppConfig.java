package com.liteapi.config;

import com.liteapi.exception.ApiException;
import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Loads application configuration from (in order of precedence):
 * 1. A {@code .env} file in the working directory
 * 2. A real OS / process environment variable {@code LITEAPI_KEY}
 * 3. An {@code application.properties} file on the classpath
 *
 * <p>The API key is never hard-coded anywhere in the source.
 */
public class AppConfig {

    private static final Logger LOG = Logger.getLogger(AppConfig.class.getName());

    private static final String KEY_NAME = "LITEAPI_KEY";
    private static final String PROPERTIES_FILE = "application.properties";

    private final String apiKey;

    public AppConfig() {
        this.apiKey = resolveApiKey();
    }

    public String getApiKey() {
        return apiKey;
    }

    private String resolveApiKey() {
        // 1. Try .env file
        try {
            LOG.fine("Looking for API key in .env file...");
            Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
            String value = dotenv.get(KEY_NAME);
            if (isPresent(value)) {
                LOG.info("API key loaded from .env file");
                return value;
            }
            LOG.fine("API key not found in .env file");
        } catch (DotenvException e) {
            LOG.warning(".env file could not be parsed: " + e.getMessage());
        }

        // 2. Try OS environment variable
        LOG.fine("Looking for API key in environment variable " + KEY_NAME + "...");
        String envValue = System.getenv(KEY_NAME);
        if (isPresent(envValue)) {
            LOG.info("API key loaded from environment variable");
            return envValue;
        }
        LOG.fine("API key not found in environment variable");

        // 3. Try application.properties on classpath
        LOG.fine("Looking for API key in " + PROPERTIES_FILE + "...");
        try (InputStream is = AppConfig.class.getClassLoader()
                .getResourceAsStream(PROPERTIES_FILE)) {
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                String propValue = props.getProperty(KEY_NAME);
                if (isPresent(propValue)) {
                    LOG.info("API key loaded from " + PROPERTIES_FILE);
                    return propValue;
                }
                LOG.fine("API key not found in " + PROPERTIES_FILE);
            } else {
                LOG.fine(PROPERTIES_FILE + " not found on classpath");
            }
        } catch (IOException e) {
            LOG.warning("Failed to read " + PROPERTIES_FILE + ": " + e.getMessage());
        }

        throw new ApiException(
                "API key not found. Set " + KEY_NAME + " in a .env file, " +
                "environment variable, or application.properties.");
    }

    private boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }
}
