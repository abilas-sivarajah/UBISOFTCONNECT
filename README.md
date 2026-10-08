# Connect Hub – Ersatz für die eingestellte Ubisoft-Connect-App (Android)

Ubisoft hat die Android-App von Ubisoft Connect im April 2025 aus dem Play Store genommen und im Mai 2025 abgeschaltet. **Connect Hub** ist ein inoffizieller Ersatz: eine schlanke Android-App, die alle Ubisoft-Dienste, die es noch im Web gibt, an einem Ort bündelt und dich angemeldet hält.

> Inoffizielles Projekt, nicht mit Ubisoft verbunden oder von Ubisoft unterstützt. Ubisoft und Ubisoft Connect sind Marken von Ubisoft Entertainment.

## Funktionen

| Bereich | Inhalt |
| --- | --- |
| **Start** | Schnellzugriff auf Konto, Units & Belohnungen, Ubisoft+, kostenlose Spiele, Spielekatalog, Rainbow Six Siege-Statistiken, Serverstatus, Support und den PC-Client |
| **News** | news.ubisoft.com in deiner Sprache |
| **Store** | store.ubisoft.com für dein Land (Käufe, Units-Rabatt, Ubisoft+) |
| **Konto** | Profil, Sicherheit, Zwei-Faktor-Authentifizierung, verknüpfte Konten |
| **Hilfe** | Ubisoft-Support, Tickets, Serverstatus |

Außerdem:

- **Eine Anmeldung für alles**: Store, Konto und Hilfe teilen sich die Ubisoft-Sitzung, sie bleibt nach dem Schließen der App erhalten.
- **Anmeldung mit Steam, PlayStation, Xbox usw.**: Login-Fenster öffnen sich innerhalb der App.
- Jeder Tab behält seine Seite, wenn du wechselst. Zurück-Taste, Pull-to-Refresh, Offline-Hinweis mit „Erneut versuchen“.
- Menü: Neu laden, im Browser öffnen, Link teilen, **Desktop-Ansicht** (für Seiten, die auf dem Handy Funktionen ausblenden), **Abmelden und Daten löschen**.
- Datei-Upload (z. B. Screenshots für Support-Tickets).
- Links zu YouTube, X, Discord usw. öffnen in der passenden App.
- Deutsch und Englisch, heller und dunkler Modus, Android 8.0 bis 16.

## Was (noch) nicht geht

Ubisoft bietet keine öffentliche Schnittstelle für Freundesliste, Chat, Gruppen, Push-Benachrichtigungen oder Spielfortschritt. Die inoffizielle Login-Schnittstelle, die Community-Tools früher genutzt haben, blockiert Drittanbieter seit April 2026. Diese Funktionen der alten App lassen sich deshalb nicht seriös nachbauen; Connect Hub zeigt nur, was Ubisoft selbst im Web anbietet.

## Installation

1. Auf dem Handy die neueste APK herunterladen:
   **[ConnectHub.apk](https://github.com/abilas-sivarajah/UBISOFTCONNECT/releases/latest/download/ConnectHub.apk)**
   (oder unter [Releases](https://github.com/abilas-sivarajah/UBISOFTCONNECT/releases)).
2. Die Datei öffnen. Android fragt einmalig, ob der Browser Apps installieren darf – erlauben.
3. „Installieren“ tippen, Connect Hub öffnen, unter **Konto** anmelden.

## Sicherheit und Datenschutz

- Du gibst Benutzername, Passwort und 2FA-Code **nur auf den offiziellen Ubisoft-Seiten** ein. Die App liest sie nicht mit und speichert keine Passwörter.
- Die App hat keine eigenen Server, kein Tracking und nur die Berechtigung „Internet“.
- Die Sitzung liegt nur auf deinem Gerät (kein Cloud-Backup). „Abmelden und Daten löschen“ entfernt Cookies, Cache und Web-Speicher.
- Nur HTTPS, kein Dateizugriff aus Webseiten heraus.

## Selbst bauen

Voraussetzungen: JDK 17 oder neuer, Android SDK (Platform 36).

```bash
./gradlew testDebugUnitTest   # Unit- und Robolectric-Tests
./gradlew assembleRelease     # app/build/outputs/apk/release/app-release.apk
```

Jeder Push baut die APK per GitHub Actions; auf dem Standard-Branch wird sie automatisch als neues Release veröffentlicht.

### Eigener Signierschlüssel (empfohlen)

Ohne Schlüssel signiert die Pipeline mit einem wechselnden Debug-Schlüssel. Android installiert dann ein Update nur nach Deinstallation der alten Version (du musst dich neu anmelden). Für nahtlose Updates einmalig einen Schlüssel anlegen und als Repository-Secrets hinterlegen (*Settings → Secrets and variables → Actions*):

```bash
keytool -genkeypair -keystore release.jks -alias connecthub -keyalg RSA -keysize 2048 -validity 10000
base64 -w0 release.jks   # Ausgabe als SIGNING_KEYSTORE_BASE64 speichern
```

| Secret | Wert |
| --- | --- |
| `SIGNING_KEYSTORE_BASE64` | Base64 der `.jks`-Datei |
| `SIGNING_KEYSTORE_PASSWORD` | Keystore-Passwort |
| `SIGNING_KEY_ALIAS` | `connecthub` |
| `SIGNING_KEY_PASSWORD` | Schlüssel-Passwort |

Den Schlüssel nie ins Repository legen: Wer ihn hat, kann ein „Update“ ausliefern, das an deine Ubisoft-Sitzung kommt.

## Aufbau

```
app/src/main/java/com/abilas/connecthub/
├── MainActivity.kt      Navigation, Startseite, Menü, externe Links, Datei-Upload, Abmelden
├── BrowserTab.kt        ein WebView pro Tab mit Pull-to-Refresh und Offline-Ansicht
├── PopupController.kt   Login-Fenster (window.open) im Vollbild
├── UbisoftLinks.kt      Ubisoft-URLs passend zu Sprache und Land des Geräts
├── UrlPolicy.kt         welche Links in der App bleiben und welche nach außen gehen
├── UserAgents.kt        Desktop-Ansicht
├── Section.kt           die fünf Tabs
└── WebViews.kt          gemeinsame WebView-Einstellungen
```
