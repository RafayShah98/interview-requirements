# Hotel Search CLI

A command-line Java 21 application that integrates with the **[LiteAPI](https://www.liteapi.travel/)** Hotel Search API.  
It authenticates, queries hotel data, and presents results in a clean, readable format.

---

## Features

| Flow | What it does |
|------|-------------|
| **Flow 1 – Search Hotels** | Accept a city / country code, call the LiteAPI hotel-search endpoint, and display hotel name, ID, address, and star rating. |
| **Flow 2 – Hotel Rates** | Accept a Hotel ID from Flow 1, POST to the rates endpoint, and display room name, rate, currency, board type, and cancellation policy. |
| **Flow 3 – Error Handling** | 401 (invalid key), 400 (bad request), 429 (rate limit with exponential-backoff retry) are all handled with clean user messages — no raw stack traces. |

**Bonus features included**
- ✅ Unit tests for parsers and location helper (`./gradlew test`)
- ✅ Exponential back-off retry on HTTP 429
- ✅ City name accepted as a CLI argument
- ✅ Pretty-printed console output

---

## Prerequisites

| Tool | Version |
|------|---------|
| Java | 21+ |
| Gradle | bundled via `./gradlew` (no install needed) |

---

## Setup (under 5 minutes)

### 1 — Clone the repository

```bash
git clone https://github.com/RafayShah98/interview-requirements.git
cd interview-requirements
```

### 2 — Add your API key

Copy the example env file and fill in your key:

```bash
cp .env.example .env
```

Open `.env` and set your key:

```
LITEAPI_KEY=your_api_key_here
```

> **Alternatives**  
> • Export an environment variable: `export LITEAPI_KEY=your_api_key_here`  
> • Create `app/src/main/resources/application.properties` containing `LITEAPI_KEY=your_api_key_here`

The `.env` file and `application.properties` are listed in `.gitignore` and will never be committed.

---

## Running the Application

### Interactive mode (prompts for city)

```bash
./gradlew run
```

### Pass city / country as a CLI argument

```bash
./gradlew run --args="New York, US"
./gradlew run --args="US"
./gradlew run --args="London, GB"
```

When prompted for a hotel, enter the row number or paste a Hotel ID directly.

---

## Running the Tests

```bash
./gradlew test
```

Test report is generated at `app/build/reports/tests/test/index.html`.

---

## Project Structure

```
.
├── app/
│   └── src/
│       ├── main/java/com/liteapi/
│       │   ├── Main.java                  # CLI entry point
│       │   ├── config/
│       │   │   └── AppConfig.java         # Loads API key from .env / env var / properties
│       │   ├── http/
│       │   │   └── LiteApiClient.java     # HTTP layer (Java HttpClient, retry logic)
│       │   ├── model/
│       │   │   ├── Hotel.java             # Hotel record
│       │   │   ├── HotelRate.java         # Rate record
│       │   │   ├── RateRequest.java       # POST body model
│       │   │   └── CancellationPolicy.java
│       │   ├── parser/
│       │   │   ├── HotelResponseParser.java  # Jackson-based JSON parser
│       │   │   └── RateResponseParser.java
│       │   └── presenter/
│       │       └── Presenter.java         # Formatted console output
│       └── test/java/com/liteapi/
│           ├── MainTest.java
│           └── parser/
│               ├── HotelResponseParserTest.java
│               └── RateResponseParserTest.java
├── .env.example                           # Template — copy to .env
├── .gitignore                             # Excludes .env, build artefacts, IDE files
└── gradle/libs.versions.toml             # Dependency version catalogue
```

---

## Configuration

The API key resolution order is:

1. `.env` file in the project root (dotenv-java)
2. OS environment variable `LITEAPI_KEY`
3. `app/src/main/resources/application.properties`

If none of these are found the application exits with a clear message.

---

## Sample Console Output

```
──────────────────────────────────────────────────────────────
  FLOW 1 — HOTEL SEARCH
──────────────────────────────────────────────────────────────
  → Using location from command-line argument: New York, US
  → Searching hotels in New York, US ...

──────────────────────────────────────────────────────────────
  #     Hotel ID        Name                           Stars
──────────────────────────────────────────────────────────────
  1     lp24373         The Manhattan Hotel            4
              123 5th Avenue, New York, US
  2     lp24374         Times Square Inn               3
              200 W 44th St, New York, US
──────────────────────────────────────────────────────────────
  Total: 2 hotel(s)

──────────────────────────────────────────────────────────────
  FLOW 2 — HOTEL RATES
──────────────────────────────────────────────────────────────
  Enter hotel number (1-2) or paste a Hotel ID: 1
  → Fetching rates for hotel lp24373  (check-in: 2025-11-10, check-out: 2025-11-12) ...

──────────────────────────────────────────────────────────────
  AVAILABLE RATES
──────────────────────────────────────────────────────────────
  [1] Deluxe King Room
      Rate    : 250.0 USD
      Board   : RO
      Cancel  :
                2025-11-08T00:00:00 — 100 USD (percentage)
────────────────────────────────────────────────────────────────
  Total: 1 rate(s)

Done.
```

---

## Technology Choices

| Area | Choice | Reason |
|------|--------|--------|
| HTTP client | `java.net.http.HttpClient` (JDK built-in) | Zero extra dependency; full async/sync support |
| JSON parsing | Jackson Databind | Industry-standard; flexible tree-model API |
| `.env` loading | dotenv-java | Lightweight; standard `.env` convention |
| Build tool | Gradle (Groovy DSL) | Required by spec |
| Testing | JUnit Jupiter 6 | Modern; integrates with Gradle natively |
