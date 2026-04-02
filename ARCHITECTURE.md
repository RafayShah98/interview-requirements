# Technical Architecture Document

## System Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                      CLI User                                    │
│              (Terminal / Command Line)                           │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         │ User Input (city, hotel number)
                         │
┌────────────────────────▼────────────────────────────────────────┐
│                  Main.java                                       │
│          ┌─────────────────────────────────────┐                │
│          │ Flow 1: Search Hotels               │                │
│          │ Flow 2: Get Rates                   │                │
│          │ Flow 3: Handle Errors               │                │
│          └──┬──────────────────────────┬───────┘                │
└─────────────┼──────────────────────────┼────────────────────────┘
              │                          │
    ┌─────────▼──────────────┐  ┌───────▼──────────────┐
    │  AppConfig             │  │   LiteApiClient      │
    │  ─────────────         │  │   ──────────────     │
    │  • Load API key        │  │  • Build requests    │
    │  • From 3 sources      │  │  • Send HTTP calls   │
    │  • Validate key        │  │  • Retry on 429      │
    │  • Fail if missing     │  │  • Handle errors     │
    │                        │  │  • Return JSON       │
    └────────────────────────┘  └───────┬──────────────┘
                                        │
                    ┌───────────────────┼───────────────────┐
                    │                   │                   │
          ┌─────────▼────────────┐ ┌────▼──────────┐ ┌────▼─────────┐
          │ HotelResponseParser  │ │ RateResponseP │ │ Presenter    │
          │ ────────────────────  │ │ ─────────────  │ │ ──────────   │
          │ • Parse hotel JSON   │ │ • Parse rates │ │ • Format     │
          │ • Extract data       │ │ • Extract     │ │   output     │
          │ • Handle nulls       │ │   policies    │ │ • Print      │
          │ • Return Hotel list  │ │ • Return list │ │   tables     │
          └──────────────────────┘ │                │ │ • Colors     │
                                   └────────────────┘ └──────────────┘
                    │
        ┌───────────┼───────────┐
        │           │           │
     ┌──▼───┐ ┌────▼──┐ ┌─────▼──────┐
     │Hotel │ │Rate   │ │Cancellation│
     │Record│ │Record │ │Policy      │
     └──────┘ └───────┘ └────────────┘

                    │
    ┌───────────────▼───────────────┐
    │    LiteAPI (REST)             │
    │  https://api.liteapi.travel   │
    │  • GET /data/hotels           │
    │  • POST /hotels/rates         │
    └───────────────────────────────┘
```

## Data Flow Diagram

### Flow 1: Hotel Search
```
User Input (city, country)
         │
         ▼
Main.resolveLocation()        Parse and validate input
         │
         ▼
LiteApiClient.searchHotels()  Build GET request with auth header
         │
         ▼
LiteAPI /data/hotels          Return JSON with hotel list
         │
         ▼
HotelResponseParser.parse()   Extract hotel data from JSON
         │
         ▼
List<Hotel>                   Hotel record objects
         │
         ▼
Presenter.printHotels()       Display formatted table
         │
         ▼
Console Output
```

### Flow 2: Hotel Rates
```
User Input (hotel number or ID)
         │
         ▼
Main.resolveHotelId()         Get selected hotel ID
         │
         ▼
RateRequest Creation          Package hotel ID + check-in/out dates
         │
         ▼
LiteApiClient.getHotelRates() Build POST request + JSON body
         │
         ▼
LiteAPI /hotels/rates         Return JSON with room rates
         │
         ▼
RateResponseParser.parse()    Extract rates + policies from JSON
         │
         ▼
List<HotelRate>               Rate record objects with policies
         │
         ▼
Presenter.printRates()        Display formatted rate table
         │
         ▼
Console Output
```

## Class Hierarchy

```
com.liteapi
├── Main.java                          ← CLI entry point
│   ├── resolveLocation()              Parse location input
│   ├── resolveHotelId()               Get hotel selection
│   └── parseLocation()                Static: location parser (testable)
│
├── config/
│   └── AppConfig.java                 Load & validate config
│       └── resolveApiKey()            3-source key resolver
│
├── http/
│   └── LiteApiClient.java             HTTP client + retry logic
│       ├── searchHotels()             GET endpoint
│       ├── getHotelRates()            POST endpoint
│       ├── executeWithRetry()         Retry with backoff
│       ├── handleErrorStatus()        Error mapping
│       └── extractMessage()           Parse error JSON
│
├── model/                             Java Records (immutable)
│   ├── Hotel.java                     Hotel data
│   ├── HotelRate.java                 Rate data
│   ├── RateRequest.java               POST body
│   └── CancellationPolicy.java        Policy data
│
├── parser/                            JSON parsing layer
│   ├── HotelResponseParser.java       Hotel JSON → objects
│   │   ├── parse()                    Entry point
│   │   ├── parseHotel()               Single hotel
│   │   ├── text()                     Safe field access
│   │   └── addressField()             Flexible address parsing
│   │
│   └── RateResponseParser.java        Rate JSON → objects
│       ├── parse()                    Entry point
│       ├── parseHotelNode()           Single hotel node
│       ├── parseRoom()                Single room
│       ├── parseRate()                Single rate
│       ├── extractAmount()            Handle amount variants
│       ├── extractCurrency()          Handle currency variants
│       └── parseCancellationPolicies() Extract policies
│
├── presenter/
│   └── Presenter.java                 Console output formatting
│       ├── printHotels()              Display hotel list
│       ├── printRates()               Display rate list
│       ├── printError()               Error messages
│       ├── printBanner()              Section headers
│       ├── buildAddress()             Format address
│       └── truncate()                 String truncation with ellipsis
│
└── exception/
    └── ApiException.java              Custom exception
        ├── statusCode                 HTTP status
        └── message                    User-friendly message
```

## Error Handling Strategy

```
HTTP Status → Exception Type → User Message
────────────────────────────────────────────

400 Bad Request
  ↓
ApiException(400, "Bad request — missing or invalid parameters...")
  ↓
Caught in Main → Presenter.printError()
  ↓
Console: "✗ Error: Bad request — missing or invalid parameters..."

401 Unauthorized
  ↓
ApiException(401, "Authentication failed — check that your API key...")
  ↓
Caught in Main → Presenter.printError()
  ↓
Console: "✗ Error: Authentication failed — check that your API key..."

429 Too Many Requests
  ↓
LiteApiClient.executeWithRetry() catches it
  ↓
Sleep 2sec, retry → Sleep 4sec, retry → Sleep 8sec, retry → FAIL
  ↓
ApiException(429, "Rate limit exceeded. Please wait and try again...")
  ↓
Console: "✗ Error: Rate limit exceeded..."

Network Error (IOException)
  ↓
ApiException("Network error: [details]", cause)
  ↓
Console: "✗ Error: Network error: [details]"

JSON Parse Error
  ↓
ApiException("Failed to parse [type] response: [details]", cause)
  ↓
Console: "✗ Error: Failed to parse [type] response..."
```

## Retry Logic (Exponential Backoff)

```
Request sent to API
         │
         ▼
Response: 200/201 OK?  ──YES──► Return body
         │
         NO
         │
         ▼
Response: 429 Rate Limited?
         │
    YES  │  NO
         │
    ┌────┴────┐
    │          │
    ▼          ▼
Attempt  Check
Count?   Error
  │      Status
Retry    │
3x?  Handle
 │   Error
[1] [2] [3]
2s  4s  8s
backoff

Sequence:
Attempt 1: sleep 2s → Retry GET/POST → Response
Attempt 2: sleep 4s → Retry GET/POST → Response
Attempt 3: sleep 8s → Retry GET/POST → Response
Attempt 4: Fail with "Rate limit exceeded"
```

## Configuration Resolution Order

```
Try 1: Load .env file
  ├─ Parse .env in project root
  ├─ Read LITEAPI_KEY=value
  └─ If found → Use this key
       │
       ▼ (if not found)

Try 2: OS Environment Variable
  ├─ System.getenv("LITEAPI_KEY")
  ├─ Check shell/system env vars
  └─ If found → Use this key
       │
       ▼ (if not found)

Try 3: application.properties on classpath
  ├─ Load app/src/main/resources/application.properties
  ├─ Read LITEAPI_KEY=value
  └─ If found → Use this key
       │
       ▼ (if not found)

Fail
  ├─ Throw ApiException
  └─ Exit with message: "API key not found. Set LITEAPI_KEY in..."
```

## JSON Parsing Strategy

### Hotel Response Parser
- Uses Jackson's tree model (not binding)
- Handles optional/missing fields with fallback values
- Tries multiple field name variants (e.g., "street1", "street")
- Returns "N/A" for missing data instead of null

### Rate Response Parser
- Handles both array and object data structures
- Extracts nested retail rates flexibly
- Parses cancellation policies if present (optional)
- Flattens room × rate matrix (each rate becomes separate object)

### Example: Flexible Amount Extraction
```
Try retailRate.total[0].amount
  ├─ Is total an array?
  │   YES → Get first element, read "amount" → Use
  │   NO  → Try next...
  │
Try retailRate.total.amount
  ├─ Is total an object?
  │   YES → Read "amount" → Use
  │   NO  → Try next...
  │
Fallback: rate.price
  ├─ Try alternative field → Use
  │   NO → Return "N/A"
```

## Testing Strategy

### Unit Test Coverage
- **MainTest.java**: Location parsing edge cases (5 tests)
- **HotelResponseParserTest.java**: Hotel parsing robustness (9 tests)
- **RateResponseParserTest.java**: Rate parsing with variants (7 tests)

### Test Categories
1. **Happy Path**: Valid complete data
2. **Missing Fields**: Null or absent optional fields
3. **Invalid Input**: Malformed JSON, null strings
4. **Edge Cases**: Empty arrays, blank strings
5. **Error Scenarios**: Exception throwing for invalid JSON

### No Network Tests
- All tests use inline JSON strings
- No mocking required (no external dependencies)
- No flaky tests dependent on network availability

## Performance Considerations

| Operation | Timeout | Notes |
|-----------|---------|-------|
| HTTP Connect | 15s | Reasonable network timeout |
| HTTP Request | 30s | Allow for API processing |
| JSON Parsing | Instant | Jackson is very fast |
| Retries | 2s+4s+8s = 14s max | Exponential backoff |
| User Input | Blocking | No timeout on stdin |

## Security Architecture

```
API Key Storage
  ├─ Never in source code (all hardcoded checks pass)
  ├─ Load from external sources only
  ├─ .env (recommended, .gitignored)
  ├─ Env var (secure, shell-managed)
  └─ properties file (.gitignored)

API Key Usage
  ├─ Attached as X-API-Key header on every request
  ├─ HTTPS only (https://api.liteapi.travel)
  ├─ No query string, no request body exposure
  └─ Timeout after 30s

Error Messages
  ├─ Never print stack traces to user
  ├─ Never reveal API response details
  ├─ Show human-friendly messages only
  └─ Log location: stdout (user controls output)

Input Validation
  ├─ Country codes: 2-letter ISO check
  ├─ City names: URL encoded before API call
  ├─ Hotel IDs: Passed through (API validates)
  └─ Dates: Fixed by application logic
```

## Extensibility Points

### Easy to Add
1. **Interactive date selection**: Modify `Main.java` lines 34-36
2. **Different room availability**: Add new endpoint in `LiteApiClient`
3. **New output formats**: Extend `Presenter.java`
4. **Additional parsing logic**: Add methods to parsers
5. **Configuration sources**: Extend `AppConfig.resolveApiKey()`

### Would Require Refactoring
1. **Async/concurrent requests**: Would need reactive stream library
2. **Database persistence**: Would need ORM integration
3. **Web UI**: Would need Spring Boot or similar
4. **GraphQL API**: Would need schema mapping

## Build & Deployment

```
Source Code
    │
    ▼
./gradlew build
    ├─ Compile Java 21
    ├─ Resolve dependencies (Maven Central)
    ├─ Run unit tests
    ├─ Package as JAR
    └─ Create distribution (ZIP, TAR)
    │
    ▼
Artifact
    ├─ FAT JAR: app/build/libs/app.jar
    ├─ Scripts: app/build/scripts/app (Unix), app.bat (Windows)
    └─ ZIP: build/distributions/app.zip
    │
    ▼
Deployment
    ├─ Unix: ./app (via wrapper script)
    ├─ Windows: app.bat (via wrapper script)
    └─ Or: java -jar app.jar --args="New York"
```

## Dependencies Justification

| Library | Why Not Alternatives? |
|---------|----------------------|
| Jackson | Industry standard; flexible tree API for unknown JSON structures |
| dotenv-java | Lightweight; follows .env convention used everywhere |
| JUnit 5 | Modern; integrates with Gradle natively |
| Java HttpClient | Zero dependencies; built into JDK 11+ |
| Java Records | Zero dependencies; modern, clean, immutable data |

