package com.fxssi.extractor.storage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fxssi.extractor.model.CurrencyPairData;
import com.fxssi.extractor.model.CurrencyPairData.TradingSignal;

/**
 * Manager für die Verwaltung der zuletzt gesendeten E-Mail-Signale
 * Verhindert E-Mail-Spam durch Tracking der letzten gesendeten Signale pro Währungspaar
 * Verwendet einen konfigurierbaren Threshold um zu entscheiden ob eine neue E-Mail gesendet werden soll
 * 
 * @author Generated for FXSSI Email Anti-Spam System
 * @version 1.0
 */
public class LastSentSignalManager {
    
    private static final Logger LOGGER = Logger.getLogger(LastSentSignalManager.class.getName());
    private static final String SIGNAL_CHANGES_SUBDIRECTORY = "signal_changes";
    private static final String LAST_SENT_FILE = "lastsend.csv";
    
    private final String dataDirectory;
    private final Path signalChangesPath;
    private final Path lastSentFilePath;
    private final ReentrantLock managerLock = new ReentrantLock();
    
    // Cache für zuletzt gesendete Signale pro Währungspaar
    private final ConcurrentHashMap<String, LastSentSignal> lastSentSignals;
    
    /**
     * Konstruktor mit Standard-Datenverzeichnis
     */
    public LastSentSignalManager() {
        this("data");
    }
    
    /**
     * Konstruktor mit konfigurierbarem Datenverzeichnis
     * @param dataDirectory Pfad zum Hauptdatenverzeichnis
     */
    public LastSentSignalManager(String dataDirectory) {
        this.dataDirectory = dataDirectory;
        this.signalChangesPath = Paths.get(dataDirectory, SIGNAL_CHANGES_SUBDIRECTORY);
        this.lastSentFilePath = signalChangesPath.resolve(LAST_SENT_FILE);
        
        this.lastSentSignals = new ConcurrentHashMap<>();
        
        LOGGER.info("LastSentSignalManager initialisiert für Verzeichnis: " + dataDirectory);
        LOGGER.info("Letzte gesendete Signale werden gespeichert in: " + lastSentFilePath.toAbsolutePath());
    }
    
    /**
     * Erstellt das Verzeichnis für Signal-Dateien falls es nicht existiert
     */
    public void createSignalChangesDirectory() {
        try {
            if (!Files.exists(signalChangesPath)) {
                Files.createDirectories(signalChangesPath);
                LOGGER.info("Signal-Verzeichnis erstellt: " + signalChangesPath.toAbsolutePath());
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Fehler beim Erstellen des Signal-Verzeichnisses: " + e.getMessage(), e);
            throw new RuntimeException("Konnte Signal-Verzeichnis nicht erstellen", e);
        }
    }
    
    /**
     * Lädt die zuletzt gesendeten Signale beim Start
     */
    public void loadLastSentSignals() {
        managerLock.lock();
        try {
            createSignalChangesDirectory();
            
            if (!Files.exists(lastSentFilePath)) {
                LOGGER.info("Keine gespeicherten letzten gesendeten Signale gefunden - beginne mit leerer Liste");
                return;
            }
            
            try (BufferedReader reader = Files.newBufferedReader(lastSentFilePath, StandardCharsets.UTF_8)) {
                String line;
                boolean isFirstLine = true;
                
                while ((line = reader.readLine()) != null) {
                    if (isFirstLine) {
                        isFirstLine = false;
                        continue; // Überspringe Header
                    }
                    
                    try {
                        LastSentSignal lastSent = LastSentSignal.fromCsvLine(line);
                        lastSentSignals.put(lastSent.getCurrencyPair(), lastSent);
                    } catch (Exception e) {
                        LOGGER.fine("Ungültige LastSent-Zeile übersprungen: " + line);
                    }
                }
                
                LOGGER.info("Letzte gesendete Signale geladen: " + lastSentSignals.size() + " Währungspaare");
                
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Fehler beim Laden der letzten gesendeten Signale: " + e.getMessage(), e);
            }
            
        } finally {
            managerLock.unlock();
        }
    }
    
    /**
     * HAUPTMETHODE: Prüft ob eine E-Mail für ein neues Signal gesendet werden soll
     * @param currencyPair Das Währungspaar
     * @param newSignal Das neue Signal
     * @param newBuyPercentage Die neue Buy-Percentage
     * @param thresholdPercent Der konfigurierte Threshold in Prozent
     * @return true wenn E-Mail gesendet werden soll, false wenn Threshold nicht erreicht
     */
    public boolean shouldSendEmail(String currencyPair, 
                                 CurrencyPairData.TradingSignal newSignal, 
                                 double newBuyPercentage, 
                                 double thresholdPercent) {
        managerLock.lock();
        try {
            LastSentSignal lastSent = lastSentSignals.get(currencyPair);
            
            // Wenn noch nie eine E-Mail gesendet wurde, sende erste E-Mail
            if (lastSent == null) {
                LOGGER.info(String.format("Erste E-Mail für %s: %s bei %.1f%%", 
                    currencyPair, newSignal.getDescription(), newBuyPercentage));
                return true;
            }
            
            // Wenn gleiches Signal wie zuletzt gesendet, keine E-Mail
            if (lastSent.getSignal() == newSignal) {
                LOGGER.fine(String.format("Gleiches Signal für %s: %s - keine E-Mail", 
                    currencyPair, newSignal.getDescription()));
                return false;
            }
            
            // Wenn Signal gewechselt hat, prüfe Threshold
            double percentageDifference = Math.abs(newBuyPercentage - lastSent.getBuyPercentage());
            
            if (percentageDifference >= thresholdPercent) {
                LOGGER.info(String.format("Threshold erreicht für %s: %.1f%% Differenz (>= %.1f%%) - %s → %s", 
                    currencyPair, percentageDifference, thresholdPercent,
                    lastSent.getSignal().getDescription(), newSignal.getDescription()));
                return true;
            } else {
                LOGGER.fine(String.format("Threshold NICHT erreicht für %s: %.1f%% Differenz (< %.1f%%) - keine E-Mail", 
                    currencyPair, percentageDifference, thresholdPercent));
                return false;
            }
            
        } finally {
            managerLock.unlock();
        }
    }
    
    /**
     * Registriert ein gesendetes Signal (nach erfolgreichem E-Mail-Versand)
     * @param currencyPair Das Währungspaar
     * @param signal Das gesendete Signal
     * @param buyPercentage Die Buy-Percentage zum Zeitpunkt des Sendens
     */
    public void recordSentSignal(String currencyPair, 
                               CurrencyPairData.TradingSignal signal, 
                               double buyPercentage) {
        managerLock.lock();
        try {
            LocalDateTime now = LocalDateTime.now();
            LastSentSignal lastSent = new LastSentSignal(currencyPair, signal, buyPercentage, now);
            
            lastSentSignals.put(currencyPair, lastSent);
            
            // Speichere sofort
            saveLastSentSignals();
            
            LOGGER.info(String.format("Gesendetes Signal registriert: %s %s bei %.1f%% um %s", 
                currencyPair, signal.getDescription(), buyPercentage, now.format(DateTimeFormatter.ofPattern("HH:mm:ss"))));
            
        } finally {
            managerLock.unlock();
        }
    }
    
    /**
     * Holt das zuletzt gesendete Signal für ein Währungspaar
     * @param currencyPair Das Währungspaar
     * @return LastSentSignal oder null wenn noch nichts gesendet wurde
     */
    public LastSentSignal getLastSentSignal(String currencyPair) {
        return lastSentSignals.get(currencyPair);
    }
    
    /**
     * Gibt Statistiken über gesendete E-Mails zurück
     */
    public String getStatistics() {
        StringBuilder stats = new StringBuilder();
        stats.append("LastSent Signal-Statistiken:\n");
        stats.append("============================\n");
        stats.append("Überwachte Währungspaare: ").append(lastSentSignals.size()).append("\n\n");
        
        if (!lastSentSignals.isEmpty()) {
            stats.append("Letzte gesendete Signale:\n");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
            
            lastSentSignals.entrySet().stream()
                .sorted((a, b) -> b.getValue().getSentTime().compareTo(a.getValue().getSentTime()))
                .forEach(entry -> {
                    LastSentSignal signal = entry.getValue();
                    stats.append(String.format("  %s: %s (%.1f%%) - %s\n",
                        entry.getKey(),
                        signal.getSignal().getDescription(),
                        signal.getBuyPercentage(),
                        signal.getSentTime().format(formatter)));
                });
        }
        
        return stats.toString();
    }
    
    /**
     * Bereinigt alte LastSent-Einträge (optional für Wartung)
     * @param daysToKeep Anzahl Tage die behalten werden sollen
     */
    public void cleanupOldEntries(int daysToKeep) {
        managerLock.lock();
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);
            
            int removedCount = 0;
            var iterator = lastSentSignals.entrySet().iterator();
            
            while (iterator.hasNext()) {
                var entry = iterator.next();
                if (entry.getValue().getSentTime().isBefore(cutoffDate)) {
                    iterator.remove();
                    removedCount++;
                }
            }
            
            if (removedCount > 0) {
                saveLastSentSignals();
                LOGGER.info("LastSent-Bereinigung abgeschlossen: " + removedCount + " alte Einträge entfernt");
            }
            
        } finally {
            managerLock.unlock();
        }
    }
    
    /**
     * Fährt den Manager ordnungsgemäß herunter
     */
    public void shutdown() {
        LOGGER.info("Fahre LastSentSignalManager herunter...");
        
        try {
            // Speichere letzte Zustände
            saveLastSentSignals();
            
            // Cache leeren
            lastSentSignals.clear();
            
            LOGGER.info("LastSentSignalManager heruntergefahren");
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Fehler beim Herunterfahren des LastSentSignalManager: " + e.getMessage(), e);
        }
    }
    
    // ===== PRIVATE HILFSMETHODEN =====
    
    /**
     * Speichert die zuletzt gesendeten Signale in die Datei
     */
    private void saveLastSentSignals() {
        try {
            createSignalChangesDirectory();
            
            try (BufferedWriter writer = Files.newBufferedWriter(lastSentFilePath, StandardCharsets.UTF_8)) {
                writer.write(LastSentSignal.getCsvHeader());
                writer.newLine();
                
                for (LastSentSignal signal : lastSentSignals.values()) {
                    writer.write(signal.toCsvLine());
                    writer.newLine();
                }
                
                writer.flush();
            }
            
            LOGGER.fine("Letzte gesendete Signale gespeichert: " + lastSentSignals.size() + " Einträge");
            
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Fehler beim Speichern der letzten gesendeten Signale: " + e.getMessage(), e);
        }
    }
    
    // ===== INNERE KLASSEN =====
    
    /**
     * Datenklasse für zuletzt gesendete Signale
     */
    public static class LastSentSignal {
        private final String currencyPair;
        private final CurrencyPairData.TradingSignal signal;
        private final double buyPercentage;
        private final LocalDateTime sentTime;
        
        public LastSentSignal(String currencyPair, CurrencyPairData.TradingSignal signal, 
                            double buyPercentage, LocalDateTime sentTime) {
            this.currencyPair = currencyPair;
            this.signal = signal;
            this.buyPercentage = buyPercentage;
            this.sentTime = sentTime;
        }
        
        /**
         * Erstellt LastSentSignal aus CSV-Zeile
         */
        public static LastSentSignal fromCsvLine(String csvLine) {
            String[] parts = csvLine.split(";");
            if (parts.length != 4) {
                throw new IllegalArgumentException("Ungültiges LastSent CSV-Format: " + csvLine);
            }
            
            try {
                String currencyPair = parts[0];
                CurrencyPairData.TradingSignal signal = CurrencyPairData.TradingSignal.valueOf(parts[1]);
                double buyPercentage = Double.parseDouble(parts[2].replace(",", "."));
                LocalDateTime sentTime = LocalDateTime.parse(parts[3], DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                
                return new LastSentSignal(currencyPair, signal, buyPercentage, sentTime);
            } catch (Exception e) {
                throw new IllegalArgumentException("Fehler beim Parsen der LastSent CSV-Zeile: " + csvLine, e);
            }
        }
        
        /**
         * Formatiert als CSV-Zeile für Speicherung
         */
        public String toCsvLine() {
            return String.format("%s;%s;%.2f;%s",
                currencyPair,
                signal.name(),
                buyPercentage,
                sentTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        
        /**
         * CSV-Header für Dateiexport
         */
        public static String getCsvHeader() {
            return "Währungspaar;Signal;Buy_Prozent;Zeitstempel";
        }
        
        // Getter
        public String getCurrencyPair() { return currencyPair; }
        public CurrencyPairData.TradingSignal getSignal() { return signal; }
        public double getBuyPercentage() { return buyPercentage; }
        public LocalDateTime getSentTime() { return sentTime; }
        
        @Override
        public String toString() {
            return String.format("LastSentSignal{%s: %s (%.1f%%) - %s}", 
                currencyPair, signal.getDescription(), buyPercentage, 
                sentTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));
        }
    }
}