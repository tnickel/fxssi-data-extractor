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
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Dateimanagement-Klasse für das Speichern und Verwalten der FXSSI-Daten
 * Verwaltet CSV-Dateien im data/ Verzeichnis
 * 
 * @author Generated for FXSSI Data Extraction
 * @version 1.0
 */
public class DataFileManager {
    
    private static final Logger LOGGER = Logger.getLogger(DataFileManager.class.getName());
    private static final String FILE_EXTENSION = ".csv";
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final String FILE_PREFIX = "fxssi_data_";
    
    private final String dataDirectory;
    private final Path dataPath;
    
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
     * Hängt neue Daten an die heutige Datei an
     * @param currencyData Liste der zu speichernden Währungsdaten
     */
    public void appendDataToFile(List<CurrencyPairData> currencyData) {
        if (currencyData == null || currencyData.isEmpty()) {
            LOGGER.warning("Keine Daten zum Speichern erhalten");
            return;
        }
        
        String filename = generateTodayFilename();
        Path filePath = dataPath.resolve(filename);
        
        try {
            boolean fileExists = Files.exists(filePath);
            
            // Erstelle oder öffne die Datei
            try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                
                // Schreibe Header falls neue Datei
                if (!fileExists) {
                    writer.write(CurrencyPairData.getCsvHeader());
                    writer.newLine();
                    LOGGER.info("Neue Datei erstellt mit Header: " + filename);
                }
                
                // Schreibe Datenzeilen
                for (CurrencyPairData data : currencyData) {
                    writer.write(data.toCsvLine());
                    writer.newLine();
                }
                
                writer.flush();
                LOGGER.info("Erfolgreich " + currencyData.size() + " Datensätze in " + filename + " gespeichert");
            }
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Fehler beim Schreiben in Datei " + filename + ": " + e.getMessage(), e);
            throw new RuntimeException("Konnte Daten nicht speichern", e);
        }
    }
    
    /**
     * Liest alle Daten aus einer bestimmten Datei
     * @param filename Name der zu lesenden Datei
     * @return Liste der gelesenen Währungsdaten
     */
    public List<CurrencyPairData> readDataFromFile(String filename) {
        Path filePath = dataPath.resolve(filename);
        List<CurrencyPairData> currencyData = new ArrayList<>();
        
        if (!Files.exists(filePath)) {
            LOGGER.warning("Datei existiert nicht: " + filename);
            return currencyData;
        }
        
        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            String line;
            boolean isFirstLine = true;
            
            while ((line = reader.readLine()) != null) {
                // Überspringe Header-Zeile
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }
                
                try {
                    CurrencyPairData data = CurrencyPairData.fromCsvLine(line);
                    currencyData.add(data);
                } catch (IllegalArgumentException e) {
                    LOGGER.warning("Ungültige CSV-Zeile übersprungen: " + line);
                }
            }
            
            LOGGER.info("Erfolgreich " + currencyData.size() + " Datensätze aus " + filename + " gelesen");
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Fehler beim Lesen der Datei " + filename + ": " + e.getMessage(), e);
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
                    
            LOGGER.info("Gefundene Datendateien: " + files.size());
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Fehler beim Auflisten der Datendateien: " + e.getMessage(), e);
        }
        
        return files;
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
            
            if (headerLine == null || !headerLine.equals(CurrencyPairData.getCsvHeader())) {
                LOGGER.warning("Ungültiger Header in Datei: " + filename);
                return false;
            }
            
            String line;
            int lineNumber = 2;
            
            while ((line = reader.readLine()) != null) {
                try {
                    CurrencyPairData.fromCsvLine(line);
                } catch (IllegalArgumentException e) {
                    LOGGER.warning("Ungültige Zeile " + lineNumber + " in Datei " + filename + ": " + line);
                    return false;
                }
                lineNumber++;
            }
            
            LOGGER.info("Datei " + filename + " ist gültig");
            return true;
            
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
            LOGGER.warning("Konnte Datum nicht aus Dateiname extrahieren: " + filename);
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