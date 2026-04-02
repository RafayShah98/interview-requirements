package com.liteapi.config;

import com.liteapi.exception.ApiException;
import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads application configuration from (in order of precedence):
 * 1. A {@code .env} file in the working directory
 * 2. A real OS / process environment variable {@code LITEAPI_KEY}
 * 3. An {@code application.properties} file on the classpath
 *
 * <p>The API key is never hard-coded anywhere in the source.
 */
public class AppConfig {

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
            Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
            String value = dotenv.get(KEY_NAME);
            if (isPresent(value)) {
                return value;
            }
        } catch (DotenvException ignored) {
            // .env file malformed — fall through to next source
        }

        // 2. Try OS environment variable
        String envValue = System.getenv(KEY_NAME);
        if (isPresent(envValue)) {
            return envValue;
        }

        // 3. Try application.properties on classpath
        try (InputStream is = AppConfig.class.getClassLoader()
                .getResourceAsStream(PROPERTIES_FILE)) {
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                String propValue = props.getProperty(KEY_NAME);
                if (isPresent(propValue)) {
                    return propValue;
                }
            }
        } catch (IOException ignored) {
            // fall through
        }

        throw new ApiException(
                "API key not found. Set " + KEY_NAME + " in a .env file, " +
                "environment variable, or application.properties.");
    }

    private boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }
}
