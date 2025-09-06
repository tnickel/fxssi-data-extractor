package com.fxsssi.extractor.gui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * JavaFX Hauptklasse für die FXSSI Data Extractor GUI
 * Vollständige Java-Implementation ohne FXML mit konfigurierbarem Datenverzeichnis
 * 
 * @author Generated for FXSSI Data Extraction GUI
 * @version 1.2 (mit breiterem Fenster für bessere Balken-Sichtbarkeit)
 */
public class FXSSIGuiApplication extends Application {
    
    private static final Logger LOGGER = Logger.getLogger(FXSSIGuiApplication.class.getName());
    private static final String WINDOW_TITLE = "FXSSI Data Extractor - Live Sentiment Monitor";
    private static final int WINDOW_WIDTH = 1400;   // Erweitert von 1200 auf 1400
    private static final int WINDOW_HEIGHT = 800;
    private static final int MIN_WINDOW_WIDTH = 1000; // Erweitert von 800 auf 1000
    private static final int MIN_WINDOW_HEIGHT = 600;
    private static final String DEFAULT_DATA_DIRECTORY = "data";
    
    private MainWindowController mainController;
    private static String configuredDataDirectory = DEFAULT_DATA_DIRECTORY;
    
    @Override
    public void start(Stage primaryStage) {
        try {
            LOGGER.info("Starte FXSSI GUI Application...");
            LOGGER.info("Konfiguriertes Datenverzeichnis: " + configuredDataDirectory);
            
            // Erstelle Main Window Controller mit konfiguriertem Datenverzeichnis
            mainController = new MainWindowController(configuredDataDirectory);
            
            // Erstelle Scene programmatisch
            Scene scene = mainController.createMainWindow(primaryStage);
            
            // Konfiguriere Hauptfenster
            setupPrimaryStage(primaryStage, scene);
            
            // Zeige Fenster
            primaryStage.show();
            
            // Starte Datenservice
            mainController.startDataService();
            
            LOGGER.info("FXSSI GUI Application erfolgreich gestartet (1400x800)");
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Fehler beim Starten der GUI Application: " + e.getMessage(), e);
            showErrorAndExit(e);
        }
    }
    
    @Override
    public void stop() {
        try {
            LOGGER.info("Stoppe FXSSI GUI Application...");
            
            if (mainController != null) {
                mainController.shutdown();
            }
            
            LOGGER.info("FXSSI GUI Application gestoppt");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Fehler beim Stoppen der GUI Application: " + e.getMessage(), e);
        }
    }
    
    /**
     * Konfiguriert das Hauptfenster mit erweiterten Abmessungen
     */
    private void setupPrimaryStage(Stage primaryStage, Scene scene) {
        // Titel mit Datenverzeichnis-Info erweitern
        String titleWithPath = WINDOW_TITLE;
        if (!configuredDataDirectory.equals(DEFAULT_DATA_DIRECTORY)) {
            titleWithPath += " - Datenverzeichnis: " + configuredDataDirectory;
        }
        
        primaryStage.setTitle(titleWithPath);
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(MIN_WINDOW_WIDTH);
        primaryStage.setMinHeight(MIN_WINDOW_HEIGHT);
        primaryStage.setWidth(WINDOW_WIDTH);
        primaryStage.setHeight(WINDOW_HEIGHT);
        
        // Setze Anwendungsicon falls vorhanden
        try {
            Image icon = new Image(getClass().getResourceAsStream("/icons/fxssi-icon.png"));
            primaryStage.getIcons().add(icon);
        } catch (Exception e) {
            LOGGER.fine("Kein Anwendungsicon gefunden - verwende Standard");
        }
        
        // Schließe Anwendung ordnungsgemäß
        primaryStage.setOnCloseRequest(event -> {
            try {
                stop();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Fehler beim ordnungsgemäßen Schließen: " + e.getMessage(), e);
            }
        });
        
        // Zentriere Fenster auf Bildschirm (optional)
        primaryStage.centerOnScreen();
        
        LOGGER.info("Hauptfenster konfiguriert: " + WINDOW_WIDTH + "x" + WINDOW_HEIGHT + 
                   " (Min: " + MIN_WINDOW_WIDTH + "x" + MIN_WINDOW_HEIGHT + ")");
    }
    
    /**
     * Zeigt Fehlermeldung und beendet Anwendung
     */
    private void showErrorAndExit(Exception e) {
        try {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("Anwendungsfehler");
            alert.setHeaderText("Die FXSSI GUI konnte nicht gestartet werden");
            alert.setContentText("Fehler: " + e.getMessage() + "\n\nBitte überprüfen Sie die Logs für weitere Details." +
                "\n\nDatenverzeichnis: " + configuredDataDirectory +
                "\n\nEmpfohlene Fensterbreite: " + WINDOW_WIDTH + "px für optimale Balken-Sichtbarkeit");
            alert.showAndWait();
        } catch (Exception alertException) {
            LOGGER.log(Level.SEVERE, "Fehler beim Anzeigen der Fehlermeldung: " + alertException.getMessage(), alertException);
        }
        System.exit(1);
    }
    
    /**
     * Setzt das konfigurierte Datenverzeichnis
     * Diese Methode muss vor dem Start der JavaFX Application aufgerufen werden
     */
    public static void setDataDirectory(String dataDirectory) {
        if (dataDirectory != null && !dataDirectory.trim().isEmpty()) {
            configuredDataDirectory = dataDirectory.trim();
            LOGGER.info("Datenverzeichnis für GUI konfiguriert: " + configuredDataDirectory);
        } else {
            LOGGER.warning("Ungültiges Datenverzeichnis, verwende Standard: " + DEFAULT_DATA_DIRECTORY);
            configuredDataDirectory = DEFAULT_DATA_DIRECTORY;
        }
    }
    
    /**
     * Gibt das aktuell konfigurierte Datenverzeichnis zurück
     */
    public static String getConfiguredDataDirectory() {
        return configuredDataDirectory;
    }
    
    /**
     * Startet die GUI-Anwendung mit Standard-Datenverzeichnis
     */
    public static void launchGui(String[] args) {
        launchGui(args, DEFAULT_DATA_DIRECTORY);
    }
    
    /**
     * Startet die GUI-Anwendung mit konfiguriertem Datenverzeichnis
     */
    public static void launchGui(String[] args, String dataDirectory) {
        LOGGER.info("Starte JavaFX GUI für FXSSI Data Extractor...");
        LOGGER.info("Datenverzeichnis: " + dataDirectory);
        LOGGER.info("Fensterabmessungen: " + WINDOW_WIDTH + "x" + WINDOW_HEIGHT + " (erweitert für bessere Balken-Sichtbarkeit)");
        
        // Setze das Datenverzeichnis vor dem Start
        setDataDirectory(dataDirectory);
        
        launch(args);
    }
    
    /**
     * Hauptmethode für GUI-only Ausführung mit Standard-Datenverzeichnis
     */
    public static void main(String[] args) {
        // Parse mögliche Datenverzeichnis-Argumente auch für direkten GUI-Start
        String dataDir = DEFAULT_DATA_DIRECTORY;
        
        for (int i = 0; i < args.length; i++) {
            if ("--data-dir".equals(args[i]) && i + 1 < args.length) {
                dataDir = args[i + 1];
                break;
            }
        }
        
        launchGui(args, dataDir);
    }
}