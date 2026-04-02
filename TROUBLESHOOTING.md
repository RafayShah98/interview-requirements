# FAQ & Troubleshooting Guide

## ❓ Frequently Asked Questions

### Q: Do I need to install Java separately?
**A:** You need Java 21+ installed on your system. Gradle will use your system Java. Check with:
```bash
java -version
```
If you don't have Java 21, download from: https://www.oracle.com/java/technologies/downloads/

### Q: Do I need to install Gradle separately?
**A:** No! This project uses the Gradle wrapper (`./gradlew` or `gradlew.bat`). It downloads and uses its own Gradle version automatically.

### Q: Can I run this on Linux/Mac?
**A:** Yes! Use `./gradlew` instead of `.\gradlew.bat`. All code is platform-independent.

### Q: How do I get a LiteAPI key?
**A:** 
1. Visit https://www.liteapi.travel/
2. Create a free account
3. Generate an API key from your dashboard
4. Copy the key into your `.env` file (or environment variable)

### Q: Can I hardcode my API key in the source?
**A:** No! That's a security risk. The application is designed to never accept hardcoded keys. Use `.env`, env vars, or properties files instead.

### Q: What dates are hardcoded?
**A:** Check-in: Nov 10, 2025 | Check-out: Nov 12, 2025
To change, edit `Main.java` lines 34-35:
```java
private static final String DEFAULT_CHECKIN  = "2025-11-10";
private static final String DEFAULT_CHECKOUT = "2025-11-12";
```

### Q: Can I change the currency or nationality?
**A:** Yes! Edit `Main.java` lines 36-37:
```java
private static final String DEFAULT_CURRENCY   = "USD";
private static final String DEFAULT_NATIONALITY = "US";
```

### Q: What country codes are supported?
**A:** Any 2-letter ISO country code (US, GB, FR, JP, DE, AU, etc.). The LiteAPI determines which are valid.

### Q: How do I search for multiple cities?
**A:** Run the app multiple times with different city arguments:
```bash
./gradlew run --args="New York, US"
# Later...
./gradlew run --args="London, GB"
```

### Q: Can I save the hotel/rate data?
**A:** Currently the app prints to console only. To save:
```bash
./gradlew run --args="New York, US" > output.txt 2>&1
```

---

## 🐛 Troubleshooting

### Issue: "API key not found"

**Symptom:**
```
✗ Error: API key not found. Set LITEAPI_KEY in a .env file, environment variable, or application.properties.
```

**Solutions:**
1. **Create `.env` file:**
   ```bash
   cp .env.example .env
   # Then edit .env and add your actual API key
   ```

2. **Set environment variable:**
   ```bash
   # Windows PowerShell
   $env:LITEAPI_KEY="your_actual_key_here"
   .\gradlew.bat run
   
   # Linux/Mac Bash
   export LITEAPI_KEY="your_actual_key_here"
   ./gradlew run
   ```

3. **Create properties file:**
   ```bash
   mkdir -p app/src/main/resources
   echo LITEAPI_KEY=your_actual_key_here > app/src/main/resources/application.properties
   ```

---

### Issue: "Authentication failed"

**Symptom:**
```
✗ Error: Authentication failed — check that your API key is correct.
```

**Causes & Solutions:**
1. **Invalid key:**
   - Generate a new key from https://www.liteapi.travel/
   - Make sure you copied the full key (no extra spaces)

2. **Key expired:**
   - Check your LiteAPI account for key expiration
   - Regenerate if needed

3. **Environment variable not set:**
   ```bash
   # Verify it's set:
   echo $env:LITEAPI_KEY  # PowerShell
   echo $LITEAPI_KEY      # Bash
   ```

---

### Issue: "Build failed" or "Cannot find symbol"

**Symptom:**
```
error: cannot find symbol
  symbol: class Hotel
```

**Solutions:**
1. **Clean and rebuild:**
   ```bash
   ./gradlew clean build
   ```

2. **Check Java version:**
   ```bash
   javac -version
   # Must be 21 or higher
   ```

3. **Update IDE:**
   - If using IntelliJ/Eclipse, reload gradle project
   - Right-click project → Reload in IntelliJ

---

### Issue: Tests fail or hang

**Symptom:**
```
Tests hang indefinitely
```

**Causes & Solutions:**
1. **No Internet connection:**
   - Tests don't need network, but dependencies might
   - Check your internet

2. **Gradle daemon issue:**
   ```bash
   ./gradlew --stop
   ./gradlew test
   ```

3. **Java version mismatch:**
   ```bash
   ./gradlew test -x test  # Skip tests
   javac -version  # Check version
   ```

---

### Issue: "Rate limit exceeded"

**Symptom:**
```
[Rate limit] Too many requests. Retrying in 2 second(s)... (attempt 1/3)
[Rate limit] Too many requests. Retrying in 4 second(s)... (attempt 2/3)
[Rate limit] Too many requests. Retrying in 8 second(s)... (attempt 3/3)
✗ Error: Rate limit exceeded. Please wait and try again later.
```

**What's happening:**
- You've made too many API requests in a short time
- The app automatically retries 3 times with backoff (2s, 4s, 8s)
- If still rate limited, you must wait before trying again

**Solutions:**
1. **Wait a while:** The API rate limit resets (usually hourly)
2. **Less frequent requests:** Wait between running the app
3. **Contact LiteAPI:** If rate limit is too restrictive, ask them to increase it

---

### Issue: "Bad request"

**Symptom:**
```
✗ Error: Bad request — missing or invalid parameters. [details]
```

**Causes & Solutions:**
1. **Invalid country code:**
   - Must be 2-letter ISO code (e.g., `US`, not `USA`)
   - Example: `./gradlew run --args="US"` ✓
   - Example: `./gradlew run --args="USA"` ✗

2. **Date format wrong:**
   - App uses YYYY-MM-DD format internally
   - Dates are hardcoded, can't be changed via CLI

3. **City name issue:**
   - Try simplifying: `Paris` instead of `Paris, France`
   - Use proper ISO country code: `./gradlew run --args="Paris, FR"`

---

### Issue: "Connection timeout"

**Symptom:**
```
✗ Error: Network error: Connection timeout
```

**Causes & Solutions:**
1. **No Internet connection:**
   - Check your network
   - Try: `ping api.liteapi.travel`

2. **VPN/Proxy issues:**
   - Try disabling VPN
   - Check proxy settings

3. **Firewall blocking:**
   - LiteAPI uses HTTPS on port 443
   - Check firewall allows outbound HTTPS

4. **API server down:**
   - Check LiteAPI status: https://www.liteapi.travel/
   - Try again in a few minutes

---

### Issue: "No hotels found"

**Symptom:**
```
  No hotels found for the given location.
```

**Causes & Solutions:**
1. **Invalid location:**
   - Try a major city: `New York, US`, `London, GB`
   - Minor locations might not have data

2. **Dates too far in future/past:**
   - Check the hardcoded dates in `Main.java`
   - Edit if needed (lines 34-35)

3. **LiteAPI data:**
   - Not all locations have hotel data
   - Try a different city

---

### Issue: "No rates available"

**Symptom:**
```
  No rates available for this hotel and the selected dates.
  Tip: Try different check-in / check-out dates.
```

**Causes & Solutions:**
1. **Hotel fully booked:**
   - Try different dates by editing `Main.java` lines 34-35

2. **Hotel closed on those dates:**
   - Try a different hotel or dates

3. **Hotel ID invalid:**
   - Make sure you selected a valid hotel from the list

---

### Issue: "Gradle build cache issues"

**Symptom:**
```
Configuration cache problem
```

**Solutions:**
```bash
# Clear all gradle caches:
./gradlew --stop
rm -rf .gradle
./gradlew clean build
```

---

### Issue: "IDE can't find classes"

**Symptom (IntelliJ/Eclipse):**
```
Cannot resolve symbol 'Hotel'
```

**Solutions:**
1. **Reload Gradle:**
   - Right-click project → Gradle → Refresh Gradle Dependencies

2. **Invalidate caches (IntelliJ):**
   - File → Invalidate Caches → Invalidate and Restart

3. **Mark directories as sources:**
   - Right-click `src/main/java` → Mark as Sources Root
   - Right-click `src/test/java` → Mark as Test Sources Root

---

## 📊 Performance Tips

1. **First run is slow:** Gradle downloads dependencies. Subsequent runs are faster.
2. **Keep Gradle daemon running:** First task takes 5-10s, later ones take 1-2s.
3. **Use `--args` for faster testing:** Avoid interactive prompts when scripting.
4. **Batch API calls:** Each location search costs one API call.

---

## 🔍 Debugging

### Enable verbose logging:
```bash
./gradlew run --debug 2>&1 | grep -i error
```

### Print what's being loaded:
1. Edit `AppConfig.java` (add print statements)
2. Run: `./gradlew run`
3. See which config source is being used

### Inspect HTTP requests:
1. Add system property: `-Djava.net.debug=all`
2. Or modify `LiteApiClient.java` to log requests/responses

### Test a specific parser:
```bash
./gradlew test --tests "*HotelResponseParserTest" -i
```

---

## ✅ Verification Checklist

Before reporting an issue, verify:

- [ ] Java 21+ installed: `java -version`
- [ ] `.env` file exists with valid API key
- [ ] Can ping LiteAPI: `ping api.liteapi.travel`
- [ ] Gradle build succeeds: `./gradlew clean build`
- [ ] Tests pass: `./gradlew test`
- [ ] Internet connection stable
- [ ] Using valid country codes (2-letter ISO)
- [ ] Using valid API key (test with https://www.liteapi.travel/)

---

## 🆘 Getting Help

If you've tried all troubleshooting steps:

1. **Check the README:** `README.md` has setup instructions
2. **Review ARCHITECTURE.md:** Design decisions explained
3. **Check test files:** Tests show expected behavior
4. **LiteAPI support:** Contact https://www.liteapi.travel/ for API issues

---

## 📝 Common Mistakes

❌ **Wrong:**
```bash
./gradlew run --args="USA"  # 3-letter code
./gradlew run --args="new york"  # No country code
export LITEAPI_KEY=my key with spaces  # Spaces in key!
java -jar app.jar  # Won't work, need gradlew
```

✅ **Right:**
```bash
./gradlew run --args="New York, US"  # Proper format
./gradlew run --args="US"  # Country code alone
export LITEAPI_KEY="my_key_without_spaces"  # Quoted
./gradlew run  # Use gradlew wrapper
```


