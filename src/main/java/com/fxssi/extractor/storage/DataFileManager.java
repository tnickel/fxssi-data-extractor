package com.fxssi.extractor.storage;

import com.fxssi.extractor.model.CurrencyPairData;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Dateimanagement-Klasse für das Speichern und Verwalten der FXSSI-Daten
 * Verwaltet CSV-Dateien im data/ Verzeichnis mit verbesserter Duplikat-Behandlung
 * 
 * @author Generated for FXSSI Data Extraction
 * @version 1.1 (mit Duplikat-Filterung und verbesserter Header-Behandlung)
 */
public class DataFileManager {
    
    private static final Logger LOGGER = Logger.getLogger(DataFileManager.class.getName());
    private static final String FILE_EXTENSION = ".csv";
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final String FILE_PREFIX = "fxssi_data_";
    
    private final String dataDirectory;
    private final Path dataPath;
    private final ReentrantLock fileLock = new ReentrantLock();
    
    /**
     * Konstruktor
     * @param dataDirectory Pfad zum Datenverzeichnis
     */
    public DataFileManager(String dataDirectory) {
        this.dataDirectory = dataDirectory;
        this.dataPath = Paths.get(dataDirectory);
        LOGGER.info("DataFileManager initialisiert für Verzeichnis: " + dataDirectory);
    }
    
    /**
     * Erstellt das Datenverzeichnis falls es nicht existiert
     */
    public void createDataDirectory() {
        try {
            if (!Files.exists(dataPath)) {
                Files.createDirectories(dataPath);
                LOGGER.info("Datenverzeichnis erstellt: " + dataPath.toAbsolutePath());
            } else {
                LOGGER.info("Datenverzeichnis existiert bereits: " + dataPath.toAbsolutePath());
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Fehler beim Erstellen des Datenverzeichnisses: " + e.getMessage(), e);
            throw new RuntimeException("Konnte Datenverzeichnis nicht erstellen", e);
        }
    }
    
    /**
     * Hängt neue Daten an die heutige Datei an (mit Duplikat-Filterung)
     * @param currencyData Liste der zu speichernden Währungsdaten
     */
    public void appendDataToFile(List<CurrencyPairData> currencyData) {
        if (currencyData == null || currencyData.isEmpty()) {
            LOGGER.warning("Keine Daten zum Speichern erhalten");
            return;
        }
        
        // Thread-sichere Ausführung
        fileLock.lock();
        try {
            String filename = generateTodayFilename();
            Path filePath = dataPath.resolve(filename);
            
            LOGGER.info("Beginne Speicherung von " + currencyData.size() + " Datensätzen in " + filename);
            
            // Filtere Duplikate basierend auf bereits existierenden Daten
            List<CurrencyPairData> filteredData = filterDuplicates(currencyData, filePath);
            
            if (filteredData.isEmpty()) {
                LOGGER.info("Alle Daten bereits vorhanden - keine neuen Datensätze zu speichern");
                return;
            }
            
            // Prüfe ob Header benötigt wird (neue Datei oder leere Datei)
            boolean needsHeader = needsHeaderCheck(filePath);
            
            // Schreibe Daten
            writeDataToFile(filePath, filteredData, needsHeader);
            
            LOGGER.info("Erfolgreich " + filteredData.size() + " neue Datensätze in " + filename + " gespeichert");
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Fehler beim Speichern der Daten: " + e.getMessage(), e);
            throw new RuntimeException("Konnte Daten nicht speichern", e);
        } finally {
            fileLock.unlock();
        }
    }
    
    /**
     * Filtert Duplikate basierend auf bereits existierenden Daten
     */
    private List<CurrencyPairData> filterDuplicates(List<CurrencyPairData> newData, Path filePath) {
        List<CurrencyPairData> filteredData = new ArrayList<>();
        
        try {
            // Lade existierende Daten falls Datei existiert
            Set<String> existingEntries = new HashSet<>();
            
            if (Files.exists(filePath)) {
                List<CurrencyPairData> existingData = readDataFromPath(filePath);
                
                // Erstelle Set von existierenden Einträgen (basierend auf Zeitstempel + Währungspaar)
                for (CurrencyPairData data : existingData) {
                    String entryKey = createEntryKey(data);
                    existingEntries.add(entryKey);
                }
                
                LOGGER.fine("Geladene existierende Einträge: " + existingEntries.size());
            }
            
            // Filtere neue Daten
            Set<String> seenInNewData = new HashSet<>();
            
            for (CurrencyPairData data : newData) {
                String entryKey = createEntryKey(data);
                
                // Überspringe wenn bereits in Datei vorhanden
                if (existingEntries.contains(entryKey)) {
                    LOGGER.fine("Duplikat übersprungen (bereits in Datei): " + data.getCurrencyPair());
                    continue;
                }
                
                // Überspringe wenn bereits in neuen Daten vorhanden
                if (seenInNewData.contains(entryKey)) {
                    LOGGER.fine("Duplikat übersprungen (bereits in neuen Daten): " + data.getCurrencyPair());
                    continue;
                }
                
                seenInNewData.add(entryKey);
                filteredData.add(data);
            }
            
            LOGGER.info("Duplikat-Filterung: " + newData.size() + " -> " + filteredData.size() + " Datensätze");
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Fehler bei Duplikat-Filterung, verwende alle Daten: " + e.getMessage(), e);
            return new ArrayList<>(newData);
        }
        
        return filteredData;
    }
    
    /**
     * Erstellt einen eindeutigen Schlüssel für einen Datensatz
     */
    private String createEntryKey(CurrencyPairData data) {
        // Verwende Zeitstempel (auf Minute genau) + Währungspaar + Buy-Percentage
        DateTimeFormatter keyFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return data.getTimestamp().format(keyFormatter) + "|" + data.getCurrencyPair() + "|" + String.format("%.0f", data.getBuyPercentage());
    }
    
    /**
     * Prüft ob ein Header benötigt wird
     */
    private boolean needsHeaderCheck(Path filePath) {
        try {
            if (!Files.exists(filePath)) {
                return true; // Neue Datei
            }
            
            // Prüfe ob Datei leer oder nur Whitespace enthält
            long fileSize = Files.size(filePath);
            if (fileSize == 0) {
                return true; // Leere Datei
            }
            
            // Prüfe die erste Zeile
            try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
                String firstLine = reader.readLine();
                
                if (firstLine == null || firstLine.trim().isEmpty()) {
                    return true; // Datei ist praktisch leer
                }
                
                // Prüfe ob erste Zeile bereits Header ist
                if (firstLine.equals(CurrencyPairData.getCsvHeader())) {
                    return false; // Header bereits vorhanden
                } else {
                    LOGGER.warning("Datei " + filePath.getFileName() + " hat keinen gültigen Header: " + firstLine);
                    return false; // Hat Daten aber falschen Header - füge keinen Header hinzu um Datei nicht zu beschädigen
                }
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Fehler bei Header-Prüfung: " + e.getMessage(), e);
            return false; // Im Zweifel keinen Header hinzufügen
        }
    }
    
    /**
     * Schreibt Daten in die Datei
     */
    private void writeDataToFile(Path filePath, List<CurrencyPairData> data, boolean needsHeader) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            
            // Schreibe Header falls benötigt
            if (needsHeader) {
                writer.write(CurrencyPairData.getCsvHeader());
                writer.newLine();
                LOGGER.info("Header geschrieben für: " + filePath.getFileName());
            }
            
            // Schreibe Datenzeilen
            for (CurrencyPairData currencyData : data) {
                writer.write(currencyData.toCsvLine());
                writer.newLine();
            }
            
            writer.flush();
            LOGGER.fine("Daten erfolgreich geschrieben");
        }
    }
    
    /**
     * Liest alle Daten aus einer bestimmten Datei
     * @param filename Name der zu lesenden Datei
     * @return Liste der gelesenen Währungsdaten
     */
    public List<CurrencyPairData> readDataFromFile(String filename) {
        Path filePath = dataPath.resolve(filename);
        return readDataFromPath(filePath);
    }
    
    /**
     * Liest Daten aus einem spezifischen Pfad
     */
    private List<CurrencyPairData> readDataFromPath(Path filePath) {
        List<CurrencyPairData> currencyData = new ArrayList<>();
        
        if (!Files.exists(filePath)) {
            LOGGER.fine("Datei existiert nicht: " + filePath.getFileName());
            return currencyData;
        }
        
        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            String line;
            boolean isFirstLine = true;
            int lineNumber = 1;
            
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                
                // Überspringe Header-Zeile
                if (isFirstLine) {
                    isFirstLine = false;
                    if (line.equals(CurrencyPairData.getCsvHeader())) {
                        continue; // Gültiger Header
                    } else {
                        // Erste Zeile ist kein Header, behandle als Datenzeile
                        LOGGER.fine("Keine Header-Zeile gefunden, beginne mit Datenzeile 1");
                        lineNumber = 1; // Reset für korrekte Zeilennummerierung
                    }
                }
                
                try {
                    CurrencyPairData data = CurrencyPairData.fromCsvLine(line);
                    currencyData.add(data);
                } catch (IllegalArgumentException e) {
                    LOGGER.fine("Ungültige CSV-Zeile " + lineNumber + " übersprungen: " + line);
                }
            }
            
            LOGGER.fine("Erfolgreich " + currencyData.size() + " Datensätze aus " + filePath.getFileName() + " gelesen");
            
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Fehler beim Lesen der Datei " + filePath.getFileName() + ": " + e.getMessage(), e);
        }
        
        return currencyData;
    }
    
    /**
     * Liest alle Daten der heutigen Datei
     * @return Liste der heutigen Währungsdaten
     */
    public List<CurrencyPairData> readTodayData() {
        String filename = generateTodayFilename();
        return readDataFromFile(filename);
    }
    
    /**
     * Liest alle Daten eines bestimmten Datums
     * @param date Datum für das die Daten gelesen werden sollen
     * @return Liste der Währungsdaten für das Datum
     */
    public List<CurrencyPairData> readDataForDate(LocalDate date) {
        String filename = generateFilenameForDate(date);
        return readDataFromFile(filename);
    }
    
    /**
     * Listet alle verfügbaren Datendateien auf
     * @return Liste der Dateinamen
     */
    public List<String> listDataFiles() {
        List<String> files = new ArrayList<>();
        
        try {
            Files.list(dataPath)
                    .filter(path -> path.toString().endsWith(FILE_EXTENSION))
                    .filter(path -> path.getFileName().toString().startsWith(FILE_PREFIX))
                    .forEach(path -> files.add(path.getFileName().toString()));
                    
            LOGGER.fine("Gefundene Datendateien: " + files.size());
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Fehler beim Auflisten der Datendateien: " + e.getMessage(), e);
        }
        
        return files;
    }
    
    /**
     * Repariert eine Datei mit doppelten Headern
     * @param filename Name der zu reparierenden Datei
     */
    public void repairFileWithDuplicateHeaders(String filename) {
        fileLock.lock();
        try {
            Path filePath = dataPath.resolve(filename);
            
            if (!Files.exists(filePath)) {
                LOGGER.warning("Datei existiert nicht: " + filename);
                return;
            }
            
            LOGGER.info("Repariere Datei mit doppelten Headern: " + filename);
            
            // Lese alle Zeilen
            List<String> allLines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
            List<String> cleanedLines = new ArrayList<>();
            
            String header = CurrencyPairData.getCsvHeader();
            boolean headerAdded = false;
            
            for (String line : allLines) {
                if (line.equals(header)) {
                    // Header-Zeile gefunden
                    if (!headerAdded) {
                        cleanedLines.add(line);
                        headerAdded = true;
                        LOGGER.fine("Header hinzugefügt");
                    } else {
                        LOGGER.fine("Doppelter Header übersprungen");
                    }
                } else if (!line.trim().isEmpty()) {
                    // Datenzeile
                    cleanedLines.add(line);
                }
            }
            
            // Schreibe bereinigte Datei
            Files.write(filePath, cleanedLines, StandardCharsets.UTF_8);
            
            LOGGER.info("Datei repariert: " + filename + " (" + cleanedLines.size() + " Zeilen behalten)");
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Fehler beim Reparieren der Datei: " + e.getMessage(), e);
        } finally {
            fileLock.unlock();
        }
    }
    
    /**
     * Löscht Datendateien die älter als die angegebene Anzahl Tage sind
     * @param daysToKeep Anzahl Tage die behalten werden sollen
     */
    public void cleanupOldFiles(int daysToKeep) {
        LOGGER.info("Beginne Bereinigung von Dateien älter als " + daysToKeep + " Tage");
        
        LocalDate cutoffDate = LocalDate.now().minusDays(daysToKeep);
        int deletedFiles = 0;
        
        try {
            Files.list(dataPath)
                    .filter(path -> path.toString().endsWith(FILE_EXTENSION))
                    .filter(path -> path.getFileName().toString().startsWith(FILE_PREFIX))
                    .forEach(path -> {
                        try {
                            String filename = path.getFileName().toString();
                            LocalDate fileDate = extractDateFromFilename(filename);
                            
                            if (fileDate != null && fileDate.isBefore(cutoffDate)) {
                                Files.delete(path);
                                LOGGER.info("Datei gelöscht: " + filename);
                            }
                        } catch (IOException e) {
                            LOGGER.warning("Fehler beim Löschen der Datei: " + path + " - " + e.getMessage());
                        }
                    });
                    
            LOGGER.info("Bereinigung abgeschlossen - " + deletedFiles + " Dateien gelöscht");
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Fehler bei der Dateibereinigung: " + e.getMessage(), e);
        }
    }
    
    /**
     * Erstellt eine Backup-Kopie der heutigen Datei
     */
    public void backupTodayFile() {
        String todayFilename = generateTodayFilename();
        Path sourcePath = dataPath.resolve(todayFilename);
        
        if (!Files.exists(sourcePath)) {
            LOGGER.warning("Keine heutige Datei zum Sichern gefunden: " + todayFilename);
            return;
        }
        
        String backupFilename = todayFilename.replace(FILE_EXTENSION, "_backup" + FILE_EXTENSION);
        Path backupPath = dataPath.resolve(backupFilename);
        
        try {
            Files.copy(sourcePath, backupPath);
            LOGGER.info("Backup erstellt: " + backupFilename);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Fehler beim Erstellen des Backups: " + e.getMessage(), e);
        }
    }
    
    /**
     * Überprüft die Integrität einer Datendatei
     * @param filename Name der zu überprüfenden Datei
     * @return true wenn Datei gültig ist
     */
    public boolean validateDataFile(String filename) {
        Path filePath = dataPath.resolve(filename);
        
        if (!Files.exists(filePath)) {
            LOGGER.warning("Datei existiert nicht: " + filename);
            return false;
        }
        
        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            String headerLine = reader.readLine();
            
            if (headerLine == null) {
                LOGGER.warning("Datei ist leer: " + filename);
                return false;
            }
            
            if (!headerLine.equals(CurrencyPairData.getCsvHeader())) {
                LOGGER.warning("Ungültiger Header in Datei: " + filename + " ('" + headerLine + "')");
                // Nicht als invalid markieren, da es alte Dateien geben könnte
            }
            
            String line;
            int lineNumber = 2;
            int validLines = 0;
            int invalidLines = 0;
            
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    lineNumber++;
                    continue;
                }
                
                try {
                    CurrencyPairData.fromCsvLine(line);
                    validLines++;
                } catch (IllegalArgumentException e) {
                    LOGGER.fine("Ungültige Zeile " + lineNumber + " in Datei " + filename + ": " + line);
                    invalidLines++;
                }
                lineNumber++;
            }
            
            LOGGER.info("Datei " + filename + " validiert: " + validLines + " gültige, " + invalidLines + " ungültige Zeilen");
            return invalidLines == 0 || (validLines > invalidLines * 10); // Toleriere wenige ungültige Zeilen
            
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Fehler beim Validieren der Datei " + filename + ": " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Generiert den Dateinamen für heute
     */
    private String generateTodayFilename() {
        return generateFilenameForDate(LocalDate.now());
    }
    
    /**
     * Generiert den Dateinamen für ein bestimmtes Datum
     */
    private String generateFilenameForDate(LocalDate date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_FORMAT);
        return FILE_PREFIX + date.format(formatter) + FILE_EXTENSION;
    }
    
    /**
     * Extrahiert das Datum aus einem Dateinamen
     */
    private LocalDate extractDateFromFilename(String filename) {
        try {
            String dateString = filename.replace(FILE_PREFIX, "").replace(FILE_EXTENSION, "");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_FORMAT);
            return LocalDate.parse(dateString, formatter);
        } catch (Exception e) {
            LOGGER.fine("Konnte Datum nicht aus Dateiname extrahieren: " + filename);
            return null;
        }
    }
    
    /**
     * Gibt Statistiken über gespeicherte Daten zurück
     */
    public String getDataStatistics() {
        List<String> files = listDataFiles();
        int totalRecords = 0;
        
        for (String filename : files) {
            List<CurrencyPairData> data = readDataFromFile(filename);
            totalRecords += data.size();
        }
        
        return String.format("Statistiken: %d Dateien, %d Datensätze total", files.size(), totalRecords);
    }
}