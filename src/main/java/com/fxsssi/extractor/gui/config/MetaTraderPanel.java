package com.fxsssi.extractor.gui.config;

import com.fxssi.extractor.notification.EmailConfig;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Panel für MetaTrader-Dual-Directory-Synchronisation-Konfiguration
 * Ermöglicht Konfiguration von 1 oder 2 MetaTrader-Verzeichnissen
 * 
 * @author Generated for FXSSI MetaTrader Integration
 * @version 1.0 - Dual-Directory Support
 */
public class MetaTraderPanel {
    
    private static final Logger LOGGER = Logger.getLogger(MetaTraderPanel.class.getName());
    
    private final Stage parentStage;
    private final Consumer<String> statusLogger;
    
    // UI-Komponenten - Verzeichnis 1
    private CheckBox syncEnabledCheckBox;
    private TextField dirField1;
    private Button browseButton1;
    private Button testButton1;
    private Label statusLabel1;
    
    // UI-Komponenten - Verzeichnis 2
    private TextField dirField2;
    private Button browseButton2;
    private Button testButton2;
    private Label statusLabel2;
    
    /**
     * Konstruktor
     * @param parentStage Parent-Stage für Dialoge
     * @param statusLogger Callback für Status-Nachrichten
     */
    public MetaTraderPanel(Stage parentStage, Consumer<String> statusLogger) {
        this.parentStage = parentStage;
        this.statusLogger = statusLogger;
    }
    
    /**
     * Erstellt das MetaTrader-Konfigurations-Panel
     * @return TitledPane mit allen UI-Komponenten
     */
    public TitledPane createPanel() {
        VBox content = new VBox(15);
        content.setPadding(new Insets(15));
        
        // Aktivierungs-CheckBox
        syncEnabledCheckBox = new CheckBox("MetaTrader-Datei-Synchronisation aktivieren");
        syncEnabledCheckBox.setFont(Font.font("System", FontWeight.BOLD, 12));
        syncEnabledCheckBox.setStyle("-fx-text-fill: #2E86AB;");
        syncEnabledCheckBox.setOnAction(e -> updateFieldsState());
        
        // === VERZEICHNIS 1 ===
        VBox dir1Section = createDirectorySection1();
        
        // === VERZEICHNIS 2 ===
        VBox dir2Section = createDirectorySection2();
        
        // === INFO-BOX ===
        VBox infoBox = createInfoBox();
        
        // === DATEI-INFO ===
        Label filePathLabel = new Label("Sync-Datei: [MT-Verzeichnis(se)]\\last_known_signals.csv");
        filePathLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #666666; -fx-font-family: 'Courier New', monospace;");
        
        content.getChildren().addAll(
            syncEnabledCheckBox,
            new Separator(),
            dir1Section,
            new Separator(),
            dir2Section,
            new Separator(),
            infoBox,
            filePathLabel
        );
        
        TitledPane panel = new TitledPane("📂 MetaTrader-Synchronisation (1-2 Verzeichnisse)", content);
        panel.setExpanded(false);
        
        LOGGER.info("MetaTraderPanel erstellt (Dual-Directory Support)");
        return panel;
    }
    
    /**
     * Erstellt Verzeichnis-1-Sektion
     */
    private VBox createDirectorySection1() {
        VBox section = new VBox(10);
        
        Label header = new Label("📂 Verzeichnis 1 (erforderlich):");
        header.setFont(Font.font("System", FontWeight.BOLD, 11));
        header.setStyle("-fx-text-fill: #1976d2;");
        
        HBox dirBox = new HBox(10);
        dirBox.setAlignment(Pos.CENTER_LEFT);
        
        Label dirLabel = new Label("Pfad:");
        dirLabel.setPrefWidth(60);
        
        dirField1 = new TextField();
        dirField1.setPrefWidth(350);
        dirField1.setPromptText("C:\\Users\\...\\Terminal\\xxx\\MQL5\\Files");
        
        browseButton1 = new Button("🔍");
        browseButton1.setOnAction(e -> browseDirectory1());
        browseButton1.setStyle("-fx-background-color: #17a2b8; -fx-text-fill: white;");
        browseButton1.setTooltip(new Tooltip("Verzeichnis 1 durchsuchen"));
        
        testButton1 = new Button("🔧");
        testButton1.setOnAction(e -> testDirectory1());
        testButton1.setStyle("-fx-background-color: #fd7e14; -fx-text-fill: white;");
        testButton1.setTooltip(new Tooltip("Verzeichnis 1 testen"));
        
        dirBox.getChildren().addAll(dirLabel, dirField1, browseButton1, testButton1);
        
        statusLabel1 = new Label("Status: Nicht konfiguriert");
        statusLabel1.setStyle("-fx-text-fill: #666666; -fx-font-size: 11px; -fx-padding: 0 0 0 70;");
        
        section.getChildren().addAll(header, dirBox, statusLabel1);
        return section;
    }
    
    /**
     * Erstellt Verzeichnis-2-Sektion
     */
    private VBox createDirectorySection2() {
        VBox section = new VBox(10);
        
        Label header = new Label("📂 Verzeichnis 2 (optional):");
        header.setFont(Font.font("System", FontWeight.BOLD, 11));
        header.setStyle("-fx-text-fill: #1976d2;");
        
        HBox dirBox = new HBox(10);
        dirBox.setAlignment(Pos.CENTER_LEFT);
        
        Label dirLabel = new Label("Pfad:");
        dirLabel.setPrefWidth(60);
        
        dirField2 = new TextField();
        dirField2.setPrefWidth(350);
        dirField2.setPromptText("Optional: Zweites MetaTrader-Verzeichnis");
        
        browseButton2 = new Button("🔍");
        browseButton2.setOnAction(e -> browseDirectory2());
        browseButton2.setStyle("-fx-background-color: #17a2b8; -fx-text-fill: white;");
        browseButton2.setTooltip(new Tooltip("Verzeichnis 2 durchsuchen"));
        
        testButton2 = new Button("🔧");
        testButton2.setOnAction(e -> testDirectory2());
        testButton2.setStyle("-fx-background-color: #fd7e14; -fx-text-fill: white;");
        testButton2.setTooltip(new Tooltip("Verzeichnis 2 testen"));
        
        dirBox.getChildren().addAll(dirLabel, dirField2, browseButton2, testButton2);
        
        statusLabel2 = new Label("Status: Nicht konfiguriert");
        statusLabel2.setStyle("-fx-text-fill: #666666; -fx-font-size: 11px; -fx-padding: 0 0 0 70;");
        
        section.getChildren().addAll(header, dirBox, statusLabel2);
        return section;
    }
    
    /**
     * Erstellt Info-Box
     */
    private VBox createInfoBox() {
        VBox infoBox = new VBox(8);
        infoBox.setStyle("-fx-background-color: #e3f2fd; -fx-padding: 15; -fx-border-color: #2196f3; -fx-border-width: 1;");
        
        Label infoTitle = new Label("📋 MetaTrader-Datei-Synchronisation (Dual-Directory)");
        infoTitle.setFont(Font.font("System", FontWeight.BOLD, 12));
        infoTitle.setStyle("-fx-text-fill: #1976d2;");
        
        Label info1 = new Label("• Synchronisiert last_known_signals.csv automatisch in 1 oder 2 MetaTrader-Verzeichnisse");
        Label info2 = new Label("• Datei wird bei jeder Signalwechsel-Erkennung aktualisiert");
        Label info3 = new Label("• Format: Währungspaar;Letztes_Signal;Prozent");
        Label info4 = new Label("• Währungsersetzung: XAUUSD→GOLD, XAGUSD→SILBER");
        Label info5 = new Label("• Verzeichnis 1 ist erforderlich, Verzeichnis 2 ist optional");
        Label info6 = new Label("• Verzeichnisse müssen existieren und beschreibbar sein");
        
        info1.setStyle("-fx-font-size: 11px; -fx-text-fill: #424242;");
        info2.setStyle("-fx-font-size: 11px; -fx-text-fill: #424242;");
        info3.setStyle("-fx-font-size: 11px; -fx-text-fill: #424242;");
        info4.setStyle("-fx-font-size: 11px; -fx-text-fill: #424242;");
        info5.setStyle("-fx-font-size: 11px; -fx-text-fill: #2e7d32; -fx-font-weight: bold;");
        info6.setStyle("-fx-font-size: 11px; -fx-text-fill: #e65100; -fx-font-weight: bold;");
        
        infoBox.getChildren().addAll(infoTitle, info1, info2, info3, info4, info5, info6);
        return infoBox;
    }
    
    /**
     * Durchsucht Verzeichnis 1
     */
    private void browseDirectory1() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("MetaTrader-Verzeichnis 1 auswählen");
        
        String currentPath = dirField1.getText().trim();
        if (!currentPath.isEmpty()) {
            File currentDir = new File(currentPath);
            if (currentDir.exists() && currentDir.isDirectory()) {
                chooser.setInitialDirectory(currentDir);
            }
        }
        
        File selected = chooser.showDialog(parentStage);
        if (selected != null) {
            dirField1.setText(selected.getAbsolutePath());
            logStatus("MetaTrader-Verzeichnis 1 ausgewählt: " + selected.getAbsolutePath());
        }
    }
    
    /**
     * Durchsucht Verzeichnis 2
     */
    private void browseDirectory2() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("MetaTrader-Verzeichnis 2 auswählen (optional)");
        
        String currentPath = dirField2.getText().trim();
        if (!currentPath.isEmpty()) {
            File currentDir = new File(currentPath);
            if (currentDir.exists() && currentDir.isDirectory()) {
                chooser.setInitialDirectory(currentDir);
            }
        }
        
        File selected = chooser.showDialog(parentStage);
        if (selected != null) {
            dirField2.setText(selected.getAbsolutePath());
            logStatus("MetaTrader-Verzeichnis 2 ausgewählt: " + selected.getAbsolutePath());
        }
    }
    
    /**
     * Testet Verzeichnis 1
     */
    private void testDirectory1() {
        String dirPath = dirField1.getText().trim();
        
        if (dirPath.isEmpty()) {
            updateStatus1("❌ Kein Pfad angegeben");
            logStatus("MetaTrader-Test 1: Kein Verzeichnis angegeben");
            return;
        }
        
        File dir = new File(dirPath);
        
        if (!dir.exists()) {
            updateStatus1("❌ Verzeichnis existiert nicht");
            logStatus("MetaTrader-Test 1 FEHLER: Verzeichnis existiert nicht");
            return;
        }
        
        if (!dir.isDirectory()) {
            updateStatus1("❌ Pfad ist kein Verzeichnis");
            logStatus("MetaTrader-Test 1 FEHLER: Pfad ist kein Verzeichnis");
            return;
        }
        
        if (!dir.canWrite()) {
            updateStatus1("⚠️ Verzeichnis nicht beschreibbar");
            logStatus("MetaTrader-Test 1 WARNUNG: Verzeichnis nicht beschreibbar");
            return;
        }
        
        updateStatus1("✅ Verzeichnis OK");
        logStatus("MetaTrader-Test 1 erfolgreich: " + dirPath);
        logStatus("- Verzeichnis existiert und ist beschreibbar");
    }
    
    /**
     * Testet Verzeichnis 2
     */
    private void testDirectory2() {
        String dirPath = dirField2.getText().trim();
        
        if (dirPath.isEmpty()) {
            updateStatus2("ℹ️ Optional - nicht konfiguriert");
            logStatus("MetaTrader-Test 2: Kein Verzeichnis angegeben (optional)");
            return;
        }
        
        File dir = new File(dirPath);
        
        if (!dir.exists()) {
            updateStatus2("❌ Verzeichnis existiert nicht");
            logStatus("MetaTrader-Test 2 FEHLER: Verzeichnis existiert nicht");
            return;
        }
        
        if (!dir.isDirectory()) {
            updateStatus2("❌ Pfad ist kein Verzeichnis");
            logStatus("MetaTrader-Test 2 FEHLER: Pfad ist kein Verzeichnis");
            return;
        }
        
        if (!dir.canWrite()) {
            updateStatus2("⚠️ Verzeichnis nicht beschreibbar");
            logStatus("MetaTrader-Test 2 WARNUNG: Verzeichnis nicht beschreibbar");
            return;
        }
        
        updateStatus2("✅ Verzeichnis OK");
        logStatus("MetaTrader-Test 2 erfolgreich: " + dirPath);
        logStatus("- Verzeichnis existiert und ist beschreibbar");
    }
    
    /**
     * Aktualisiert Aktivierungszustand der Felder
     */
    private void updateFieldsState() {
        boolean enabled = syncEnabledCheckBox.isSelected();
        
        // Verzeichnis 1
        dirField1.setDisable(!enabled);
        browseButton1.setDisable(!enabled);
        testButton1.setDisable(!enabled);
        
        // Verzeichnis 2
        dirField2.setDisable(!enabled);
        browseButton2.setDisable(!enabled);
        testButton2.setDisable(!enabled);
        
        // Status-Update
        if (enabled) {
            if (dirField1.getText().trim().isEmpty()) {
                updateStatus1("⚠️ Bitte Verzeichnis 1 konfigurieren");
            }
            if (dirField2.getText().trim().isEmpty()) {
                updateStatus2("ℹ️ Optional - nicht konfiguriert");
            }
        } else {
            updateStatus1("Deaktiviert");
            updateStatus2("Deaktiviert");
        }
    }
    
    /**
     * Aktualisiert Status-Label 1
     */
    private void updateStatus1(String status) {
        Platform.runLater(() -> statusLabel1.setText("Dir 1: " + status));
    }
    
    /**
     * Aktualisiert Status-Label 2
     */
    private void updateStatus2(String status) {
        Platform.runLater(() -> statusLabel2.setText("Dir 2: " + status));
    }
    
    /**
     * Loggt Status-Nachricht
     */
    private void logStatus(String message) {
        if (statusLogger != null) {
            statusLogger.accept(message + "\n");
        }
    }
    
    /**
     * Lädt Konfiguration in UI-Felder
     * @param config EmailConfig mit MetaTrader-Einstellungen
     */
    public void loadConfiguration(EmailConfig config) {
        syncEnabledCheckBox.setSelected(config.isMetatraderSyncEnabled());
        
        if (config.hasMetatraderDirectory()) {
            dirField1.setText(config.getMetatraderDirectory());
            LOGGER.info("MetaTrader-Verzeichnis 1 geladen: " + config.getMetatraderDirectory());
        } else {
            dirField1.setText("");
        }
        
        if (config.hasMetatraderDirectory2()) {
            dirField2.setText(config.getMetatraderDirectory2());
            LOGGER.info("MetaTrader-Verzeichnis 2 geladen: " + config.getMetatraderDirectory2());
        } else {
            dirField2.setText("");
        }
        
        updateFieldsState();
        
        // Status-Update für beide Verzeichnisse
        if (config.isMetatraderSyncEnabled()) {
            if (config.hasMetatraderDirectory()) {
                File dir1 = new File(config.getMetatraderDirectory());
                if (dir1.exists() && dir1.isDirectory() && dir1.canWrite()) {
                    updateStatus1("✅ Geladen und verfügbar");
                } else {
                    updateStatus1("⚠️ Geladen aber nicht verfügbar");
                }
            } else {
                updateStatus1("⚠️ Nicht konfiguriert");
            }
            
            if (config.hasMetatraderDirectory2()) {
                File dir2 = new File(config.getMetatraderDirectory2());
                if (dir2.exists() && dir2.isDirectory() && dir2.canWrite()) {
                    updateStatus2("✅ Geladen und verfügbar");
                } else {
                    updateStatus2("⚠️ Geladen aber nicht verfügbar");
                }
            } else {
                updateStatus2("ℹ️ Optional - nicht konfiguriert");
            }
        } else {
            updateStatus1("Deaktiviert");
            updateStatus2("Deaktiviert");
        }
    }
    
    /**
     * Speichert UI-Werte in Konfiguration
     * @param config EmailConfig zum Aktualisieren
     */
    public void saveConfiguration(EmailConfig config) {
        config.setMetatraderSyncEnabled(syncEnabledCheckBox.isSelected());
        config.setMetatraderDirectory(dirField1.getText().trim());
        config.setMetatraderDirectory2(dirField2.getText().trim());
        
        LOGGER.info("MetaTrader-Konfiguration gespeichert: Enabled=" + config.isMetatraderSyncEnabled() + 
                   ", Dirs=" + config.getMetatraderDirectoryCount());
    }
    
    /**
     * Validiert MetaTrader-Konfiguration
     * @return ValidationResult mit Ergebnis
     */
    public ValidationResult validate() {
        if (!syncEnabledCheckBox.isSelected()) {
            return new ValidationResult(true, ""); // Keine Validierung wenn deaktiviert
        }
        
        String dir1 = dirField1.getText().trim();
        String dir2 = dirField2.getText().trim();
        
        // Verzeichnis 1 ist erforderlich
        if (dir1.isEmpty()) {
            return new ValidationResult(false, 
                "MetaTrader-Synchronisation aktiviert, aber Verzeichnis 1 ist nicht konfiguriert.\n" +
                "Verzeichnis 1 ist erforderlich, Verzeichnis 2 ist optional.");
        }
        
        // Validiere Verzeichnis 1
        File dirFile1 = new File(dir1);
        if (!dirFile1.exists()) {
            return new ValidationResult(false, "MetaTrader-Verzeichnis 1 existiert nicht:\n" + dir1);
        }
        if (!dirFile1.isDirectory()) {
            return new ValidationResult(false, "MetaTrader-Pfad 1 ist kein Verzeichnis:\n" + dir1);
        }
        if (!dirFile1.canWrite()) {
            return new ValidationResult(false, 
                "MetaTrader-Verzeichnis 1 ist nicht beschreibbar:\n" + dir1 + "\n\nSynchronisation wird fehlschlagen!");
        }
        
        // Validiere Verzeichnis 2 (falls gesetzt)
        if (!dir2.isEmpty()) {
            File dirFile2 = new File(dir2);
            if (!dirFile2.exists()) {
                return new ValidationResult(false, 
                    "MetaTrader-Verzeichnis 2 existiert nicht:\n" + dir2 + "\n\n" +
                    "Verzeichnis 2 ist optional. Möchten Sie es leer lassen?");
            }
            if (!dirFile2.isDirectory()) {
                return new ValidationResult(false, "MetaTrader-Pfad 2 ist kein Verzeichnis:\n" + dir2);
            }
            if (!dirFile2.canWrite()) {
                LOGGER.warning("MetaTrader-Verzeichnis 2 ist nicht beschreibbar: " + dir2);
                // Nicht abbrechen bei Dir 2, nur warnen
            }
        }
        
        return new ValidationResult(true, "");
    }
    
    /**
     * Validierungs-Ergebnis
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;
        
        public ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }
        
        public boolean isValid() { return valid; }
        public String getErrorMessage() { return errorMessage; }
    }
}