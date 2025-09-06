package com.fxssi.extractor.storage;

import com.fxssi.extractor.model.CurrencyPairData;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Manager für die Verwaltung von Währungspaar-spezifischen Datendateien
 * Erstellt für jedes Währungspaar eine eigene CSV-Datei zur kontinuierlichen Datenspeicherung
 * 
 * @author Generated for FXSSI Data Extraction
 * @version 1.0
 */
public class CurrencyPairDataManager {
    
    private static final Logger LOGGER = Logger.getLogger(CurrencyPairDataManager.class.getName());
    private static final String FILE_EXTENSION = ".csv";
    private static final String CURRENCY_DATA_SUBDIRECTORY = "currency_pairs";
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private final String dataDirectory;
    private final Path currencyDataPath;
    private final ConcurrentHashMap<String, ReentrantLock> fileLocks;
    private final ReentrantLock managerLock = new ReentrantLock();
    
    /**
     * Konstruktor mit Standard-Datenverzeichnis
     */
    public CurrencyPairDataManager() {
        this("data");
    }
    
    /**
     * Konstruktor mit konfigurierbarem Datenverzeichnis
     * @param dataDirectory Pfad zum Hauptdatenverzeichnis
     */
    public CurrencyPairDataManager(String dataDirectory) {
        this.dataDirectory = dataDirectory;
        this.currencyDataPath = Paths.get(dataDirectory, CURRENCY_DATA_SUBDIRECTORY);
        this.fileLocks = new ConcurrentHashMap<>();
        
        LOGGER.info("CurrencyPairDataManager initialisiert für Verzeichnis: " + dataDirectory);
        LOGGER.info("Währungspaar-Dateien werden gespeichert in: " + currencyDataPath.toAbsolutePath());
    }
    
    /**
     * Erstellt das Verzeichnis für Währungspaar-Dateien falls es nicht existiert
     */
    public void createCurrencyDataDirectory() {
        try {
            if (!Files.exists(currencyDataPath)) {
                Files.createDirectories(currencyDataPath);
                LOGGER.info("Währungspaar-Datenverzeichnis erstellt: " + currencyDataPath.toAbsolutePath());
            } else {
                LOGGER.info("Währungspaar-Datenverzeichnis existiert bereits: " + currencyDataPath.toAbsolutePath());
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Fehler beim Erstellen des Währungspaar-Verzeichnisses: " + e.getMessage(), e);
            throw new RuntimeException("Konnte Währungspaar-Verzeichnis nicht erstellen", e);
        }
    }
    
    /**
     * Speichert neue Daten für alle Währungspaare
     * @param currencyDataList Liste der zu speichernden Währungsdaten
     */
    public void appendDataForAllPairs(List<CurrencyPairData> currencyDataList) {
        if (currencyDataList == null || currencyDataList.isEmpty()) {
            LOGGER.warning("Keine Daten zum Speichern erhalten");
            return;
        }
        
        LOGGER.info("Beginne Speicherung von " + currencyDataList.size() + " Währungspaaren in separate Dateien");
        
        int successCount = 0;
        int errorCount = 0;
        
        for (CurrencyPairData currencyData : currencyDataList) {
            try {
                appendDataForSinglePair(currencyData);
                successCount++;
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Fehler beim Speichern von " + currencyData.getCurrencyPair() + ": " + e.getMessage(), e);
                errorCount++;
            }
        }
        
        LOGGER.info("Speicherung abgeschlossen: " + successCount + " erfolgreich, " + errorCount + " Fehler");
    }
    
    /**
     * Speichert Daten für ein einzelnes Währungspaar
     * @param currencyData Die zu speichernden Währungsdaten
     */
    public void appendDataForSinglePair(CurrencyPairData currencyData) {
        if (currencyData == null || currencyData.getCurrencyPair() == null) {
            LOGGER.warning("Ungültige Währungsdaten erhalten");
            return;
        }
        
        String currencyPair = normalizeCurrencyPairName(currencyData.getCurrencyPair());
        String filename = currencyPair + FILE_EXTENSION;
        Path filePath = currencyDataPath.resolve(filename);
        
        // Thread-sichere Ausführung pro Währungspaar
        ReentrantLock fileLock = getFileLock(currencyPair);
        fileLock.lock();
        
        try {
            // Stelle sicher, dass das Verzeichnis existiert
            createCurrencyDataDirectory();
            
            // Prüfe ob Duplikat vermieden werden soll
            if (isDuplicate(currencyData, filePath)) {
                LOGGER.fine("Duplikat für " + currencyPair + " übersprungen");
                return;
            }
            
            // Prüfe ob Header benötigt wird
            boolean needsHeader = needsHeaderCheck(filePath);
            
            // Schreibe Daten
            writeDataToFile(filePath, currencyData, needsHeader);
            
            LOGGER.fine("Daten für " + currencyPair + " erfolgreich gespeichert");
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Fehler beim Speichern der Daten für " + currencyPair + ": " + e.getMessage(), e);
            throw new RuntimeException("Konnte Daten für " + currencyPair + " nicht speichern", e);
        } finally {
            fileLock.unlock();
        }
    }
    
    /**
     * Liest alle historischen Daten für ein Währungspaar
     * @param currencyPair Das Währungspaar (z.B. "EUR/USD" oder "EURUSD")
     * @return Liste der historischen Daten
     */
    public List<CurrencyPairData> readDataForCurrencyPair(String currencyPair) {
        String normalizedPair = normalizeCurrencyPairName(currencyPair);
        String filename = normalizedPair + FILE_EXTENSION;
        Path filePath = currencyDataPath.resolve(filename);
        
        return readDataFromPath(filePath, currencyPair);
    }
    
    /**
     * Liest die letzten N Einträge für ein Währungspaar
     * @param currencyPair Das Währungspaar
     * @param count Anzahl der gewünschten Einträge (von neuesten)
     * @return Liste der letzten Einträge
     */
    public List<CurrencyPairData> readLastEntriesForCurrencyPair(String currencyPair, int count) {
        List<CurrencyPairData> allData = readDataForCurrencyPair(currencyPair);
        
        if (allData.isEmpty() || count <= 0) {
            return new ArrayList<>();
        }
        
        // Sortiere nach Zeitstempel (neueste zuerst) und nimm die ersten 'count' Einträge
        allData.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));
        
        int endIndex = Math.min(count, allData.size());
        return new ArrayList<>(allData.subList(0, endIndex));
    }
    
    /**
     * Listet alle verfügbaren Währungspaare auf
     * @return Set mit allen Währungspaar-Namen
     */
    public Set<String> listAvailableCurrencyPairs() {
        Set<String> currencyPairs = new HashSet<>();
        
        try {
            if (!Files.exists(currencyDataPath)) {
                return currencyPairs;
            }
            
            Files.list(currencyDataPath)
                    .filter(path -> path.toString().endsWith(FILE_EXTENSION))
                    .forEach(path -> {
                        String filename = path.getFileName().toString();
                        String currencyPair = filename.replace(FILE_EXTENSION, "");
                        currencyPairs.add(currencyPair);
                    });
                    
            LOGGER.fine("Gefundene Währungspaare: " + currencyPairs.size());
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Fehler beim Auflisten der Währungspaare: " + e.getMessage(), e);
        }
        
        return currencyPairs;
    }
    
    /**
     * Gibt Statistiken für ein Währungspaar zurück
     * @param currencyPair Das Währungspaar
     * @return Statistik-Objekt mit Informationen
     */
    public CurrencyPairStatistics getStatisticsForCurrencyPair(String currencyPair) {
        List<CurrencyPairData> data = readDataForCurrencyPair(currencyPair);
        
        if (data.isEmpty()) {
            return new CurrencyPairStatistics(currencyPair, 0, null, null);
        }
        
        // Sortiere nach Zeitstempel
        data.sort((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()));
        
        LocalDateTime firstEntry = data.get(0).getTimestamp();
        LocalDateTime lastEntry = data.get(data.size() - 1).getTimestamp();
        
        return new CurrencyPairStatistics(currencyPair, data.size(), firstEntry, lastEntry);
    }
    
    /**
     * Gibt Gesamtstatistiken für alle Währungspaare zurück
     * @return Zusammenfassung aller Statistiken
     */
    public String getOverallStatistics() {
        Set<String> pairs = listAvailableCurrencyPairs();
        
        if (pairs.isEmpty()) {
            return "Keine Währungspaar-Daten vorhanden";
        }
        
        StringBuilder stats = new StringBuilder();
        stats.append("Währungspaar-Statistiken:\n");
        stats.append("========================\n");
        stats.append("Verfügbare Währungspaare: ").append(pairs.size()).append("\n\n");
        
        int totalRecords = 0;
        for (String pair : pairs) {
            CurrencyPairStatistics pairStats = getStatisticsForCurrencyPair(pair);
            totalRecords += pairStats.getTotalRecords();
            
            stats.append(String.format("%-10s: %4d Einträge", pair, pairStats.getTotalRecords()));
            if (pairStats.getFirstEntry() != null) {
                stats.append(String.format(" (von %s bis %s)", 
                    pairStats.getFirstEntry().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                    pairStats.getLastEntry().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                ));
            }
            stats.append("\n");
        }
        
        stats.append("\nGesamt: ").append(totalRecords).append(" Datensätze");
        return stats.toString();
    }
    
    /**
     * Bereinigt alte Einträge für alle Währungspaare
     * @param daysToKeep Anzahl Tage die behalten werden sollen
     */
    public void cleanupOldData(int daysToKeep) {
        LOGGER.info("Beginne Bereinigung von Einträgen älter als " + daysToKeep + " Tage");
        
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);
        Set<String> pairs = listAvailableCurrencyPairs();
        
        int totalRemovedRecords = 0;
        int processedPairs = 0;
        
        for (String pair : pairs) {
            try {
                int removedRecords = cleanupOldDataForPair(pair, cutoffDate);
                totalRemovedRecords += removedRecords;
                processedPairs++;
                
                if (removedRecords > 0) {
                    LOGGER.info("Bereinigt " + removedRecords + " alte Einträge für " + pair);
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Fehler bei Bereinigung für " + pair + ": " + e.getMessage(), e);
            }
        }
        
        LOGGER.info("Bereinigung abgeschlossen: " + totalRemovedRecords + " Einträge aus " + processedPairs + " Währungspaaren entfernt");
    }
    
    /**
     * Validiert die Datenintegrität für alle Währungspaare
     * @return Validierungsbericht
     */
    public String validateAllData() {
        Set<String> pairs = listAvailableCurrencyPairs();
        StringBuilder report = new StringBuilder();
        
        report.append("Datenintegritäts-Validierung:\n");
        report.append("=============================\n");
        
        int validPairs = 0;
        int invalidPairs = 0;
        
        for (String pair : pairs) {
            try {
                ValidationResult result = validateDataForPair(pair);
                
                if (result.isValid()) {
                    validPairs++;
                    report.append(String.format("%-10s: ✓ %d Einträge, %d gültig\n", 
                        pair, result.getTotalRecords(), result.getValidRecords()));
                } else {
                    invalidPairs++;
                    report.append(String.format("%-10s: ✗ %d Einträge, %d ungültig, %d Fehler\n", 
                        pair, result.getTotalRecords(), result.getValidRecords(), result.getInvalidRecords()));
                }
            } catch (Exception e) {
                invalidPairs++;
                report.append(String.format("%-10s: ✗ Fehler bei Validierung: %s\n", pair, e.getMessage()));
            }
        }
        
        report.append(String.format("\nZusammenfassung: %d gültig, %d mit Problemen\n", validPairs, invalidPairs));
        return report.toString();
    }
    
    /**
     * Exportiert Daten für ein Währungspaar in ein anderes Format oder Verzeichnis
     * @param currencyPair Das Währungspaar
     * @param targetPath Ziel-Pfad für den Export
     */
    public void exportCurrencyPairData(String currencyPair, Path targetPath) {
        List<CurrencyPairData> data = readDataForCurrencyPair(currencyPair);
        
        if (data.isEmpty()) {
            LOGGER.warning("Keine Daten für Export von " + currencyPair + " gefunden");
            return;
        }
        
        try {
            Files.createDirectories(targetPath.getParent());
            
            try (BufferedWriter writer = Files.newBufferedWriter(targetPath, StandardCharsets.UTF_8)) {
                // Schreibe Header
                writer.write(getCurrencyPairCsvHeader());
                writer.newLine();
                
                // Schreibe Daten
                for (CurrencyPairData currencyData : data) {
                    writer.write(formatCurrencyDataToCsv(currencyData));
                    writer.newLine();
                }
            }
            
            LOGGER.info("Daten für " + currencyPair + " erfolgreich exportiert nach: " + targetPath);
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Fehler beim Export von " + currencyPair + ": " + e.getMessage(), e);
            throw new RuntimeException("Export fehlgeschlagen", e);
        }
    }
    
    // ===== PRIVATE HILFSMETHODEN =====
    
    /**
     * Normalisiert den Währungspaar-Namen für Dateinamen
     */
    private String normalizeCurrencyPairName(String currencyPair) {
        if (currencyPair == null) return "UNKNOWN";
        
        // Entferne Sonderzeichen und ersetze durch Unterstriche
        String normalized = currencyPair.replaceAll("[^A-Za-z0-9]", "_");
        
        // Entferne mehrfache Unterstriche
        normalized = normalized.replaceAll("_+", "_");
        
        // Entferne führende/abschließende Unterstriche
        normalized = normalized.replaceAll("^_+|_+$", "");
        
        return normalized.toUpperCase();
    }
    
    /**
     * Holt oder erstellt ein File-Lock für ein Währungspaar
     */
    private ReentrantLock getFileLock(String currencyPair) {
        return fileLocks.computeIfAbsent(currencyPair, k -> new ReentrantLock());
    }
    
    /**
     * Prüft ob ein Datensatz ein Duplikat ist
     */
    private boolean isDuplicate(CurrencyPairData newData, Path filePath) {
        if (!Files.exists(filePath)) {
            return false;
        }
        
        try {
            // Lade die letzten 5 Einträge für Duplikat-Prüfung
            List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
            
            if (lines.size() <= 1) { // Nur Header oder leer
                return false;
            }
            
            String newDataLine = formatCurrencyDataToCsv(newData);
            
            // Prüfe die letzten 5 Zeilen auf Duplikate
            int startIndex = Math.max(1, lines.size() - 5); // Überspringe Header
            for (int i = startIndex; i < lines.size(); i++) {
                if (lines.get(i).equals(newDataLine)) {
                    return true;
                }
            }
            
            return false;
            
        } catch (Exception e) {
            LOGGER.fine("Fehler bei Duplikat-Prüfung: " + e.getMessage());
            return false; // Im Zweifel als nicht-Duplikat behandeln
        }
    }
    
    /**
     * Prüft ob ein Header benötigt wird
     */
    private boolean needsHeaderCheck(Path filePath) {
        try {
            if (!Files.exists(filePath)) {
                return true;
            }
            
            if (Files.size(filePath) == 0) {
                return true;
            }
            
            try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
                String firstLine = reader.readLine();
                return firstLine == null || firstLine.trim().isEmpty() || 
                       !firstLine.equals(getCurrencyPairCsvHeader());
            }
            
        } catch (Exception e) {
            LOGGER.fine("Fehler bei Header-Prüfung: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Schreibt Daten in eine Datei
     */
    private void writeDataToFile(Path filePath, CurrencyPairData data, boolean needsHeader) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            
            if (needsHeader) {
                writer.write(getCurrencyPairCsvHeader());
                writer.newLine();
            }
            
            writer.write(formatCurrencyDataToCsv(data));
            writer.newLine();
            writer.flush();
        }
    }
    
    /**
     * Liest Daten aus einem Pfad
     */
    private List<CurrencyPairData> readDataFromPath(Path filePath, String currencyPair) {
        List<CurrencyPairData> data = new ArrayList<>();
        
        if (!Files.exists(filePath)) {
            LOGGER.fine("Datei für " + currencyPair + " existiert nicht: " + filePath.getFileName());
            return data;
        }
        
        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            String line;
            boolean isFirstLine = true;
            int lineNumber = 1;
            
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                
                if (isFirstLine) {
                    isFirstLine = false;
                    if (line.equals(getCurrencyPairCsvHeader())) {
                        continue;
                    }
                    lineNumber = 1; // Reset für korrekte Zeilennummerierung
                }
                
                try {
                    CurrencyPairData currencyData = parseCurrencyDataFromCsv(line, currencyPair);
                    data.add(currencyData);
                } catch (Exception e) {
                    LOGGER.fine("Ungültige CSV-Zeile " + lineNumber + " in " + currencyPair + " übersprungen: " + line);
                }
            }
            
            LOGGER.fine("Erfolgreich " + data.size() + " Datensätze für " + currencyPair + " geladen");
            
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Fehler beim Lesen der Datei für " + currencyPair + ": " + e.getMessage(), e);
        }
        
        return data;
    }
    
    /**
     * Bereinigt alte Daten für ein einzelnes Währungspaar
     */
    private int cleanupOldDataForPair(String currencyPair, LocalDateTime cutoffDate) {
        List<CurrencyPairData> allData = readDataForCurrencyPair(currencyPair);
        
        List<CurrencyPairData> filteredData = new ArrayList<>();
        int removedCount = 0;
        
        for (CurrencyPairData data : allData) {
            if (data.getTimestamp().isAfter(cutoffDate)) {
                filteredData.add(data);
            } else {
                removedCount++;
            }
        }
        
        if (removedCount > 0) {
            // Schreibe gefilterte Daten zurück
            rewriteFileWithData(currencyPair, filteredData);
        }
        
        return removedCount;
    }
    
    /**
     * Schreibt eine komplette Datei mit neuen Daten
     */
    private void rewriteFileWithData(String currencyPair, List<CurrencyPairData> data) {
        String normalizedPair = normalizeCurrencyPairName(currencyPair);
        String filename = normalizedPair + FILE_EXTENSION;
        Path filePath = currencyDataPath.resolve(filename);
        
        ReentrantLock fileLock = getFileLock(normalizedPair);
        fileLock.lock();
        
        try {
            // Lösche alte Datei und erstelle neue
            if (Files.exists(filePath)) {
                Files.delete(filePath);
            }
            
            try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
                
                // Schreibe Header
                writer.write(getCurrencyPairCsvHeader());
                writer.newLine();
                
                // Schreibe alle Daten
                for (CurrencyPairData currencyData : data) {
                    writer.write(formatCurrencyDataToCsv(currencyData));
                    writer.newLine();
                }
                
                writer.flush();
            }
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Fehler beim Neuschreiben der Datei für " + currencyPair + ": " + e.getMessage(), e);
            throw new RuntimeException("Konnte Datei nicht neuschreiben", e);
        } finally {
            fileLock.unlock();
        }
    }
    
    /**
     * Validiert Daten für ein einzelnes Währungspaar
     */
    private ValidationResult validateDataForPair(String currencyPair) {
        List<CurrencyPairData> data = readDataForCurrencyPair(currencyPair);
        
        int totalRecords = data.size();
        int validRecords = 0;
        int invalidRecords = 0;
        
        for (CurrencyPairData currencyData : data) {
            if (currencyData.isDataConsistent() && 
                currencyData.getBuyPercentage() >= 0 && 
                currencyData.getSellPercentage() >= 0 &&
                currencyData.getTimestamp() != null) {
                validRecords++;
            } else {
                invalidRecords++;
            }
        }
        
        return new ValidationResult(totalRecords, validRecords, invalidRecords);
    }
    
    /**
     * CSV-Header für Währungspaar-Dateien
     */
    private String getCurrencyPairCsvHeader() {
        return "Zeitstempel;Buy_Prozent;Sell_Prozent;Handelssignal";
    }
    
    /**
     * Formatiert CurrencyPairData zu CSV-Zeile
     */
    private String formatCurrencyDataToCsv(CurrencyPairData data) {
        return String.format("%s;%.2f;%.2f;%s",
            data.getTimestamp().format(TIMESTAMP_FORMATTER),
            data.getBuyPercentage(),
            data.getSellPercentage(),
            data.getTradingSignal().name()
        );
    }
    
    /**
     * Parst CSV-Zeile zu CurrencyPairData
     */
    private CurrencyPairData parseCurrencyDataFromCsv(String csvLine, String currencyPair) {
        String[] parts = csvLine.split(";");
        if (parts.length != 4) {
            throw new IllegalArgumentException("Ungültiges CSV-Format: " + csvLine);
        }
        
        LocalDateTime timestamp = LocalDateTime.parse(parts[0], TIMESTAMP_FORMATTER);
        double buyPercentage = Double.parseDouble(parts[1]);
        double sellPercentage = Double.parseDouble(parts[2]);
        CurrencyPairData.TradingSignal signal = CurrencyPairData.TradingSignal.valueOf(parts[3]);
        
        return new CurrencyPairData(currencyPair, buyPercentage, sellPercentage, signal, timestamp);
    }
    
    // ===== INNERE KLASSEN =====
    
    /**
     * Statistik-Klasse für ein Währungspaar
     */
    public static class CurrencyPairStatistics {
        private final String currencyPair;
        private final int totalRecords;
        private final LocalDateTime firstEntry;
        private final LocalDateTime lastEntry;
        
        public CurrencyPairStatistics(String currencyPair, int totalRecords, 
                                    LocalDateTime firstEntry, LocalDateTime lastEntry) {
            this.currencyPair = currencyPair;
            this.totalRecords = totalRecords;
            this.firstEntry = firstEntry;
            this.lastEntry = lastEntry;
        }
        
        public String getCurrencyPair() { return currencyPair; }
        public int getTotalRecords() { return totalRecords; }
        public LocalDateTime getFirstEntry() { return firstEntry; }
        public LocalDateTime getLastEntry() { return lastEntry; }
        
        @Override
        public String toString() {
            return String.format("CurrencyPairStatistics{pair='%s', records=%d, from=%s, to=%s}",
                currencyPair, totalRecords, firstEntry, lastEntry);
        }
    }
    
    /**
     * Validierungsergebnis-Klasse
     */
    private static class ValidationResult {
        private final int totalRecords;
        private final int validRecords;
        private final int invalidRecords;
        
        public ValidationResult(int totalRecords, int validRecords, int invalidRecords) {
            this.totalRecords = totalRecords;
            this.validRecords = validRecords;
            this.invalidRecords = invalidRecords;
        }
        
        public int getTotalRecords() { return totalRecords; }
        public int getValidRecords() { return validRecords; }
        public int getInvalidRecords() { return invalidRecords; }
        public boolean isValid() { return invalidRecords == 0; }
    }
}