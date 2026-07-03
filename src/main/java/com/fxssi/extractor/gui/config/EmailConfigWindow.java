package com.fxssi.extractor.gui.config;


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
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.logging.Logger;
import java.util.concurrent.CompletableFuture;

/**
 * Konfigurationsfenster für E-Mail-Einstellungen
 * Verwendet spezialisierte Panels für verschiedene Konfigurationsbereiche
 * REFACTORED: Verwendet MetaTraderPanel für Dual-Directory-Support
 * 
 * Features:
 * - Server-Konfiguration (SMTP, Port, SSL/TLS)
 * - Benachrichtigungspräferenzen (Kritisch/Hoch/Alle)
 * - Signal-Threshold-Konfiguration
 * - MetaTrader-Dual-Directory-Synchronisation
 * 
 * @author Generated for FXSSI Email Configuration
 * @version 2.1 - Vollständig mit allen Benachrichtigungsoptionen
 */
public class EmailConfigWindow {
    
    private static final Logger LOGGER = Logger.getLogger(EmailConfigWindow.class.getName());
    
    private Stage stage;
    private BorderPane root;
    
    // Spezialisierte Panels
    private MetaTraderPanel metaTraderPanel;
    
    // UI-Komponenten - Server-Konfiguration
    private TextField smtpHostField;
    private Spinner<Integer> smtpPortSpinner;
    private TextField usernameField;
    private PasswordField passwordField;
    private TextField fromEmailField;
    private TextField toEmailField;
    private CheckBox emailEnabledCheckBox;
    
    // UI-Komponenten - Benachrichtigungspräferenzen
    private CheckBox criticalChangesCheckBox;
    private CheckBox highChangesCheckBox;
    private CheckBox allChangesCheckBox;
    private Spinner<Integer> maxEmailsSpinner;
    
    // UI-Komponenten - Threshold
    private Spinner<Double> signalThresholdSpinner;
    
    // UI-Komponenten - Buttons
    private Button saveButton;
    private Button cancelButton;
    private TextArea statusArea;
    private ProgressIndicator progressIndicator;
    
    // Services
    private final EmailConfig emailConfig;
    private EmailService emailService;
    private final String dataDirectory;
    
    // Callback-Interface für MetaTrader-Konfiguration (für Kompatibilität)
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
        
        // Erstelle MetaTrader-Panel
        this.metaTraderPanel = new MetaTraderPanel(parentStage, this::appendStatus);
        
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
        stage.setTitle("E-Mail + MetaTrader-Konfiguration (Refactored) - FXSSI Monitor");
        stage.setWidth(850);
        stage.setHeight(1000);  // Höher wegen Benachrichtigungspräferenzen
        stage.setMinWidth(750);
        stage.setMinHeight(900);
        
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
        Scene scene = new Scene(root);
        stage.setScene(scene);
        
        LOGGER.info("EmailConfigWindow erstellt (Refactored mit Panel-Architektur)");
    }
    
    /**
     * Erstellt den oberen Bereich (Titel)
     */
    private VBox createTopArea() {
        VBox topArea = new VBox(10);
        
        Label titleLabel = new Label("🔧 E-Mail-Konfiguration + MetaTrader-Synchronisation");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        titleLabel.setStyle("-fx-text-fill: #2E86AB;");
        
        Label descriptionLabel = new Label("Refactored mit Panel-Architektur: Server, Benachrichtigungen, Threshold & MetaTrader (1-2 Dirs)");
        descriptionLabel.setFont(Font.font(14));
        descriptionLabel.setStyle("-fx-text-fill: #666666;");
        descriptionLabel.setWrapText(true);
        
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
        
        // Benachrichtigungspräferenzen
        TitledPane notificationPane = createNotificationPane();
        
        // Threshold-Konfiguration
        TitledPane thresholdPane = createThresholdPane();
        
        // MetaTrader-Konfiguration (verwendet spezialisiertes Panel!)
        TitledPane metatraderPane = metaTraderPanel.createPanel();
        
        // Status-Bereich
        TitledPane statusPane = createStatusPane();
        
        centerContent.getChildren().addAll(serverPane, notificationPane, thresholdPane, metatraderPane, statusPane);
        
        ScrollPane scrollPane = new ScrollPane(centerContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        
        return scrollPane;
    }
    
    /**
     * Erstellt vereinfachtes Server-Konfigurations-Panel
     */
    private TitledPane createServerConfigPane() {
        VBox content = new VBox(15);
        content.setPadding(new Insets(15));
        
        // E-Mail aktiviert
        emailEnabledCheckBox = new CheckBox("E-Mail-Benachrichtigungen aktivieren");
        emailEnabledCheckBox.setFont(Font.font("System", FontWeight.BOLD, 12));
        
        // SMTP-Host
        HBox hostBox = new HBox(10);
        hostBox.setAlignment(Pos.CENTER_LEFT);
        Label hostLabel = new Label("SMTP-Server:");
        hostLabel.setPrefWidth(120);
        smtpHostField = new TextField();
        smtpHostField.setPrefWidth(300);
        smtpHostField.setPromptText("mail.gmx.net");
        hostBox.getChildren().addAll(hostLabel, smtpHostField);
        
        // SMTP-Port
        HBox portBox = new HBox(10);
        portBox.setAlignment(Pos.CENTER_LEFT);
        Label portLabel = new Label("SMTP-Port:");
        portLabel.setPrefWidth(120);
        smtpPortSpinner = new Spinner<>(1, 65535, 587);
        smtpPortSpinner.setEditable(true);
        smtpPortSpinner.setPrefWidth(100);
        portBox.getChildren().addAll(portLabel, smtpPortSpinner);
        
        // Username
        HBox userBox = new HBox(10);
        userBox.setAlignment(Pos.CENTER_LEFT);
        Label userLabel = new Label("Benutzername:");
        userLabel.setPrefWidth(120);
        usernameField = new TextField();
        usernameField.setPrefWidth(300);
        userBox.getChildren().addAll(userLabel, usernameField);
        
        // Password
        HBox passBox = new HBox(10);
        passBox.setAlignment(Pos.CENTER_LEFT);
        Label passLabel = new Label("Passwort:");
        passLabel.setPrefWidth(120);
        passwordField = new PasswordField();
        passwordField.setPrefWidth(300);
        passBox.getChildren().addAll(passLabel, passwordField);
        
        // From Email
        HBox fromBox = new HBox(10);
        fromBox.setAlignment(Pos.CENTER_LEFT);
        Label fromLabel = new Label("Von (E-Mail):");
        fromLabel.setPrefWidth(120);
        fromEmailField = new TextField();
        fromEmailField.setPrefWidth(300);
        fromEmailField.setPromptText("absender@gmx.de");
        fromBox.getChildren().addAll(fromLabel, fromEmailField);
        
        // To Email
        HBox toBox = new HBox(10);
        toBox.setAlignment(Pos.CENTER_LEFT);
        Label toLabel = new Label("An (E-Mail):");
        toLabel.setPrefWidth(120);
        toEmailField = new TextField();
        toEmailField.setPrefWidth(300);
        toEmailField.setPromptText("empfaenger@example.com");
        toBox.getChildren().addAll(toLabel, toEmailField);
        
        content.getChildren().addAll(emailEnabledCheckBox, new Separator(), 
                                     hostBox, portBox, userBox, passBox, fromBox, toBox);
        
        TitledPane pane = new TitledPane("📧 Server-Konfiguration", content);
        pane.setExpanded(true);
        return pane;
    }
    
    /**
     * Erstellt Benachrichtigungspräferenzen-Panel
     */
    private TitledPane createNotificationPane() {
        VBox content = new VBox(15);
        content.setPadding(new Insets(15));
        
        Label headerLabel = new Label("Wählen Sie bei welchen Ereignissen Sie benachrichtigt werden möchten:");
        headerLabel.setFont(Font.font("System", FontWeight.BOLD, 11));
        headerLabel.setWrapText(true);
        
        // Kritische Änderungen
        criticalChangesCheckBox = new CheckBox("Bei kritischen Signalwechseln (>10% Änderung)");
        criticalChangesCheckBox.setStyle("-fx-font-size: 12px;");
        
        // Hohe Änderungen
        highChangesCheckBox = new CheckBox("Bei hohen Signalwechseln (5-10% Änderung)");
        highChangesCheckBox.setStyle("-fx-font-size: 12px;");
        
        // Alle Änderungen
        allChangesCheckBox = new CheckBox("Bei allen Signalwechseln (alle Änderungen)");
        allChangesCheckBox.setStyle("-fx-font-size: 12px;");
        
        // Max E-Mails
        HBox maxEmailBox = new HBox(10);
        maxEmailBox.setAlignment(Pos.CENTER_LEFT);
        
        Label maxEmailLabel = new Label("Max. E-Mails pro Stunde:");
        maxEmailLabel.setPrefWidth(180);
        
        maxEmailsSpinner = new Spinner<>(1, 100, 10);
        maxEmailsSpinner.setEditable(true);
        maxEmailsSpinner.setPrefWidth(100);
        
        Label infoLabel = new Label("(Schutz vor Spam)");
        infoLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #666666;");
        
        maxEmailBox.getChildren().addAll(maxEmailLabel, maxEmailsSpinner, infoLabel);
        
        // Info-Box
        VBox infoBox = new VBox(5);
        infoBox.setStyle("-fx-background-color: #fff3cd; -fx-padding: 10; -fx-border-color: #ffc107; -fx-border-width: 1;");
        
        Label infoTitle = new Label("💡 Empfehlung:");
        infoTitle.setFont(Font.font("System", FontWeight.BOLD, 11));
        infoTitle.setStyle("-fx-text-fill: #856404;");
        
        Label infoText = new Label("Aktivieren Sie \"Kritische\" und \"Hohe\" Änderungen für wichtige Benachrichtigungen. \"Alle Änderungen\" kann zu vielen E-Mails führen.");
        infoText.setWrapText(true);
        infoText.setStyle("-fx-font-size: 10px; -fx-text-fill: #856404;");
        
        infoBox.getChildren().addAll(infoTitle, infoText);
        
        content.getChildren().addAll(
            headerLabel,
            new Separator(),
            criticalChangesCheckBox,
            highChangesCheckBox,
            allChangesCheckBox,
            new Separator(),
            maxEmailBox,
            infoBox
        );
        
        TitledPane pane = new TitledPane("🔔 Benachrichtigungspräferenzen", content);
        pane.setExpanded(true);
        return pane;
    }
    
    /**
     * Erstellt Threshold-Panel
     */
    private TitledPane createThresholdPane() {
        VBox content = new VBox(15);
        content.setPadding(new Insets(15));
        
        Label info = new Label("Signal-Threshold für Anti-Spam (in %):");
        info.setFont(Font.font("System", FontWeight.BOLD, 11));
        
        HBox thresholdBox = new HBox(10);
        thresholdBox.setAlignment(Pos.CENTER_LEFT);
        
        Label thresholdLabel = new Label("Threshold:");
        thresholdLabel.setPrefWidth(120);
        
        signalThresholdSpinner = new Spinner<>(0.1, 50.0, 3.0, 0.1);
        signalThresholdSpinner.setEditable(true);
        signalThresholdSpinner.setPrefWidth(100);
        
        Label unitLabel = new Label("%");
        
        thresholdBox.getChildren().addAll(thresholdLabel, signalThresholdSpinner, unitLabel);
        
        Label explanation = new Label("Empfohlen: 3-5% für ausgewogenes Verhalten");
        explanation.setStyle("-fx-font-size: 10px; -fx-text-fill: #666666;");
        
        content.getChildren().addAll(info, thresholdBox, explanation);
        
        TitledPane pane = new TitledPane("⚙️ Signal-Threshold", content);
        pane.setExpanded(false);
        return pane;
    }
    
    /**
     * Erstellt Status-Bereich
     */
    private TitledPane createStatusPane() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        
        HBox statusHeader = new HBox(10);
        statusHeader.setAlignment(Pos.CENTER_LEFT);
        
        Label statusLabel = new Label("Status-Protokoll:");
        statusLabel.setFont(Font.font("System", FontWeight.BOLD, 11));
        
        progressIndicator = new ProgressIndicator();
        progressIndicator.setPrefSize(20, 20);
        progressIndicator.setVisible(false);
        
        statusHeader.getChildren().addAll(statusLabel, progressIndicator);
        
        statusArea = new TextArea();
        statusArea.setEditable(false);
        statusArea.setPrefRowCount(8);
        statusArea.setWrapText(true);
        statusArea.setStyle("-fx-font-family: 'Courier New', monospace; -fx-font-size: 10px;");
        statusArea.setText("Bereit für Konfiguration...\n");
        
        content.getChildren().addAll(statusHeader, statusArea);
        
        TitledPane pane = new TitledPane("📋 Status", content);
        pane.setExpanded(true);
        return pane;
    }
    
    /**
     * Erstellt Bottom-Bereich mit Buttons
     */
    private HBox createBottomArea() {
        HBox bottomArea = new HBox(15);
        bottomArea.setPadding(new Insets(15, 0, 0, 0));
        bottomArea.setAlignment(Pos.CENTER_RIGHT);
        
        saveButton = new Button("💾 Speichern");
        saveButton.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;");
        saveButton.setPrefWidth(150);
        saveButton.setOnAction(e -> saveConfiguration());
        
        cancelButton = new Button("❌ Abbrechen");
        cancelButton.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; -fx-font-size: 13px;");
        cancelButton.setPrefWidth(150);
        cancelButton.setOnAction(e -> stage.close());
        
        bottomArea.getChildren().addAll(saveButton, cancelButton);
        return bottomArea;
    }
    
    /**
     * Lädt Konfiguration in UI-Felder
     */
    private void loadConfigurationIntoFields() {
        try {
            // Server-Konfiguration
            emailEnabledCheckBox.setSelected(emailConfig.isEmailEnabled());
            smtpHostField.setText(emailConfig.getSmtpHost());
            smtpPortSpinner.getValueFactory().setValue(emailConfig.getSmtpPort());
            usernameField.setText(emailConfig.getUsername());
            passwordField.setText(emailConfig.getPassword());
            fromEmailField.setText(emailConfig.getFromEmail());
            toEmailField.setText(emailConfig.getToEmail());
            
            // Benachrichtigungspräferenzen
            criticalChangesCheckBox.setSelected(emailConfig.isSendOnCriticalChanges());
            highChangesCheckBox.setSelected(emailConfig.isSendOnHighChanges());
            allChangesCheckBox.setSelected(emailConfig.isSendOnAllChanges());
            maxEmailsSpinner.getValueFactory().setValue(emailConfig.getMaxEmailsPerHour());
            
            // Threshold
            signalThresholdSpinner.getValueFactory().setValue(emailConfig.getSignalChangeThreshold());
            
            // MetaTrader-Konfiguration über Panel laden
            metaTraderPanel.loadConfiguration(emailConfig);
            
            appendStatus("Konfiguration geladen\n");
            LOGGER.info("Konfiguration in UI-Felder geladen");
            
        } catch (Exception e) {
            LOGGER.severe("Fehler beim Laden der Konfiguration: " + e.getMessage());
            appendStatus("❌ Fehler beim Laden: " + e.getMessage() + "\n");
        }
    }
    
    /**
     * Speichert die Konfiguration
     */
    private void saveConfiguration() {
        try {
            // Validiere MetaTrader-Konfiguration über Panel
            MetaTraderPanel.ValidationResult mtValidation = metaTraderPanel.validate();
            if (!mtValidation.isValid()) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("MetaTrader-Konfiguration ungültig");
                alert.setHeaderText("MetaTrader-Konfiguration ist ungültig");
                alert.setContentText(mtValidation.getErrorMessage());
                alert.showAndWait();
                return;
            }
            
            // Aktualisiere Konfiguration aus UI-Feldern
            emailConfig.setEmailEnabled(emailEnabledCheckBox.isSelected());
            emailConfig.setSmtpHost(smtpHostField.getText().trim());
            emailConfig.setSmtpPort(smtpPortSpinner.getValue());
            emailConfig.setUsername(usernameField.getText().trim());
            emailConfig.setPassword(passwordField.getText());
            emailConfig.setFromEmail(fromEmailField.getText().trim());
            emailConfig.setToEmail(toEmailField.getText().trim());
            
            // Benachrichtigungspräferenzen
            emailConfig.setSendOnCriticalChanges(criticalChangesCheckBox.isSelected());
            emailConfig.setSendOnHighChanges(highChangesCheckBox.isSelected());
            emailConfig.setSendOnAllChanges(allChangesCheckBox.isSelected());
            emailConfig.setMaxEmailsPerHour(maxEmailsSpinner.getValue());
            
            // Threshold
            emailConfig.setSignalChangeThreshold(signalThresholdSpinner.getValue());
            
            // Speichere MetaTrader-Konfiguration über Panel
            metaTraderPanel.saveConfiguration(emailConfig);
            
            // Validiere komplette Konfiguration
            EmailConfig.ValidationResult validation = emailConfig.validateConfig();
            if (!validation.isValid()) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Konfiguration ungültig");
                alert.setHeaderText("Konfiguration ist ungültig");
                alert.setContentText(validation.getErrorMessage());
                alert.showAndWait();
                return;
            }
            
            // Speichere Konfiguration
            emailConfig.saveConfig();
            
            // Aktualisiere E-Mail-Service
            emailService.updateConfig(emailConfig);
            
            // MetaTrader-Konfiguration über Callback weiterleiten (für Kompatibilität)
            if (metaTraderCallback != null && emailConfig.isMetatraderSyncEnabled()) {
                try {
                    // Übergebe erstes Verzeichnis an Callback (für Abwärtskompatibilität)
                    metaTraderCallback.configure(
                        emailConfig.isMetatraderSyncEnabled(),
                        emailConfig.getMetatraderDirectory()
                    );
                    LOGGER.info("MetaTrader-Konfiguration über Callback weitergeleitet");
                } catch (Exception e) {
                    LOGGER.warning("Fehler beim MetaTrader-Callback: " + e.getMessage());
                    appendStatus("- Warnung: MetaTrader-Integration nicht vollständig aktiviert\n");
                }
            }
            
            // Erfolgreiche Speicherung protokollieren
            appendStatus("💾 Konfiguration erfolgreich gespeichert\n");
            appendStatus("- E-Mail-Benachrichtigungen: " + (emailConfig.isEmailEnabled() ? "Aktiviert" : "Deaktiviert") + "\n");
            appendStatus("- Server: " + emailConfig.getSmtpHost() + ":" + emailConfig.getSmtpPort() + "\n");
            appendStatus("- Benachrichtigungen:\n");
            appendStatus("  └─ Kritische Änderungen: " + (emailConfig.isSendOnCriticalChanges() ? "Ja" : "Nein") + "\n");
            appendStatus("  └─ Hohe Änderungen: " + (emailConfig.isSendOnHighChanges() ? "Ja" : "Nein") + "\n");
            appendStatus("  └─ Alle Änderungen: " + (emailConfig.isSendOnAllChanges() ? "Ja" : "Nein") + "\n");
            appendStatus("- Max. E-Mails/Stunde: " + emailConfig.getMaxEmailsPerHour() + "\n");
            appendStatus("- Signal-Threshold: " + emailConfig.getSignalChangeThreshold() + "%\n");
            appendStatus("- MetaTrader-Sync: " + (emailConfig.isMetatraderSyncEnabled() ? "Aktiviert" : "Deaktiviert") + "\n");
            
            if (emailConfig.isMetatraderSyncEnabled()) {
                int dirCount = emailConfig.getMetatraderDirectoryCount();
                appendStatus("- MetaTrader-Verzeichnisse: " + dirCount + "\n");
                if (emailConfig.hasMetatraderDirectory()) {
                    appendStatus("  └─ Dir 1: " + emailConfig.getMetatraderDirectory() + "\n");
                }
                if (emailConfig.hasMetatraderDirectory2()) {
                    appendStatus("  └─ Dir 2: " + emailConfig.getMetatraderDirectory2() + "\n");
                }
            }
            
            LOGGER.info("Konfiguration erfolgreich gespeichert");
            
            // Erfolgsmeldung
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Konfiguration gespeichert");
            alert.setHeaderText("Erfolgreich gespeichert!");
            
            StringBuilder alertContent = new StringBuilder();
            alertContent.append("Die Einstellungen wurden gespeichert und sind sofort aktiv.\n\n");
            alertContent.append("E-Mail: ").append(emailConfig.isEmailEnabled() ? "Aktiviert" : "Deaktiviert").append("\n");
            
            if (emailConfig.isEmailEnabled()) {
                alertContent.append("\nBenachrichtigungen:\n");
                alertContent.append("• Kritisch: ").append(emailConfig.isSendOnCriticalChanges() ? "Ja" : "Nein").append("\n");
                alertContent.append("• Hoch: ").append(emailConfig.isSendOnHighChanges() ? "Ja" : "Nein").append("\n");
                alertContent.append("• Alle: ").append(emailConfig.isSendOnAllChanges() ? "Ja" : "Nein").append("\n");
                alertContent.append("• Max/Stunde: ").append(emailConfig.getMaxEmailsPerHour()).append("\n");
            }
            
            alertContent.append("\nSignal-Threshold: ").append(emailConfig.getSignalChangeThreshold()).append("%\n");
            alertContent.append("MetaTrader-Sync: ").append(emailConfig.isMetatraderSyncEnabled() ? 
                emailConfig.getMetatraderDirectoryCount() + " Verzeichnis(se)" : "Deaktiviert");
            
            alert.setContentText(alertContent.toString());
            alert.showAndWait();

            // Fenster nach erfolgreichem Speichern schließen
            stage.close();

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
     * Fügt Text zum Status-Bereich hinzu
     */
    private void appendStatus(String text) {
        Platform.runLater(() -> {
            statusArea.appendText(text);
            statusArea.setScrollTop(Double.MAX_VALUE);
        });
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
     * Setzt den Callback für MetaTrader-Konfiguration (für Kompatibilität mit MainWindowController)
     * @param callback Callback-Interface für MetaTrader-Konfiguration
     */
    public void setMetaTraderConfigurationCallback(MetaTraderConfigurationCallback callback) {
        this.metaTraderCallback = callback;
        LOGGER.info("MetaTrader-Konfiguration-Callback gesetzt");
    }
}