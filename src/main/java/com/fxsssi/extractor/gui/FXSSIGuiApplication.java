package com.fxsssi.extractor.gui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * JavaFX Hauptklasse für die FXSSI Data Extractor GUI
 * Vollständige Java-Implementation ohne FXML
 * 
 * @author Generated for FXSSI Data Extraction GUI
 * @version 1.0
 */
public class FXSSIGuiApplication extends Application {
    
    private static final Logger LOGGER = Logger.getLogger(FXSSIGuiApplication.class.getName());
    private static final String WINDOW_TITLE = "FXSSI Data Extractor - Live Sentiment Monitor";
    private static final int WINDOW_WIDTH = 1200;
    private static final int WINDOW_HEIGHT = 800;
    private static final int MIN_WINDOW_WIDTH = 800;
    private static final int MIN_WINDOW_HEIGHT = 600;
    
    private MainWindowController mainController;
    
    @Override
    public void start(Stage primaryStage) {
        try {
            LOGGER.info("Starte FXSSI GUI Application...");
            
            // Erstelle Main Window Controller
            mainController = new MainWindowController();
            
            // Erstelle Scene programmatisch
            Scene scene = mainController.createMainWindow(primaryStage);
            
            // Konfiguriere Hauptfenster
            setupPrimaryStage(primaryStage, scene);
            
            // Zeige Fenster
            primaryStage.show();
            
            // Starte Datenservice
            mainController.startDataService();
            
            LOGGER.info("FXSSI GUI Application erfolgreich gestartet");
            
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
     * Konfiguriert das Hauptfenster
     */
    private void setupPrimaryStage(Stage primaryStage, Scene scene) {
        primaryStage.setTitle(WINDOW_TITLE);
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
            alert.setContentText("Fehler: " + e.getMessage() + "\n\nBitte überprüfen Sie die Logs für weitere Details.");
            alert.showAndWait();
        } catch (Exception alertException) {
            LOGGER.log(Level.SEVERE, "Fehler beim Anzeigen der Fehlermeldung: " + alertException.getMessage(), alertException);
        }
        System.exit(1);
    }
    
    /**
     * Startet die GUI-Anwendung
     */
    public static void launchGui(String[] args) {
        LOGGER.info("Starte JavaFX GUI für FXSSI Data Extractor...");
        launch(args);
    }
    
    /**
     * Hauptmethode für GUI-only Ausführung
     */
    public static void main(String[] args) {
        launchGui(args);
    }
}