package com.fxsssi.extractor.gui;

import com.fxssi.extractor.notification.EmailConfig;
import com.fxssi.extractor.notification.EmailService;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.util.logging.Logger;

/**
 * Konfigurationsfenster für E-Mail-Einstellungen
 * Ermöglicht die Konfiguration von GMX-Server und Benachrichtigungspräferenzen
 * ERWEITERT um Signal-Threshold Anti-Spam-Konfiguration
 * ERWEITERT um MetaTrader-Verzeichnis-Synchronisation
 * 
 * @author Generated for FXSSI Email Configuration
 * @version 1.2 - MetaTrader-Verzeichnis Integration
 */
public class EmailConfigWindow {
    
    private static final Logger LOGGER = Logger.getLogger(EmailConfigWindow.class.getName());
    
    private Stage stage;
    private Scene scene;
    private BorderPane root;
    
    // UI-Komponenten - Server-Konfiguration
    private TextField smtpHostField;
    private Spinner<Integer> smtpPortSpinner;
    private CheckBox startTlsCheckBox;
    private CheckBox sslCheckBox;
    private TextField usernameField;
    private PasswordField passwordField;
    
    // UI-Komponenten - E-Mail-Konfiguration
    private TextField fromEmailField;
    private TextField fromNameField;
    private TextField toEmailField;
    private CheckBox emailEnabledCheckBox;
    
    // UI-Komponenten - Benachrichtigungspräferenzen
    private CheckBox criticalChangesCheckBox;
    private CheckBox highChangesCheckBox;
    private CheckBox allChangesCheckBox;
    private Spinner<Integer> maxEmailsSpinner;
    
    // UI-Komponenten - Signal-Threshold Anti-Spam
    private Spinner<Double> signalThresholdSpinner;
    private Button thresholdHelpButton;
    private Label thresholdExampleLabel;
    
    // NEU: UI-Komponenten - MetaTrader-Konfiguration
    private TextField metatraderDirField;
    private Button metatraderBrowseButton;
    private Button metatraderTestButton;
    private Label metatraderStatusLabel;
    private CheckBox metatraderSyncEnabledCheckBox;
    
    // UI-Komponenten - Buttons
    private Button gmxDefaultsButton;
    private Button testConnectionButton;
    private Button sendTestEmailButton;
    private Button saveButton;
    private Button cancelButton;
    
    // UI-Komponenten - Status
    private TextArea statusArea;
    private ProgressIndicator progressIndicator;
    
    // Services
    private final EmailConfig emailConfig;
    private EmailService emailService;
    private final String dataDirectory;
    
    // NEU: Callback-Interface für MetaTrader-Konfiguration
    @FunctionalInterface
    public interface MetaTraderConfigurationCallback {
        void configure(boolean enabled, String directory);
    }
    
    private MetaTraderConfigurationCallback metaTraderCallback = null;
    
    /**
     * Konstruktor
     * @param parentStage Parent-Fenster
     * @param dataDirectory Datenverzeichnis für Konfiguration
     */
    public EmailConfigWindow(Stage parentStage, String dataDirectory) {
        this.dataDirectory = dataDirectory;
        this.emailConfig = new EmailConfig(dataDirectory);
        
        // Lade bestehende Konfiguration
        emailConfig.loadConfig();
        
        // Initialisiere E-Mail-Service
        this.emailService = new EmailService(emailConfig);
        
        createWindow(parentStage);
        loadConfigurationIntoFields();
    }
    
    /**
     * Erstellt und konfiguriert das Konfigurationsfenster
     */
    private void createWindow(Stage parentStage) {
        stage = new Stage();
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(parentStage);
        stage.setTitle("E-Mail-Konfiguration + MetaTrader-Synchronisation - FXSSI Monitor");
        stage.setWidth(800);  // Breiter wegen MetaTrader-Konfiguration
        stage.setHeight(1000); // Höher wegen zusätzlicher Felder
        stage.setMinWidth(700);
        stage.setMinHeight(800);
        
        // Zentriere relativ zum Parent
        if (parentStage != null) {
            stage.setX(parentStage.getX() + 50);
            stage.setY(parentStage.getY() + 30);
        }
        
        // Erstelle Layout
        root = new BorderPane();
        root.setPadding(new Insets(20));
        
        // Erstelle UI-Bereiche
        VBox topArea = createTopArea();
        ScrollPane centerArea = createCenterArea();
        HBox bottomArea = createBottomArea();
        
        root.setTop(topArea);
        root.setCenter(centerArea);
        root.setBottom(bottomArea);
        
        // Erstelle Scene
        scene = new Scene(root);
        stage.setScene(scene);
        
        LOGGER.info("E-Mail-Konfigurationsfenster erstellt (mit MetaTrader-Synchronisation)");
    }
    
    /**
     * Erstellt den oberen Bereich (Titel)
     */
    private VBox createTopArea() {
        VBox topArea = new VBox(10);
        
        Label titleLabel = new Label("📧 E-Mail-Konfiguration + MetaTrader-Synchronisation");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        titleLabel.setStyle("-fx-text-fill: #2E86AB;");
        
        Label descriptionLabel = new Label("Konfigurieren Sie GMX-Server, Anti-Spam-Filter, Benachrichtigungseinstellungen und MetaTrader-Datei-Synchronisation");
        descriptionLabel.setFont(Font.font(14));
        descriptionLabel.setStyle("-fx-text-fill: #666666;");
        
        topArea.getChildren().addAll(titleLabel, descriptionLabel, new Separator());
        return topArea;
    }
    
    /**
     * Erstellt den mittleren Bereich (Konfigurationsfelder) mit ScrollPane
     */
    private ScrollPane createCenterArea() {
        VBox centerContent = new VBox(20);
        centerContent.setPadding(new Insets(10));
        
        // Server-Konfiguration
        TitledPane serverPane = createServerConfigPane();
        
        // E-Mail-Konfiguration
        TitledPane emailPane = createEmailConfigPane();
        
        // Benachrichtigungspräferenzen
        TitledPane notificationPane = createNotificationConfigPane();
        
        // NEU: MetaTrader-Konfiguration
        TitledPane metatraderPane = createMetaTraderConfigPane();
        
        // Status-Bereich
        TitledPane statusPane = createStatusPane();
        
        centerContent.getChildren().addAll(serverPane, emailPane, notificationPane, metatraderPane, statusPane);
        
        ScrollPane scrollPane = new ScrollPane(centerContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        
        return scrollPane;
    }
    
    /**
     * NEU: Erstellt den MetaTrader-Konfigurationsbereich
     */
    private TitledPane createMetaTraderConfigPane() {
        VBox metatraderContent = new VBox(15);
        metatraderContent.setPadding(new Insets(15));
        
        // Aktivierungs-CheckBox
        metatraderSyncEnabledCheckBox = new CheckBox("MetaTrader-Datei-Synchronisation aktivieren");
        metatraderSyncEnabledCheckBox.setFont(Font.font("System", FontWeight.BOLD, 12));
        metatraderSyncEnabledCheckBox.setStyle("-fx-text-fill: #2E86AB;");
        metatraderSyncEnabledCheckBox.setOnAction(e -> updateMetaTraderFieldsState());
        
        // Verzeichnis-Eingabe
        HBox dirBox = new HBox(10);
        dirBox.setAlignment(Pos.CENTER_LEFT);
        Label dirLabel = new Label("MetaTrader-Verzeichnis:");
        dirLabel.setPrefWidth(180);
        
        metatraderDirField = new TextField();
        metatraderDirField.setPrefWidth(300);
        metatraderDirField.setPromptText("C:\\Users\\Username\\AppData\\Roaming\\MetaQuotes\\Terminal\\xxx\\MQL5\\Files");
        
        metatraderBrowseButton = new Button("📁 Durchsuchen");
        metatraderBrowseButton.setOnAction(e -> browseMetaTraderDirectory());
        metatraderBrowseButton.setStyle("-fx-background-color: #17a2b8; -fx-text-fill: white;");
        
        dirBox.getChildren().addAll(dirLabel, metatraderDirField, metatraderBrowseButton);
        
        // Test-Button
        HBox testBox = new HBox(10);
        testBox.setAlignment(Pos.CENTER_LEFT);
        metatraderTestButton = new Button("🔧 Verzeichnis testen");
        metatraderTestButton.setOnAction(e -> testMetaTraderDirectory());
        metatraderTestButton.setStyle("-fx-background-color: #fd7e14; -fx-text-fill: white;");
        
        testBox.getChildren().add(metatraderTestButton);
        
        // Status-Label
        metatraderStatusLabel = new Label("Status: Nicht konfiguriert");
        metatraderStatusLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 11px;");
        
        // Info-Box
        VBox infoBox = new VBox(8);
        infoBox.setStyle("-fx-background-color: #e3f2fd; -fx-padding: 15; -fx-border-color: #2196f3; -fx-border-width: 1;");
        
        Label infoTitle = new Label("📋 MetaTrader-Datei-Synchronisation");
        infoTitle.setFont(Font.font("System", FontWeight.BOLD, 12));
        infoTitle.setStyle("-fx-text-fill: #1976d2;");
        
        Label info1 = new Label("• Synchronisiert last_known_signals.csv automatisch ins MetaTrader-Verzeichnis");
        Label info2 = new Label("• Datei wird bei jeder Signalwechsel-Erkennung aktualisiert");
        Label info3 = new Label("• Ermöglicht MT5/MT4 Expert Advisors Zugriff auf aktuelle Signale");
        Label info4 = new Label("• Typischer Pfad: Terminal\\xxx\\MQL5\\Files\\");
        Label info5 = new Label("• Verzeichnis muss existieren und beschreibbar sein");
        
        info1.setStyle("-fx-font-size: 11px; -fx-text-fill: #424242;");
        info2.setStyle("-fx-font-size: 11px; -fx-text-fill: #424242;");
        info3.setStyle("-fx-font-size: 11px; -fx-text-fill: #424242;");
        info4.setStyle("-fx-font-size: 11px; -fx-text-fill: #424242;");
        info5.setStyle("-fx-font-size: 11px; -fx-text-fill: #e65100; -fx-font-weight: bold;");
        
        infoBox.getChildren().addAll(infoTitle, info1, info2, info3, info4, info5);
        
        // Dateipfad-Info
        Label filePathLabel = new Label("Synchronisierte Datei: [MetaTrader-Verzeichnis]\\last_known_signals.csv");
        filePathLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #666666; -fx-font-family: 'Courier New', monospace;");
        
        metatraderContent.getChildren().addAll(
            metatraderSyncEnabledCheckBox, dirBox, testBox, metatraderStatusLabel,
            new Separator(), infoBox, filePathLabel
        );
        
        TitledPane metatraderPane = new TitledPane("📂 MetaTrader-Synchronisation", metatraderContent);
        metatraderPane.setExpanded(false);
        return metatraderPane;
    }
    
    /**
     * NEU: Durchsucht nach MetaTrader-Verzeichnis
     */
    private void browseMetaTraderDirectory() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("MetaTrader-Verzeichnis auswählen");
        
        // Setze initialen Pfad falls vorhanden
        String currentPath = metatraderDirField.getText().trim();
        if (!currentPath.isEmpty()) {
            File currentDir = new File(currentPath);
            if (currentDir.exists() && currentDir.isDirectory()) {
                directoryChooser.setInitialDirectory(currentDir);
            }
        } else {
            // Versuche typische MetaTrader-Pfade zu finden
            String userHome = System.getProperty("user.home");
            File mtPath = new File(userHome + "\\AppData\\Roaming\\MetaQuotes\\Terminal");
            if (mtPath.exists()) {
                directoryChooser.setInitialDirectory(mtPath);
            }
        }
        
        File selectedDirectory = directoryChooser.showDialog(stage);
        
        if (selectedDirectory != null) {
            metatraderDirField.setText(selectedDirectory.getAbsolutePath());
            updateMetaTraderStatus("Verzeichnis ausgewählt - bitte testen");
            LOGGER.info("MetaTrader-Verzeichnis ausgewählt: " + selectedDirectory.getAbsolutePath());
        }
    }
    
    /**
     * NEU: Testet das MetaTrader-Verzeichnis
     */
    private void testMetaTraderDirectory() {
        String dirPath = metatraderDirField.getText().trim();
        
        if (dirPath.isEmpty()) {
            updateMetaTraderStatus("❌ Kein Verzeichnis angegeben");
            showMetaTraderAlert("Fehler", "Bitte geben Sie ein MetaTrader-Verzeichnis an.");
            return;
        }
        
        try {
            File dir = new File(dirPath);
            
            if (!dir.exists()) {
                updateMetaTraderStatus("❌ Verzeichnis existiert nicht");
                showMetaTraderAlert("Verzeichnis nicht gefunden", 
                    "Das angegebene Verzeichnis existiert nicht:\n" + dirPath + 
                    "\n\nBitte überprüfen Sie den Pfad.");
                return;
            }
            
            if (!dir.isDirectory()) {
                updateMetaTraderStatus("❌ Pfad ist kein Verzeichnis");
                showMetaTraderAlert("Ungültiger Pfad", 
                    "Der angegebene Pfad ist kein Verzeichnis:\n" + dirPath);
                return;
            }
            
            if (!dir.canWrite()) {
                updateMetaTraderStatus("❌ Verzeichnis nicht beschreibbar");
                showMetaTraderAlert("Keine Schreibberechtigung", 
                    "Das Verzeichnis ist nicht beschreibbar:\n" + dirPath + 
                    "\n\nBitte überprüfen Sie die Berechtigungen.");
                return;
            }
            
            // Test-Datei erstellen
            File testFile = new File(dir, "fxssi_test.tmp");
            try {
                if (testFile.createNewFile()) {
                    testFile.delete();
                    updateMetaTraderStatus("✅ Verzeichnis gültig und beschreibbar");
                    showMetaTraderAlert("Test erfolgreich", 
                        "✅ MetaTrader-Verzeichnis ist gültig!\n\n" +
                        "Verzeichnis: " + dirPath + "\n" +
                        "Status: Existiert, beschreibbar\n" +
                        "Synchronisation: Bereit\n\n" +
                        "Die Datei 'last_known_signals.csv' wird automatisch synchronisiert.");
                    
                    LOGGER.info("MetaTrader-Verzeichnis erfolgreich getestet: " + dirPath);
                } else {
                    updateMetaTraderStatus("❌ Fehler beim Schreibtest");
                    showMetaTraderAlert("Schreibtest fehlgeschlagen", 
                        "Konnte keine Test-Datei im Verzeichnis erstellen:\n" + dirPath);
                }
            } catch (Exception e) {
                updateMetaTraderStatus("❌ Schreibtest fehlgeschlagen: " + e.getMessage());
                showMetaTraderAlert("Fehler beim Schreibtest", 
                    "Fehler beim Erstellen einer Test-Datei:\n" + e.getMessage());
            }
            
        } catch (Exception e) {
            updateMetaTraderStatus("❌ Fehler beim Test: " + e.getMessage());
            showMetaTraderAlert("Testfehler", "Fehler beim Testen des Verzeichnisses:\n" + e.getMessage());
            LOGGER.warning("Fehler beim Testen des MetaTrader-Verzeichnisses: " + e.getMessage());
        }
    }
    
    /**
     * NEU: Aktualisiert den MetaTrader-Status
     */
    private void updateMetaTraderStatus(String status) {
        if (metatraderStatusLabel != null) {
            Platform.runLater(() -> metatraderStatusLabel.setText("Status: " + status));
        }
    }
    
    /**
     * NEU: Zeigt MetaTrader-Alert
     */
    private void showMetaTraderAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("MetaTrader-Verzeichnis: " + title);
            alert.setHeaderText("MetaTrader-Datei-Synchronisation");
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    
    /**
     * NEU: Aktualisiert den Zustand der MetaTrader-Felder
     */
    private void updateMetaTraderFieldsState() {
        boolean enabled = metatraderSyncEnabledCheckBox.isSelected();
        
        metatraderDirField.setDisable(!enabled);
        metatraderBrowseButton.setDisable(!enabled);
        metatraderTestButton.setDisable(!enabled);
        
        if (!enabled) {
            updateMetaTraderStatus("Deaktiviert");
        } else if (metatraderDirField.getText().trim().isEmpty()) {
            updateMetaTraderStatus("Verzeichnis erforderlich");
        }
    }
    
    /**
     * Erstellt den Server-Konfigurationsbereich
     */
    private TitledPane createServerConfigPane() {
        VBox serverContent = new VBox(15);
        serverContent.setPadding(new Insets(15));
        
        // SMTP-Host
        HBox hostBox = new HBox(10);
        hostBox.setAlignment(Pos.CENTER_LEFT);
        Label hostLabel = new Label("SMTP-Server:");
        hostLabel.setPrefWidth(120);
        smtpHostField = new TextField();
        smtpHostField.setPrefWidth(200);
        smtpHostField.setPromptText("mail.gmx.net");
        
        gmxDefaultsButton = new Button("GMX Standard");
        gmxDefaultsButton.setOnAction(e -> setGmxDefaults());
        gmxDefaultsButton.setStyle("-fx-background-color: #28a745; -fx-text-fill: white;");
        
        hostBox.getChildren().addAll(hostLabel, smtpHostField, gmxDefaultsButton);
        
        // SMTP-Port
        HBox portBox = new HBox(10);
        portBox.setAlignment(Pos.CENTER_LEFT);
        Label portLabel = new Label("Port:");
        portLabel.setPrefWidth(120);
        smtpPortSpinner = new Spinner<>(1, 65535, 587);
        smtpPortSpinner.setPrefWidth(100);
        smtpPortSpinner.setEditable(true);
        
        Label portHintLabel = new Label("(587 für STARTTLS, 465 für SSL)");
        portHintLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 11px;");
        
        portBox.getChildren().addAll(portLabel, smtpPortSpinner, portHintLabel);
        
        // Verschlüsselung
        HBox encryptionBox = new HBox(15);
        encryptionBox.setAlignment(Pos.CENTER_LEFT);
        Label encryptionLabel = new Label("Verschlüsselung:");
        encryptionLabel.setPrefWidth(120);
        
        startTlsCheckBox = new CheckBox("STARTTLS");
        sslCheckBox = new CheckBox("SSL/TLS");
        
        // Mutual exclusion für Verschlüsselung
        startTlsCheckBox.setOnAction(e -> {
            if (startTlsCheckBox.isSelected()) {
                sslCheckBox.setSelected(false);
            }
        });
        sslCheckBox.setOnAction(e -> {
            if (sslCheckBox.isSelected()) {
                startTlsCheckBox.setSelected(false);
            }
        });
        
        encryptionBox.getChildren().addAll(encryptionLabel, startTlsCheckBox, sslCheckBox);
        
        // Benutzername
        HBox usernameBox = new HBox(10);
        usernameBox.setAlignment(Pos.CENTER_LEFT);
        Label usernameLabel = new Label("Benutzername:");
        usernameLabel.setPrefWidth(120);
        usernameField = new TextField();
        usernameField.setPrefWidth(250);
        usernameField.setPromptText("Ihre GMX E-Mail-Adresse");
        
        usernameBox.getChildren().addAll(usernameLabel, usernameField);
        
        // Passwort
        HBox passwordBox = new HBox(10);
        passwordBox.setAlignment(Pos.CENTER_LEFT);
        Label passwordLabel = new Label("Passwort:");
        passwordLabel.setPrefWidth(120);
        passwordField = new PasswordField();
        passwordField.setPrefWidth(250);
        passwordField.setPromptText("Ihr GMX Passwort");
        
        passwordBox.getChildren().addAll(passwordLabel, passwordField);
        
        // Test-Button
        HBox testBox = new HBox(10);
        testBox.setAlignment(Pos.CENTER_LEFT);
        testConnectionButton = new Button("🔧 Verbindung testen");
        testConnectionButton.setOnAction(e -> testConnection());
        testConnectionButton.setStyle("-fx-background-color: #007bff; -fx-text-fill: white;");
        
        testBox.getChildren().add(testConnectionButton);
        
        serverContent.getChildren().addAll(hostBox, portBox, encryptionBox, usernameBox, passwordBox, testBox);
        
        TitledPane serverPane = new TitledPane("🖥️ SMTP-Server Konfiguration", serverContent);
        serverPane.setExpanded(true);
        return serverPane;
    }
    
    /**
     * Erstellt den E-Mail-Konfigurationsbereich
     */
    private TitledPane createEmailConfigPane() {
        VBox emailContent = new VBox(15);
        emailContent.setPadding(new Insets(15));
        
        // Aktivierung
        emailEnabledCheckBox = new CheckBox("E-Mail-Benachrichtigungen aktivieren");
        emailEnabledCheckBox.setFont(Font.font("System", FontWeight.BOLD, 12));
        emailEnabledCheckBox.setStyle("-fx-text-fill: #28a745;");
        
        // Absender-E-Mail
        HBox fromEmailBox = new HBox(10);
        fromEmailBox.setAlignment(Pos.CENTER_LEFT);
        Label fromEmailLabel = new Label("Von (E-Mail):");
        fromEmailLabel.setPrefWidth(120);
        fromEmailField = new TextField();
        fromEmailField.setPrefWidth(250);
        fromEmailField.setPromptText("absender@gmx.de");
        
        fromEmailBox.getChildren().addAll(fromEmailLabel, fromEmailField);
        
        // Absender-Name
        HBox fromNameBox = new HBox(10);
        fromNameBox.setAlignment(Pos.CENTER_LEFT);
        Label fromNameLabel = new Label("Von (Name):");
        fromNameLabel.setPrefWidth(120);
        fromNameField = new TextField();
        fromNameField.setPrefWidth(250);
        fromNameField.setPromptText("FXSSI Monitor");
        
        fromNameBox.getChildren().addAll(fromNameLabel, fromNameField);
        
        // Empfänger-E-Mail
        HBox toEmailBox = new HBox(10);
        toEmailBox.setAlignment(Pos.CENTER_LEFT);
        Label toEmailLabel = new Label("An (E-Mail):");
        toEmailLabel.setPrefWidth(120);
        toEmailField = new TextField();
        toEmailField.setPrefWidth(250);
        toEmailField.setPromptText("empfaenger@domain.de");
        
        toEmailBox.getChildren().addAll(toEmailLabel, toEmailField);
        
        // Test-E-Mail senden
        HBox testEmailBox = new HBox(10);
        testEmailBox.setAlignment(Pos.CENTER_LEFT);
        sendTestEmailButton = new Button("📧 Test-E-Mail senden");
        sendTestEmailButton.setOnAction(e -> sendTestEmail());
        sendTestEmailButton.setStyle("-fx-background-color: #ffc107; -fx-text-fill: black;");
        
        testEmailBox.getChildren().add(sendTestEmailButton);
        
        emailContent.getChildren().addAll(emailEnabledCheckBox, fromEmailBox, fromNameBox, toEmailBox, testEmailBox);
        
        TitledPane emailPane = new TitledPane("📧 E-Mail Einstellungen", emailContent);
        emailPane.setExpanded(true);
        return emailPane;
    }
    
    /**
     * Erstellt den Benachrichtigungskonfigurationsbereich
     * ERWEITERT um Signal-Threshold Anti-Spam-Konfiguration
     */
    private TitledPane createNotificationConfigPane() {
        VBox notificationContent = new VBox(15);
        notificationContent.setPadding(new Insets(15));
        
        // Benachrichtigungstypen
        Label typeLabel = new Label("Benachrichtigungen senden bei:");
        typeLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
        
        criticalChangesCheckBox = new CheckBox("🚨 Kritischen Signalwechseln (BUY ↔ SELL)");
        criticalChangesCheckBox.setStyle("-fx-text-fill: #dc3545;");
        
        highChangesCheckBox = new CheckBox("⚠️ Wichtigen Signalwechseln (BUY/SELL → NEUTRAL)");
        highChangesCheckBox.setStyle("-fx-text-fill: #fd7e14;");
        
        allChangesCheckBox = new CheckBox("🔄 Allen Signalwechseln");
        allChangesCheckBox.setStyle("-fx-text-fill: #6c757d;");
        
        // Trennlinie vor Anti-Spam-Bereich
        Separator antiSpamSeparator = new Separator();
        
        // Anti-Spam-Überschrift
        Label antiSpamLabel = new Label("🛡️ Anti-Spam-Filter (Signal-Threshold):");
        antiSpamLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
        antiSpamLabel.setStyle("-fx-text-fill: #6f42c1;");
        
        // Signal-Threshold Konfiguration
        HBox thresholdBox = new HBox(10);
        thresholdBox.setAlignment(Pos.CENTER_LEFT);
        Label thresholdLabel = new Label("Mindest-Änderung:");
        thresholdLabel.setPrefWidth(150);
        
        signalThresholdSpinner = new Spinner<>(0.1, 50.0, 3.0, 0.1);
        signalThresholdSpinner.setPrefWidth(80);
        signalThresholdSpinner.setEditable(true);
        signalThresholdSpinner.valueProperty().addListener((obs, oldVal, newVal) -> updateThresholdExample());
        
        Label percentLabel = new Label("%");
        percentLabel.setStyle("-fx-font-weight: bold;");
        
        // Hilfe-Button für ausführliche Erklärung
        thresholdHelpButton = new Button("📖 Hilfe");
        thresholdHelpButton.setOnAction(e -> showThresholdHelp());
        thresholdHelpButton.setStyle("-fx-background-color: #17a2b8; -fx-text-fill: white;");
        
        thresholdBox.getChildren().addAll(thresholdLabel, signalThresholdSpinner, percentLabel, thresholdHelpButton);
        
        // Tooltip mit kompakter Erklärung
        Tooltip thresholdTooltip = new Tooltip();
        thresholdTooltip.setWrapText(true);
        thresholdTooltip.setPrefWidth(400);
        thresholdTooltip.setStyle("-fx-font-size: 11px;");
        signalThresholdSpinner.setTooltip(thresholdTooltip);
        
        // Beispiel-Label das sich dynamisch aktualisiert
        thresholdExampleLabel = new Label();
        thresholdExampleLabel.setStyle("-fx-text-fill: #495057; -fx-font-size: 11px; -fx-padding: 5 0 0 150;");
        thresholdExampleLabel.setWrapText(true);
        
        // Info-Box mit wichtigsten Informationen
        VBox thresholdInfoBox = new VBox(8);
        thresholdInfoBox.setStyle("-fx-background-color: #f8f9fa; -fx-padding: 10; -fx-border-color: #dee2e6; -fx-border-width: 1;");
        
        Label infoTitle = new Label("💡 Wie funktioniert der Anti-Spam-Filter?");
        infoTitle.setFont(Font.font("System", FontWeight.BOLD, 11));
        
        Label info1 = new Label("• Verhindert E-Mail-Spam bei Signal-Schwankungen um 55%-Marke");
        Label info2 = new Label("• E-Mail wird nur gesendet wenn Änderung ≥ Threshold");
        Label info3 = new Label("• Letzte gesendete Signale werden in CSV gespeichert:");
        Label info4 = new Label("  " + dataDirectory + "/signal_changes/lastsend.csv");
        
        info1.setStyle("-fx-font-size: 11px;");
        info2.setStyle("-fx-font-size: 11px;");
        info3.setStyle("-fx-font-size: 11px;");
        info4.setStyle("-fx-font-size: 11px; -fx-text-fill: #666666; -fx-font-family: 'Courier New', monospace;");
        
        thresholdInfoBox.getChildren().addAll(infoTitle, info1, info2, info3, info4);
        
        // E-Mail-Limit
        HBox limitBox = new HBox(10);
        limitBox.setAlignment(Pos.CENTER_LEFT);
        Label limitLabel = new Label("Max. E-Mails/Stunde:");
        limitLabel.setPrefWidth(150);
        maxEmailsSpinner = new Spinner<>(1, 100, 10);
        maxEmailsSpinner.setPrefWidth(80);
        maxEmailsSpinner.setEditable(true);
        
        Label limitHintLabel = new Label("(Schutz vor Spam)");
        limitHintLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 11px;");
        
        limitBox.getChildren().addAll(limitLabel, maxEmailsSpinner, limitHintLabel);
        
        notificationContent.getChildren().addAll(
            typeLabel, criticalChangesCheckBox, highChangesCheckBox, allChangesCheckBox, 
            antiSpamSeparator, antiSpamLabel, thresholdBox, thresholdExampleLabel, thresholdInfoBox,
            new Separator(), limitBox
        );
        
        TitledPane notificationPane = new TitledPane("📢 Benachrichtigungseinstellungen", notificationContent);
        notificationPane.setExpanded(true);
        return notificationPane;
    }
    
    /**
     * Erstellt den Status-Bereich
     */
    private TitledPane createStatusPane() {
        VBox statusContent = new VBox(10);
        statusContent.setPadding(new Insets(15));
        
        // Progress Indicator
        progressIndicator = new ProgressIndicator();
        progressIndicator.setPrefSize(30, 30);
        progressIndicator.setVisible(false);
        
        HBox progressBox = new HBox(10);
        progressBox.setAlignment(Pos.CENTER_LEFT);
        progressBox.getChildren().add(progressIndicator);
        
        // Status-TextArea
        statusArea = new TextArea();
        statusArea.setPrefHeight(120);
        statusArea.setEditable(false);
        statusArea.setStyle("-fx-font-family: 'Courier New', monospace; -fx-font-size: 11px;");
        statusArea.setText("Bereit für Konfiguration...\n\nHinweise:\n- GMX benötigt STARTTLS auf Port 587\n- Verwenden Sie Ihre vollständige E-Mail-Adresse als Benutzername\n- Signal-Threshold verhindert E-Mail-Spam bei Schwankungen\n- MetaTrader-Synchronisation für EA-Integration");
        
        statusContent.getChildren().addAll(progressBox, statusArea);
        
        TitledPane statusPane = new TitledPane("📊 Status & Meldungen", statusContent);
        statusPane.setExpanded(false);
        return statusPane;
    }
    
    /**
     * Erstellt den unteren Bereich (Buttons)
     */
    private HBox createBottomArea() {
        HBox bottomArea = new HBox(15);
        bottomArea.setAlignment(Pos.CENTER_RIGHT);
        bottomArea.setPadding(new Insets(15, 0, 0, 0));
        
        saveButton = new Button("💾 Speichern");
        saveButton.setOnAction(e -> saveConfiguration());
        saveButton.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-weight: bold;");
        saveButton.setPrefWidth(120);
        
        cancelButton = new Button("Abbrechen");
        cancelButton.setOnAction(e -> stage.close());
        cancelButton.setPrefWidth(120);
        
        bottomArea.getChildren().addAll(saveButton, cancelButton);
        return bottomArea;
    }
    
    /**
     * Lädt die bestehende Konfiguration in die UI-Felder
     * ERWEITERT um Signal-Threshold und MetaTrader-Konfiguration
     */
    private void loadConfigurationIntoFields() {
        // Server-Konfiguration
        smtpHostField.setText(emailConfig.getSmtpHost());
        smtpPortSpinner.getValueFactory().setValue(emailConfig.getSmtpPort());
        startTlsCheckBox.setSelected(emailConfig.isUseStartTLS());
        sslCheckBox.setSelected(emailConfig.isUseSSL());
        usernameField.setText(emailConfig.getUsername());
        passwordField.setText(emailConfig.getPassword());
        
        // E-Mail-Konfiguration
        fromEmailField.setText(emailConfig.getFromEmail());
        fromNameField.setText(emailConfig.getFromName());
        toEmailField.setText(emailConfig.getToEmail());
        emailEnabledCheckBox.setSelected(emailConfig.isEmailEnabled());
        
        // Benachrichtigungspräferenzen
        criticalChangesCheckBox.setSelected(emailConfig.isSendOnCriticalChanges());
        highChangesCheckBox.setSelected(emailConfig.isSendOnHighChanges());
        allChangesCheckBox.setSelected(emailConfig.isSendOnAllChanges());
        maxEmailsSpinner.getValueFactory().setValue(emailConfig.getMaxEmailsPerHour());
        
        // Signal-Threshold
        signalThresholdSpinner.getValueFactory().setValue(emailConfig.getSignalChangeThreshold());
        updateThresholdTooltipAndExample();
        
        // NEU: MetaTrader-Konfiguration laden
        loadMetaTraderConfiguration();
        
        LOGGER.info("Konfiguration in UI-Felder geladen (inkl. MetaTrader-Konfiguration)");
    }
    
    /**
     * NEU: Lädt die MetaTrader-Konfiguration
     */
    private void loadMetaTraderConfiguration() {
        try {
            // Hier würde normalerweise die MetaTrader-Konfiguration aus EmailConfig gelesen
            // Da EmailConfig das noch nicht unterstützt, verwenden wir Standardwerte
            
            // TODO: EmailConfig um MetaTrader-Felder erweitern
            // Vorerst: Felder leer lassen
            metatraderSyncEnabledCheckBox.setSelected(false);
            metatraderDirField.setText("");
            updateMetaTraderFieldsState();
            updateMetaTraderStatus("Nicht konfiguriert");
            
        } catch (Exception e) {
            LOGGER.warning("Fehler beim Laden der MetaTrader-Konfiguration: " + e.getMessage());
            metatraderSyncEnabledCheckBox.setSelected(false);
            updateMetaTraderFieldsState();
        }
    }
    
    /**
     * Aktualisiert Tooltip und Beispiel für Threshold (OHNE Rekursion)
     */
    private void updateThresholdTooltipAndExample() {
        double threshold = signalThresholdSpinner.getValue();
        
        // Aktualisiere EmailConfig für korrekte Tooltip-Generierung
        emailConfig.setSignalChangeThreshold(threshold);
        
        // Aktualisiere Tooltip
        String tooltipText = emailConfig.getSignalThresholdTooltip();
        signalThresholdSpinner.getTooltip().setText(tooltipText);
        
        // Aktualisiere Beispiel-Label
        String example = String.format(
            "Beispiel: Letzte E-Mail bei 55%% → Neue E-Mail erst ab %.1f%% oder %.1f%% (Differenz ≥ %.1f%%)",
            55.0 + threshold, 55.0 - threshold, threshold
        );
        thresholdExampleLabel.setText(example);
    }
    
    /**
     * Wrapper-Methode für Spinner-Listener (ruft die Haupt-Update-Methode auf)
     */
    private void updateThresholdExample() {
        updateThresholdTooltipAndExample();
    }
    
    /**
     * Zeigt das ausführliche Hilfe-Dialog für Signal-Threshold
     */
    private void showThresholdHelp() {
        // Aktualisiere Konfiguration für korrekte Erklärung
        updateConfigFromFields();
        
        Alert helpAlert = new Alert(Alert.AlertType.INFORMATION);
        helpAlert.setTitle("Signal-Threshold Anti-Spam-System");
        helpAlert.setHeaderText("🛡️ Ausführliche Funktionserklärung");
        
        // Verwende die ausführliche Erklärung aus EmailConfig
        String explanation = emailConfig.getSignalThresholdExplanation();
        
        // Erstelle scrollbare TextArea für lange Erklärung
        TextArea explanationArea = new TextArea(explanation);
        explanationArea.setEditable(false);
        explanationArea.setWrapText(true);
        explanationArea.setPrefWidth(600);
        explanationArea.setPrefHeight(500);
        explanationArea.setStyle("-fx-font-family: 'Courier New', monospace; -fx-font-size: 11px;");
        
        // Setze die TextArea als Content
        helpAlert.getDialogPane().setContent(explanationArea);
        helpAlert.getDialogPane().setPrefWidth(650);
        helpAlert.getDialogPane().setPrefHeight(600);
        
        // Zeige Dialog
        helpAlert.showAndWait();
        
        LOGGER.info("Signal-Threshold Hilfe-Dialog angezeigt");
    }
    
    /**
     * Setzt GMX-Standard-Werte
     */
    private void setGmxDefaults() {
        smtpHostField.setText("mail.gmx.net");
        smtpPortSpinner.getValueFactory().setValue(587);
        startTlsCheckBox.setSelected(true);
        sslCheckBox.setSelected(false);
        
        appendStatus("✅ GMX-Standard-Einstellungen gesetzt:\n");
        appendStatus("- Server: mail.gmx.net:587\n");
        appendStatus("- Verschlüsselung: STARTTLS\n");
        appendStatus("- Bitte Benutzername und Passwort eingeben\n\n");
        
        LOGGER.info("GMX-Standard-Einstellungen gesetzt");
    }
    
    /**
     * Testet die Verbindung zum E-Mail-Server
     */
    private void testConnection() {
        showProgress("Teste Verbindung...");
        
        new Thread(() -> {
            try {
                // Aktualisiere Konfiguration mit aktuellen Werten
                updateConfigFromFields();
                emailService.updateConfig(emailConfig);
                
                // Teste Verbindung
                EmailService.EmailSendResult result = emailService.testConnection();
                
                Platform.runLater(() -> {
                    hideProgress();
                    if (result.isSuccess()) {
                        appendStatus("✅ " + result.getMessage() + "\n");
                        appendStatus("- Verbindung erfolgreich hergestellt\n");
                        appendStatus("- Server-Konfiguration ist korrekt\n\n");
                    } else {
                        appendStatus("❌ " + result.getMessage() + "\n");
                        appendStatus("- Prüfen Sie Server, Port und Anmeldedaten\n");
                        appendStatus("- GMX: mail.gmx.net:587 mit STARTTLS\n\n");
                    }
                });
                
            } catch (Exception e) {
                Platform.runLater(() -> {
                    hideProgress();
                    appendStatus("❌ Verbindungstest fehlgeschlagen: " + e.getMessage() + "\n\n");
                });
            }
        }).start();
    }
    
    /**
     * Sendet eine Test-E-Mail
     */
    private void sendTestEmail() {
        showProgress("Sende Test-E-Mail...");
        
        new Thread(() -> {
            try {
                // Aktualisiere Konfiguration mit aktuellen Werten
                updateConfigFromFields();
                emailService.updateConfig(emailConfig);
                
                // Sende Test-E-Mail
                EmailService.EmailSendResult result = emailService.sendTestEmail();
                
                Platform.runLater(() -> {
                    hideProgress();
                    if (result.isSuccess()) {
                        appendStatus("📧 " + result.getMessage() + "\n");
                        appendStatus("- Test-E-Mail erfolgreich versendet\n");
                        appendStatus("- Prüfen Sie Ihren Posteingang\n");
                        appendStatus("- Threshold: " + emailConfig.getSignalChangeThreshold() + "% konfiguriert\n\n");
                        
                        // Zeige Erfolgsmeldung
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Test-E-Mail gesendet");
                        alert.setHeaderText("E-Mail erfolgreich versendet!");
                        alert.setContentText("Die Test-E-Mail wurde an " + emailConfig.getToEmail() + " gesendet.\n\nSignal-Threshold: " + emailConfig.getSignalChangeThreshold() + "%\n\nPrüfen Sie Ihren Posteingang (auch Spam-Ordner).");
                        alert.showAndWait();
                        
                    } else {
                        appendStatus("❌ " + result.getMessage() + "\n");
                        appendStatus("- Test-E-Mail konnte nicht gesendet werden\n");
                        appendStatus("- Prüfen Sie alle Einstellungen\n\n");
                        
                        // Zeige Fehlermeldung
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Test-E-Mail fehlgeschlagen");
                        alert.setHeaderText("E-Mail konnte nicht gesendet werden");
                        alert.setContentText(result.getMessage());
                        alert.showAndWait();
                    }
                });
                
            } catch (Exception e) {
                Platform.runLater(() -> {
                    hideProgress();
                    appendStatus("❌ Test-E-Mail fehlgeschlagen: " + e.getMessage() + "\n\n");
                });
            }
        }).start();
    }
    
    /**
     * Speichert die Konfiguration
     * ERWEITERT um Signal-Threshold und MetaTrader-Konfiguration
     */
    private void saveConfiguration() {
        try {
            // Validiere Eingaben
            EmailConfig.ValidationResult validation = validateInputs();
            if (!validation.isValid()) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Ungültige Eingaben");
                alert.setHeaderText("Konfiguration kann nicht gespeichert werden");
                alert.setContentText(validation.getErrorMessage());
                alert.showAndWait();
                return;
            }
            
            // NEU: Validiere MetaTrader-Konfiguration
            if (!validateMetaTraderConfiguration()) {
                return; // Fehler bereits angezeigt
            }
            
            // Aktualisiere Konfiguration
            updateConfigFromFields();
            
            // Speichere Konfiguration
            emailConfig.saveConfig();
            
            // Aktualisiere E-Mail-Service
            emailService.updateConfig(emailConfig);
            
            // NEU: Konfiguriere MetaTrader-Synchronisation
            configureMetaTraderSynchronization();
            
            // NEU: MetaTrader-Konfiguration über Callback weiterleiten
            if (metaTraderCallback != null) {
                try {
                    metaTraderCallback.configure(
                        metatraderSyncEnabledCheckBox.isSelected(),
                        metatraderDirField.getText().trim()
                    );
                    LOGGER.info("MetaTrader-Konfiguration über Callback weitergeleitet");
                } catch (Exception e) {
                    LOGGER.warning("Fehler beim MetaTrader-Callback: " + e.getMessage());
                    appendStatus("- Warnung: MetaTrader-Integration nicht vollständig aktiviert\n");
                }
            } else {
                LOGGER.fine("Kein MetaTrader-Callback definiert");
            }
            
            appendStatus("💾 Konfiguration erfolgreich gespeichert\n");
            appendStatus("- E-Mail-Benachrichtigungen: " + (emailConfig.isEmailEnabled() ? "Aktiviert" : "Deaktiviert") + "\n");
            appendStatus("- Server: " + emailConfig.getSmtpHost() + ":" + emailConfig.getSmtpPort() + "\n");
            appendStatus("- Signal-Threshold: " + emailConfig.getSignalChangeThreshold() + "%\n");
            appendStatus("- MetaTrader-Sync: " + (metatraderSyncEnabledCheckBox.isSelected() ? "Aktiviert" : "Deaktiviert") + "\n");
            appendStatus("- CSV-Speicherort: " + dataDirectory + "/signal_changes/\n\n");
            
            LOGGER.info("Konfiguration erfolgreich gespeichert (inkl. MetaTrader: " + metatraderSyncEnabledCheckBox.isSelected() + ")");
            
            // Erfolgsmeldung
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Konfiguration gespeichert");
            alert.setHeaderText("E-Mail-Konfiguration + MetaTrader-Sync erfolgreich gespeichert!");
            alert.setContentText("Die Einstellungen wurden gespeichert und sind sofort aktiv.\n\n" +
                "Signal-Threshold: " + emailConfig.getSignalChangeThreshold() + "%\n" +
                "E-Mail-Benachrichtigungen: " + (emailConfig.isEmailEnabled() ? "Aktiviert" : "Deaktiviert") + "\n" +
                "MetaTrader-Synchronisation: " + (metatraderSyncEnabledCheckBox.isSelected() ? "Aktiviert" : "Deaktiviert") + "\n" +
                "CSV-Speicherort: " + dataDirectory + "/signal_changes/");
            alert.showAndWait();
            
        } catch (Exception e) {
            LOGGER.severe("Fehler beim Speichern der Konfiguration: " + e.getMessage());
            
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Speicherfehler");
            alert.setHeaderText("Konfiguration konnte nicht gespeichert werden");
            alert.setContentText("Fehler: " + e.getMessage());
            alert.showAndWait();
        }
    }
    
    /**
     * NEU: Validiert die MetaTrader-Konfiguration
     */
    private boolean validateMetaTraderConfiguration() {
        if (!metatraderSyncEnabledCheckBox.isSelected()) {
            return true; // Deaktiviert = kein Problem
        }
        
        String dirPath = metatraderDirField.getText().trim();
        if (dirPath.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("MetaTrader-Konfiguration unvollständig");
            alert.setHeaderText("MetaTrader-Verzeichnis erforderlich");
            alert.setContentText("Wenn die MetaTrader-Synchronisation aktiviert ist, muss ein gültiges Verzeichnis angegeben werden.\n\nBitte wählen Sie ein Verzeichnis aus oder deaktivieren Sie die Synchronisation.");
            alert.showAndWait();
            return false;
        }
        
        File dir = new File(dirPath);
        if (!dir.exists() || !dir.isDirectory() || !dir.canWrite()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Ungültiges MetaTrader-Verzeichnis");
            alert.setHeaderText("MetaTrader-Verzeichnis nicht verwendbar");
            alert.setContentText("Das angegebene Verzeichnis ist nicht gültig:\n" + dirPath + 
                "\n\nProblem: " + 
                (!dir.exists() ? "Verzeichnis existiert nicht" :
                 !dir.isDirectory() ? "Pfad ist kein Verzeichnis" :
                 "Verzeichnis ist nicht beschreibbar") +
                "\n\nBitte testen Sie das Verzeichnis oder wählen Sie ein anderes aus.");
            alert.showAndWait();
            return false;
        }
        
        return true;
    }
    
    /**
     * NEU: Konfiguriert die MetaTrader-Synchronisation im SignalChangeHistoryManager
     */
    private void configureMetaTraderSynchronization() {
        try {
            // Hole den SignalChangeHistoryManager über den EmailService
            // (Da der EmailService möglicherweise eine Referenz darauf hat)
            
            // HINWEIS: Da die direkte Verbindung noch nicht implementiert ist,
            // speichern wir die Konfiguration erstmal in einem separaten Weg
            
            if (metatraderSyncEnabledCheckBox.isSelected()) {
                String dirPath = metatraderDirField.getText().trim();
                
                // TODO: Hier würde die Konfiguration an den SignalChangeHistoryManager weitergegeben
                // Für jetzt: Logge die Konfiguration
                LOGGER.info("MetaTrader-Synchronisation konfiguriert: " + dirPath);
                updateMetaTraderStatus("✅ Konfiguriert und aktiv");
                
                appendStatus("- MetaTrader-Verzeichnis: " + dirPath + "\n");
                appendStatus("- Datei-Synchronisation aktiviert\n");
                
            } else {
                LOGGER.info("MetaTrader-Synchronisation deaktiviert");
                updateMetaTraderStatus("Deaktiviert");
                appendStatus("- MetaTrader-Synchronisation deaktiviert\n");
            }
            
        } catch (Exception e) {
            LOGGER.warning("Fehler beim Konfigurieren der MetaTrader-Synchronisation: " + e.getMessage());
            updateMetaTraderStatus("❌ Konfigurationsfehler");
            appendStatus("- Fehler bei MetaTrader-Konfiguration: " + e.getMessage() + "\n");
        }
    }
    
    /**
     * Aktualisiert die Konfiguration aus den UI-Feldern
     * ERWEITERT um Signal-Threshold
     */
    private void updateConfigFromFields() {
        // Server-Konfiguration
        emailConfig.setSmtpHost(smtpHostField.getText().trim());
        emailConfig.setSmtpPort(smtpPortSpinner.getValue());
        emailConfig.setUseStartTLS(startTlsCheckBox.isSelected());
        emailConfig.setUseSSL(sslCheckBox.isSelected());
        emailConfig.setUsername(usernameField.getText().trim());
        emailConfig.setPassword(passwordField.getText());
        
        // E-Mail-Konfiguration
        emailConfig.setFromEmail(fromEmailField.getText().trim());
        emailConfig.setFromName(fromNameField.getText().trim());
        emailConfig.setToEmail(toEmailField.getText().trim());
        emailConfig.setEmailEnabled(emailEnabledCheckBox.isSelected());
        
        // Benachrichtigungspräferenzen
        emailConfig.setSendOnCriticalChanges(criticalChangesCheckBox.isSelected());
        emailConfig.setSendOnHighChanges(highChangesCheckBox.isSelected());
        emailConfig.setSendOnAllChanges(allChangesCheckBox.isSelected());
        emailConfig.setMaxEmailsPerHour(maxEmailsSpinner.getValue());
        
        // Signal-Threshold
        emailConfig.setSignalChangeThreshold(signalThresholdSpinner.getValue());
        
        // NEU: MetaTrader-Konfiguration (hier würde man normalerweise in EmailConfig speichern)
        // TODO: EmailConfig um MetaTrader-Felder erweitern
    }
    
    /**
     * Validiert die Eingaben
     */
    private EmailConfig.ValidationResult validateInputs() {
        updateConfigFromFields();
        return emailConfig.validateConfig();
    }
    
    /**
     * Zeigt Progress-Indikator
     */
    private void showProgress(String message) {
        progressIndicator.setVisible(true);
        appendStatus("⏳ " + message + "\n");
        
        // Deaktiviere Buttons während der Verarbeitung
        testConnectionButton.setDisable(true);
        sendTestEmailButton.setDisable(true);
        saveButton.setDisable(true);
    }
    
    /**
     * Versteckt Progress-Indikator
     */
    private void hideProgress() {
        progressIndicator.setVisible(false);
        
        // Reaktiviere Buttons
        testConnectionButton.setDisable(false);
        sendTestEmailButton.setDisable(false);
        saveButton.setDisable(false);
    }
    
    /**
     * Fügt Text zum Status-Bereich hinzu
     */
    private void appendStatus(String text) {
        statusArea.appendText(text);
        statusArea.setScrollTop(Double.MAX_VALUE);
    }
    
    /**
     * NEU: Setzt den Callback für MetaTrader-Konfiguration
     * @param callback Callback-Interface für MetaTrader-Konfiguration
     */
    public void setMetaTraderConfigurationCallback(MetaTraderConfigurationCallback callback) {
        this.metaTraderCallback = callback;
        LOGGER.info("MetaTrader-Konfiguration-Callback gesetzt");
    }
    
    /**
     * Zeigt das Fenster an
     */
    public void show() {
        stage.show();
    }
    
    /**
     * Schließt das Fenster
     */
    public void close() {
        stage.close();
    }
    
    /**
     * Gibt die aktuelle E-Mail-Konfiguration zurück
     */
    public EmailConfig getEmailConfig() {
        return emailConfig;
    }
    
    /**
     * Gibt den E-Mail-Service zurück
     */
    public EmailService getEmailService() {
        return emailService;
    }
    
    /**
     * NEU: Gibt das konfigurierte MetaTrader-Verzeichnis zurück
     */
    public String getMetaTraderDirectory() {
        if (metatraderSyncEnabledCheckBox.isSelected()) {
            return metatraderDirField.getText().trim();
        }
        return null;
    }
    
    /**
     * NEU: Prüft ob MetaTrader-Synchronisation aktiviert ist
     */
    public boolean isMetaTraderSyncEnabled() {
        return metatraderSyncEnabledCheckBox.isSelected() && 
               !metatraderDirField.getText().trim().isEmpty();
    }
}