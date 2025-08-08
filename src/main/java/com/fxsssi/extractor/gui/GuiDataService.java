package com.fxsssi.extractor.gui;



import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fxssi.extractor.model.CurrencyPairData;
import com.fxssi.extractor.scraper.FXSSIScraper;
import com.fxssi.extractor.storage.DataFileManager;

/**
 * Service-Klasse für die Bereitstellung von FXSSI-Daten für die GUI
 * Integriert die bestehenden Scraper- und Storage-Komponenten
 * 
 * @author Generated for FXSSI Data Extraction GUI
 * @version 1.0
 */
public class GuiDataService {
    
    private static final Logger LOGGER = Logger.getLogger(GuiDataService.class.getName());
    private static final String DATA_DIRECTORY = "data";
    private static final int CACHE_TIMEOUT_MINUTES = 2; // Cache-Timeout in Minuten
    
    private FXSSIScraper scraper;
    private DataFileManager fileManager;
    private List<CurrencyPairData> cachedData;
    private LocalDateTime lastCacheUpdate;
    private boolean isInitialized = false;
    
    /**
     * Konstruktor
     */
    public GuiDataService() {
        this.cachedData = new ArrayList<>();
        LOGGER.info("GuiDataService erstellt");
    }
    
    /**
     * Initialisiert den Datenservice
     */
    public void initialize() {
        try {
            LOGGER.info("Initialisiere GuiDataService...");
            
            // Initialisiere Komponenten
            scraper = new FXSSIScraper();
            fileManager = new DataFileManager(DATA_DIRECTORY);
            
            // Erstelle Datenverzeichnis
            fileManager.createDataDirectory();
            
            // Teste Verbindung
            boolean connectionOk = scraper.testConnection();
            if (!connectionOk) {
                LOGGER.warning("Verbindung zu FXSSI fehlgeschlagen - verwende gespeicherte Daten");
            }
            
            isInitialized = true;
            LOGGER.info("GuiDataService erfolgreich initialisiert");
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Fehler beim Initialisieren des GuiDataService: " + e.getMessage(), e);
            throw new RuntimeException("GuiDataService konnte nicht initialisiert werden", e);
        }
    }
    
    /**
     * Holt aktuelle Daten (mit Caching für Performance)
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
        
        LOGGER.info("Lade frische Daten...");
        
        try {
            // Versuche neue Daten zu laden
            List<CurrencyPairData> freshData = scraper.extractCurrentRatioData();
            
            if (freshData != null && !freshData.isEmpty()) {
                // Speichere Daten
                fileManager.appendDataToFile(freshData);
                
                // Aktualisiere Cache
                updateCache(freshData);
                
                LOGGER.info("Erfolgreich " + freshData.size() + " aktuelle Datensätze geladen");
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
     * Holt historische Daten für einen bestimmten Zeitraum
     */
    public List<CurrencyPairData> getHistoricalData(int daysBack) {
        if (!isInitialized) {
            throw new IllegalStateException("GuiDataService ist nicht initialisiert");
        }
        
        List<CurrencyPairData> historicalData = new ArrayList<>();
        
        try {
            LOGGER.info("Lade historische Daten für " + daysBack + " Tage...");
            
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
     * Gibt Statistiken über gespeicherte Daten zurück
     */
    public DataStatistics getDataStatistics() {
        if (!isInitialized) {
            return new DataStatistics(0, 0, "Service nicht initialisiert");
        }
        
        try {
            String stats = fileManager.getDataStatistics();
            List<String> files = fileManager.listDataFiles();
            List<CurrencyPairData> todayData = fileManager.readTodayData();
            
            return new DataStatistics(files.size(), todayData.size(), stats);
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Fehler beim Abrufen der Statistiken: " + e.getMessage(), e);
            return new DataStatistics(0, 0, "Fehler beim Abrufen der Statistiken");
        }
    }
    
    /**
     * Bereinigt alte Datendateien
     */
    public void cleanupOldData(int daysToKeep) {
        if (!isInitialized) {
            return;
        }
        
        try {
            LOGGER.info("Bereinige Daten älter als " + daysToKeep + " Tage...");
            fileManager.cleanupOldFiles(daysToKeep);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Fehler bei der Datenbereinigung: " + e.getMessage(), e);
        }
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
            
            // Service-Status zurücksetzen
            isInitialized = false;
            
            LOGGER.info("GuiDataService heruntergefahren");
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Fehler beim Herunterfahren des GuiDataService: " + e.getMessage(), e);
        }
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
    
    /**
     * Datenstatistik-Klasse
     */
    public static class DataStatistics {
        private final int totalFiles;
        private final int todayRecords;
        private final String details;
        
        public DataStatistics(int totalFiles, int todayRecords, String details) {
            this.totalFiles = totalFiles;
            this.todayRecords = todayRecords;
            this.details = details;
        }
        
        public int getTotalFiles() { return totalFiles; }
        public int getTodayRecords() { return todayRecords; }
        public String getDetails() { return details; }
        
        @Override
        public String toString() {
            return String.format("Dateien: %d, Heutige Datensätze: %d, Details: %s", 
                totalFiles, todayRecords, details);
        }
    }
}