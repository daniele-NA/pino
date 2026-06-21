# Pino

A tiny Android **home screen widget** that shows your phone's **local Wi-Fi IP** and the
connected **Wi-Fi network (SSID)** at a glance — handy for grabbing that IP fast while
working over ADB.

## Features

- 4×1 home screen widget with the Wi-Fi IP and SSID.
- **Real-time updates**: a `ConnectivityManager` network callback backed by a
  `PendingIntent` refreshes the widget on every Wi-Fi change — no polling, survives
  process death.
- Fixed violet gradient background; text/icon colors adapt to light/dark.

## Tech

- Java + Android Views (no Kotlin/Compose).
- Material 3 (`Theme.Material3.DayNight`), Roboto font (`res/font/roboto.ttf`).
- `minSdk` 28, `targetSdk` 37, `compileSdk` 37.

## Build

```bash
./gradlew :app:assembleDebug
```

## Permissions

`ACCESS_FINE_LOCATION` is required to read the Wi-Fi SSID (Android platform rule). The app
requests it on first launch; until granted, the widget shows a hint instead of the SSID.
`ACCESS_WIFI_STATE` / `ACCESS_NETWORK_STATE` are used to read the IP and watch for changes.

## Usage

Long-press an empty spot on your home screen → **Widgets** → **Pino**, then drop the 4×1
widget where you want it.

## License

[MIT](LICENSE)
