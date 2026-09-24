# NexusSphere — Local Setup Guide

## Prerequisites

| Tool | Version | Purpose |
|---|---|---|
| Android Studio | Hedgehog 2023.1+ (or Ladybug 2024.2+) | Primary IDE for KMP |
| JDK | 17+ | Kotlin compilation |
| Android SDK | API 26 (minSdk) – API 35 (compileSdk) | Android target |
| Python | 3.11+ | Flask server |

iOS requires a macOS machine — deferred to Phase 9.

---

## 1. Clone & open

```bash
git clone https://github.com/creova-gif/nexus-sphere.git
cd nexus-sphere
```

Open the project root in Android Studio. It will detect the `settings.gradle.kts` and sync automatically.

---

## 2. First Gradle sync

Android Studio will download:
- Gradle 8.9 (defined in `gradle/wrapper/gradle-wrapper.properties`)
- All Kotlin Multiplatform dependencies from `gradle/libs.versions.toml`

If the sync fails on JDK version, go to **File → Project Structure → SDK Location** and set JDK 17.

---

## 3. Run on Android

1. Connect an Android device (API 26+) or start an emulator
2. Select the `composeApp` run configuration
3. Click Run ▶

Or from terminal:
```bash
./gradlew :composeApp:installDebug
```

---

## 4. Run on Desktop (JVM)

```bash
./gradlew :composeApp:run
```

This launches the desktop window (1280×800). No device required.

---

## 5. Run Kotlin unit tests

```bash
./gradlew :shared:allTests
```

Tests cover: KellyCriterion, SharpeRatio, HistoricalVar, SignalScore, OrderGuard.

---

## 6. Run the Flask server (for live broker data)

```bash
cd /path/to/nexus-sphere
python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt    # or: pip install flask snaptrade-python-sdk anthropic openai
```

Create a `.env` file (never commit it):
```
SNAPTRADE_CLIENT_ID=your_client_id
SNAPTRADE_CONSUMER_KEY=your_consumer_key
NS_MAX_ORDER_CAD=2000
NS_KILL_SWITCH=
```

Start the server:
```bash
python main.py
```

The KMP app connects to `http://localhost:5000` by default (configured in `NexusApiClient.LOCAL_BASE_URL`).

---

## 7. Environment Variables

| Variable | Purpose | Default |
|---|---|---|
| `SNAPTRADE_CLIENT_ID` | SnapTrade app ID | required for live data |
| `SNAPTRADE_CONSUMER_KEY` | SnapTrade signing key | required for live data |
| `NS_MAX_ORDER_CAD` | Hard order cap (CAD) | 2000 |
| `NS_KILL_SWITCH` | Block all orders if `true` | unset |

The KMP Android/Desktop app works in **demo mode** without any server running.
Demo mode shows approximate TFSA holdings data and is clearly labelled.

---

## 8. Run Python tests (CI suite)

```bash
pip install pytest
pytest tests/ -v
```

---

## 9. Project structure quick-reference

```
nexus-sphere/
├── shared/                   ← Kotlin shared module (domain, models, risk, data)
│   └── src/commonMain/       ← runs on Android + Desktop
├── composeApp/               ← Compose Multiplatform UI
│   └── src/
│       ├── commonMain/       ← shared screens + viewmodels
│       ├── androidMain/      ← MainActivity
│       └── desktopMain/      ← main.kt (desktop entry)
├── main.py                   ← Flask server (SnapTrade proxy)
├── guardrails.py             ← server-side order safety
├── tests/                    ← Python test suite (pytest)
├── adr/                      ← Architecture Decision Records
└── docs/                     ← Architecture, migration plan, setup
```
