# Thermik Vario

Variometer-App für den Gleitschirmflug. Android-App (Capacitor) mit eingebautem Handy-Barometer,
Oberfläche im Stil der Naviter-Geräte (Nimo, SeeYou Navigator).

## Installieren
1. Unter **Releases** die neueste `thermik-vario.apk` auf dem Handy herunterladen.
2. Öffnen und die Installation aus dieser Quelle erlauben.
3. Beim ersten Start „GPS starten“ tippen und den Standort erlauben.

## Funktionen
- Steigen aus dem **eingebauten Barometer** (25 Werte/s, Kalman-Filter mit Beschleunigungssensor)
- Ohne Barometer: GPS-Höhe plus Beschleunigungssensor (träger)
- Vario-Ton (Steigen piept, Sinkalarm), Variobalken, Ø 30 s
- Höhe (einmalig an GPS angeglichen), Höhe über Start, Speed, Kurs, Gleitzahl, Wind aus dem Kreisflug
- Thermikassistent mit Spur und geschätztem Kern
- Automatische Start-/Landeerkennung, IGC-Datei speichern (Downloads/ThermikVario) oder teilen
- Bildschirm bleibt an, Simulation zum Ausprobieren am Boden

## Aufbau
- `www/index.html` – die komplette App-Oberfläche und Logik (auch als Web-App nutzbar)
- `android/app/src/main/java/de/thermikvario/app/BarometerPlugin.java` – liest den Drucksensor, speichert/teilt IGC
- `.github/workflows/android.yml` – baut bei jedem Push auf `main` die APK und legt ein Release an

## Lokal bauen
```
npm ci
npx cap sync android
cd android && ./gradlew assembleDebug
```

## Grenzen
- Kein Ersatz für ein zugelassenes Fluginstrument. Keine Lufträume, keine Wegpunkte, kein Live-Tracking.
- Bei gesperrtem Bildschirm pausiert die App; deshalb bleibt der Bildschirm an.
- Bluetooth-Vario-Sensoren gehen nur in der Web-Version (Chrome), nicht in der App.
