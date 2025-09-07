# FXSSI Data Extractor

[![Java](https://img.shields.io/badge/Java-11+-orange.svg)](https://openjdk.java.net/)
[![JavaFX](https://img.shields.io/badge/JavaFX-17+-blue.svg)](https://openjfx.io/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Version](https://img.shields.io/badge/Version-2.2-brightgreen.svg)](CHANGELOG.md)

Ein fortschrittliches Java-Tool für die automatisierte Extraktion und Analyse von Forex-Sentiment-Daten von FXSSI.com mit Live-Monitoring, Signalwechsel-Erkennung und E-Mail-Benachrichtigungen.

## 🌟 Features

### Dual-Mode Support
- **Console-Modus**: Automatisierte stündliche Datensammlung im Hintergrund
- **GUI-Modus**: Interaktive JavaFX-Oberfläche mit Live-Monitoring

### Datenextraktion & -speicherung
- ✅ Automatische Extraktion von Buy/Sell-Verhältnissen von FXSSI.com
- ✅ Vierfache Datenspeicherung: Tägliche + Währungspaar-spezifische + Signalwechsel + E-Mail-Konfiguration
- ✅ Robustes HTML-Parsing mit JSoup und Fallback-Strategien
- ✅ Thread-sichere CSV-Dateiverwaltung mit UTF-8-Encoding

### Live-Monitoring & Analyse
- 🔄 **Signalwechsel-Erkennung**: Automatische Erkennung von BUY ↔ SELL Wechseln
- 📊 **Historische Datenanalyse**: Vollständige CSV-Historie pro Währungspaar
- 📈 **Visual Ratio-Balken**: Horizontale Buy/Sell-Verhältnis-Darstellung
- 🎯 **Trading-Signal-Icons**: Visuelle BUY/SELL/NEUTRAL Indikatoren

### E-Mail-Benachrichtigungen
- 📧 **GMX-Server Integration**: Vorkonfiguriert für GMX-E-Mail-Accounts
- 🚨 **Signalwechsel-Alerts**: Automatische E-Mails bei kritischen Änderungen
- ⚠️ **Wichtigkeits-Filterung**: Kritisch, Hoch, Mittel, Niedrig
- 🛡️ **Spam-Schutz**: Konfigurierbare E-Mail-Limits pro Stunde

### GUI-Features
- 🖥️ **Moderne JavaFX-Oberfläche**: Programmatisch erstellt (ohne FXML)
- 🔄 **Auto-Refresh**: Konfigurierbare Intervalle (1-60 Minuten)
- 📊 **Historische Daten-Viewer**: Popup-Fenster mit detaillierter CSV-Analyse
- 🔧 **E-Mail-Konfiguration**: Vollständige GMX-Setup-Oberfläche
- 💾 **CSV-Export**: Direkte Exportfunktion für alle Daten

## 🛠️ Technologie-Stack

- **Java 11+** - Core Programming Language
- **JavaFX 17+** - GUI Framework
- **JSoup** - Web Scraping & HTML Parsing
- **Java Mail API** - E-Mail-Funktionalität
- **ScheduledExecutorService** - Zeitsteuerung
- **CompletableFuture** - Asynchrone Operationen
- **Maven** - Build Management
- **JUnit 5** - Testing Framework

## 📋 Voraussetzungen

- Java 11 oder höher
- JavaFX 17+ (für GUI-Modus)
- Internet-Verbindung für FXSSI.com Zugriff
- GMX-E-Mail-Account (optional, für Benachrichtigungen)

## 🚀 Installation

### 1. Repository klonen
```bash
git clone https://github.com/yourusername/fxssi-data-extractor.git
cd fxssi-data-extractor
```

### 2. Maven Build
```bash
mvn clean compile
```

### 3. Ausführen

#### Console-Modus (Standard)
```bash
java -cp target/classes FXSSIDataExtractor
```

#### GUI-Modus
```bash
java -cp target/classes FXSSIDataExtractor --gui
```

#### Mit custom Datenverzeichnis
```bash
java -cp target/classes FXSSIDataExtractor --gui --data-dir /path/to/data
```

## 💻 Verwendung

### Command Line Optionen

```bash
java FXSSIDataExtractor [OPTIONEN]

Optionen:
  --gui                    Startet die grafische Benutzeroberfläche
  --console               Startet im Console-Modus (Standard)
  --data-dir <PFAD>       Setzt das Datenverzeichnis (Standard: ./data)
  --help                  Zeigt die Hilfe an

Beispiele:
  java FXSSIDataExtractor --gui
  java FXSSIDataExtractor --console --data-dir /home/user/fxssi-data
  java FXSSIDataExtractor --data-dir C:\FXSSIData --gui
```

### Console-Modus
- Startet automatische stündliche Datenextraktion
- Läuft als Hintergrund-Service
- Speichert in alle Datensysteme gleichzeitig
- Erkennt Signalwechsel und versendet E-Mails

### GUI-Modus
- **Live-Tabelle**: Zeigt aktuelle Sentiment-Daten mit visuellen Balken
- **Auto-Refresh**: Konfigurierbar von 1-60 Minuten
- **Signalwechsel-Spalte**: Klickbare Icons für Detail-Historie
- **Historische Daten**: Doppelklick auf Währungspaar öffnet CSV-Historie
- **E-Mail-Konfiguration**: Button für vollständige GMX-Setup

## 🏗️ Architektur

### Hauptkomponenten

```
src/main/java/
├── FXSSIDataExtractor.java              # Hauptklasse & Orchestrator
├── com/fxssi/extractor/
│   ├── model/
│   │   ├── CurrencyPairData.java        # Datenmodell für Währungspaare
│   │   └── SignalChangeEvent.java       # Signalwechsel-Events
│   ├── scraper/
│   │   └── FXSSIScraper.java            # Web-Scraper für FXSSI.com
│   ├── storage/
│   │   ├── DataFileManager.java         # Tägliche CSV-Dateien
│   │   ├── CurrencyPairDataManager.java # Währungspaar-spezifische Dateien
│   │   └── SignalChangeHistoryManager.java # Signalwechsel-Historie
│   ├── scheduler/
│   │   └── HourlyScheduler.java         # Zeitsteuerung
│   ├── notification/
│   │   ├── EmailConfig.java             # E-Mail-Konfiguration
│   │   └── EmailService.java            # E-Mail-Versendung
│   └── gui/
│       ├── FXSSIGuiApplication.java     # JavaFX Application
│       ├── MainWindowController.java    # Haupt-GUI-Controller
│       ├── EmailConfigWindow.java       # E-Mail-Konfigurationsfenster
│       ├── HistoricalDataWindow.java    # Historische Daten Viewer
│       └── [Custom TableCells]          # Spezialisierte UI-Komponenten
```

### Design Patterns
- **Facade Pattern**: FXSSIScraper vereinfacht Web-Scraping
- **Service Layer**: GuiDataService für Datenbereitstellung
- **Observer Pattern**: Signalwechsel-Benachrichtigungen
- **Factory Pattern**: Custom TableCell-Factories
- **Singleton Pattern**: Konfigurationsmanagement

## 💾 Datenstruktur

### Speicherorte
```
data/
├── fxssi_data_YYYY-MM-DD.csv           # Tägliche Dateien (alle Währungspaare)
├── currency_pairs/                      # Währungspaar-spezifische Dateien
│   ├── EUR_USD.csv
│   ├── GBP_USD.csv
│   └── [weitere Währungspaare].csv
├── signal_changes/                      # Signalwechsel-Historie
│   ├── signal_changes_history.csv
│   └── last_known_signals.csv
└── config/                             # Konfigurationsdateien
    └── email_config.properties
```

### CSV-Format

#### Tägliche Dateien
```csv
Zeitstempel;Währungspaar;Buy_Prozent;Sell_Prozent;Handelssignal
2025-01-15 14:30:00;EUR/USD;45.2;54.8;BUY
```

#### Währungspaar-spezifische Dateien
```csv
Zeitstempel;Buy_Prozent;Sell_Prozent;Handelssignal
2025-01-15 14:30:00;45.2;54.8;BUY
```

#### Signalwechsel-Historie
```csv
Zeitstempel;Währungspaar;Von_Signal;Zu_Signal;Von_Buy_Prozent;Zu_Buy_Prozent
2025-01-15 14:30:00;EUR/USD;NEUTRAL;BUY;50.0;38.5
```

## 📧 E-Mail-Konfiguration

### GMX-Setup (Empfohlen)
1. Öffne GUI-Modus und klicke "📧 E-Mail-Konfiguration"
2. Konfiguration:
   - **SMTP-Server**: `mail.gmx.net`
   - **Port**: `587`
   - **Verschlüsselung**: STARTTLS aktivieren
   - **Benutzername**: Ihre vollständige GMX E-Mail-Adresse
   - **Passwort**: Ihr GMX Passwort

### Benachrichtigungstypen
- 🚨 **Kritisch**: BUY ↔ SELL Direktumkehrungen
- ⚠️ **Hoch**: BUY/SELL ↔ NEUTRAL Wechsel
- 🔄 **Alle**: Jeder Signalwechsel

### Spam-Schutz
- Konfigurierbare Limits: 1-100 E-Mails pro Stunde
- Automatische Rate-Limiting
- Duplikat-Erkennung

## 🔧 Konfiguration

### Wichtige Einstellungen

#### Refresh-Intervalle
- **Minimum**: 1 Minute
- **Maximum**: 60 Minuten
- **Standard**: 5 Minuten

#### Datenbereinigung
```java
// Bereinige Daten älter als 30 Tage
extractor.cleanupOldDataInBothSystems(30);
```

#### Custom Scheduler
```java
// Custom Intervall für Testing
HourlyScheduler customScheduler = HourlyScheduler.createCustomIntervalScheduler(task, 2);
```

## 🐛 Troubleshooting

### Häufige Probleme

#### 1. FXSSI-Website nicht erreichbar
```
LÖSUNG: Überprüfe Internet-Verbindung, Scraper verwendet automatische Fallback-Strategien
```

#### 2. E-Mail-Versendung fehlgeschlagen
```
PRÜFE: 
- GMX-Anmeldedaten korrekt
- STARTTLS aktiviert
- Port 587 verwendet
- Firewall-Einstellungen
```

#### 3. CSV-Parsing-Fehler
```
HÄUFIGE URSACHE: Deutsches Dezimalformat (Komma statt Punkt)
LÖSUNG: Der Parser unterstützt automatische Konvertierung
```

#### 4. JavaFX-Module nicht gefunden
```bash
# Linux/Mac
export PATH_TO_FX=/path/to/javafx-sdk-17/lib
java --module-path $PATH_TO_FX --add-modules javafx.controls,javafx.fxml -cp target/classes FXSSIDataExtractor --gui

# Windows
set PATH_TO_FX=C:\path\to\javafx-sdk-17\lib
java --module-path %PATH_TO_FX% --add-modules javafx.controls,javafx.fxml -cp target/classes FXSSIDataExtractor --gui
```

### Debug-Modus
```bash
# Erweiterte Logging-Ausgabe
java -Dlog.level=DEBUG -cp target/classes FXSSIDataExtractor
```

### Log-Dateien
```
logs/
├── fxssi-extractor.log          # Allgemeine Logs
├── fxssi-extractor-error.log    # Nur Fehler
└── data-extraction.log          # Datenextraktion Details
```

## 🔄 Signalwechsel-System

### Signaltypen
- **BUY**: Buy-Percentage < 40% (Contrarian Trading)
- **SELL**: Buy-Percentage > 60% (Contrarian Trading)
- **NEUTRAL**: 40% ≤ Buy-Percentage ≤ 60%
- **UNKNOWN**: Unbestimmbar

### Wichtigkeitsstufen
- **🚨 CRITICAL**: Direkte BUY ↔ SELL Umkehrung
- **⚠️ HIGH**: Von/Zu NEUTRAL Wechsel
- **🟡 MEDIUM**: Von/Zu UNKNOWN Wechsel
- **🟢 LOW**: Sonstige Wechsel

### Aktualität
- **🔴 SEHR AKTUELL**: Letzte 2 Stunden
- **🟡 AKTUELL**: Heute (24 Stunden)
- **🟢 DIESE WOCHE**: Letzte 7 Tage
- **⚪ ÄLTER**: Älter als 7 Tage

## 📊 API-Referenz

### Hauptklassen

#### FXSSIDataExtractor
```java
// Konstruktoren
FXSSIDataExtractor()                           // Standard-Verzeichnis
FXSSIDataExtractor(boolean guiMode)            // Modus-Auswahl
FXSSIDataExtractor(boolean guiMode, String dataDirectory) // Vollständig

// Hauptmethoden
void start()                                   // Startet Console-Modus
void stop()                                    // Stoppt Programm
void executeManualDataExtraction()             // Manuelle Extraktion
String validateBothStorageSystems()            // Validiert alle Daten
```

#### GuiDataService
```java
// Datenabfrage
List<CurrencyPairData> getCurrentData()                    // Aktuelle Daten
List<CurrencyPairData> forceDataRefresh()                 // Erzwinge Refresh
List<CurrencyPairData> getHistoricalDataForCurrencyPair(String pair) // Historie

// Signalwechsel
List<SignalChangeEvent> getSignalChangeHistory(String pair)      // Wechsel-Historie
SignalChangeEvent getMostRecentSignalChange(String pair)         // Letzter Wechsel

// E-Mail
EmailService.EmailSendResult sendTestEmail()              // Test-E-Mail
EmailService.EmailSendResult testEmailConnection()        // Verbindungstest
```

#### EmailService
```java
// E-Mail-Versendung
EmailSendResult sendTestEmail()                           // Test-E-Mail
EmailSendResult sendSignalChangeNotification(List<SignalChangeEvent> changes) // Signalwechsel-E-Mail
EmailSendResult testConnection()                          // Server-Test
String getEmailStatistics()                              // E-Mail-Statistiken
```

## 🤝 Entwicklung & Beitrag

### Entwicklungsumgebung
```bash
# Repository klonen
git clone https://github.com/yourusername/fxssi-data-extractor.git

# Dependencies installieren
mvn install

# Tests ausführen
mvn test

# Build erstellen
mvn clean package
```

### Code-Stil
- Java 11+ Features verwenden
- Thread-Sicherheit beachten
- Umfassende Logging-Ausgaben
- JavaDoc für öffentliche APIs
- Unit Tests für neue Features

### Branch-Strategie
- `main`: Stable Release-Branch
- `develop`: Development-Branch
- `feature/*`: Feature-Branches
- `hotfix/*`: Hotfix-Branches

### Pull Request Guidelines
1. Feature-Branch von `develop` erstellen
2. Änderungen implementieren
3. Tests hinzufügen/aktualisieren
4. Code dokumentieren
5. Pull Request erstellen

## 📝 Changelog

### Version 2.2 (Aktuell)
- ✅ Vollständige E-Mail-Integration mit GMX-Support
- ✅ Erweiterte Signalwechsel-Erkennung mit Wichtigkeits-Klassifizierung
- ✅ Historische Daten-Viewer mit CSV-Export
- ✅ Vier-fache Datenspeicherung (Täglich + Währungspaar + Signalwechsel + E-Mail-Config)
- ✅ Verbesserte GUI mit 1700px Breite für E-Mail-Features
- ✅ Thread-sichere E-Mail-Versendung mit Rate-Limiting

### Version 2.1
- ✅ Signalwechsel-Erkennung und -Historie
- ✅ Währungspaar-spezifische Dateispeicherung
- ✅ Erweiterte GUI mit Custom TableCells

### Version 2.0
- ✅ JavaFX GUI-Implementation
- ✅ Dual-Mode Support (Console + GUI)
- ✅ Auto-Refresh mit konfigurierbaren Intervallen

## 📄 Lizenz

Dieses Projekt steht unter der MIT-Lizenz. Siehe [LICENSE](LICENSE) für Details.

## 🙏 Danksagungen

- [FXSSI.com](https://fxssi.com) für die Bereitstellung von Sentiment-Daten
- [JSoup](https://jsoup.org/) für das robuste HTML-Parsing
- [JavaFX](https://openjfx.io/) für das moderne GUI-Framework
- Java Community für die umfangreichen Libraries

## 📞 Support

- **Issues**: [GitHub Issues](https://github.com/yourusername/fxssi-data-extractor/issues)
- **Discussions**: [GitHub Discussions](https://github.com/yourusername/fxssi-data-extractor/discussions)
- **Wiki**: [Project Wiki](https://github.com/yourusername/fxssi-data-extractor/wiki)

---

**⚠️ Haftungsausschluss**: Dieses Tool dient nur zu Bildungs- und Forschungszwecken. Forex-Trading birgt erhebliche Risiken. Die Entwickler übernehmen keine Verantwortung für finanzielle Verluste durch die Nutzung dieses Tools.

**🔒 Datenschutz**: Alle E-Mail-Konfigurationen werden lokal gespeichert. Keine Daten werden an Dritte übertragen außer den konfigurierten E-Mail-Benachrichtigungen.
