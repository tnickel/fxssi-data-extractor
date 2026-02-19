# FXSSI Data Extractor - Interne Projektdokumentation

**Version:** 1.0.0
**Stand:** Februar 2026
**Repo:** https://github.com/tnickel/fxssi-data-extractor

---

## Überblick

Forex-Sentiment-Daten von fxssi.com scrapen, speichern und per Email benachrichtigen.
Läuft in zwei Modi: **GUI** (JavaFX, interaktiv) und **Console** (headless, stündlich geplant).

### Contrarian-Logik
| Buy% | Signal |
|------|--------|
| > 60% | SELL (gegen die Masse) |
| < 40% | BUY |
| 40–60% | NEUTRAL |

CNN Fear & Greed Index wird als `BTC/USD`-Symbol integriert:
0–44 → BUY, 45–55 → NEUTRAL, 56–100 → SELL

---

## Umgebung / Tools

| Tool | Version | Pfad |
|------|---------|------|
| Java | 17 | System-PATH |
| Maven | 3.9.6 | `C:\Users\tnickel\apache-maven-3.9.6\bin\mvn.cmd` |
| JavaFX | 17.0.2 | Maven-Abhängigkeit (wird eingebettet) |
| Git | - | System-PATH |
| Eclipse | 2025 | Workspace: `D:\git\eclipse-workspace2025\` |

### Maven-Befehl (da mvn nicht im PATH ist)
```
powershell -Command "Set-Location 'D:\git\eclipse-workspace2025\fxssi-data-extractor'; & 'C:\Users\tnickel\apache-maven-3.9.6\bin\mvn.cmd' clean package -DskipTests"
```

---

## Projektstruktur

```
fxssi-data-extractor/
├── pom.xml                          # Maven Build (Version 1.0.0)
├── config/
│   └── config.properties            # App-Konfiguration (Version, Datenpfad, Log-Level)
├── doku/
│   └── PROJECT.md                   # Diese Datei
├── data/                            # Laufzeit-Datenspeicher (nicht in Git)
│   ├── config/
│   │   └── email_config.properties  # Email-SMTP-Konfiguration
│   ├── currency_pairs/              # Pro-Paar CSV-Dateien (EUR_USD.csv etc.)
│   ├── signal_changes/
│   │   ├── signal_changes_history.csv
│   │   ├── lastsend.csv             # Anti-Spam: wann/was zuletzt gesendet
│   │   └── last_known_signals.csv   # MetaTrader-Export
│   └── fxssi_data_YYYY-MM-DD.csv   # Tägliche Rohdaten
├── install/                         # Bundled JDK + Startskripte
│   └── bin/
│       └── StartFXSSI.bat
└── src/main/java/
    ├── FXSSIDataExtractor.java       # Einstiegspunkt (--gui / --console)
    └── com/fxssi/extractor/
        ├── model/
        │   ├── CurrencyPairData.java       # Datenmodell (Paar, Buy%, Sell%, Signal)
        │   └── SignalChangeEvent.java      # Signalwechsel-Event (inkl. Wichtigkeit)
        ├── scraper/
        │   ├── FXSSIScraper.java           # JSoup-Scraper für fxssi.com
        │   └── FearGreedScraper.java       # CNN Fear & Greed API → BTC/USD
        ├── storage/
        │   ├── DataFileManager.java        # Tägliche CSV-Dateien
        │   ├── CurrencyPairDataManager.java # Pro-Paar CSV-Dateien
        │   ├── SignalChangeHistoryManager.java # Wechselerkennung + Email-Trigger
        │   └── LastSentSignalManager.java  # Anti-Spam (Threshold-Prüfung)
        ├── scheduler/
        │   └── HourlyScheduler.java        # Console-Modus: stündlicher Task
        ├── notification/
        │   ├── EmailConfig.java            # SMTP-Konfiguration + Checkbox-Einstellungen
        │   └── EmailService.java           # Email-Versand, Filterung, Rate-Limiting
        └── com/fxsssi/extractor/gui/
            ├── FXSSIGuiApplication.java    # JavaFX Application (APP_VERSION hier)
            ├── MainWindowController.java   # Hauptfenster-Logik, Tabelle, Buttons
            ├── GuiDataService.java         # Daten-Service für GUI (Cache, Refresh)
            ├── DataRefreshManager.java     # Auto-Refresh-Scheduler (1–60 Min.)
            ├── RatioBarTableCell.java      # Grafische Buy/Sell-Balken in Tabelle
            ├── SignalIconTableCell.java     # Signal-Icon in Tabelle
            ├── SignalChangeTableCell.java   # Wechsel-Anzeige in Tabelle
            ├── SignalHistoryChartTableCell.java # Mini-Chart in Tabelle
            ├── HistoricalDataWindow.java   # Historische Daten Fenster
            ├── SignalChangeHistoryWindow.java # Signalwechsel-History Fenster
            └── config/
                ├── EmailConfigWindow.java  # Email-Konfig-Fenster (speichert + schließt)
                └── MetaTraderPanel.java    # MetaTrader-Verzeichnis-Panel
```

---

## Build-Artefakte (nach `mvn clean package`)

```
target/
├── fxssi-data-extractor.jar         # Basis-JAR
├── fxssi-data-extractor-gui.jar     # Fat-JAR mit JavaFX (für GUI-Start)
└── fxssi-data-extractor-console.jar # Fat-JAR ohne JavaFX (für Console-Start)
```

---

## Email-System

### Ablauf bei jedem Daten-Refresh
```
forceDataRefresh()
  → saveToAllSystems()
    → SignalChangeHistoryManager.processNewData()
      → EmailService.sendSignalChangeNotificationWithThreshold()
        → filterSignalsAboveThreshold()   ← Threshold + Checkbox-Filter
          → shouldSendEmail() in LastSentSignalManager
        → sendEmail() via SMTP
        → LastSentSignalManager.recordSentSignal()
        → MetaTrader-Sync (last_known_signals.csv kopieren)
```

### Checkbox-Filter (EmailService.filterSignalsAboveThreshold)
| Checkbox | Bedingung |
|----------|-----------|
| Alle Änderungen | Threshold überschritten, egal welcher Wechsel |
| Kritisch | Nur BUY ↔ SELL |
| Hoch | Nur Wechsel über/durch NEUTRAL |

### Anti-Spam
- **Threshold** (default 3%): `|neuer_buy% - letzter_buy%| >= threshold`
- **Rate-Limit** (default 10/Stunde): `ConcurrentLinkedQueue` mit Zeitprüfung
- **lastsend.csv**: Persistenter Stand über Neustarts hinweg

### SMTP-Standard (GMX)
- Host: `mail.gmx.net`, Port: `587`, STARTTLS
- Passwort: Base64-kodiert in `data/config/email_config.properties`

---

## MetaTrader-Integration

Nach jedem Email-Versand wird `last_known_signals.csv` in bis zu 2 MT5-Verzeichnisse kopiert.
Format: `EURUSD;BUY` (Semikolon-getrennt, kein Slash im Paar-Namen)
Sonderregeln: `XAUUSD→GOLD`, `XAGUSD→SILBER`

---

## Bekannte Bugs / behobene Fehler

| Datum | Fix | Datei |
|-------|-----|-------|
| 2026-02-19 | Doppelter Email-Versand entfernt (`sendSignalChangeNotificationsIfEnabled()`) | MainWindowController.java |
| 2026-02-19 | Checkboxen (Kritisch/Hoch/Alle) werden jetzt ausgewertet | EmailService.java |
| 2026-02-19 | Konfig-Fenster schließt sich nach Speichern | EmailConfigWindow.java |

---

## Versionierung

| Wo | Wert |
|----|------|
| `pom.xml` → `<version>` | 1.0.0 |
| `config/config.properties` → `app.version` | 1.0.0 |
| `FXSSIGuiApplication.java` → `APP_VERSION` | "1.0.0" |

Bei neuer Version alle drei Stellen aktualisieren.
