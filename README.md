# AgriDroneSafety

Android app for agricultural drone battery safety, telemetry monitoring, and return-to-home assessment.

## Overview
AgriDroneSafety monitors live drone telemetry, evaluates battery health, estimates safe return-to-launch (RTL) energy, and surfaces safety alerts to the pilot. It supports demo-mode simulation and real-time telemetry from the Skydroid RCSDK path.

## Core Features
- Live telemetry ingestion from demo data or Skydroid-connected devices
- Battery pack and per-cell voltage analysis
- Rapid voltage sag and discharge-rate detection
- Distance-to-home and RTL power estimation
- Safety states: NORMAL, NOTICE, WARNING, CELL_FAULT, CRITICAL, EMERGENCY
- Dashboard UI with voice, sound, vibration, and spray-interlock logic
- Local flight logging with Room persistence

## Tech Stack
- Android (min SDK 33)
- Java 11
- Gradle + AndroidX
- Material UI, RecyclerView, Room
- MAVLink-based telemetry parsing
- Skydroid RCSDK adapter

## Project Structure
- `app/src/main/java/com/agridrone/safety/config` — safety thresholds and telemetry settings
- `app/src/main/java/com/agridrone/safety/data` — telemetry models, repository, and data sources
- `app/src/main/java/com/agridrone/safety/domain` — battery, RTL, and safety logic
- `app/src/main/java/com/agridrone/safety/system` — alerting and spray interlock controllers
- `app/src/main/java/com/agridrone/safety/ui` — activity, ViewModel, and dashboard rendering

## Getting Started
1. Install Android Studio and JDK 11.
2. Open the project folder.
3. Let Gradle sync.
4. Run the app on an emulator or connected Android device.

```bash
./gradlew assembleDebug
./gradlew installDebug
```

## Safety Note
This app is an advisory monitoring tool, not a certified flight-control system. All battery thresholds and RTL decisions should be validated against the actual drone hardware and aircraft-specific safety requirements before operational use.
