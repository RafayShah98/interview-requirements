# 🚀 Quick Start Guide

## 1️⃣ Get Your API Key
- Go to https://www.liteapi.travel/
- Sign up for a free account
- Generate an API key

## 2️⃣ Setup (Choose ONE method)

### Method A: Using .env file (Recommended)
```bash
cd interview-requirements
cp .env.example .env
# Edit .env and replace 'your_api_key_here' with your actual key
```

### Method B: Using Environment Variable
```bash
# Windows PowerShell
$env:LITEAPI_KEY="your_api_key_here"
cd interview-requirements
.\gradlew.bat run

# Linux/Mac
export LITEAPI_KEY="your_api_key_here"
cd interview-requirements
./gradlew run
```

### Method C: Using application.properties
```bash
mkdir -p app/src/main/resources
echo "LITEAPI_KEY=your_api_key_here" > app/src/main/resources/application.properties
```

## 3️⃣ Run the Application

### Interactive Mode (prompts for city)
```bash
./gradlew run
```

Then enter something like:
- `New York, US`
- `London, GB`
- `Tokyo, JP`
- Or just `US` for country-only search

### Direct Mode (pass city as argument)
```bash
./gradlew run --args="New York, US"
./gradlew run --args="London, GB"
./gradlew run --args="Paris"
```

### After hotels load:
- Select a hotel by number (e.g., `1`, `2`)
- OR paste a Hotel ID directly (e.g., `lp24373`)

The app will then fetch and display available room rates!

## 4️⃣ Run Tests
```bash
./gradlew test
```

## ❓ Troubleshooting

### Error: "API key not found"
- Make sure `.env` file exists in project root: `interview-requirements/.env`
- Or set environment variable: `$env:LITEAPI_KEY="your_key"`
- Or create: `app/src/main/resources/application.properties`

### Error: "Authentication failed"
- Your API key is invalid or expired
- Generate a new one from https://www.liteapi.travel/

### Slow response / Rate limited
- The app handles 429 errors with automatic retry
- Be patient, it will retry up to 3 times with increasing backoff

### Port/Connection errors
- Check your internet connection
- API endpoint: https://api.liteapi.travel/v3.0

## 📋 What Happens When You Run It

1. **Flow 1**: Loads and displays hotels in your chosen location
2. **Flow 2**: You pick a hotel, app fetches room rates for Nov 10-12, 2025
3. **Output**: Beautiful formatted console display with hotel details and cancellation policies

## 💡 Tips

- City names are case-insensitive
- Country codes must be 2-letter ISO codes (US, GB, FR, JP, etc.)
- You can mix formats: `"New York, US"` or just `"New York"` (defaults to US)
- Dates are hardcoded to Nov 10-12, 2025 (you can edit in `Main.java` if needed)

