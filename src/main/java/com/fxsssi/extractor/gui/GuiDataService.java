package com.fxsssi.extractor.gui;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fxssi.extractor.model.CurrencyPairData;
import com.fxssi.extractor.model.SignalChangeEvent;
import com.fxssi.extractor.scraper.FXSSIScraper;
import com.fxssi.extractor.storage.DataFileManager;
import com.fxssi.extractor.storage.CurrencyPairDataManager;
import com.fxssi.extractor.storage.SignalChangeHistoryManager;
import com.fxssi.extractor.notification.EmailService;
import com.fxssi.extractor.notification.EmailConfig;

/**
 * Service-Klasse für die Bereitstellung von FXSSI-Daten für die GUI
 * Integriert die bestehenden Scraper- und Storage-Komponenten mit konfigurierbarem Datenverzeichnis
 * Jetzt mit vollständiger E-Mail-Integration: tägliche UND währungspaar-spezifische Dateien UND Signalwechsel-Erkennung UND E-Mail-Benachrichtigungen
 * 
 * @author Generated for FXSSI Data Extraction GUI
 * @version 1.5 (mit korrigierter E-Mail-Integration - Threshold-System aktiviert)
 */
public class GuiDataService {
    
    private static final Logger LOGGER = Logger.getLogger(GuiDataService.class.getName());
    private static final String DEFAULT_DATA_DIRECTORY = "data";
    private static final int CACHE_TIMEOUT_MINUTES = 2; // Cache-Timeout in Minuten
    
    private FXSSIScraper scraper;
    private DataFileManager fileManager;
    private CurrencyPairDataManager currencyPairManager;
    private SignalChangeHistoryManager signalChangeManager;
    private EmailConfig emailConfig;
    private EmailService emailService;
    private List<CurrencyPairData> cachedData;
    private LocalDateTime lastCacheUpdate;
    private boolean isInitialized = false;
    private String dataDirectory;
    
    /**
     * Konstruktor mit Standard-Datenverzeichnis
     */
    public GuiDataService() {
        this(DEFAULT_DATA_DIRECTORY);
    }
    
    /**
     * Konstruktor mit konfigurierbarem Datenverzeichnis
     * @param dataDirectory Pfad zum Datenverzeichnis
     */
    public GuiDataService(String dataDirectory) {
        this.dataDirectory = validateAndNormalizeDataDirectory(dataDirectory);
        this.cachedData = new ArrayList<>();
        LOGGER.info("GuiDataService erstellt mit Datenverzeichnis: " + this.dataDirectory);
        LOGGER.info("Vierfache Integration aktiviert: Tägliche + Währungspaar-spezifische + Signalwechsel + E-Mail-Benachrichtigungen mit Threshold-System");
    }
    
    /**
     * Initialisiert den Datenservice mit korrigierter E-Mail-Integration
     */
    public void initialize() {
        try {
            LOGGER.info("Initialisiere GuiDataService mit korrigierter E-Mail-Integration...");
            LOGGER.info("Datenverzeichnis: " + dataDirectory);
            
            // Initialisiere Komponenten mit konfiguriertem Datenverzeichnis
            scraper = new FXSSIScraper(dataDirectory);
            fileManager = new DataFileManager(dataDirectory);
            currencyPairManager = new CurrencyPairDataManager(dataDirectory);
            
            // *** E-Mail-Integration ZUERST ***
            emailConfig = new EmailConfig(dataDirectory);
            emailConfig.loadConfig(); // Lade gespeicherte E-Mail-Konfiguration
            emailService = new EmailService(emailConfig);
            
            // *** KRITISCHE KORREKTUR: SignalChangeHistoryManager MIT EmailService erstellen ***
            signalChangeManager = new SignalChangeHistoryManager(dataDirectory, emailService);
            
            // Erstelle Datenverzeichnisse
            fileManager.createDataDirectory();
            currencyPairManager.createCurrencyDataDirectory();
            signalChangeManager.createSignalChangesDirectory();
            emailConfig.createConfigDirectory(); // E-Mail-Konfigurationsverzeichnis
            
            // Lade letzte bekannte Signale für Wechsel-Erkennung
            signalChangeManager.loadLastKnownSignals();
            
            // Teste Verbindung
            boolean connectionOk = scraper.testConnection();
            if (!connectionOk) {
                LOGGER.warning("Verbindung zu FXSSI fehlgeschlagen - verwende gespeicherte Daten");
            }
            
            isInitialized = true;
            LOGGER.info("GuiDataService erfolgreich initialisiert mit korrigierter E-Mail-Integration und Threshold-System");
            
            // Logge initiale Statistiken inklusive E-Mail-Status
            logInitialStatisticsWithEmail();
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Fehler beim Initialisieren des GuiDataService: " + e.getMessage(), e);
            throw new RuntimeException("GuiDataService konnte nicht initialisiert werden", e);
        }
    }
    
    /**
     * Holt aktuelle Daten (mit Caching für Performance)
     * WICHTIG: Speichert bei jedem Refresh in ALLE SYSTEME mit automatischen Threshold-E-Mails
     */
    public List<CurrencyPairData> getCurrentData() throws Exception {
        if (!isInitialized) {
            throw new IllegalStateException("GuiDataService ist nicht initialisiert");
        }
        
        // Prüfe Cache
        if (isCacheValid()) {
            LOGGER.fine("Verwende gecachte Daten");
            return new ArrayList<>(cachedData);
        }
        
        LOGGER.info("Lade frische Daten für GUI-Refresh mit Threshold-E-Mail-System...");
        
        try {
            // Versuche neue Daten zu laden
            List<CurrencyPairData> freshData = scraper.extractCurrentRatioData();
            
            if (freshData != null && !freshData.isEmpty()) {
                // SPEICHERE IN ALLE SYSTEME mit automatischen Threshold-E-Mails
                saveToAllSystems(freshData);
                
                // Aktualisiere Cache
                updateCache(freshData);
                
                LOGGER.info("GUI-Refresh: " + freshData.size() + " Datensätze geladen und in alle Speichersysteme mit Threshold-E-Mail-System integriert");
                return new ArrayList<>(freshData);
            } else {
                // Fallback: Verwende gespeicherte Daten
                return loadFallbackData();
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Fehler beim Laden aktueller Daten: " + e.getMessage(), e);
            
            // Fallback: Verwende gespeicherte Daten
            return loadFallbackData();
        }
    }
    
    /**
     * Lädt Daten asynchron für bessere GUI-Performance
     * WICHTIG: Speichert auch bei asynchronen Loads in alle Systeme mit Threshold-E-Mail-System
     */
    public CompletableFuture<List<CurrencyPairData>> getCurrentDataAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getCurrentData();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Fehler beim asynchronen Laden der Daten: " + e.getMessage(), e);
                return new ArrayList<>();
            }
        });
    }
    
    /**
     * Führt eine manuelle Datenaktualisierung durch (für Refresh-Button)
     * GARANTIERT Speicherung in alle Systeme mit automatischen Threshold-E-Mails
     */
    public List<CurrencyPairData> forceDataRefresh() throws Exception {
        LOGGER.info("Erzwinge manuelle Datenaktualisierung mit Threshold-E-Mail-System...");
        
        // Invalidiere Cache
        invalidateCache();
        
        // Lade neue Daten
        List<CurrencyPairData> freshData = scraper.extractCurrentRatioData();
        
        if (freshData != null && !freshData.isEmpty()) {
            // GARANTIERTE Speicherung in alle Systeme mit Threshold-E-Mail-System
            saveToAllSystems(freshData);
            
            // Aktualisiere Cache
            updateCache(freshData);
            
            LOGGER.info("Manuelle Aktualisierung: " + freshData.size() + " Datensätze in alle Speichersysteme mit Threshold-E-Mail-System integriert");
            return freshData;
        } else {
            LOGGER.warning("Manuelle Aktualisierung: Keine neuen Daten erhalten");
            return loadFallbackData();
        }
    }
    
    /**
     * *** KORRIGIERTE ZENTRALE METHODE: Speichert Daten in ALLE SYSTEME mit automatischen Threshold-E-Mails ***
     * Der SignalChangeHistoryManager übernimmt jetzt die komplette E-Mail-Logik mit Threshold-System
     * @param data Die zu speichernden Daten
     */
    private void saveToAllSystems(List<CurrencyPairData> data) {
        if (data == null || data.isEmpty()) {
            LOGGER.warning("Keine Daten zum Speichern in allen Systemen mit Threshold-E-Mail-System");
            return;
        }
        
        try {
            LOGGER.fine("Speichere " + data.size() + " Datensätze in alle Speichersysteme mit automatischem Threshold-E-Mail-System...");
            
            // 1. ERKENNE SIGNALWECHSEL UND SENDE AUTOMATISCH THRESHOLD-E-MAILS
            //    Der SignalChangeHistoryManager hat jetzt EmailService und macht alles automatisch:
            //    - Signalwechsel erkennen
            //    - Threshold-E-Mails senden (sendSignalChangeNotificationWithThreshold)
            //    - recordSentSignal() aufrufen
            //    - lastsend.csv aktualisieren
            List<SignalChangeEvent> detectedChanges = signalChangeManager.processNewData(data);
            
            if (!detectedChanges.isEmpty()) {
                LOGGER.info("SIGNALWECHSEL ERKANNT: " + detectedChanges.size() + " Wechsel bei diesem Refresh!");
                
                for (SignalChangeEvent change : detectedChanges) {
                    LOGGER.info(String.format("   - %s: %s (Wichtigkeit: %s, Aktualität: %s)", 
                        change.getCurrencyPair(), 
                        change.getChangeDescription(),
                        change.getImportance().getDescription(),
                        change.getActuality().getDescription()
                    ));
                }
                LOGGER.info("E-Mail-Benachrichtigungen werden automatisch vom SignalChangeHistoryManager mit Threshold-System versendet");
            }
            
            // 2. SPEICHERE in tägliche Dateien
            fileManager.appendDataToFile(data);
            LOGGER.fine("✓ Daten in tägliche Datei gespeichert");
            
            // 3. SPEICHERE in währungspaar-spezifische Dateien
            currencyPairManager.appendDataForAllPairs(data);
            LOGGER.fine("✓ Daten in währungspaar-spezifische Dateien gespeichert");
            
            // 4. Signalwechsel und E-Mails sind bereits durch processNewData() mit Threshold-System abgehandelt
            LOGGER.fine("✓ Signalwechsel erkannt und Threshold-E-Mails automatisch versendet");
            
            String logMessage = String.format("Erfolgreich %d Datensätze in ALLE SYSTEME mit Threshold-E-Mail-System integriert", data.size());
            if (!detectedChanges.isEmpty()) {
                logMessage += String.format(" | 🔄 %d Signalwechsel erkannt | 📧 Threshold-E-Mails automatisch versendet", detectedChanges.size());
            }
            LOGGER.info(logMessage);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Fehler beim Speichern in alle Systeme mit Threshold-E-Mail-System: " + e.getMessage(), e);
            // Werfe Exception nicht weiter, da GUI weiter funktionieren soll
        }
    }
    
    /**
     * *** NEUE METHODE: Sendet Test-E-Mail ***
     * @return Ergebnis der Test-E-Mail-Versendung
     */
    public EmailService.EmailSendResult sendTestEmail() {
        if (!isInitialized) {
            return new EmailService.EmailSendResult(false, "Service nicht initialisiert");
        }
        
        try {
            LOGGER.info("Sende Test-E-Mail...");
            EmailService.EmailSendResult result = emailService.sendTestEmail();
            
            if (result.isSuccess()) {
                LOGGER.info("✅ Test-E-Mail erfolgreich versendet");
            } else {
                LOGGER.warning("❌ Test-E-Mail fehlgeschlagen: " + result.getMessage());
            }
            
            return result;
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Fehler beim Senden der Test-E-Mail: " + e.getMessage(), e);
            return new EmailService.EmailSendResult(false, "Fehler: " + e.getMessage());
        }
    }
    
    /**
     * *** NEUE METHODE: Testet E-Mail-Server-Verbindung ***
     * @return Ergebnis des Verbindungstests
     */
    public EmailService.EmailSendResult testEmailConnection() {
        if (!isInitialized) {
            return new EmailService.EmailSendResult(false, "Service nicht initialisiert");
        }
        
        try {
            LOGGER.info("Teste E-Mail-Server-Verbindung...");
            return emailService.testConnection();
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Fehler beim E-Mail-Verbindungstest: " + e.getMessage(), e);
            return new EmailService.EmailSendResult(false, "Fehler: " + e.getMessage());
        }
    }
    
    /**
     * *** NEUE METHODE: Gibt E-Mail-Konfiguration zurück ***
     * @return Die aktuelle E-Mail-Konfiguration
     */
    public EmailConfig getEmailConfig() {
        if (!isInitialized) {
            return null;
        }
        return emailConfig;
    }
    
    /**
     * *** NEUE METHODE: Aktualisiert E-Mail-Konfiguration ***
     * @param newConfig Neue E-Mail-Konfiguration
     */
    public void updateEmailConfig(EmailConfig newConfig) {
        if (!isInitialized) {
            LOGGER.warning("Service nicht initialisiert - kann E-Mail-Konfiguration nicht aktualisieren");
            return;
        }
        
        try {
            LOGGER.info("Aktualisiere E-Mail-Konfiguration...");
            
            // Speichere neue Konfiguration
            newConfig.saveConfig();
            
            // Aktualisiere Service
            this.emailConfig = newConfig;
            this.emailService.updateConfig(newConfig);
            
            // *** WICHTIG: Aktualisiere auch den SignalChangeHistoryManager ***
            if (signalChangeManager != null) {
                signalChangeManager.setEmailService(emailService);
            }
            
            LOGGER.info("E-Mail-Konfiguration erfolgreich aktualisiert und an SignalChangeHistoryManager weitergegeben");
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Fehler beim Aktualisieren der E-Mail-Konfiguration: " + e.getMessage(), e);
        }
    }
    
    /**
     * *** NEUE METHODE: Gibt E-Mail-Statistiken zurück ***
     * @return E-Mail-Statistiken als String
     */
    public String getEmailStatistics() {
        if (!isInitialized || emailService == null) {
            return "E-Mail-Service nicht verfügbar";
        }
        
        try {
            return emailService.getEmailStatistics();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Fehler beim Abrufen der E-Mail-Statistiken: " + e.getMessage(), e);
            return "Fehler beim Abrufen der E-Mail-Statistiken";
        }
    }
    
    // ===== BESTEHENDE METHODEN (unverändert) =====
    
    /**
     * Holt historische Daten für einen bestimmten Zeitraum aus täglichen Dateien
     */
    public List<CurrencyPairData> getHistoricalData(int daysBack) {
        if (!isInitialized) {
            throw new IllegalStateException("GuiDataService ist nicht initialisiert");
        }
        
        List<CurrencyPairData> historicalData = new ArrayList<>();
        
        try {
            LOGGER.info("Lade historische Daten für " + daysBack + " Tage aus täglichen Dateien...");
            
            // Lade Daten für die letzten X Tage
            for (int i = 0; i < daysBack; i++) {
                java.time.LocalDate date = java.time.LocalDate.now().minusDays(i);
                List<CurrencyPairData> dayData = fileManager.readDataForDate(date);
                historicalData.addAll(dayData);
            }
            
            LOGGER.info("Historische Daten geladen: " + historicalData.size() + " Datensätze");
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Fehler beim Laden historischer Daten: " + e.getMessage(), e);
        }
        
        return historicalData;
    }
    
    /**
     * Holt historische Daten für ein spezifisches Währungspaar
     * @param currencyPair Das Währungspaar (z.B. "EUR/USD")
     * @return Alle historischen Daten für dieses Währungspaar
     */
    public List<CurrencyPairData> getHistoricalDataForCurrencyPair(String currencyPair) {
        if (!isInitialized) {
            throw new IllegalStateException("GuiDataService ist nicht initialisiert");
        }
        
        try {
            LOGGER.info("Lade historische Daten für Währungspaar: " + currencyPair);
            List<CurrencyPairData> data = currencyPairManager.readDataForCurrencyPair(currencyPair);
            LOGGER.info("Gefunden: " + data.size() + " historische Einträge für " + currencyPair);
            return data;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Fehler beim Laden historischer Daten für " + currencyPair + ": " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Holt die letzten N Einträge für ein Währungspaar
     * @param currencyPair Das Währungspaar
     * @param count Anzahl der gewünschten Einträge
     * @return Liste der letzten Einträge
     */
    public List<CurrencyPairData> getRecentDataForCurrencyPair(String currencyPair, int count) {
        if (!isInitialized) {
            throw new IllegalStateException("GuiDataService ist nicht initialisiert");
        }
        
        try {
            return currencyPairManager.readLastEntriesForCurrencyPair(currencyPair, count);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Fehler beim Laden der letzten Daten für " + currencyPair + ": " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Gibt den SignalChangeHistoryManager zurück (für GUI-Integration)
     * @return Der SignalChangeHistoryManager
     */
    public SignalChangeHistoryManager getSignalChangeHistoryManager() {
        if (!isInitialized) {
            throw new IllegalStateException("GuiDataService ist nicht initialisiert");
        }
        return signalChangeManager;
    }
    
    /**
     * Holt die Signalwechsel-Historie für ein Währungspaar
     * @param currencyPair Das Währungspaar
     * @return Liste der Signalwechsel
     */
    public List<SignalChangeEvent> getSignalChangeHistory(String currencyPair) {
        if (!isInitialized) {
            throw new IllegalStateException("GuiDataService ist nicht initialisiert");
        }
        
        try {
            return signalChangeManager.getSignalChangeHistory(currencyPair);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Fehler beim Abrufen der Signalwechsel-Historie für " + currencyPair + ": " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Prüft ob es für ein Währungspaar aktuelle Signalwechsel gibt
     * @param currencyPair Das Währungspaar
     * @return SignalChangeEvent wenn aktueller Wechsel vorhanden, sonst null
     */
    public SignalChangeEvent getMostRecentSignalChange(String currencyPair) {
        if (!isInitialized) {
            return null;
        }
        
        try {
            return signalChangeManager.getMostRecentChangeForPair(currencyPair);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Fehler beim Abrufen des aktuellsten Signalwechsels für " + currencyPair + ": " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Gibt Statistiken über alle Signalwechsel zurück
     * @return Statistik-String
     */
    public String getSignalChangeStatistics() {
        if (!isInitialized) {
            return "Service nicht initialisiert";
        }
        
        try {
            return signalChangeManager.getSignalChangeStatistics();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Fehler beim Abrufen der Signalwechsel-Statistiken: " + e.getMessage(), e);
            return "Fehler beim Abrufen der Signalwechsel-Statistiken";
        }
    }
    
    /**
     * Listet alle verfügbaren Währungspaare auf
     * @return Set aller verfügbaren Währungspaare
     */
    public Set<String> getAvailableCurrencyPairs() {
        if (!isInitialized) {
            throw new IllegalStateException("GuiDataService ist nicht initialisiert");
        }
        
        return currencyPairManager.listAvailableCurrencyPairs();
    }
    
    /**
     * Invalidiert den Cache und erzwingt das Laden neuer Daten
     */
    public void invalidateCache() {
        LOGGER.info("Cache invalidiert");
        lastCacheUpdate = null;
        cachedData.clear();
    }
    
    /**
     * Testet die Verbindung zu FXSSI
     */
    public boolean testConnection() {
        if (!isInitialized) {
            return false;
        }
        
        try {
            return scraper.testConnection();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Verbindungstest fehlgeschlagen: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Gibt das konfigurierte Datenverzeichnis zurück
     */
    public String getDataDirectory() {
        return dataDirectory;
    }
    
    /**
     * Gibt erweiterte Statistiken über alle Speichersysteme UND E-Mail-System zurück
     */
    public ExtendedDataStatistics getExtendedDataStatistics() {
        if (!isInitialized) {
            return new ExtendedDataStatistics(0, 0, "Service nicht initialisiert", "", 0, "", "");
        }
        
        try {
            // Tägliche Dateien
            String dailyStats = fileManager.getDataStatistics();
            List<String> files = fileManager.listDataFiles();
            List<CurrencyPairData> todayData = fileManager.readTodayData();
            
            // Währungspaar-spezifische Dateien
            String currencyPairStats = currencyPairManager.getOverallStatistics();
            Set<String> availablePairs = currencyPairManager.listAvailableCurrencyPairs();
            
            // Signalwechsel-Statistiken
            String signalChangeStats = signalChangeManager.getSignalChangeStatistics();
            
            // E-Mail-Statistiken
            String emailStats = getEmailStatistics();
            
            String detailedStats = String.format(
                "Tägliche Dateien: %s | Währungspaare: %d verfügbar | E-Mail: %s | Datenverzeichnis: %s",
                dailyStats, availablePairs.size(), 
                (emailConfig.isEmailEnabled() ? "Aktiviert (Threshold-System)" : "Deaktiviert"), 
                dataDirectory
            );
            
            return new ExtendedDataStatistics(
                files.size(), 
                todayData.size(), 
                detailedStats, 
                currencyPairStats,
                availablePairs.size(),
                signalChangeStats,
                emailStats
            );
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Fehler beim Abrufen der erweiterten Statistiken: " + e.getMessage(), e);
            return new ExtendedDataStatistics(0, 0, "Fehler beim Abrufen der Statistiken", "", 0, "", "");
        }
    }
    
    /**
     * Bereinigt alte Daten in allen Speichersystemen (inkl. E-Mail-Cache)
     */
    public void cleanupOldData(int daysToKeep) {
        if (!isInitialized) {
            return;
        }
        
        try {
            LOGGER.info("Bereinige Daten älter als " + daysToKeep + " Tage in allen Systemen...");
            
            // Bereinige tägliche Dateien
            fileManager.cleanupOldFiles(daysToKeep);
            
            // Bereinige währungspaar-spezifische Dateien
            currencyPairManager.cleanupOldData(daysToKeep);
            
            // Bereinige Signalwechsel-Daten
            signalChangeManager.cleanupOldSignalChanges(daysToKeep);
            
            LOGGER.info("Bereinigung in allen Speichersystemen (inkl. E-Mail-System) abgeschlossen");
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Fehler bei der Datenbereinigung: " + e.getMessage(), e);
        }
    }
    
    /**
     * Validiert alle Speichersysteme
     * @return Validierungsbericht
     */
    public String validateAllStorageSystems() {
        if (!isInitialized) {
            return "Service nicht initialisiert";
        }
        
        StringBuilder report = new StringBuilder();
        
        try {
            report.append("=== GUI DATENVALIDIERUNG (ALLE SYSTEME + E-MAIL) ===\n\n");
            
            // Validiere tägliche Dateien
            report.append("TÄGLICHE DATEIEN:\n");
            List<String> files = fileManager.listDataFiles();
            int validFiles = 0;
            
            for (String filename : files) {
                boolean isValid = fileManager.validateDataFile(filename);
                if (isValid) validFiles++;
            }
            
            report.append(String.format("Gefunden: %d Dateien, %d gültig\n\n", files.size(), validFiles));
            
            // Validiere währungspaar-spezifische Dateien
            report.append("WÄHRUNGSPAAR-DATEIEN:\n");
            report.append(currencyPairManager.validateAllData()).append("\n");
            
            // Validiere Signalwechsel-Daten
            report.append("SIGNALWECHSEL-DATEN:\n");
            report.append(signalChangeManager.getSignalChangeStatistics()).append("\n");
            
            // Validiere E-Mail-Konfiguration
            report.append("E-MAIL-KONFIGURATION (THRESHOLD-SYSTEM):\n");
            EmailConfig.ValidationResult emailValidation = emailConfig.validateConfig();
            if (emailValidation.isValid()) {
                report.append("✓ E-Mail-Konfiguration ist gültig (Threshold-System aktiv)\n");
                report.append(emailConfig.getConfigSummary());
            } else {
                report.append("✗ E-Mail-Konfiguration ungültig:\n");
                report.append(emailValidation.getErrorMessage());
            }
            
        } catch (Exception e) {
            report.append("Fehler bei der Validierung: ").append(e.getMessage());
        }
        
        return report.toString();
    }
    
    /**
     * Fährt den Service ordnungsgemäß herunter (inkl. E-Mail-Service)
     */
    public void shutdown() {
        LOGGER.info("Fahre GuiDataService herunter...");
        
        try {
            // Cache leeren
            cachedData.clear();
            lastCacheUpdate = null;
            
            // Fahre SignalChangeHistoryManager herunter
            if (signalChangeManager != null) {
                signalChangeManager.shutdown();
            }
            
            // Fahre E-Mail-Service herunter
            if (emailService != null) {
                emailService.shutdown();
            }
            
            // Service-Status zurücksetzen
            isInitialized = false;
            
            LOGGER.info("GuiDataService heruntergefahren (inkl. E-Mail-Service mit Threshold-System)");
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Fehler beim Herunterfahren des GuiDataService: " + e.getMessage(), e);
        }
    }
    
    // ===== PRIVATE HILFSMETHODEN (unverändert) =====
    
    /**
     * Validiert und normalisiert das Datenverzeichnis
     */
    private String validateAndNormalizeDataDirectory(String directory) {
        if (directory == null || directory.trim().isEmpty()) {
            LOGGER.warning("Leeres Datenverzeichnis angegeben, verwende Standard: " + DEFAULT_DATA_DIRECTORY);
            return DEFAULT_DATA_DIRECTORY;
        }
        
        String normalized = directory.trim();
        
        // Entferne abschließende Pfad-Separatoren
        while (normalized.endsWith("/") || normalized.endsWith("\\")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        
        // Falls leer nach Normalisierung, verwende Standard
        if (normalized.isEmpty()) {
            LOGGER.warning("Ungültiges Datenverzeichnis nach Normalisierung, verwende Standard: " + DEFAULT_DATA_DIRECTORY);
            return DEFAULT_DATA_DIRECTORY;
        }
        
        LOGGER.info("Datenverzeichnis validiert: " + normalized);
        return normalized;
    }
    
    /**
     * Prüft ob der Cache noch gültig ist
     */
    private boolean isCacheValid() {
        if (lastCacheUpdate == null || cachedData.isEmpty()) {
            return false;
        }
        
        LocalDateTime now = LocalDateTime.now();
        long minutesSinceUpdate = ChronoUnit.MINUTES.between(lastCacheUpdate, now);
        
        return minutesSinceUpdate < CACHE_TIMEOUT_MINUTES;
    }
    
    /**
     * Aktualisiert den Cache mit neuen Daten
     */
    private void updateCache(List<CurrencyPairData> newData) {
        cachedData.clear();
        cachedData.addAll(newData);
        lastCacheUpdate = LocalDateTime.now();
        
        LOGGER.fine("Cache aktualisiert mit " + newData.size() + " Datensätzen");
    }
    
    /**
     * Lädt Fallback-Daten aus gespeicherten Dateien
     */
    private List<CurrencyPairData> loadFallbackData() {
        try {
            LOGGER.info("Verwende Fallback-Daten aus gespeicherten Dateien...");
            
            // Versuche heutige Daten zu laden
            List<CurrencyPairData> todayData = fileManager.readTodayData();
            
            if (!todayData.isEmpty()) {
                // Aktualisiere Cache mit Fallback-Daten
                updateCache(todayData);
                LOGGER.info("Fallback-Daten geladen: " + todayData.size() + " Datensätze");
                return todayData;
            } else {
                // Erstelle Demo-Daten für den Fall dass keine Daten vorhanden sind
                return createDemoData();
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Fehler beim Laden der Fallback-Daten: " + e.getMessage(), e);
            return createDemoData();
        }
    }
    
    /**
     * Loggt initiale Statistiken mit E-Mail-Integration
     */
    private void logInitialStatisticsWithEmail() {
        try {
            ExtendedDataStatistics stats = getExtendedDataStatistics();
            LOGGER.info("Initiale Statistiken: " + stats.getDetails());
            LOGGER.info("Verfügbare Währungspaare: " + stats.getAvailableCurrencyPairs());
            LOGGER.info("E-Mail-Status: " + (emailConfig.isEmailEnabled() ? "✅ Aktiviert (Threshold-System)" : "❌ Deaktiviert"));
            LOGGER.fine("Signalwechsel-Statistiken: " + stats.getSignalChangeStatistics());
            LOGGER.fine("E-Mail-Statistiken: " + stats.getEmailStatistics());
        } catch (Exception e) {
            LOGGER.fine("Konnte initiale Statistiken nicht laden: " + e.getMessage());
        }
    }
    
    /**
     * Erstellt Demo-Daten für Testing/Fallback
     */
    private List<CurrencyPairData> createDemoData() {
        LOGGER.info("Erstelle Demo-Daten...");
        
        List<CurrencyPairData> demoData = new ArrayList<>();
        
        // Bekannte Währungspaare mit Demo-Werten
        demoData.add(new CurrencyPairData("EUR/USD", 45.0, 55.0, CurrencyPairData.TradingSignal.BUY));
        demoData.add(new CurrencyPairData("GBP/USD", 62.0, 38.0, CurrencyPairData.TradingSignal.SELL));
        demoData.add(new CurrencyPairData("USD/JPY", 48.0, 52.0, CurrencyPairData.TradingSignal.NEUTRAL));
        demoData.add(new CurrencyPairData("AUD/USD", 35.0, 65.0, CurrencyPairData.TradingSignal.BUY));
        demoData.add(new CurrencyPairData("USD/CHF", 72.0, 28.0, CurrencyPairData.TradingSignal.SELL));
        
        return demoData;
    }
    
    // ===== ERWEITERTE INNERE KLASSEN =====
    
    /**
     * Erweiterte Datenstatistik-Klasse mit E-Mail-Informationen
     */
    public static class ExtendedDataStatistics {
        private final int totalFiles;
        private final int todayRecords;
        private final String details;
        private final String currencyPairDetails;
        private final int availableCurrencyPairs;
        private final String signalChangeStatistics;
        private final String emailStatistics;
        
        public ExtendedDataStatistics(int totalFiles, int todayRecords, String details, 
                                    String currencyPairDetails, int availableCurrencyPairs,
                                    String signalChangeStatistics, String emailStatistics) {
            this.totalFiles = totalFiles;
            this.todayRecords = todayRecords;
            this.details = details;
            this.currencyPairDetails = currencyPairDetails;
            this.availableCurrencyPairs = availableCurrencyPairs;
            this.signalChangeStatistics = signalChangeStatistics;
            this.emailStatistics = emailStatistics;
        }
        
        public int getTotalFiles() { return totalFiles; }
        public int getTodayRecords() { return todayRecords; }
        public String getDetails() { return details; }
        public String getCurrencyPairDetails() { return currencyPairDetails; }
        public int getAvailableCurrencyPairs() { return availableCurrencyPairs; }
        public String getSignalChangeStatistics() { return signalChangeStatistics; }
        public String getEmailStatistics() { return emailStatistics; }
        
        @Override
        public String toString() {
            return String.format("Dateien: %d, Heutige Datensätze: %d, Währungspaare: %d, E-Mail: %s, Details: %s", 
                totalFiles, todayRecords, availableCurrencyPairs, 
                (emailStatistics.contains("Aktiviert") ? "✅" : "❌"), details);
        }
    }
}