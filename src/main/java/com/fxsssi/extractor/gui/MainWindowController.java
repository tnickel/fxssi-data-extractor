package com.fxsssi.extractor.gui;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fxssi.extractor.model.CurrencyPairData;
import com.fxssi.extractor.model.SignalChangeEvent;
import com.fxssi.extractor.notification.EmailConfig;
import com.fxssi.extractor.notification.EmailService;
import com.fxsssi.extractor.gui.config.EmailConfigWindow;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Callback;

/**
 * Vollständige Java-GUI für das FXSSI Data Extractor Hauptfenster
 * Erstellt alle UI-Komponenten programmatisch ohne FXML mit konfigurierbarem Datenverzeichnis
 * Jetzt mit Signalwechsel-Spalte für Live-Wechsel-Erkennung UND historischen Daten Features
 * ERWEITERT um E-Mail-Benachrichtigungen für Signalwechsel UND vergrößerte Abmessungen
 * 
 * GEÄNDERT: Zwei Refresh-Modi:
 * 1. Intervall-Refresh (Checkbox, Default: deaktiviert) - alle X Minuten
 * 2. Täglicher FXSSI-Check (Checkbox, Default: aktiviert um 12:00 Uhr) - einmal täglich
 * 
 * @author Generated for FXSSI Data Extraction GUI
 * @version 1.8 (mit Intervall- und Tageszeit-Refresh)
 */
public class MainWindowController {
    
    private static final Logger LOGGER = Logger.getLogger(MainWindowController.class.getName());
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final String DEFAULT_DATA_DIRECTORY = "data";
    
    // UI-Komponenten
    private Stage stage;
    private Scene scene;
    private BorderPane root;
    
    // Tabelle und Daten
    private TableView<CurrencyPairTableRow> currencyTable;
    private TableColumn<CurrencyPairTableRow, String> symbolColumn;
    private TableColumn<CurrencyPairTableRow, CurrencyPairTableRow> ratioColumn;
    private TableColumn<CurrencyPairTableRow, CurrencyPairTableRow> signalColumn;
    private TableColumn<CurrencyPairTableRow, CurrencyPairTableRow> changeColumn;
    private ObservableList<CurrencyPairTableRow> tableData;
    
    // Steuerelemente
    private Label statusLabel;
    private Label lastUpdateLabel;
    private Label dataDirectoryLabel;
    private Label storageInfoLabel;
    private Label emailStatusLabel;
    private Button refreshButton;
    private Button historicalDataButton;
    private Button debugButton;
    private Button emailConfigButton;
    
    // NEU: Intervall-Refresh Steuerelemente
    private CheckBox intervalRefreshCheckBox;
    private Spinner<Integer> refreshIntervalSpinner;
    
    // NEU: Täglicher Refresh Steuerelemente
    private CheckBox dailyRefreshCheckBox;
    private Spinner<Integer> dailyHourSpinner;
    private Spinner<Integer> dailyMinuteSpinner;
    
    // NEU: Refresh-Status-Label
    private Label refreshStatusLabel;
    
    // Services
    private DataRefreshManager refreshManager;
    private GuiDataService dataService;
    private EmailConfig emailConfig;
    private EmailService emailService;
    private String dataDirectory;
    
    /**
     * Konstruktor mit Standard-Datenverzeichnis
     */
    public MainWindowController() {
        this(DEFAULT_DATA_DIRECTORY);
    }
    
    /**
     * Konstruktor mit konfigurierbarem Datenverzeichnis
     * @param dataDirectory Pfad zum Datenverzeichnis
     */
    public MainWindowController(String dataDirectory) {
        this.dataDirectory = validateDataDirectory(dataDirectory);
        LOGGER.info("MainWindowController erstellt mit Datenverzeichnis: " + this.dataDirectory);
        LOGGER.info("Refresh-Modi: Intervall (Default: aus) + Täglicher Check (Default: 12:00 Uhr)");
    }
    
    /**
     * Erstellt und konfiguriert das komplette Hauptfenster
     */
    public Scene createMainWindow(Stage primaryStage) {
        this.stage = primaryStage;
        
        LOGGER.info("Erstelle Hauptfenster mit Intervall- und Tageszeit-Refresh...");
        LOGGER.info("Datenverzeichnis: " + dataDirectory);
        
        // Initialisiere Datenstrukturen
        tableData = FXCollections.observableArrayList();
        dataService = new GuiDataService(dataDirectory);
        refreshManager = new DataRefreshManager(this::refreshData);
        
        // Initialisiere E-Mail-Services
        initializeEmailServices();
        
        // Erstelle Root-Layout
        root = new BorderPane();
        root.getStyleClass().add("root");
        
        // Erstelle UI-Bereiche
        VBox topArea = createTopArea();
        VBox centerArea = createCenterArea();
        HBox bottomArea = createBottomArea();
        
        // Setze Layout-Bereiche
        root.setTop(topArea);
        root.setCenter(centerArea);
        root.setBottom(bottomArea);
        
        scene = new Scene(root, 2450, 1040);
        
        // Lade CSS (falls vorhanden)
        loadStylesheets();
        
        LOGGER.info("Hauptfenster erfolgreich erstellt (2450x1040)");
        return scene;
    }
    
    /**
     * Initialisiert die E-Mail-Services
     */
    private void initializeEmailServices() {
        try {
            LOGGER.info("Initialisiere E-Mail-Services...");
            
            emailConfig = new EmailConfig(dataDirectory);
            emailConfig.loadConfig();
            
            emailService = new EmailService(emailConfig);
            
            LOGGER.info("E-Mail-Services initialisiert - Status: " + 
                (emailConfig.isEmailEnabled() ? "Aktiviert" : "Deaktiviert"));
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Fehler beim Initialisieren der E-Mail-Services: " + e.getMessage(), e);
            
            emailConfig = new EmailConfig(dataDirectory);
            emailService = new EmailService(emailConfig);
        }
    }
    
    /**
     * Erstellt den oberen Bereich (Titel + Toolbar)
     */
    private VBox createTopArea() {
        VBox topArea = new VBox();
        
        HBox titleBar = createTitleBar();
        HBox toolbar = createToolbar();
        Separator separator = new Separator();
        
        topArea.getChildren().addAll(titleBar, toolbar, separator);
        return topArea;
    }
    
    /**
     * Erstellt die Titel-Leiste
     */
    private HBox createTitleBar() {
        HBox titleBar = new HBox();
        titleBar.setAlignment(Pos.CENTER);
        titleBar.setPadding(new Insets(10, 20, 10, 20));
        titleBar.getStyleClass().add("title-bar");
        
        Label titleLabel = new Label("FXSSI Live Sentiment Monitor mit Signalwechsel + E-Mail-Benachrichtigungen");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 22));
        titleLabel.getStyleClass().add("title-label");
        
        titleBar.getChildren().add(titleLabel);
        return titleBar;
    }
    
    /**
     * Erstellt die Toolbar mit Steuerelementen
     * GEÄNDERT: Zwei-Zeilen-Layout für Intervall-Refresh und Täglichen-Refresh
     * - Zeile 1: Buttons + Intervall-Refresh (Checkbox + Spinner), Default: DEAKTIVIERT
     * - Zeile 2: Täglicher Refresh (Checkbox + Uhrzeit-Spinner), Default: AKTIVIERT 12:00
     */
    private HBox createToolbar() {
        VBox toolbarContent = new VBox(5);
        toolbarContent.setPadding(new Insets(10, 20, 10, 20));
        toolbarContent.getStyleClass().add("toolbar");
        
        // === ZEILE 1: Buttons + Intervall-Refresh ===
        HBox row1 = new HBox(15);
        row1.setAlignment(Pos.CENTER_LEFT);
        
        // Refresh-Button
        refreshButton = new Button("🔄 Refresh + Signalwechsel-Check");
        refreshButton.setFont(Font.font(12));
        refreshButton.getStyleClass().add("refresh-button");
        refreshButton.setOnAction(event -> refreshData());
        
        // Historische Daten Button
        historicalDataButton = new Button("📊 Historische Daten");
        historicalDataButton.setFont(Font.font(12));
        historicalDataButton.getStyleClass().add("historical-data-button");
        historicalDataButton.setOnAction(event -> showHistoricalDataForSelectedPair());
        
        // E-Mail-Konfigurations-Button
        emailConfigButton = new Button("📧 E-Mail-Konfiguration");
        emailConfigButton.setFont(Font.font(12));
        emailConfigButton.getStyleClass().add("email-config-button");
        emailConfigButton.setOnAction(event -> openEmailConfiguration());
        
        // Debug Button
        debugButton = new Button("🔧 Debug CSV");
        debugButton.setFont(Font.font(10));
        debugButton.getStyleClass().add("debug-button");
        debugButton.setOnAction(event -> debugHistoricalDataLoading());
        
        // Separator
        Separator separator1 = new Separator();
        separator1.setOrientation(javafx.geometry.Orientation.VERTICAL);
        
        // --- Intervall-Refresh (Default: DEAKTIVIERT) ---
        intervalRefreshCheckBox = new CheckBox("Intervall-Refresh:");
        intervalRefreshCheckBox.setFont(Font.font(12));
        intervalRefreshCheckBox.setSelected(false); // DEFAULT: Deaktiviert
        intervalRefreshCheckBox.getStyleClass().add("interval-refresh-checkbox");
        intervalRefreshCheckBox.setOnAction(event -> {
            if (intervalRefreshCheckBox.isSelected()) {
                refreshManager.startAutoRefresh(refreshIntervalSpinner.getValue());
                LOGGER.info("Intervall-Refresh aktiviert: alle " + refreshIntervalSpinner.getValue() + " Minuten");
            } else {
                refreshManager.stopAutoRefresh();
                LOGGER.info("Intervall-Refresh deaktiviert");
            }
            updateRefreshStatus();
        });
        
        // Intervall Spinner
        refreshIntervalSpinner = new Spinner<>(1, 60, 15);
        refreshIntervalSpinner.setPrefWidth(80);
        refreshIntervalSpinner.getStyleClass().add("interval-spinner");
        refreshIntervalSpinner.setDisable(true); // Default deaktiviert weil Checkbox aus
        refreshIntervalSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (intervalRefreshCheckBox.isSelected()) {
                refreshManager.updateRefreshInterval(newVal);
                LOGGER.info("Intervall geändert auf " + newVal + " Minuten");
            }
            updateRefreshStatus();
        });
        
        // Spinner aktivieren/deaktivieren basierend auf Checkbox
        intervalRefreshCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            refreshIntervalSpinner.setDisable(!newVal);
        });
        
        Label minLabel = new Label("Min.");
        minLabel.setFont(Font.font(11));
        
        // Platzhalter
        Region spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);
        
        // Status-Bereich (rechte Seite)
        VBox statusArea = createStatusArea();
        
        row1.getChildren().addAll(
            refreshButton, historicalDataButton, emailConfigButton, debugButton,
            separator1,
            intervalRefreshCheckBox, refreshIntervalSpinner, minLabel,
            spacer1, statusArea
        );
        
        // === ZEILE 2: Täglicher Refresh (Default: AKTIVIERT 12:00 Uhr) ===
        HBox row2 = new HBox(15);
        row2.setAlignment(Pos.CENTER_LEFT);
        
        // Täglicher Refresh Checkbox
        dailyRefreshCheckBox = new CheckBox("Täglicher FXSSI-Check um:");
        dailyRefreshCheckBox.setFont(Font.font(12));
        dailyRefreshCheckBox.setSelected(true); // DEFAULT: Aktiviert
        dailyRefreshCheckBox.getStyleClass().add("daily-refresh-checkbox");
        dailyRefreshCheckBox.setOnAction(event -> {
            if (dailyRefreshCheckBox.isSelected()) {
                refreshManager.startDailyRefresh(dailyHourSpinner.getValue(), dailyMinuteSpinner.getValue());
                LOGGER.info(String.format("Täglicher Refresh aktiviert: %02d:%02d Uhr",
                    dailyHourSpinner.getValue(), dailyMinuteSpinner.getValue()));
            } else {
                refreshManager.stopDailyRefresh();
                LOGGER.info("Täglicher Refresh deaktiviert");
            }
            updateRefreshStatus();
        });
        
        // Stunden-Spinner (0-23), Default 12
        dailyHourSpinner = new Spinner<>(0, 23, 12);
        dailyHourSpinner.setPrefWidth(70);
        dailyHourSpinner.setEditable(true);
        dailyHourSpinner.getStyleClass().add("daily-hour-spinner");
        dailyHourSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (dailyRefreshCheckBox.isSelected()) {
                refreshManager.updateDailyRefreshTime(newVal, dailyMinuteSpinner.getValue());
                LOGGER.info(String.format("Tägliche Refresh-Zeit geändert auf %02d:%02d",
                    newVal, dailyMinuteSpinner.getValue()));
            }
            updateRefreshStatus();
        });
        
        Label colonLabel = new Label(":");
        colonLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        
        // Minuten-Spinner (0-59), Default 0
        dailyMinuteSpinner = new Spinner<>(0, 59, 0);
        dailyMinuteSpinner.setPrefWidth(70);
        dailyMinuteSpinner.setEditable(true);
        dailyMinuteSpinner.getStyleClass().add("daily-minute-spinner");
        dailyMinuteSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (dailyRefreshCheckBox.isSelected()) {
                refreshManager.updateDailyRefreshTime(dailyHourSpinner.getValue(), newVal);
                LOGGER.info(String.format("Tägliche Refresh-Zeit geändert auf %02d:%02d",
                    dailyHourSpinner.getValue(), newVal));
            }
            updateRefreshStatus();
        });
        
        Label uhrLabel = new Label("Uhr");
        uhrLabel.setFont(Font.font(11));
        
        // Spinner aktivieren/deaktivieren basierend auf Checkbox
        dailyRefreshCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            dailyHourSpinner.setDisable(!newVal);
            dailyMinuteSpinner.setDisable(!newVal);
        });
        
        // Separator
        Separator separator2 = new Separator();
        separator2.setOrientation(javafx.geometry.Orientation.VERTICAL);
        
        // Refresh-Status-Label
        refreshStatusLabel = new Label("Täglicher Check: 12:00 Uhr aktiv");
        refreshStatusLabel.setFont(Font.font(10));
        refreshStatusLabel.setStyle("-fx-text-fill: #2E86AB;");
        
        row2.getChildren().addAll(
            dailyRefreshCheckBox, dailyHourSpinner, colonLabel, dailyMinuteSpinner, uhrLabel,
            separator2, refreshStatusLabel
        );
        
        toolbarContent.getChildren().addAll(row1, row2);
        
        // Wrapping HBox für Kompatibilität mit dem BorderPane-Layout
        HBox toolbar = new HBox();
        toolbar.getChildren().add(toolbarContent);
        HBox.setHgrow(toolbarContent, Priority.ALWAYS);
        
        return toolbar;
    }
    
    /**
     * NEU: Aktualisiert das Refresh-Status-Label basierend auf den aktiven Modi
     */
    private void updateRefreshStatus() {
        if (refreshStatusLabel == null) return;
        
        Platform.runLater(() -> {
            StringBuilder status = new StringBuilder();
            
            boolean intervalActive = intervalRefreshCheckBox != null && intervalRefreshCheckBox.isSelected();
            boolean dailyActive = dailyRefreshCheckBox != null && dailyRefreshCheckBox.isSelected();
            
            if (intervalActive && dailyActive) {
                status.append(String.format("✅ Intervall: alle %d Min. + Täglicher Check: %02d:%02d Uhr",
                    refreshIntervalSpinner.getValue(),
                    dailyHourSpinner.getValue(),
                    dailyMinuteSpinner.getValue()));
                refreshStatusLabel.setStyle("-fx-text-fill: #2E7D32; -fx-font-weight: bold;");
                
            } else if (intervalActive) {
                status.append(String.format("✅ Intervall-Refresh: alle %d Min.",
                    refreshIntervalSpinner.getValue()));
                refreshStatusLabel.setStyle("-fx-text-fill: #1565C0;");
                
            } else if (dailyActive) {
                status.append(String.format("✅ Täglicher Check: %02d:%02d Uhr",
                    dailyHourSpinner.getValue(),
                    dailyMinuteSpinner.getValue()));
                refreshStatusLabel.setStyle("-fx-text-fill: #2E86AB;");
                
            } else {
                status.append("⚠️ Kein automatischer Refresh aktiv - nur manuell");
                refreshStatusLabel.setStyle("-fx-text-fill: #E65100; -fx-font-weight: bold;");
            }
            
            refreshStatusLabel.setText(status.toString());
        });
    }
    
    /**
     * Öffnet das E-Mail-Konfigurationsfenster
     */
    private void openEmailConfiguration() {
        try {
            LOGGER.info("Öffne E-Mail-Konfigurationsfenster mit MetaTrader-Integration...");
            
            EmailConfigWindow emailConfigWindow = new EmailConfigWindow(stage, dataDirectory);
            
            emailConfigWindow.setMetaTraderConfigurationCallback((enabled, directory) -> {
                try {
                    if (enabled && directory != null && !directory.trim().isEmpty()) {
                        dataService.getSignalChangeHistoryManager().setMetatraderFileDir(directory.trim());
                        LOGGER.info("MetaTrader-Synchronisation aktiviert: " + directory);
                        
                        Platform.runLater(() -> {
                            updateStatus("MetaTrader-Synchronisation aktiviert: " + directory);
                            
                            Alert alert = new Alert(Alert.AlertType.INFORMATION);
                            alert.setTitle("MetaTrader-Synchronisation");
                            alert.setHeaderText("MetaTrader-Datei-Synchronisation aktiviert");
                            alert.setContentText("✅ Die last_known_signals.csv wird automatisch synchronisiert nach:\n" + 
                                directory + "\n\nDie Synchronisation ist sofort aktiv.");
                            alert.showAndWait();
                        });
                        
                    } else {
                        dataService.getSignalChangeHistoryManager().setMetatraderFileDir(null);
                        LOGGER.info("MetaTrader-Synchronisation deaktiviert");
                        
                        Platform.runLater(() -> {
                            updateStatus("MetaTrader-Synchronisation deaktiviert");
                        });
                    }
                    
                } catch (IllegalArgumentException e) {
                    LOGGER.warning("Ungültiges MetaTrader-Verzeichnis: " + e.getMessage());
                    
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.WARNING);
                        alert.setTitle("MetaTrader-Verzeichnis ungültig");
                        alert.setHeaderText("MetaTrader-Synchronisation konnte nicht aktiviert werden");
                        alert.setContentText("Das angegebene Verzeichnis ist ungültig:\n\n" + e.getMessage());
                        alert.showAndWait();
                        
                        updateStatus("MetaTrader-Konfiguration fehlgeschlagen: " + e.getMessage());
                    });
                    
                } catch (Exception e) {
                    LOGGER.severe("Fehler bei MetaTrader-Konfiguration: " + e.getMessage());
                    
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("MetaTrader-Konfigurationsfehler");
                        alert.setHeaderText("MetaTrader-Synchronisation konnte nicht konfiguriert werden");
                        alert.setContentText("Unerwarteter Fehler: " + e.getMessage());
                        alert.showAndWait();
                        
                        updateStatus("MetaTrader-Konfiguration fehlgeschlagen: " + e.getMessage());
                    });
                }
            });
            
            try {
                String currentMetaTraderDir = dataService.getSignalChangeHistoryManager().getMetatraderFileDir();
                boolean isSyncEnabled = dataService.getSignalChangeHistoryManager().isMetatraderSyncEnabled();
                
                if (isSyncEnabled && currentMetaTraderDir != null) {
                    LOGGER.info("Lade bestehende MetaTrader-Konfiguration: " + currentMetaTraderDir);
                }
                
            } catch (Exception e) {
                LOGGER.fine("Konnte aktuelle MetaTrader-Konfiguration nicht laden: " + e.getMessage());
            }
            
            emailConfigWindow.show();
            
            LOGGER.info("E-Mail-Konfigurationsfenster mit MetaTrader-Integration geöffnet");
            
        } catch (Exception e) {
            LOGGER.severe("Fehler beim Öffnen der E-Mail-Konfiguration: " + e.getMessage());
            
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Fehler");
            alert.setHeaderText("E-Mail-Konfiguration konnte nicht geöffnet werden");
            alert.setContentText("Fehler: " + e.getMessage());
            alert.showAndWait();
        }
    }
    
    /**
     * Erstellt den Status-Bereich
     */
    private VBox createStatusArea() {
        VBox statusArea = new VBox();
        statusArea.setAlignment(Pos.CENTER_RIGHT);
        
        statusLabel = new Label("Bereit");
        statusLabel.setFont(Font.font(12));
        statusLabel.getStyleClass().add("status-label");
        
        lastUpdateLabel = new Label("Keine Daten geladen");
        lastUpdateLabel.setFont(Font.font(10));
        lastUpdateLabel.getStyleClass().add("last-update-label");
        
        dataDirectoryLabel = new Label("Datenverzeichnis: " + dataDirectory);
        dataDirectoryLabel.setFont(Font.font(9));
        dataDirectoryLabel.getStyleClass().add("data-directory-label");
        
        storageInfoLabel = new Label("Speicherung: Tägliche + Währungspaar + Signalwechsel");
        storageInfoLabel.setFont(Font.font(9));
        storageInfoLabel.getStyleClass().add("storage-info-label");
        
        emailStatusLabel = new Label("E-Mail: " + (emailConfig != null && emailConfig.isEmailEnabled() ? "✅ Aktiviert" : "❌ Deaktiviert"));
        emailStatusLabel.setFont(Font.font(9));
        emailStatusLabel.getStyleClass().add("email-status-label");
        
        statusArea.getChildren().addAll(statusLabel, lastUpdateLabel, dataDirectoryLabel, storageInfoLabel, emailStatusLabel);
        return statusArea;
    }
    
    /**
     * Erstellt den mittleren Bereich (Tabelle)
     */
    private VBox createCenterArea() {
        VBox centerArea = new VBox(10);
        centerArea.setPadding(new Insets(20, 20, 20, 20));
        
        HBox headerArea = createTableHeader();
        
        currencyTable = createCurrencyTable();
        VBox.setVgrow(currencyTable, Priority.ALWAYS);
        
        centerArea.getChildren().addAll(headerArea, currencyTable);
        return centerArea;
    }
    
    /**
     * Erstellt den Tabellen-Header
     */
    private HBox createTableHeader() {
        HBox headerArea = new HBox(10);
        headerArea.setAlignment(Pos.CENTER_LEFT);
        
        Label sectionHeader = new Label("Live Currency Sentiment Data mit Signalwechsel + E-Mail + MINI-CHARTS");
        sectionHeader.setFont(Font.font("System", FontWeight.BOLD, 16));
        sectionHeader.getStyleClass().add("section-header");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Label sectionDescription = new Label("🔄 Klicken Sie auf Wechsel-Icons für Details | 📊 Doppelklick für historische Daten | 📧 E-Mail-Benachrichtigungen | 📈 7T/30T Mini-Charts");
        sectionDescription.setFont(Font.font(12));
        sectionDescription.getStyleClass().add("section-description");
        
        headerArea.getChildren().addAll(sectionHeader, spacer, sectionDescription);
        return headerArea;
    }
    
    /**
     * Erstellt die Currency Table mit AKTIVIERTER Selektion für historische Daten
     */
    private TableView<CurrencyPairTableRow> createCurrencyTable() {
        TableView<CurrencyPairTableRow> table = new TableView<>();
        table.getStyleClass().add("currency-table");
        
        table.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.SINGLE);
        
        // Symbol-Spalte
        symbolColumn = new TableColumn<>("Symbol");
        symbolColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getCurrencyPair()));
        symbolColumn.setPrefWidth(120);
        symbolColumn.setResizable(false);
        symbolColumn.getStyleClass().add("symbol-column");
        
        // Ratio-Spalte
        ratioColumn = new TableColumn<>("Ratio");
        ratioColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue()));
        ratioColumn.setCellFactory(new RatioBarCellFactory());
        ratioColumn.setPrefWidth(450);
        ratioColumn.getStyleClass().add("ratio-column");
        
        // Signal-Spalte
        signalColumn = new TableColumn<>("Signal");
        signalColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue()));
        signalColumn.setCellFactory(new SignalIconCellFactory());
        signalColumn.setPrefWidth(80);
        signalColumn.setResizable(false);
        signalColumn.getStyleClass().add("signal-column");
        
        // Signalwechsel-Spalte
        changeColumn = new TableColumn<>("🔄 Wechsel");
        changeColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue()));
        changeColumn.setCellFactory(new SignalChangeCellFactory());
        changeColumn.setPrefWidth(100);
        changeColumn.setResizable(false);
        changeColumn.getStyleClass().add("change-column");
        
        // 7-Tage Chart-Spalte
        TableColumn<CurrencyPairTableRow, CurrencyPairTableRow> chart7DaysColumn = 
            new TableColumn<>("📊 7T");
        chart7DaysColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue()));
        chart7DaysColumn.setCellFactory(new SignalChart7DaysCellFactory());
        chart7DaysColumn.setPrefWidth(120);
        chart7DaysColumn.setMinWidth(120);
        chart7DaysColumn.setMaxWidth(120);
        chart7DaysColumn.setResizable(false);
        chart7DaysColumn.setSortable(false);
        chart7DaysColumn.getStyleClass().add("chart-7days-column");
        
        // 30-Tage Chart-Spalte
        TableColumn<CurrencyPairTableRow, CurrencyPairTableRow> chart30DaysColumn = 
            new TableColumn<>("📈 30T");
        chart30DaysColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue()));
        chart30DaysColumn.setCellFactory(new SignalChart30DaysCellFactory());
        chart30DaysColumn.setPrefWidth(120);
        chart30DaysColumn.setMinWidth(120);
        chart30DaysColumn.setMaxWidth(120);
        chart30DaysColumn.setResizable(false);
        chart30DaysColumn.setSortable(false);
        chart30DaysColumn.getStyleClass().add("chart-30days-column");
        
        table.getColumns().addAll(symbolColumn, ratioColumn, signalColumn, changeColumn, 
                                  chart7DaysColumn, chart30DaysColumn);
        
        table.setItems(tableData);
        
        // Row-Factory mit Doppelklick für historische Daten
        table.setRowFactory(tv -> {
            TableRow<CurrencyPairTableRow> row = new TableRow<>();
            row.getStyleClass().add("currency-table-row");
            
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    showHistoricalDataForPair(row.getItem().getCurrencyPair());
                }
            });
            
            return row;
        });
        
        // Placeholder für leere Tabelle
        VBox placeholder = createTablePlaceholder();
        table.setPlaceholder(placeholder);
        
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        
        LOGGER.info("Tabelle erstellt mit 6 Spalten: Symbol + Ratio + Signal + Wechsel + 7T-Chart + 30T-Chart");
        return table;
    }
    
    /**
     * Cell Factory für 7-Tage Signalverlauf-Charts
     */
    private class SignalChart7DaysCellFactory implements Callback<TableColumn<CurrencyPairTableRow, CurrencyPairTableRow>, TableCell<CurrencyPairTableRow, CurrencyPairTableRow>> {
        @Override
        public TableCell<CurrencyPairTableRow, CurrencyPairTableRow> call(TableColumn<CurrencyPairTableRow, CurrencyPairTableRow> param) {
            try {
                return new SignalHistoryChartTableCell(7, dataService);
            } catch (Exception e) {
                LOGGER.warning("Fehler beim Erstellen der 7-Tage Chart-Zelle: " + e.getMessage());
                return new TableCell<CurrencyPairTableRow, CurrencyPairTableRow>() {
                    @Override
                    protected void updateItem(CurrencyPairTableRow item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText("");
                        } else {
                            setText("❌ 7T");
                        }
                    }
                };
            }
        }
    }

    /**
     * Cell Factory für 30-Tage Signalverlauf-Charts
     */
    private class SignalChart30DaysCellFactory implements Callback<TableColumn<CurrencyPairTableRow, CurrencyPairTableRow>, TableCell<CurrencyPairTableRow, CurrencyPairTableRow>> {
        @Override
        public TableCell<CurrencyPairTableRow, CurrencyPairTableRow> call(TableColumn<CurrencyPairTableRow, CurrencyPairTableRow> param) {
            try {
                return new SignalHistoryChartTableCell(30, dataService);
            } catch (Exception e) {
                LOGGER.warning("Fehler beim Erstellen der 30-Tage Chart-Zelle: " + e.getMessage());
                return new TableCell<CurrencyPairTableRow, CurrencyPairTableRow>() {
                    @Override
                    protected void updateItem(CurrencyPairTableRow item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText("");
                        } else {
                            setText("❌ 30T");
                        }
                    }
                };
            }
        }
    }
    
    /**
     * Erstellt den Placeholder für leere Tabelle
     */
    private VBox createTablePlaceholder() {
        VBox placeholder = new VBox(10);
        placeholder.setAlignment(Pos.CENTER);
        
        Label placeholderText = new Label("Keine Daten verfügbar");
        placeholderText.setFont(Font.font(14));
        placeholderText.getStyleClass().add("placeholder-text");
        
        Label placeholderHint = new Label("Klicken Sie auf 'Refresh' um Daten zu laden");
        placeholderHint.setFont(Font.font(12));
        placeholderHint.getStyleClass().add("placeholder-hint");
        
        Label dataDirectoryHint = new Label("Datenverzeichnis: " + dataDirectory);
        dataDirectoryHint.setFont(Font.font(10));
        dataDirectoryHint.getStyleClass().add("placeholder-hint-small");
        
        Label storageHint = new Label("Speichert in: Tägliche + Währungspaar + Signalwechsel-Dateien");
        storageHint.setFont(Font.font(10));
        storageHint.getStyleClass().add("placeholder-hint-small");
        
        Label changeHint = new Label("🔄 Signalwechsel werden automatisch erkannt und angezeigt");
        changeHint.setFont(Font.font(10));
        changeHint.getStyleClass().add("placeholder-hint-small");
        
        Label historicalHint = new Label("📊 Historische Daten: Button klicken oder Doppelklick auf Zeile");
        historicalHint.setFont(Font.font(10));
        historicalHint.getStyleClass().add("placeholder-hint-small");
        
        Label emailHint = new Label("📧 E-Mail-Benachrichtigungen: Konfiguration über E-Mail-Button");
        emailHint.setFont(Font.font(10));
        emailHint.getStyleClass().add("placeholder-hint-small");
        
        Label chartHint = new Label("📈 MINI-CHARTS: 7T/30T Spalten zeigen Signalverläufe mit Wechselpunkten");
        chartHint.setFont(Font.font(10));
        chartHint.getStyleClass().add("placeholder-hint-small");
        chartHint.setStyle("-fx-text-fill: #2E86AB; -fx-font-weight: bold;");
        
        Label refreshHint = new Label("⏰ Refresh-Modi: Intervall (konfigurierbar) oder Täglicher Check (Default 12:00 Uhr)");
        refreshHint.setFont(Font.font(10));
        refreshHint.getStyleClass().add("placeholder-hint-small");
        refreshHint.setStyle("-fx-text-fill: #2E86AB; -fx-font-weight: bold;");
        
        placeholder.getChildren().addAll(placeholderText, placeholderHint, dataDirectoryHint, 
                                       storageHint, changeHint, historicalHint, emailHint,
                                       chartHint, refreshHint);
        return placeholder;
    }
    
    /**
     * Erstellt den unteren Bereich (Status-Leiste)
     */
    private HBox createBottomArea() {
        HBox bottomArea = new HBox(20);
        bottomArea.setAlignment(Pos.CENTER_LEFT);
        bottomArea.setPadding(new Insets(5, 20, 5, 20));
        bottomArea.getStyleClass().add("status-bar");
        
        Label appInfo = new Label("FXSSI Data Extractor v1.8");
        appInfo.setFont(Font.font(10));
        appInfo.getStyleClass().add("app-info");
        
        Separator separator1 = new Separator();
        separator1.setOrientation(javafx.geometry.Orientation.VERTICAL);
        
        Label appDescription = new Label("Sentiment + Signalwechsel + Historische Daten + E-Mail + MINI-CHARTS (7T/30T)");
        appDescription.setFont(Font.font(10));
        appDescription.getStyleClass().add("app-description");
        
        Separator separator2 = new Separator();
        separator2.setOrientation(javafx.geometry.Orientation.VERTICAL);
        
        Label dataDirectoryInfo = new Label("Daten: " + 
            (dataDirectory.length() > 30 ? "..." + dataDirectory.substring(dataDirectory.length() - 27) : dataDirectory));
        dataDirectoryInfo.setFont(Font.font(10));
        dataDirectoryInfo.getStyleClass().add("data-directory-info");
        
        Separator separator3 = new Separator();
        separator3.setOrientation(javafx.geometry.Orientation.VERTICAL);
        
        Label storageInfo = new Label("Speicher: Täglich + Währungspaare + Signalwechsel + Historisch + E-Mail + Charts");
        storageInfo.setFont(Font.font(10));
        storageInfo.getStyleClass().add("storage-info");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Label dataSource = new Label("Live + Historische Daten von FXSSI.com + E-Mail + MINI-CHARTS");
        dataSource.setFont(Font.font(10));
        dataSource.getStyleClass().add("data-source");
        
        bottomArea.getChildren().addAll(
            appInfo, separator1, appDescription, separator2, dataDirectoryInfo,
            separator3, storageInfo, spacer, dataSource
        );
        
        return bottomArea;
    }
    
    /**
     * Zeigt historische Daten für das ausgewählte Währungspaar
     */
    private void showHistoricalDataForSelectedPair() {
        CurrencyPairTableRow selectedItem = currencyTable.getSelectionModel().getSelectedItem();
        
        if (selectedItem == null) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Kein Währungspaar ausgewählt");
            alert.setHeaderText("Bitte wählen Sie ein Währungspaar aus");
            alert.setContentText("Klicken Sie auf eine Zeile in der Tabelle, um ein Währungspaar auszuwählen, und versuchen Sie es erneut.\n\nAlternativ können Sie auch direkt auf eine Zeile doppelklicken.");
            alert.showAndWait();
            return;
        }
        
        String currencyPair = selectedItem.getCurrencyPair();
        showHistoricalDataForPair(currencyPair);
    }
    
    /**
     * Zeigt historische Daten für ein bestimmtes Währungspaar
     */
    private void showHistoricalDataForPair(String currencyPair) {
        try {
            LOGGER.info("Öffne historische Daten für: " + currencyPair);
            
            HistoricalDataWindow historicalWindow = new HistoricalDataWindow(
                stage, 
                currencyPair, 
                dataService
            );
            
            historicalWindow.show();
            
        } catch (Exception e) {
            LOGGER.severe("Fehler beim Öffnen der historischen Daten: " + e.getMessage());
            
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Fehler");
            alert.setHeaderText("Historische Daten konnten nicht geöffnet werden");
            alert.setContentText("Fehler: " + e.getMessage() + 
                "\n\nMögliche Ursachen:" +
                "\n• CSV-Datei nicht gefunden" +
                "\n• Deutsches Dezimalformat (Komma statt Punkt)" +
                "\n• Ungültiges CSV-Format" +
                "\n\nPrüfen Sie die Logs für Details.");
            alert.showAndWait();
        }
    }
    
    /**
     * Debug-Methode zum Testen der CSV-Datenladung
     */
    private void debugHistoricalDataLoading() {
        LOGGER.info("=== DEBUG: Teste historische Datenladung ===");
        
        try {
            LOGGER.info("Datenverzeichnis: " + dataService.getDataDirectory());
            
            Set<String> availablePairs = dataService.getAvailableCurrencyPairs();
            LOGGER.info("Verfügbare Währungspaare: " + availablePairs);
            LOGGER.info("Anzahl verfügbare Paare: " + availablePairs.size());
            
            if (availablePairs.isEmpty()) {
                LOGGER.warning("KEINE WÄHRUNGSPAARE GEFUNDEN!");
                showDebugAlert("Keine Währungspaare gefunden", 
                    "Im Verzeichnis " + dataService.getDataDirectory() + "/currency_pairs/ wurden keine CSV-Dateien gefunden.");
                return;
            }
            
            String testCurrencyPair = availablePairs.iterator().next();
            LOGGER.info("Teste mit Währungspaar: " + testCurrencyPair);
            
            List<CurrencyPairData> historicalData = dataService.getHistoricalDataForCurrencyPair(testCurrencyPair);
            LOGGER.info("Geladene historische Daten: " + historicalData.size() + " Einträge");
            
            if (!historicalData.isEmpty()) {
                LOGGER.info("Erste 3 Datensätze:");
                for (int i = 0; i < Math.min(3, historicalData.size()); i++) {
                    CurrencyPairData data = historicalData.get(i);
                    LOGGER.info("  " + (i+1) + ": " + data.toString());
                }
                
                showDebugAlert("Debug-Test erfolgreich", 
                    "✅ Historische Daten erfolgreich geladen!\n\n" +
                    "Währungspaar: " + testCurrencyPair + "\n" +
                    "Gefundene Datensätze: " + historicalData.size() + "\n" +
                    "Verfügbare Währungspaare: " + availablePairs.size());
                
            } else {
                LOGGER.warning("KEINE HISTORISCHEN DATEN GELADEN für " + testCurrencyPair);
                showDebugAlert("Keine Daten geladen", 
                    "❌ Keine historischen Daten für " + testCurrencyPair + " geladen.\n\n" +
                    "Prüfen Sie die Logs für Details.");
            }
            
        } catch (Exception e) {
            LOGGER.severe("Fehler beim Debug-Test: " + e.getMessage());
            e.printStackTrace();
            
            showDebugAlert("Debug-Test Fehler", 
                "❌ Fehler beim Debug-Test:\n\n" + e.getMessage());
        }
        
        LOGGER.info("=== DEBUG-Test abgeschlossen ===");
    }
    
    /**
     * Hilfsmethode für Debug-Alerts
     */
    private void showDebugAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Debug: " + title);
            alert.setHeaderText("CSV-Datenladung Debug-Test");
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    
    /**
     * Startet den Datenservice
     * GEÄNDERT: Startet standardmäßig den täglichen Refresh statt Intervall-Refresh
     */
    public void startDataService() {
        try {
            dataService.initialize();
            
            // Initiale Datenladung
            refreshData();
            
            // Starte Refresh basierend auf Checkbox-Zuständen
            // Default: Intervall-Refresh ist DEAKTIVIERT
            if (intervalRefreshCheckBox != null && intervalRefreshCheckBox.isSelected()) {
                refreshManager.startAutoRefresh(refreshIntervalSpinner.getValue());
                LOGGER.info("Intervall-Refresh gestartet: alle " + refreshIntervalSpinner.getValue() + " Minuten");
            }
            
            // Default: Täglicher Refresh ist AKTIVIERT um 12:00 Uhr
            if (dailyRefreshCheckBox != null && dailyRefreshCheckBox.isSelected()) {
                refreshManager.startDailyRefresh(dailyHourSpinner.getValue(), dailyMinuteSpinner.getValue());
                LOGGER.info(String.format("Täglicher Refresh gestartet: %02d:%02d Uhr",
                    dailyHourSpinner.getValue(), dailyMinuteSpinner.getValue()));
            }
            
            LOGGER.info("Datenservice gestartet mit Datenverzeichnis: " + dataDirectory);
            LOGGER.info("Refresh-Modus: Intervall=" + 
                       (intervalRefreshCheckBox != null && intervalRefreshCheckBox.isSelected() ? "aktiv" : "inaktiv") +
                       ", Täglicher Check=" + 
                       (dailyRefreshCheckBox != null && dailyRefreshCheckBox.isSelected() ? 
                           String.format("aktiv (%02d:%02d)", dailyHourSpinner.getValue(), dailyMinuteSpinner.getValue()) : 
                           "inaktiv"));
            
            // Zeige initiale Statistiken
            updateStorageStatistics();
            
            // Update E-Mail-Status
            updateEmailStatus();
            
            // Update Refresh-Status
            updateRefreshStatus();
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Fehler beim Starten des Datenservice: " + e.getMessage(), e);
            updateStatus("Fehler beim Starten des Datenservice: " + e.getMessage());
        }
    }
    
    /**
     * Aktualisiert die E-Mail-Status-Anzeige
     */
    private void updateEmailStatus() {
        try {
            if (emailConfig != null) {
                final String statusText;
                if (emailConfig.isEmailEnabled()) {
                    statusText = "E-Mail: ✅ Aktiviert (" + emailConfig.getToEmail() + ")";
                } else {
                    statusText = "E-Mail: ❌ Deaktiviert";
                }
                
                Platform.runLater(() -> {
                    if (emailStatusLabel != null) {
                        emailStatusLabel.setText(statusText);
                    }
                });
                
                LOGGER.fine("E-Mail-Status aktualisiert: " + statusText);
            }
        } catch (Exception e) {
            LOGGER.fine("Konnte E-Mail-Status nicht aktualisieren: " + e.getMessage());
        }
    }
    
    /**
     * Aktualisiert die Daten in der Tabelle mit Signalwechsel-Erkennung UND E-Mail-Versendung
     */
    private void refreshData() {
        Platform.runLater(() -> {
            updateStatus("Lade Daten und erkenne Signalwechsel...");
            refreshButton.setDisable(true);
            if (historicalDataButton != null) historicalDataButton.setDisable(true);
            if (emailConfigButton != null) emailConfigButton.setDisable(true);
        });
        
        // Lade Daten asynchron
        new Thread(() -> {
            try {
                List<CurrencyPairData> data = dataService.forceDataRefresh();

                // MetaTrader-Synchronisation nach jedem Refresh
                syncMetaTraderAfterRefresh();
                
                Platform.runLater(() -> {
                    updateTableData(data);
                    updateStatus("Daten aktualisiert (" + data.size() + " Währungspaare) - Signalwechsel erkannt + E-Mail geprüft");
                    
                    if (lastUpdateLabel != null) {
                        lastUpdateLabel.setText("Letzte Aktualisierung: " + 
                            java.time.LocalTime.now().format(TIME_FORMATTER));
                    }
                    
                    refreshButton.setDisable(false);
                    if (historicalDataButton != null) historicalDataButton.setDisable(false);
                    if (emailConfigButton != null) emailConfigButton.setDisable(false);
                    
                    updateStorageStatistics();
                    updateEmailStatus();
                    refreshSignalChangeCells();
                    refreshChartColumns();
                });
                
                LOGGER.info("GUI-Refresh abgeschlossen: " + data.size() + " Datensätze");
                
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Fehler beim Laden der Daten: " + e.getMessage(), e);
                
                Platform.runLater(() -> {
                    updateStatus("Fehler beim Laden der Daten: " + e.getMessage());
                    refreshButton.setDisable(false);
                    if (historicalDataButton != null) historicalDataButton.setDisable(false);
                    if (emailConfigButton != null) emailConfigButton.setDisable(false);
                });
            }
        }).start();
    }
    
    /**
     * Refresht Chart-Spalten
     */
    private void refreshChartColumns() {
        try {
            Platform.runLater(() -> {
                if (currencyTable != null) {
                    currencyTable.refresh();
                    LOGGER.fine("Chart-Spalten refreshed");
                }
            });
        } catch (Exception e) {
            LOGGER.fine("Fehler beim Refreshen der Chart-Spalten: " + e.getMessage());
        }
    }
    
    /**
     * Sendet E-Mail-Benachrichtigungen für erkannte Signalwechsel
     */
    private void sendSignalChangeNotificationsIfEnabled() {
        try {
            if (emailConfig == null || !emailConfig.isEmailEnabled()) {
                LOGGER.fine("E-Mail-Benachrichtigungen deaktiviert");
                return;
            }
            
            List<SignalChangeEvent> recentChanges = getRecentSignalChangesFromAllPairs(2);
            
            if (recentChanges.isEmpty()) {
                LOGGER.fine("Keine aktuellen Signalwechsel für E-Mail-Benachrichtigung gefunden");
                return;
            }
            
            LOGGER.info("Prüfe " + recentChanges.size() + " aktuelle Signalwechsel für E-Mail-Versendung...");
            
            EmailService.EmailSendResult result = emailService.sendSignalChangeNotification(recentChanges);
            
            if (result.isSuccess()) {
                LOGGER.info("📧 E-Mail-Benachrichtigung erfolgreich gesendet: " + result.getMessage());
            } else {
                LOGGER.warning("❌ E-Mail-Benachrichtigung fehlgeschlagen: " + result.getMessage());
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Fehler beim Versenden von E-Mail-Benachrichtigungen: " + e.getMessage(), e);
        }
    }
    
    /**
     * Holt aktuelle Signalwechsel von allen Währungspaaren
     */
    private List<SignalChangeEvent> getRecentSignalChangesFromAllPairs(int hours) {
        List<SignalChangeEvent> allRecentChanges = new ArrayList<>();
        
        try {
            Set<String> availablePairs = dataService.getAvailableCurrencyPairs();
            
            for (String currencyPair : availablePairs) {
                List<SignalChangeEvent> pairChanges = dataService.getSignalChangeHistoryManager()
                    .getSignalChangesWithinHours(currencyPair, hours);
                allRecentChanges.addAll(pairChanges);
            }
            
            allRecentChanges.sort((a, b) -> b.getChangeTime().compareTo(a.getChangeTime()));
            
        } catch (Exception e) {
            LOGGER.warning("Fehler beim Abrufen aktueller Signalwechsel: " + e.getMessage());
        }
        
        return allRecentChanges;
    }
    
    /**
     * Aktualisiert die Tabellendaten
     */
    private void updateTableData(List<CurrencyPairData> data) {
        tableData.clear();
        
        for (CurrencyPairData currencyData : data) {
            CurrencyPairTableRow row = new CurrencyPairTableRow(
                currencyData.getCurrencyPair(),
                currencyData.getBuyPercentage(),
                currencyData.getSellPercentage(),
                currencyData.getTradingSignal()
            );
            tableData.add(row);
        }
        
        LOGGER.fine("Tabelle mit " + data.size() + " Einträgen aktualisiert");
    }
    
    /**
     * Aktualisiert die Signalwechsel-Zellen nach einem Refresh
     */
    private void refreshSignalChangeCells() {
        try {
            if (changeColumn != null) {
                Platform.runLater(() -> {
                    currencyTable.refresh();
                });
            }
        } catch (Exception e) {
            LOGGER.fine("Fehler beim Aktualisieren der Signalwechsel-Zellen: " + e.getMessage());
        }
    }
    
    /**
     * Aktualisiert die Speicher-Statistiken in der GUI
     */
    private void updateStorageStatistics() {
        try {
            GuiDataService.ExtendedDataStatistics stats = dataService.getExtendedDataStatistics();
            Set<String> availablePairs = dataService.getAvailableCurrencyPairs();
            
            String storageText = String.format("Speicherung: %d tägl. Dateien, %d Währungspaare, Signalwechsel + Charts + E-Mail aktiv", 
                stats.getTotalFiles(), availablePairs.size());
            
            Platform.runLater(() -> {
                if (storageInfoLabel != null) {
                    storageInfoLabel.setText(storageText);
                }
            });
            
            LOGGER.fine("Speicher-Statistiken aktualisiert: " + stats.toString());
            
        } catch (Exception e) {
            LOGGER.fine("Konnte Speicher-Statistiken nicht aktualisieren: " + e.getMessage());
        }
    }
    
    /**
     * Aktualisiert den Status-Text
     */
    private void updateStatus(String status) {
        if (statusLabel != null) {
            statusLabel.setText(status);
        }
        LOGGER.fine("Status: " + status);
    }
    
    /**
     * Lädt CSS-Stylesheets falls vorhanden
     */
    private void loadStylesheets() {
        try {
            String cssPath = getClass().getResource("/css/fxssi-styles.css").toExternalForm();
            scene.getStylesheets().add(cssPath);
            LOGGER.fine("CSS-Stylesheet geladen: " + cssPath);
        } catch (Exception e) {
            LOGGER.fine("Kein CSS-Stylesheet gefunden - verwende Standard-Styling");
        }
    }
    
    /**
     * Gibt das konfigurierte Datenverzeichnis zurück
     */
    public String getDataDirectory() {
        return dataDirectory;
    }
    
    /**
     * Gibt verfügbare Währungspaare zurück
     */
    public Set<String> getAvailableCurrencyPairs() {
        try {
            return dataService.getAvailableCurrencyPairs();
        } catch (Exception e) {
            LOGGER.warning("Konnte verfügbare Währungspaare nicht abrufen: " + e.getMessage());
            return java.util.Collections.emptySet();
        }
    }
    
    /**
     * Holt historische Daten für ein Währungspaar
     */
    public List<CurrencyPairData> getHistoricalDataForPair(String currencyPair, int count) {
        try {
            return dataService.getRecentDataForCurrencyPair(currencyPair, count);
        } catch (Exception e) {
            LOGGER.warning("Konnte historische Daten für " + currencyPair + " nicht abrufen: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Gibt die E-Mail-Konfiguration zurück
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
     * Setzt die Stage-Referenz
     */
    public void setStage(Stage stage) {
        this.stage = stage;
    }
    
    /**
     * Fährt den Controller ordnungsgemäß herunter
     */
    public void shutdown() {
        try {
            LOGGER.info("Fahre MainWindowController herunter...");
            
            if (refreshManager != null) {
                refreshManager.shutdown();
            }
            
            if (dataService != null) {
                dataService.shutdown();
            }
            
            if (emailService != null) {
                emailService.shutdown();
            }
            
            LOGGER.info("MainWindowController heruntergefahren");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Fehler beim Herunterfahren: " + e.getMessage(), e);
        }
    }
    
    /**
     * Validiert das Datenverzeichnis
     */
    private String validateDataDirectory(String directory) {
        if (directory == null || directory.trim().isEmpty()) {
            LOGGER.warning("Leeres Datenverzeichnis angegeben, verwende Standard: " + DEFAULT_DATA_DIRECTORY);
            return DEFAULT_DATA_DIRECTORY;
        }
        return directory.trim();
    }
    
    /**
     * MetaTrader-Synchronisation nach jedem Refresh
     */
    private void syncMetaTraderAfterRefresh() {
        try {
            if (emailConfig == null) {
                emailConfig = new EmailConfig(dataDirectory);
            }
            emailConfig.loadConfig();
            
            boolean isMetaTraderEnabled = emailConfig.isMetatraderSyncEnabled();
            String metatraderDir = emailConfig.getMetatraderDirectory();
            
            LOGGER.info("📋 MetaTrader-Sync-Check nach Refresh:");
            LOGGER.info("   - Aktiviert (EmailConfig): " + isMetaTraderEnabled);
            LOGGER.info("   - Verzeichnis (EmailConfig): " + (metatraderDir != null ? metatraderDir : "nicht gesetzt"));
            
            if (!isMetaTraderEnabled) {
                LOGGER.fine("MetaTrader-Synchronisation deaktiviert - übersprungen");
                return;
            }
            
            if (metatraderDir == null || metatraderDir.trim().isEmpty()) {
                LOGGER.warning("MetaTrader-Synchronisation aktiviert, aber kein Verzeichnis konfiguriert!");
                return;
            }
            
            if (dataService == null || dataService.getSignalChangeHistoryManager() == null) {
                LOGGER.warning("SignalChangeHistoryManager nicht verfügbar - kann nicht synchronisieren");
                return;
            }
            
            var signalChangeManager = dataService.getSignalChangeHistoryManager();
            
            String currentDir = signalChangeManager.getMetatraderFileDir();
            boolean currentlyEnabled = signalChangeManager.isMetatraderSyncEnabled();
            
            LOGGER.info("   - Aktiviert (SignalChangeManager): " + currentlyEnabled);
            LOGGER.info("   - Verzeichnis (SignalChangeManager): " + (currentDir != null ? currentDir : "nicht gesetzt"));
            
            if (!metatraderDir.equals(currentDir) || !currentlyEnabled) {
                LOGGER.info("🔧 Synchronisiere SignalChangeManager mit EmailConfig...");
                try {
                    signalChangeManager.setMetatraderFileDir(metatraderDir);
                    LOGGER.info("✅ SignalChangeManager erfolgreich konfiguriert: " + metatraderDir);
                } catch (Exception e) {
                    LOGGER.warning("❌ Konnte MetaTrader-Verzeichnis nicht setzen: " + e.getMessage());
                    return;
                }
            }
            
            LOGGER.info("🔄 Führe MetaTrader-Synchronisation nach Refresh aus...");
            
            try {
                java.lang.reflect.Method syncMethod = signalChangeManager.getClass()
                    .getDeclaredMethod("syncLastKnownSignalsToMetaTrader");
                syncMethod.setAccessible(true);
                syncMethod.invoke(signalChangeManager);
                
                LOGGER.info("✅ MetaTrader-Synchronisation nach Refresh abgeschlossen");
                
            } catch (NoSuchMethodException e) {
                LOGGER.info("⚠️ Verwende Fallback-Methode für Synchronisation...");
                
                java.lang.reflect.Method saveMethod = signalChangeManager.getClass()
                    .getDeclaredMethod("saveLastKnownSignals");
                saveMethod.setAccessible(true);
                saveMethod.invoke(signalChangeManager);
                
                LOGGER.info("✅ MetaTrader-Synchronisation via Fallback abgeschlossen");
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "❌ Fehler bei MetaTrader-Synchronisation nach Refresh: " + e.getMessage(), e);
        }
    }
    
    // ===== INNERE KLASSEN =====
    
    /**
     * Innere Klasse für Tabellenzeilendaten
     */
    public static class CurrencyPairTableRow {
        private final String currencyPair;
        private final double buyPercentage;
        private final double sellPercentage;
        private final CurrencyPairData.TradingSignal tradingSignal;
        
        public CurrencyPairTableRow(String currencyPair, double buyPercentage, 
                                   double sellPercentage, CurrencyPairData.TradingSignal tradingSignal) {
            this.currencyPair = currencyPair;
            this.buyPercentage = buyPercentage;
            this.sellPercentage = sellPercentage;
            this.tradingSignal = tradingSignal;
        }
        
        public String getCurrencyPair() { return currencyPair; }
        public double getBuyPercentage() { return buyPercentage; }
        public double getSellPercentage() { return sellPercentage; }
        public CurrencyPairData.TradingSignal getTradingSignal() { return tradingSignal; }
    }
    
    /**
     * Cell Factory für Ratio-Balken
     */
    private static class RatioBarCellFactory implements Callback<TableColumn<CurrencyPairTableRow, CurrencyPairTableRow>, TableCell<CurrencyPairTableRow, CurrencyPairTableRow>> {
        @Override
        public TableCell<CurrencyPairTableRow, CurrencyPairTableRow> call(TableColumn<CurrencyPairTableRow, CurrencyPairTableRow> param) {
            return new RatioBarTableCell();
        }
    }
    
    /**
     * Cell Factory für Signal-Icons
     */
    private static class SignalIconCellFactory implements Callback<TableColumn<CurrencyPairTableRow, CurrencyPairTableRow>, TableCell<CurrencyPairTableRow, CurrencyPairTableRow>> {
        @Override
        public TableCell<CurrencyPairTableRow, CurrencyPairTableRow> call(TableColumn<CurrencyPairTableRow, CurrencyPairTableRow> param) {
            return new SignalIconTableCell();
        }
    }
    
    /**
     * Cell Factory für Signalwechsel-Anzeige
     */
    private class SignalChangeCellFactory implements Callback<TableColumn<CurrencyPairTableRow, CurrencyPairTableRow>, TableCell<CurrencyPairTableRow, CurrencyPairTableRow>> {
        @Override
        public TableCell<CurrencyPairTableRow, CurrencyPairTableRow> call(TableColumn<CurrencyPairTableRow, CurrencyPairTableRow> param) {
            try {
                return new SignalChangeTableCell(
                    dataService.getSignalChangeHistoryManager(), 
                    stage
                );
            } catch (Exception e) {
                LOGGER.warning("Fehler beim Erstellen der SignalChangeTableCell: " + e.getMessage());
                return new TableCell<CurrencyPairTableRow, CurrencyPairTableRow>() {
                    @Override
                    protected void updateItem(CurrencyPairTableRow item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText("");
                        } else {
                            setText("❌");
                        }
                    }
                };
            }
        }
    }
}