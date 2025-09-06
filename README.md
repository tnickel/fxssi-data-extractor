# FXSSI Data Extractor - Forex Sentiment Analysis Tool
**Automatisierte Extraktion und Analyse von Forex-Sentiment-Daten von FXSSI.com**
Java-Anwendung zur kontinuierlichen Sammlung von Währungspaardaten mit Buy/Sell-Verhältnissen für Trading-Signal-Generierung. Unterstützt sowohl Hintergrund-Datensammlung (Console-Modus) als auch interaktive Live-Analyse (JavaFX GUI).
**Hauptfunktionen:**
• **Dual-Mode Architektur**: Console-Modus für automatische stündliche Datensammlung + JavaFX GUI für Live-Monitoring
• **Web-Scraping**: Robuste JSoup-basierte Extraktion von FXSSI Current Ratio Daten für 15+ Währungspaare
• **Dreifache Datenspeicherung**: Tägliche CSV-Dateien + währungspaar-spezifische Dateien + Signalwechsel-Historie
• **Live-GUI**: Interaktive Tabelle mit Ratio-Balken, Signal-Icons und konfigurierbarem Auto-Refresh (1-60 Min.)
• **Trading-Signale**: Automatische BUY/SELL/NEUTRAL Signal-Generierung basierend auf Contrarian-Approach
• **Signalwechsel-Erkennung**: Automatische Erkennung und Logging von Signal-Änderungen mit Wichtigkeits-Klassifizierung
• **Historische Daten**: Popup-Fenster zur Anzeige kompletter CSV-Historie pro Währungspaar mit Statistiken
**Technische Features:**
• **Zeitsteuerung**: ScheduledExecutorService für präzise stündliche Ausführung + GUI Auto-Refresh
• **Thread-Sicherheit**: Asynchrone Datenladung mit CompletableFuture + thread-sichere GUI-Updates
• **Robustes Parsing**: Fallback-Strategien bei Website-Änderungen + Demo-Daten für Testing
• **Datenvalidierung**: Konsistenz-Checks + Duplikat-Filterung + Backup-Mechanismen
• **Konfigurierbar**: Command-Line-Parameter für Datenverzeichnis + GUI/Console-Modus-Switching
**Use Cases:** Ideal für Forex-Trader zur Sentiment-Analyse, Contrarian-Trading-Strategien und kontinuierlichen Marktbeobachtung.
**Technologie-Stack:** Java 11+, JavaFX GUI, JSoup Web-Scraping, ScheduledExecutorService, UTF-8 CSV-Export
**Architekturbasis:** Modulares Design mit Service-Layer, Facade-Pattern und Thread-sicherer Ausführung
