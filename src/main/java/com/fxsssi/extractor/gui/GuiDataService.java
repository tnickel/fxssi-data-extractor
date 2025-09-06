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

/**
 * Service-Klasse für die Bereitstellung von FXSSI-Daten für die GUI
 * Integriert die bestehenden Scraper- und Storage-Komponenten mit konfigurierbarem Datenverzeichnis
 * Jetzt mit dreifacher Speicherung: tägliche UND währungspaar-spezifische Dateien UND Signalwechsel-Erkennung
 * 
 * @author Generated for FXSSI Data Extraction GUI
 * @version 1.3 (mit vollständiger SignalChangeHistoryManager Integration)
 */
public class GuiDataService {
    
    private static final Logger LOGGER = Logger.getLogger(GuiDataService.class.getName());
    private static final String DEFAULT_DATA_DIRECTORY = "data";
    private static final int CACHE_TIMEOUT_MINUTES = 2; // Cache-Timeout in Minuten
    
    private FXSSIScraper scraper;
    private DataFileManager fileManager;
    private CurrencyPairDataManager currencyPairManager;
    private SignalChangeHistoryManager signalChangeManager;
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
        LOGGER.info("Dreifache Speicherung aktiviert: Tägliche + Währungspaar-spezifische + Signalwechsel-Dateien");
    }
    
    /**
     * Initialisiert den Datenservice
     */
    public void initialize() {
        try {
            LOGGER.info("Initialisiere GuiDataService...");
            LOGGER.info("Datenverzeichnis: " + dataDirectory);
            
            // Initialisiere Komponenten mit konfiguriertem Datenverzeichnis
            scraper = new FXSSIScraper(dataDirectory);
            fileManager = new DataFileManager(dataDirectory);
            currencyPairManager = new CurrencyPairDataManager(dataDirectory);
            signalChangeManager = new SignalChangeHistoryManager(dataDirectory);
            
            // Erstelle Datenverzeichnisse
            fileManager.createDataDirectory();
            currencyPairManager.createCurrencyDataDirectory();
            signalChangeManager.createSignalChangesDirectory();
            
            // Lade letzte bekannte Signale für Wechsel-Erkennung
            signalChangeManager.loadLastKnownSignals();
            
            // Teste Verbindung
            boolean connectionOk = scraper.testConnection();
            if (!connectionOk) {
                LOGGER.warning("Verbindung zu FXSSI fehlgeschlagen - verwende gespeicherte Daten");
            }
            
            isInitialized = true;
            LOGGER.info("GuiDataService erfolgreich initialisiert mit dreifacher Speicherung UND Signalwechsel-Erkennung");
            
            // Logge initiale Statistiken
            logInitialStatistics();
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Fehler beim Initialisieren des GuiDataService: " + e.getMessage(), e);
            throw new RuntimeException("GuiDataService konnte nicht initialisiert werden", e);
        }
    }
    
    /**
     * Holt aktuelle Daten (mit Caching für Performance)
     * WICHTIG: Speichert bei jedem Refresh in ALLE DREI Speichersysteme UND erkennt Signalwechsel
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
        
        LOGGER.info("Lade frische Daten für GUI-Refresh...");
        
        try {
            // Versuche neue Daten zu laden
            List<CurrencyPairData> freshData = scraper.extractCurrentRatioData();
            
            if (freshData != null && !freshData.isEmpty()) {
                // SPEICHERE IN ALLE DREI SYSTEME bei jedem Refresh
                saveToAllStorageSystems(freshData);
                
                // Aktualisiere Cache
                updateCache(freshData);
                
                LOGGER.info("GUI-Refresh: " + freshData.size() + " Datensätze geladen und in alle Speichersysteme gespeichert");
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
     * WICHTIG: Speichert auch bei asynchronen Loads in alle Systeme
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
     * GARANTIERT Speicherung in alle Systeme UND Signalwechsel-Erkennung
     */
    public List<CurrencyPairData> forceDataRefresh() throws Exception {
        LOGGER.info("Erzwinge manuelle Datenaktualisierung...");
        
        // Invalidiere Cache
        invalidateCache();
        
        // Lade neue Daten
        List<CurrencyPairData> freshData = scraper.extractCurrentRatioData();
        
        if (freshData != null && !freshData.isEmpty()) {
            // GARANTIERTE Speicherung in alle Systeme
            saveToAllStorageSystems(freshData);
            
            // Aktualisiere Cache
            updateCache(freshData);
            
            LOGGER.info("Manuelle Aktualisierung: " + freshData.size() + " Datensätze in alle Speichersysteme gespeichert");
            return freshData;
        } else {
            LOGGER.warning("Manuelle Aktualisierung: Keine neuen Daten erhalten");
            return loadFallbackData();
        }
    }
    
    /**
     * ZENTRALE METHODE: Speichert Daten in ALLE DREI Speichersysteme UND erkennt Signalwechsel
     * @param data Die zu speichernden Daten
     */
    private void saveToAllStorageSystems(List<CurrencyPairData> data) {
        if (data == null || data.isEmpty()) {
            LOGGER.warning("Keine Daten zum Speichern in allen Systemen");
            return;
        }
        
        try {
            LOGGER.fine("Speichere " + data.size() + " Datensätze in alle Speichersysteme und erkenne Signalwechsel...");
            
            // 1. ERKENNE SIGNALWECHSEL (vor der Speicherung!)
            List<SignalChangeEvent> detectedChanges = signalChangeManager.processNewData(data);
            
            if (!detectedChanges.isEmpty()) {
                LOGGER.info("🚨 SIGNALWECHSEL ERKANNT: " + detectedChanges.size() + " Wechsel bei diesem Refresh!");
                
                for (SignalChangeEvent change : detectedChanges) {
                    LOGGER.info(String.format("   - %s: %s (Wichtigkeit: %s, Aktualität: %s)", 
                        change.getCurrencyPair(), 
                        change.getChangeDescription(),
                        change.getImportance().getDescription(),
                        change.getActuality().getDescription()
                    ));
                }
            }
            
            // 2. SPEICHERE in tägliche Dateien
            fileManager.appendDataToFile(data);
            LOGGER.fine("✓ Daten in tägliche Datei gespeichert");
            
            // 3. SPEICHERE in währungspaar-spezifische Dateien
            currencyPairManager.appendDataForAllPairs(data);
            LOGGER.fine("✓ Daten in währungspaar-spezifische Dateien gespeichert");
            
            // 4. Signalwechsel sind bereits durch processNewData() gespeichert
            LOGGER.fine("✓ Signalwechsel erkannt und gespeichert");
            
            String logMessage = String.format("Erfolgreich %d Datensätze in ALLE DREI Speichersysteme gespeichert", data.size());
            if (!detectedChanges.isEmpty()) {
                logMessage += String.format(" | 🔄 %d Signalwechsel erkannt", detectedChanges.size());
            }
            LOGGER.info(logMessage);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Fehler beim Speichern in alle Systeme: " + e.getMessage(), e);
            // Werfe Exception nicht weiter, da GUI weiter funktionieren soll
        }
    }
    
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
     * Prüft ob es aktuelle Signalwechsel für ein Währungspaar gibt
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
     * Gibt erweiterte Statistiken über alle Speichersysteme zurück
     */
    public ExtendedDataStatistics getExtendedDataStatistics() {
        if (!isInitialized) {
            return new ExtendedDataStatistics(0, 0, "Service nicht initialisiert", "", 0, "");
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
            
            String detailedStats = String.format(
                "Tägliche Dateien: %s | Währungspaare: %d verfügbar | Datenverzeichnis: %s",
                dailyStats, availablePairs.size(), dataDirectory
            );
            
            return new ExtendedDataStatistics(
                files.size(), 
                todayData.size(), 
                detailedStats, 
                currencyPairStats,
                availablePairs.size(),
                signalChangeStats
            );
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Fehler beim Abrufen der erweiterten Statistiken: " + e.getMessage(), e);
            return new ExtendedDataStatistics(0, 0, "Fehler beim Abrufen der Statistiken", "", 0, "");
        }
    }
    
    /**
     * Bereinigt alte Daten in allen Speichersystemen
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
            
            LOGGER.info("Bereinigung in allen Speichersystemen abgeschlossen");
            
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
            report.append("=== GUI DATENVALIDIERUNG (ALLE SYSTEME) ===\n\n");
            
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
            report.append(signalChangeManager.getSignalChangeStatistics());
            
        } catch (Exception e) {
            report.append("Fehler bei der Validierung: ").append(e.getMessage());
        }
        
        return report.toString();
    }
    
    /**
     * Fährt den Service ordnungsgemäß herunter
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
            
            // Service-Status zurücksetzen
            isInitialized = false;
            
            LOGGER.info("GuiDataService heruntergefahren");
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Fehler beim Herunterfahren des GuiDataService: " + e.getMessage(), e);
        }
    }
    
    // ===== PRIVATE HILFSMETHODEN =====
    
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
     * Loggt initiale Statistiken
     */
    private void logInitialStatistics() {
        try {
            ExtendedDataStatistics stats = getExtendedDataStatistics();
            LOGGER.info("Initiale Statistiken: " + stats.getDetails());
            LOGGER.info("Verfügbare Währungspaare: " + stats.getAvailableCurrencyPairs());
            LOGGER.fine("Signalwechsel-Statistiken: " + stats.getSignalChangeStatistics());
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
    
    // ===== INNERE KLASSEN =====
    
    /**
     * Erweiterte Datenstatistik-Klasse mit Informationen über alle Speichersysteme
     */
    public static class ExtendedDataStatistics {
        private final int totalFiles;
        private final int todayRecords;
        private final String details;
        private final String currencyPairDetails;
        private final int availableCurrencyPairs;
        private final String signalChangeStatistics;
        
        public ExtendedDataStatistics(int totalFiles, int todayRecords, String details, 
                                    String currencyPairDetails, int availableCurrencyPairs,
                                    String signalChangeStatistics) {
            this.totalFiles = totalFiles;
            this.todayRecords = todayRecords;
            this.details = details;
            this.currencyPairDetails = currencyPairDetails;
            this.availableCurrencyPairs = availableCurrencyPairs;
            this.signalChangeStatistics = signalChangeStatistics;
        }
        
        public int getTotalFiles() { return totalFiles; }
        public int getTodayRecords() { return todayRecords; }
        public String getDetails() { return details; }
        public String getCurrencyPairDetails() { return currencyPairDetails; }
        public int getAvailableCurrencyPairs() { return availableCurrencyPairs; }
        public String getSignalChangeStatistics() { return signalChangeStatistics; }
        
        @Override
        public String toString() {
            return String.format("Dateien: %d, Heutige Datensätze: %d, Währungspaare: %d, Details: %s", 
                totalFiles, todayRecords, availableCurrencyPairs, details);
        }
    }
}