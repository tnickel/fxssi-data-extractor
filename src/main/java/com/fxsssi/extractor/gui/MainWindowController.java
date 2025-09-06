package com.fxsssi.extractor.gui;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fxssi.extractor.model.CurrencyPairData;

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
 * 
 * @author Generated for FXSSI Data Extraction GUI
 * @version 1.5 (mit vollständiger historischer Daten Integration)
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
    private Button refreshButton;
    private Button historicalDataButton; // NEU: Historische Daten Button
    private Button debugButton; // NEU: Debug Button
    private Spinner<Integer> refreshIntervalSpinner;
    private CheckBox autoRefreshCheckBox;
    
    // Services
    private DataRefreshManager refreshManager;
    private GuiDataService dataService;
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
        LOGGER.info("Signalwechsel-Erkennung + Historische Daten aktiviert");
    }
    
    /**
     * Erstellt und konfiguriert das komplette Hauptfenster
     */
    public Scene createMainWindow(Stage primaryStage) {
        this.stage = primaryStage;
        
        LOGGER.info("Erstelle Hauptfenster mit Signalwechsel + Historischen Daten Features...");
        LOGGER.info("Datenverzeichnis: " + dataDirectory);
        
        // Initialisiere Datenstrukturen
        tableData = FXCollections.observableArrayList();
        dataService = new GuiDataService(dataDirectory);
        refreshManager = new DataRefreshManager(this::refreshData);
        
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
        
        // Erstelle Scene mit noch breiterem Fenster für alle Features
        scene = new Scene(root, 1600, 800); // Verbreitert für historische Daten Features
        
        // Lade CSS (falls vorhanden)
        loadStylesheets();
        
        LOGGER.info("Hauptfenster erfolgreich erstellt (1600x800) mit allen Features");
        return scene;
    }
    
    /**
     * Erstellt den oberen Bereich (Titel + Toolbar)
     */
    private VBox createTopArea() {
        VBox topArea = new VBox();
        
        // Titel-Bereich
        HBox titleBar = createTitleBar();
        
        // Toolbar
        HBox toolbar = createToolbar();
        
        // Separator
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
        
        Label titleLabel = new Label("FXSSI Live Sentiment Monitor mit Signalwechsel + Historischen Daten");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 22));
        titleLabel.getStyleClass().add("title-label");
        
        titleBar.getChildren().add(titleLabel);
        return titleBar;
    }
    
    /**
     * Erstellt die Toolbar mit Steuerelementen + NEUER historische Daten Button
     */
    private HBox createToolbar() {
        HBox toolbar = new HBox(15);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(10, 20, 10, 20));
        toolbar.getStyleClass().add("toolbar");
        
        // Refresh-Button (mit Hinweis auf Signalwechsel-Erkennung)
        refreshButton = new Button("🔄 Refresh + Signalwechsel-Check");
        refreshButton.setFont(Font.font(12));
        refreshButton.getStyleClass().add("refresh-button");
        refreshButton.setOnAction(event -> refreshData());
        
        // *** NEU: Historische Daten Button ***
        historicalDataButton = new Button("📊 Historische Daten");
        historicalDataButton.setFont(Font.font(12));
        historicalDataButton.getStyleClass().add("historical-data-button");
        historicalDataButton.setOnAction(event -> showHistoricalDataForSelectedPair());
        
        // *** NEU: Debug Button (temporär) ***
        debugButton = new Button("🔧 Debug CSV");
        debugButton.setFont(Font.font(10));
        debugButton.getStyleClass().add("debug-button");
        debugButton.setOnAction(event -> debugHistoricalDataLoading());
        
        // Separator
        Separator separator1 = new Separator();
        separator1.setOrientation(javafx.geometry.Orientation.VERTICAL);
        
        // Auto-Refresh Label
        Label autoRefreshLabel = new Label("Auto-Refresh:");
        autoRefreshLabel.setFont(Font.font(12));
        autoRefreshLabel.getStyleClass().add("toolbar-label");
        
        // Auto-Refresh CheckBox
        autoRefreshCheckBox = new CheckBox("Aktiviert");
        autoRefreshCheckBox.setFont(Font.font(12));
        autoRefreshCheckBox.setSelected(true);
        autoRefreshCheckBox.getStyleClass().add("auto-refresh-checkbox");
        autoRefreshCheckBox.setOnAction(event -> {
            if (autoRefreshCheckBox.isSelected()) {
                refreshManager.startAutoRefresh(refreshIntervalSpinner.getValue());
                LOGGER.info("Auto-Refresh aktiviert - Signalwechsel werden automatisch erkannt");
            } else {
                refreshManager.stopAutoRefresh();
                LOGGER.info("Auto-Refresh deaktiviert");
            }
        });
        
        // Intervall Label
        Label intervalLabel = new Label("Intervall (Min.):");
        intervalLabel.setFont(Font.font(12));
        intervalLabel.getStyleClass().add("toolbar-label");
        
        // Intervall Spinner
        refreshIntervalSpinner = new Spinner<>(1, 60, 5);
        refreshIntervalSpinner.setPrefWidth(80);
        refreshIntervalSpinner.getStyleClass().add("interval-spinner");
        refreshIntervalSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (autoRefreshCheckBox.isSelected()) {
                refreshManager.updateRefreshInterval(newVal);
                LOGGER.info("Refresh-Intervall geändert auf " + newVal + " Minuten (mit Signalwechsel-Erkennung)");
            }
        });
        
        // Platzhalter für rechte Seite
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // Status-Bereich
        VBox statusArea = createStatusArea();
        
        // *** ERWEITERTE Toolbar mit allen neuen Buttons ***
        toolbar.getChildren().addAll(
            refreshButton, historicalDataButton, debugButton, separator1, autoRefreshLabel, autoRefreshCheckBox,
            intervalLabel, refreshIntervalSpinner, spacer, statusArea
        );
        
        return toolbar;
    }
    
    /**
     * Erstellt den Status-Bereich mit erweiterten Informationen
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
        
        // Datenverzeichnis-Anzeige
        dataDirectoryLabel = new Label("Datenverzeichnis: " + dataDirectory);
        dataDirectoryLabel.setFont(Font.font(9));
        dataDirectoryLabel.getStyleClass().add("data-directory-label");
        
        // Speicher-Info-Label
        storageInfoLabel = new Label("Speicherung: Tägliche + Währungspaar + Signalwechsel + Historische Daten");
        storageInfoLabel.setFont(Font.font(9));
        storageInfoLabel.getStyleClass().add("storage-info-label");
        
        statusArea.getChildren().addAll(statusLabel, lastUpdateLabel, dataDirectoryLabel, storageInfoLabel);
        return statusArea;
    }
    
    /**
     * Erstellt den mittleren Bereich (Tabelle)
     */
    private VBox createCenterArea() {
        VBox centerArea = new VBox(10);
        centerArea.setPadding(new Insets(20, 20, 20, 20));
        
        // Header
        HBox headerArea = createTableHeader();
        
        // Tabelle
        currencyTable = createCurrencyTable();
        VBox.setVgrow(currencyTable, Priority.ALWAYS);
        
        centerArea.getChildren().addAll(headerArea, currencyTable);
        return centerArea;
    }
    
    /**
     * Erstellt den Tabellen-Header mit Hinweis auf historische Daten
     */
    private HBox createTableHeader() {
        HBox headerArea = new HBox(10);
        headerArea.setAlignment(Pos.CENTER_LEFT);
        
        Label sectionHeader = new Label("Live Currency Sentiment Data mit Signalwechsel + Historischen Daten");
        sectionHeader.setFont(Font.font("System", FontWeight.BOLD, 16));
        sectionHeader.getStyleClass().add("section-header");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Label sectionDescription = new Label("🔄 Klicken Sie auf Wechsel-Icons für Details | 📊 Doppelklick für historische Daten");
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
        
        // *** NEU: Aktiviere Selektion für historische Daten ***
        table.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.SINGLE);
        
        // Symbol-Spalte
        symbolColumn = new TableColumn<>("Symbol");
        symbolColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getCurrencyPair()));
        symbolColumn.setPrefWidth(120);
        symbolColumn.setResizable(false);
        symbolColumn.getStyleClass().add("symbol-column");
        
        // Ratio-Spalte - angepasst für neue Spalte
        ratioColumn = new TableColumn<>("Ratio");
        ratioColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue()));
        ratioColumn.setCellFactory(new RatioBarCellFactory());
        ratioColumn.setPrefWidth(400); // Angepasst für mehr Spalten
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
        
        // Spalten zur Tabelle hinzufügen
        table.getColumns().addAll(symbolColumn, ratioColumn, signalColumn, changeColumn);
        
        // Tabellen-Konfiguration
        table.setItems(tableData);
        
        // *** NEU: Row-Factory mit Selektion für historische Daten ***
        table.setRowFactory(tv -> {
            TableRow<CurrencyPairTableRow> row = new TableRow<>();
            row.getStyleClass().add("currency-table-row");
            
            // Doppelklick öffnet historische Daten
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
        
        // Spaltengrößen-Policy
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        LOGGER.info("Tabelle erstellt mit Selektion für historische Daten + 4 Spalten");
        return table;
    }
    
    /**
     * Erstellt den Placeholder für leere Tabelle mit allen Features
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
        
        placeholder.getChildren().addAll(placeholderText, placeholderHint, dataDirectoryHint, 
                                       storageHint, changeHint, historicalHint);
        return placeholder;
    }
    
    /**
     * Erstellt den unteren Bereich (Status-Leiste) mit erweiterten Informationen
     */
    private HBox createBottomArea() {
        HBox bottomArea = new HBox(20);
        bottomArea.setAlignment(Pos.CENTER_LEFT);
        bottomArea.setPadding(new Insets(5, 20, 5, 20));
        bottomArea.getStyleClass().add("status-bar");
        
        Label appInfo = new Label("FXSSI Data Extractor v1.5");
        appInfo.setFont(Font.font(10));
        appInfo.getStyleClass().add("app-info");
        
        Separator separator1 = new Separator();
        separator1.setOrientation(javafx.geometry.Orientation.VERTICAL);
        
        Label appDescription = new Label("Sentiment + Signalwechsel + Historische Daten");
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
        
        Label storageInfo = new Label("Speicher: Täglich + Währungspaare + Signalwechsel + Historisch");
        storageInfo.setFont(Font.font(10));
        storageInfo.getStyleClass().add("storage-info");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Label dataSource = new Label("Live + Historische Daten von FXSSI.com");
        dataSource.setFont(Font.font(10));
        dataSource.getStyleClass().add("data-source");
        
        bottomArea.getChildren().addAll(
            appInfo, separator1, appDescription, separator2, dataDirectoryInfo, 
            separator3, storageInfo, spacer, dataSource
        );
        
        return bottomArea;
    }
    
    /**
     * *** NEUE METHODE: Zeigt historische Daten für das ausgewählte Währungspaar ***
     */
    private void showHistoricalDataForSelectedPair() {
        CurrencyPairTableRow selectedItem = currencyTable.getSelectionModel().getSelectedItem();
        
        if (selectedItem == null) {
            // Keine Auswahl - zeige Hinweis
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
     * *** NEUE METHODE: Zeigt historische Daten für ein bestimmtes Währungspaar ***
     */
    private void showHistoricalDataForPair(String currencyPair) {
        try {
            LOGGER.info("Öffne historische Daten für: " + currencyPair);
            
            // Erstelle und zeige das historische Daten-Fenster
            HistoricalDataWindow historicalWindow = new HistoricalDataWindow(
                stage, 
                currencyPair, 
                dataService
            );
            
            historicalWindow.show();
            
        } catch (Exception e) {
            LOGGER.severe("Fehler beim Öffnen der historischen Daten: " + e.getMessage());
            
            // Zeige Fehlermeldung
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
     * *** NEUE METHODE: Debug-Methode zum Testen der CSV-Datenladung ***
     */
    private void debugHistoricalDataLoading() {
        LOGGER.info("=== DEBUG: Teste historische Datenladung ===");
        
        try {
            // Test mit verfügbaren Währungspaaren
            LOGGER.info("Datenverzeichnis: " + dataService.getDataDirectory());
            
            // Lade verfügbare Währungspaare
            Set<String> availablePairs = dataService.getAvailableCurrencyPairs();
            LOGGER.info("Verfügbare Währungspaare: " + availablePairs);
            LOGGER.info("Anzahl verfügbare Paare: " + availablePairs.size());
            
            if (availablePairs.isEmpty()) {
                LOGGER.warning("KEINE WÄHRUNGSPAARE GEFUNDEN!");
                showDebugAlert("Keine Währungspaare gefunden", 
                    "Im Verzeichnis " + dataService.getDataDirectory() + "/currency_pairs/ wurden keine CSV-Dateien gefunden.");
                return;
            }
            
            // Teste mit erstem verfügbaren Währungspaar
            String testCurrencyPair = availablePairs.iterator().next();
            LOGGER.info("Teste mit Währungspaar: " + testCurrencyPair);
            
            // Teste historische Datenladung
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
                    "Verfügbare Währungspaare: " + availablePairs.size() + "\n\n" +
                    "Das historische Daten Feature sollte jetzt funktionieren.");
                
            } else {
                LOGGER.warning("KEINE HISTORISCHEN DATEN GELADEN für " + testCurrencyPair);
                showDebugAlert("Keine Daten geladen", 
                    "❌ Keine historischen Daten für " + testCurrencyPair + " geladen.\n\n" +
                    "Mögliche Ursachen:\n" +
                    "• CSV-Datei ist leer\n" +
                    "• Deutsches Dezimalformat (Komma statt Punkt)\n" +
                    "• Ungültiges CSV-Format\n\n" +
                    "Prüfen Sie die Logs für Details.");
            }
            
        } catch (Exception e) {
            LOGGER.severe("Fehler beim Debug-Test: " + e.getMessage());
            e.printStackTrace();
            
            showDebugAlert("Debug-Test Fehler", 
                "❌ Fehler beim Debug-Test:\n\n" + e.getMessage() + 
                "\n\nPrüfen Sie die Logs für Details.");
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
     */
    public void startDataService() {
        try {
            dataService.initialize();
            
            // Initiale Datenladung
            refreshData();
            
            // Starte Auto-Refresh falls aktiviert
            if (autoRefreshCheckBox.isSelected()) {
                refreshManager.startAutoRefresh(refreshIntervalSpinner.getValue());
                LOGGER.info("Auto-Refresh gestartet mit Signalwechsel-Erkennung alle " + refreshIntervalSpinner.getValue() + " Minuten");
            }
            
            LOGGER.info("Datenservice gestartet mit Datenverzeichnis: " + dataDirectory);
            LOGGER.info("Alle Features aktiviert: Live-Daten + Signalwechsel + Historische Daten");
            
            // Zeige initiale Statistiken
            updateStorageStatistics();
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Fehler beim Starten des Datenservice: " + e.getMessage(), e);
            updateStatus("Fehler beim Starten des Datenservice: " + e.getMessage());
        }
    }
    
    /**
     * Aktualisiert die Daten in der Tabelle mit Signalwechsel-Erkennung
     * WICHTIG: Diese Methode wird bei JEDEM Refresh (manuell + automatisch) aufgerufen
     */
    private void refreshData() {
        Platform.runLater(() -> {
            updateStatus("Lade Daten und erkenne Signalwechsel...");
            refreshButton.setDisable(true);
            if (historicalDataButton != null) historicalDataButton.setDisable(true);
        });
        
        // Lade Daten asynchron
        new Thread(() -> {
            try {
                // WICHTIG: Diese Methode garantiert Signalwechsel-Erkennung
                List<CurrencyPairData> data = dataService.forceDataRefresh();
                
                Platform.runLater(() -> {
                    updateTableData(data);
                    updateStatus("Daten aktualisiert (" + data.size() + " Währungspaare) - Signalwechsel erkannt");
                    lastUpdateLabel.setText("Letzte Aktualisierung: " + 
                        java.time.LocalTime.now().format(TIME_FORMATTER) + " (mit Signalwechsel-Check)");
                    refreshButton.setDisable(false);
                    if (historicalDataButton != null) historicalDataButton.setDisable(false);
                    
                    // Aktualisiere Speicher-Statistiken
                    updateStorageStatistics();
                    
                    // Aktualisiere Signalwechsel-Zellen
                    refreshSignalChangeCells();
                });
                
                LOGGER.info("GUI-Refresh abgeschlossen: " + data.size() + " Datensätze + Signalwechsel-Erkennung");
                
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Fehler beim Laden der Daten: " + e.getMessage(), e);
                
                Platform.runLater(() -> {
                    updateStatus("Fehler beim Laden der Daten: " + e.getMessage());
                    refreshButton.setDisable(false);
                    if (historicalDataButton != null) historicalDataButton.setDisable(false);
                });
            }
        }).start();
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
        
        LOGGER.fine("Tabelle mit " + data.size() + " Einträgen aktualisiert (inkl. alle Features)");
    }
    
    /**
     * Aktualisiert die Signalwechsel-Zellen nach einem Refresh
     */
    private void refreshSignalChangeCells() {
        try {
            // Force refresh der Signalwechsel-Spalte
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
            
            String storageText = String.format("Speicherung: %d tägl. Dateien, %d Währungspaare, Signalwechsel + Historisch aktiv", 
                stats.getTotalFiles(), availablePairs.size());
            
            Platform.runLater(() -> {
                storageInfoLabel.setText(storageText);
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
     * Gibt verfügbare Währungspaare zurück (für erweiterte GUI-Features)
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
     * Holt historische Daten für ein Währungspaar (für erweiterte Features)
     * @param currencyPair Das Währungspaar
     * @param count Anzahl der gewünschten Einträge
     * @return Liste der historischen Daten
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
                // Fallback: Leere Zelle
                return new TableCell<CurrencyPairTableRow, CurrencyPairTableRow>() {
                    @Override
                    protected void updateItem(CurrencyPairTableRow item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText("");
                        } else {
                            setText("❌"); // Zeigt Fehler an
                        }
                    }
                };
            }
        }
    }
}