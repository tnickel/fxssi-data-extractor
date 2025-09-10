package com.fxsssi.extractor.gui;

import com.fxssi.extractor.model.CurrencyPairData;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Erweiterte Popup-Fenster für die Anzeige historischer CSV-Daten mit Split-View
 * Links: Scrollbare Tabelle mit allen historischen Daten
 * Rechts: 7-Tage-Chart mit Buy-Percentage-Verlauf und Signalfarben
 * 
 * @author Generated for FXSSI Historical Data Display with Chart Integration
 * @version 2.0 - Split Window mit Chart-Integration
 */
public class HistoricalDataWindow {
    
    private static final Logger LOGGER = Logger.getLogger(HistoricalDataWindow.class.getName());
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter CHART_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM. HH:mm");
    
    private Stage stage;
    private Scene scene;
    private BorderPane root;
    private SplitPane splitPane;
    
    // UI-Komponenten
    private Label titleLabel;
    private Label summaryLabel;
    private TableView<HistoricalDataTableRow> historicalTable;
    private ComboBox<DataRangeOption> dataRangeCombo;
    private ComboBox<ChartRangeOption> chartRangeCombo;
    private Button refreshButton;
    private Button exportButton;
    private Button closeButton;
    private ProgressIndicator loadingIndicator;
    
    // Chart-Komponenten
    private LineChart<Number, Number> buyPercentageChart;
    private NumberAxis chartXAxis;
    private NumberAxis chartYAxis;
    private Label chartTitleLabel;
    
    // Daten
    private final String currencyPair;
    private final GuiDataService dataService;
    private ObservableList<HistoricalDataTableRow> tableData;
    private List<CurrencyPairData> currentData;
    
    /**
     * Konstruktor
     * @param parentStage Parent-Fenster
     * @param currencyPair Das Währungspaar für das die historischen Daten angezeigt werden sollen
     * @param dataService Der Data Service für Datenzugriff
     */
    public HistoricalDataWindow(Stage parentStage, String currencyPair, GuiDataService dataService) {
        this.currencyPair = currencyPair;
        this.dataService = dataService;
        this.tableData = FXCollections.observableArrayList();
        this.currentData = FXCollections.observableArrayList();
        
        createWindow(parentStage);
        loadHistoricalData();
    }
    
    /**
     * Erstellt und konfiguriert das Popup-Fenster mit Split-Layout
     */
    private void createWindow(Stage parentStage) {
        stage = new Stage();
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(parentStage);
        stage.setTitle("Historische Daten & Chart: " + currencyPair);
        stage.setWidth(1200);  // Breiter für Split-View
        stage.setHeight(800);  // Höher für bessere Chart-Darstellung
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        
        // Zentriere relativ zum Parent
        if (parentStage != null) {
            stage.setX(parentStage.getX() + 25);
            stage.setY(parentStage.getY() + 25);
        }
        
        // Erstelle Layout
        root = new BorderPane();
        root.setPadding(new Insets(15));
        
        // Erstelle UI-Bereiche
        VBox topArea = createTopArea();
        SplitPane centerArea = createSplitCenterArea();  // NEU: Split-Layout
        HBox bottomArea = createBottomArea();
        
        root.setTop(topArea);
        root.setCenter(centerArea);
        root.setBottom(bottomArea);
        
        // Erstelle Scene
        scene = new Scene(root);
        stage.setScene(scene);
        
        LOGGER.info("Erweiterte historische Daten-Fenster mit Chart erstellt für: " + currencyPair);
    }
    
    /**
     * Erstellt den oberen Bereich (Titel + Filter) - erweitert um Chart-Filter
     */
    private VBox createTopArea() {
        VBox topArea = new VBox(10);
        
        // Titel-Bereich
        HBox titleArea = new HBox(15);
        titleArea.setAlignment(Pos.CENTER_LEFT);
        
        titleLabel = new Label("📊 Historische Daten & Chart: " + currencyPair);
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // Loading Indicator
        loadingIndicator = new ProgressIndicator();
        loadingIndicator.setPrefSize(20, 20);
        loadingIndicator.setVisible(false);
        
        titleArea.getChildren().addAll(titleLabel, spacer, loadingIndicator);
        
        // Filter-Bereich (erweitert)
        HBox filterArea = new HBox(15);
        filterArea.setAlignment(Pos.CENTER_LEFT);
        
        // Tabellen-Filter
        Label tableFilterLabel = new Label("Tabelle:");
        tableFilterLabel.setFont(Font.font(12));
        
        dataRangeCombo = new ComboBox<>();
        dataRangeCombo.getItems().addAll(DataRangeOption.values());
        dataRangeCombo.setValue(DataRangeOption.ALL);
        dataRangeCombo.setOnAction(e -> loadHistoricalData());
        
        // NEU: Chart-Filter
        Label chartFilterLabel = new Label("Chart:");
        chartFilterLabel.setFont(Font.font(12));
        
        chartRangeCombo = new ComboBox<>();
        chartRangeCombo.getItems().addAll(ChartRangeOption.values());
        chartRangeCombo.setValue(ChartRangeOption.LAST_7_DAYS);
        chartRangeCombo.setOnAction(e -> updateChart());
        
        // Buttons
        refreshButton = new Button("🔄 Aktualisieren");
        refreshButton.setOnAction(e -> loadHistoricalData());
        
        exportButton = new Button("📄 CSV Export");
        exportButton.setOnAction(e -> exportToCSV());
        
        filterArea.getChildren().addAll(
            tableFilterLabel, dataRangeCombo, 
            new Separator(Orientation.VERTICAL),
            chartFilterLabel, chartRangeCombo,
            new Separator(Orientation.VERTICAL),
            refreshButton, exportButton
        );
        
        // Summary-Bereich
        summaryLabel = new Label("Lade Daten...");
        summaryLabel.setFont(Font.font(11));
        summaryLabel.setStyle("-fx-text-fill: #666666;");
        
        topArea.getChildren().addAll(titleArea, filterArea, summaryLabel, new Separator());
        return topArea;
    }
    
    /**
     * NEU: Erstellt das Split-Layout für Tabelle und Chart
     */
    private SplitPane createSplitCenterArea() {
        splitPane = new SplitPane();
        splitPane.setOrientation(Orientation.HORIZONTAL);
        splitPane.setDividerPositions(0.5); // 50/50 Aufteilung
        
        // Linke Seite: Tabelle
        VBox leftPane = createTablePane();
        
        // Rechte Seite: Chart
        VBox rightPane = createChartPane();
        
        splitPane.getItems().addAll(leftPane, rightPane);
        
        // Verhindere zu kleine Bereiche
        splitPane.setDividerPositions(0.5);
        
        return splitPane;
    }
    
    /**
     * NEU: Erstellt den Tabellen-Bereich (linke Seite)
     */
    private VBox createTablePane() {
        VBox tablePane = new VBox(10);
        tablePane.setPadding(new Insets(5));
        VBox.setVgrow(tablePane, Priority.ALWAYS);
        
        // Tabellen-Header
        Label tableTitle = new Label("📋 CSV-Daten: " + currencyPair.replace("/", "_") + ".csv");
        tableTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
        
        // Historische Daten Tabelle
        historicalTable = createHistoricalDataTable();
        VBox.setVgrow(historicalTable, Priority.ALWAYS);
        
        tablePane.getChildren().addAll(tableTitle, historicalTable);
        return tablePane;
    }
    
    /**
     * NEU: Erstellt den Chart-Bereich (rechte Seite)
     */
    private VBox createChartPane() {
        VBox chartPane = new VBox(10);
        chartPane.setPadding(new Insets(5));
        VBox.setVgrow(chartPane, Priority.ALWAYS);
        
        // Chart-Header
        chartTitleLabel = new Label("📈 7-Tage-Verlauf: Buy-Percentage");
        chartTitleLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        
        // Chart erstellen
        buyPercentageChart = createBuyPercentageChart();
        VBox.setVgrow(buyPercentageChart, Priority.ALWAYS);
        
        // Chart-Legende
        Label chartLegend = new Label("🟢 Buy (< 40%)  🔴 Sell (> 60%)  ⚪ Neutral (40-60%)");
        chartLegend.setFont(Font.font(10));
        chartLegend.setStyle("-fx-text-fill: #666666;");
        
        chartPane.getChildren().addAll(chartTitleLabel, buyPercentageChart, chartLegend);
        return chartPane;
    }
    
    /**
     * NEU: Erstellt den Buy-Percentage Chart
     */
    private LineChart<Number, Number> createBuyPercentageChart() {
        // X-Achse (Zeit als Timestamp)
        chartXAxis = new NumberAxis();
        chartXAxis.setLabel("Zeit");
        chartXAxis.setAutoRanging(true);
        chartXAxis.setForceZeroInRange(false);
        
        // Y-Achse (Buy-Percentage)
        chartYAxis = new NumberAxis(0, 100, 10);
        chartYAxis.setLabel("Buy Percentage (%)");
        chartYAxis.setAutoRanging(false);
        
        // Chart erstellen
        LineChart<Number, Number> chart = new LineChart<>(chartXAxis, chartYAxis);
        chart.setTitle("Buy-Percentage Verlauf");
        chart.setCreateSymbols(true);
        chart.setLegendVisible(false);
        chart.getStylesheets().add("data:text/css," +
            ".chart-series-line { -fx-stroke-width: 2px; }" +
            ".chart-line-symbol { -fx-background-radius: 4px; -fx-padding: 4px; }"
        );
        
        // Custom Tick Label Formatter für X-Achse
        chartXAxis.setTickLabelFormatter(new javafx.util.StringConverter<Number>() {
            @Override
            public String toString(Number timestamp) {
                if (timestamp == null) return "";
                try {
                    LocalDateTime dateTime = LocalDateTime.ofEpochSecond(
                        timestamp.longValue(), 0, ZoneOffset.UTC
                    );
                    return dateTime.format(CHART_DATE_FORMATTER);
                } catch (Exception e) {
                    return timestamp.toString();
                }
            }
            
            @Override
            public Number fromString(String string) {
                return 0; // Nicht verwendet
            }
        });
        
        return chart;
    }
    
    /**
     * Erstellt die Tabelle für historische Daten (bestehende Funktionalität)
     */
    private TableView<HistoricalDataTableRow> createHistoricalDataTable() {
        TableView<HistoricalDataTableRow> table = new TableView<>();
        table.setItems(tableData);
        
        // Zeitstempel-Spalte
        TableColumn<HistoricalDataTableRow, String> timeColumn = new TableColumn<>("Zeitstempel");
        timeColumn.setCellValueFactory(new PropertyValueFactory<>("timeString"));
        timeColumn.setPrefWidth(140);
        
        // Buy%-Spalte
        TableColumn<HistoricalDataTableRow, String> buyColumn = new TableColumn<>("Buy %");
        buyColumn.setCellValueFactory(new PropertyValueFactory<>("buyPercentageString"));
        buyColumn.setPrefWidth(80);
        
        // Sell%-Spalte
        TableColumn<HistoricalDataTableRow, String> sellColumn = new TableColumn<>("Sell %");
        sellColumn.setCellValueFactory(new PropertyValueFactory<>("sellPercentageString"));
        sellColumn.setPrefWidth(80);
        
        // Trading Signal-Spalte
        TableColumn<HistoricalDataTableRow, String> signalColumn = new TableColumn<>("Signal");
        signalColumn.setCellValueFactory(new PropertyValueFactory<>("tradingSignalString"));
        signalColumn.setPrefWidth(100);
        
        // Differenz zur vorherigen Messung-Spalte
        TableColumn<HistoricalDataTableRow, String> changeColumn = new TableColumn<>("Änderung Buy%");
        changeColumn.setCellValueFactory(new PropertyValueFactory<>("buyPercentageChange"));
        changeColumn.setPrefWidth(120);
        
        // Bemerkungen-Spalte
        TableColumn<HistoricalDataTableRow, String> notesColumn = new TableColumn<>("Bemerkungen");
        notesColumn.setCellValueFactory(new PropertyValueFactory<>("notes"));
        notesColumn.setPrefWidth(150);
        
        table.getColumns().addAll(timeColumn, buyColumn, sellColumn, signalColumn, changeColumn, notesColumn);
        
        // Row-Factory für abwechselnde Farben und Highlight
        table.setRowFactory(tv -> {
            TableRow<HistoricalDataTableRow> row = new TableRow<HistoricalDataTableRow>() {
                @Override
                protected void updateItem(HistoricalDataTableRow item, boolean empty) {
                    super.updateItem(item, empty);
                    
                    if (empty || item == null) {
                        setStyle("");
                    } else {
                        // Farbliche Hervorhebung basierend auf Signal
                        switch (item.getOriginalData().getTradingSignal()) {
                            case BUY:
                                setStyle("-fx-background-color: #e8f5e8;"); // Helles Grün
                                break;
                            case SELL:
                                setStyle("-fx-background-color: #ffebee;"); // Helles Rot
                                break;
                            case NEUTRAL:
                                setStyle("-fx-background-color: #f5f5f5;"); // Helles Grau
                                break;
                            default:
                                setStyle("");
                                break;
                        }
                    }
                }
            };
            return row;
        });
        
        // Placeholder für leere Tabelle
        Label placeholder = new Label("Keine historischen Daten verfügbar\n\nMögliche Gründe:\n• Noch keine Daten für " + currencyPair + " gespeichert\n• CSV-Datei existiert nicht\n• Datenverzeichnis nicht gefunden");
        placeholder.setStyle("-fx-text-fill: #888888; -fx-text-alignment: center;");
        table.setPlaceholder(placeholder);
        
        return table;
    }
    
    /**
     * NEU: Aktualisiert den Chart basierend auf aktuellen Daten und Filtereinstellung
     */
    private void updateChart() {
        if (currentData == null || currentData.isEmpty()) {
            buyPercentageChart.getData().clear();
            return;
        }
        
        try {
            ChartRangeOption range = chartRangeCombo.getValue();
            
            // Filtere Daten nach gewähltem Zeitraum
            List<CurrencyPairData> filteredData = filterDataForChart(currentData, range);
            
            if (filteredData.isEmpty()) {
                buyPercentageChart.getData().clear();
                chartTitleLabel.setText("📈 Keine Daten für " + range.getDescription());
                return;
            }
            
            // Sortiere Daten nach Zeit (älteste zuerst für Chart)
            filteredData.sort((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()));
            
            // Erstelle Chart-Serie
            XYChart.Series<Number, Number> series = new XYChart.Series<>();
            series.setName("Buy Percentage");
            
            for (CurrencyPairData data : filteredData) {
                long timestamp = data.getTimestamp().toEpochSecond(ZoneOffset.UTC);
                XYChart.Data<Number, Number> dataPoint = new XYChart.Data<>(timestamp, data.getBuyPercentage());
                
                // Custom Symbol basierend auf Trading Signal
                dataPoint.nodeProperty().addListener((obs, oldNode, newNode) -> {
                    if (newNode != null) {
                        String symbolColor = getSignalColor(data.getTradingSignal());
                        newNode.setStyle("-fx-background-color: " + symbolColor + "; -fx-background-radius: 6px;");
                        
                        // Tooltip mit genauer Zeit und Signal-Info
                        Tooltip tooltip = new Tooltip(String.format(
                            "%s\nBuy: %.1f%% | Sell: %.1f%%\nSignal: %s",
                            data.getTimestamp().format(DATE_TIME_FORMATTER),
                            data.getBuyPercentage(),
                            data.getSellPercentage(),
                            data.getTradingSignal().getDescription()
                        ));
                        Tooltip.install(newNode, tooltip);
                    }
                });
                
                series.getData().add(dataPoint);
            }
            
            // Aktualisiere Chart
            buyPercentageChart.getData().clear();
            buyPercentageChart.getData().add(series);
            
            // Aktualisiere Chart-Titel
            chartTitleLabel.setText(String.format("📈 %s (%d Datenpunkte)", 
                range.getDescription(), filteredData.size()));
            
            // Aktualisiere X-Achsen-Bereich
            if (filteredData.size() > 1) {
                long minTime = filteredData.get(0).getTimestamp().toEpochSecond(ZoneOffset.UTC);
                long maxTime = filteredData.get(filteredData.size() - 1).getTimestamp().toEpochSecond(ZoneOffset.UTC);
                chartXAxis.setAutoRanging(false);
                chartXAxis.setLowerBound(minTime);
                chartXAxis.setUpperBound(maxTime);
                chartXAxis.setTickUnit((maxTime - minTime) / 8.0); // ~8 Ticks
            } else {
                chartXAxis.setAutoRanging(true);
            }
            
            LOGGER.info("Chart aktualisiert: " + filteredData.size() + " Datenpunkte für " + range.getDescription());
            
        } catch (Exception e) {
            LOGGER.warning("Fehler beim Aktualisieren des Charts: " + e.getMessage());
            chartTitleLabel.setText("📈 Fehler beim Laden der Chart-Daten");
        }
    }
    
    /**
     * NEU: Filtert Daten für Chart basierend auf gewähltem Zeitraum
     */
    private List<CurrencyPairData> filterDataForChart(List<CurrencyPairData> data, ChartRangeOption range) {
        LocalDateTime cutoff = LocalDateTime.now();
        
        switch (range) {
            case LAST_24_HOURS:
                cutoff = cutoff.minusHours(24);
                break;
            case LAST_3_DAYS:
                cutoff = cutoff.minusDays(3);
                break;
            case LAST_7_DAYS:
                cutoff = cutoff.minusDays(7);
                break;
            case LAST_14_DAYS:
                cutoff = cutoff.minusDays(14);
                break;
            case LAST_30_DAYS:
                cutoff = cutoff.minusDays(30);
                break;
            case ALL_DATA:
            default:
                return data; // Keine Filterung
        }
        
        final LocalDateTime finalCutoff = cutoff;
        return data.stream()
                .filter(d -> d.getTimestamp().isAfter(finalCutoff))
                .collect(Collectors.toList());
    }
    
    /**
     * KORRIGIERT: Gibt die KONSISTENTE Signalfarbe für Chart-Symbole zurück
     * Exakt gleiche Farblogik wie in der Tabelle
     */
    private String getConsistentSignalColor(CurrencyPairData.TradingSignal signal) {
        switch (signal) {
            case BUY:
                return "#4CAF50"; // Grün - wie grüne Tabellenzeilen
            case SELL:
                return "#f44336"; // Rot - wie rote Tabellenzeilen  
            case NEUTRAL:
                return "#9E9E9E"; // Grau - wie graue Tabellenzeilen
            default:
                return "#FF9800"; // Orange für UNKNOWN
        }
    }
    
    /**
     * DEPRECATED: Alte Methode - wird durch getConsistentSignalColor ersetzt
     * Nur für Kompatibilität falls noch Referenzen existieren
     */
    @Deprecated
    private String getSignalColor(CurrencyPairData.TradingSignal signal) {
        return getConsistentSignalColor(signal);
    }
    
    /**
     * NEU: Gibt Erklärung für Trading Signal zurück
     */
    private String getSignalExplanation(CurrencyPairData.TradingSignal signal) {
        switch (signal) {
            case BUY:
                return "Buy < 40% - Contrarian Signal";
            case SELL:
                return "Buy > 60% - Contrarian Signal";
            case NEUTRAL:
                return "Buy 40-60% - Neutral Zone";
            default:
                return "Unbekanntes Signal";
        }
    }
    
    /**
     * Erstellt den unteren Bereich (Buttons) - unverändert
     */
    private HBox createBottomArea() {
        HBox bottomArea = new HBox(10);
        bottomArea.setAlignment(Pos.CENTER_RIGHT);
        bottomArea.setPadding(new Insets(10, 0, 0, 0));
        
        Button statisticsButton = new Button("📈 Statistiken");
        statisticsButton.setOnAction(e -> showStatistics());
        
        closeButton = new Button("Schließen");
        closeButton.setOnAction(e -> stage.close());
        closeButton.setDefaultButton(true);
        
        bottomArea.getChildren().addAll(statisticsButton, closeButton);
        return bottomArea;
    }
    
    /**
     * Lädt die historischen Daten und aktualisiert Tabelle UND Chart
     */
    private void loadHistoricalData() {
        // Zeige Loading-Indikator
        loadingIndicator.setVisible(true);
        refreshButton.setDisable(true);
        summaryLabel.setText("Lade historische Daten...");
        
        // Lade Daten in separatem Thread
        new Thread(() -> {
            try {
                DataRangeOption range = dataRangeCombo.getValue();
                List<CurrencyPairData> historicalData;
                
                // Lade Daten basierend auf Filter
                switch (range) {
                    case LAST_100:
                        historicalData = dataService.getRecentDataForCurrencyPair(currencyPair, 100);
                        break;
                    case LAST_500:
                        historicalData = dataService.getRecentDataForCurrencyPair(currencyPair, 500);
                        break;
                    case LAST_1000:
                        historicalData = dataService.getRecentDataForCurrencyPair(currencyPair, 1000);
                        break;
                    case ALL:
                    default:
                        historicalData = dataService.getHistoricalDataForCurrencyPair(currencyPair);
                        break;
                }
                
                // Aktualisiere UI im JavaFX Application Thread
                javafx.application.Platform.runLater(() -> {
                    // Speichere aktuelle Daten für Chart
                    currentData = historicalData;
                    
                    // Aktualisiere Tabelle
                    updateTable(historicalData, range);
                    updateSummary(historicalData, range);
                    
                    // NEU: Aktualisiere Chart
                    updateChart();
                    
                    loadingIndicator.setVisible(false);
                    refreshButton.setDisable(false);
                });
                
                LOGGER.info("Historische Daten geladen: " + historicalData.size() + " Einträge für " + currencyPair);
                
            } catch (Exception e) {
                LOGGER.warning("Fehler beim Laden der historischen Daten: " + e.getMessage());
                
                javafx.application.Platform.runLater(() -> {
                    summaryLabel.setText("Fehler beim Laden der Daten: " + e.getMessage());
                    loadingIndicator.setVisible(false);
                    refreshButton.setDisable(false);
                });
            }
        }).start();
    }
    
    /**
     * Aktualisiert die Tabelle mit historischen Daten (unverändert)
     */
    private void updateTable(List<CurrencyPairData> data, DataRangeOption range) {
        tableData.clear();
        
        // Sortiere Daten nach Zeitstempel (neueste zuerst)
        data.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));
        
        CurrencyPairData previousData = null;
        
        for (CurrencyPairData currentData : data) {
            HistoricalDataTableRow row = new HistoricalDataTableRow(currentData, previousData);
            tableData.add(row);
            previousData = currentData;
        }
    }
    
    /**
     * Aktualisiert die Zusammenfassung (unverändert)
     */
    private void updateSummary(List<CurrencyPairData> data, DataRangeOption range) {
        if (data.isEmpty()) {
            summaryLabel.setText("Keine historischen Daten für " + currencyPair + " gefunden.");
            return;
        }
        
        // Berechne Statistiken
        double avgBuy = data.stream().mapToDouble(CurrencyPairData::getBuyPercentage).average().orElse(0.0);
        double minBuy = data.stream().mapToDouble(CurrencyPairData::getBuyPercentage).min().orElse(0.0);
        double maxBuy = data.stream().mapToDouble(CurrencyPairData::getBuyPercentage).max().orElse(0.0);
        
        long buySignals = data.stream().filter(d -> d.getTradingSignal() == CurrencyPairData.TradingSignal.BUY).count();
        long sellSignals = data.stream().filter(d -> d.getTradingSignal() == CurrencyPairData.TradingSignal.SELL).count();
        long neutralSignals = data.stream().filter(d -> d.getTradingSignal() == CurrencyPairData.TradingSignal.NEUTRAL).count();
        
        String summaryText = String.format(
            "%d Datensätze (%s) | Ø Buy: %.1f%% | Min: %.1f%% | Max: %.1f%% | Signale: %d📈 %d📉 %d↔️",
            data.size(), range.getDescription(), avgBuy, minBuy, maxBuy, buySignals, sellSignals, neutralSignals
        );
        
        summaryLabel.setText(summaryText);
    }
    
    /**
     * Exportiert die Daten als CSV (unverändert)
     */
    private void exportToCSV() {
        try {
            List<CurrencyPairData> allData = dataService.getHistoricalDataForCurrencyPair(currencyPair);
            
            if (allData.isEmpty()) {
                showAlert("Keine Daten zum Exportieren vorhanden.");
                return;
            }
            
            // Erstelle CSV-Content
            StringBuilder csv = new StringBuilder();
            csv.append(CurrencyPairData.getCsvHeader()).append("\n");
            
            for (CurrencyPairData data : allData) {
                csv.append(data.toCsvLine()).append("\n");
            }
            
            // Zeige CSV-Content in neuem Fenster
            TextArea csvArea = new TextArea(csv.toString());
            csvArea.setEditable(false);
            csvArea.setPrefSize(700, 500);
            csvArea.setStyle("-fx-font-family: 'Courier New', monospace; -fx-font-size: 11px;");
            
            Stage csvStage = new Stage();
            csvStage.setTitle("CSV Export: " + currencyPair);
            csvStage.setScene(new Scene(new VBox(10,
                new Label("CSV-Daten für " + currencyPair + " (kopieren Sie den Inhalt):"),
                csvArea,
                new Label("Datei-Pfad: " + dataService.getDataDirectory() + "/currency_pairs/" + currencyPair.replace("/", "_") + ".csv")
            ), 720, 600));
            csvStage.show();
            
        } catch (Exception e) {
            showAlert("Fehler beim Export: " + e.getMessage());
        }
    }
    
    /**
     * Zeigt Statistiken an (unverändert)
     */
    private void showStatistics() {
        try {
            List<CurrencyPairData> allData = dataService.getHistoricalDataForCurrencyPair(currencyPair);
            
            if (allData.isEmpty()) {
                showAlert("Keine Daten für Statistiken verfügbar.");
                return;
            }
            
            // Berechne detaillierte Statistiken
            StringBuilder stats = new StringBuilder();
            stats.append("DETAILLIERTE STATISTIKEN FÜR ").append(currencyPair).append("\n");
            stats.append("=======================================\n\n");
            
            stats.append("Gesamt-Datensätze: ").append(allData.size()).append("\n");
            
            // Zeitraum
            allData.sort((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()));
            stats.append("Zeitraum: ").append(allData.get(0).getTimestamp().format(DATE_TIME_FORMATTER))
                 .append(" bis ").append(allData.get(allData.size()-1).getTimestamp().format(DATE_TIME_FORMATTER)).append("\n\n");
            
            // Buy-Percentage Statistiken
            double avgBuy = allData.stream().mapToDouble(CurrencyPairData::getBuyPercentage).average().orElse(0.0);
            double minBuy = allData.stream().mapToDouble(CurrencyPairData::getBuyPercentage).min().orElse(0.0);
            double maxBuy = allData.stream().mapToDouble(CurrencyPairData::getBuyPercentage).max().orElse(0.0);
            
            stats.append("BUY-PERCENTAGE STATISTIKEN:\n");
            stats.append("Durchschnitt: ").append(String.format("%.2f%%", avgBuy)).append("\n");
            stats.append("Minimum: ").append(String.format("%.2f%%", minBuy)).append("\n");
            stats.append("Maximum: ").append(String.format("%.2f%%", maxBuy)).append("\n\n");
            
            // Signal-Verteilung
            long buySignals = allData.stream().filter(d -> d.getTradingSignal() == CurrencyPairData.TradingSignal.BUY).count();
            long sellSignals = allData.stream().filter(d -> d.getTradingSignal() == CurrencyPairData.TradingSignal.SELL).count();
            long neutralSignals = allData.stream().filter(d -> d.getTradingSignal() == CurrencyPairData.TradingSignal.NEUTRAL).count();
            long unknownSignals = allData.stream().filter(d -> d.getTradingSignal() == CurrencyPairData.TradingSignal.UNKNOWN).count();
            
            stats.append("SIGNAL-VERTEILUNG:\n");
            stats.append("BUY: ").append(buySignals).append(" (").append(String.format("%.1f%%", (buySignals * 100.0 / allData.size()))).append(")\n");
            stats.append("SELL: ").append(sellSignals).append(" (").append(String.format("%.1f%%", (sellSignals * 100.0 / allData.size()))).append(")\n");
            stats.append("NEUTRAL: ").append(neutralSignals).append(" (").append(String.format("%.1f%%", (neutralSignals * 100.0 / allData.size()))).append(")\n");
            stats.append("UNKNOWN: ").append(unknownSignals).append(" (").append(String.format("%.1f%%", (unknownSignals * 100.0 / allData.size()))).append(")\n\n");
            
            // Extreme Werte
            stats.append("EXTREME WERTE (Buy > 80% oder < 20%):\n");
            long extremeHigh = allData.stream().filter(d -> d.getBuyPercentage() > 80).count();
            long extremeLow = allData.stream().filter(d -> d.getBuyPercentage() < 20).count();
            stats.append("Buy > 80%: ").append(extremeHigh).append(" Vorkommen\n");
            stats.append("Buy < 20%: ").append(extremeLow).append(" Vorkommen\n");
            
            // Zeige Statistiken in neuem Fenster
            TextArea statsArea = new TextArea(stats.toString());
            statsArea.setEditable(false);
            statsArea.setPrefSize(500, 400);
            statsArea.setStyle("-fx-font-family: 'Courier New', monospace; -fx-font-size: 12px;");
            
            Stage statsStage = new Stage();
            statsStage.setTitle("Statistiken: " + currencyPair);
            statsStage.setScene(new Scene(new VBox(10,
                new Label("Detaillierte Statistiken für " + currencyPair + ":"),
                statsArea
            ), 520, 450));
            statsStage.show();
            
        } catch (Exception e) {
            showAlert("Fehler beim Berechnen der Statistiken: " + e.getMessage());
        }
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
     * Hilfsmethode für Alert-Dialoge
     */
    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    // ===== INNERE KLASSEN =====
    
    /**
     * Enum für Datenbereich-Filter (unverändert)
     */
    public enum DataRangeOption {
        LAST_100("Letzte 100 Einträge"),
        LAST_500("Letzte 500 Einträge"),
        LAST_1000("Letzte 1000 Einträge"),
        ALL("Alle Daten");
        
        private final String description;
        
        DataRangeOption(String description) {
            this.description = description;
        }
        
        public String getDescription() { return description; }
        
        @Override
        public String toString() { return description; }
    }
    
    /**
     * NEU: Enum für Chart-Zeitraum-Filter
     */
    public enum ChartRangeOption {
        LAST_24_HOURS("Letzte 24 Stunden"),
        LAST_3_DAYS("Letzte 3 Tage"),
        LAST_7_DAYS("Letzte 7 Tage"),
        LAST_14_DAYS("Letzte 14 Tage"),
        LAST_30_DAYS("Letzte 30 Tage"),
        ALL_DATA("Alle Daten");
        
        private final String description;
        
        ChartRangeOption(String description) {
            this.description = description;
        }
        
        public String getDescription() { return description; }
        
        @Override
        public String toString() { return description; }
    }
    
    /**
     * Wrapper-Klasse für Tabellenzeilendaten (unverändert)
     */
    public static class HistoricalDataTableRow {
        private final CurrencyPairData originalData;
        private final String timeString;
        private final String buyPercentageString;
        private final String sellPercentageString;
        private final String tradingSignalString;
        private final String buyPercentageChange;
        private final String notes;
        
        public HistoricalDataTableRow(CurrencyPairData data, CurrencyPairData previousData) {
            this.originalData = data;
            this.timeString = data.getTimestamp().format(DATE_TIME_FORMATTER);
            this.buyPercentageString = String.format("%.1f%%", data.getBuyPercentage());
            this.sellPercentageString = String.format("%.1f%%", data.getSellPercentage());
            this.tradingSignalString = getSignalIcon(data.getTradingSignal()) + " " + data.getTradingSignal().getDescription();
            
            // Berechne Änderung zur vorherigen Messung
            if (previousData != null) {
                double change = data.getBuyPercentage() - previousData.getBuyPercentage();
                if (Math.abs(change) < 0.1) {
                    this.buyPercentageChange = "±0%";
                } else if (change > 0) {
                    this.buyPercentageChange = String.format("+%.1f%%", change);
                } else {
                    this.buyPercentageChange = String.format("%.1f%%", change);
                }
            } else {
                this.buyPercentageChange = "-";
            }
            
            // Erstelle Bemerkungen für spezielle Werte
            StringBuilder noteBuilder = new StringBuilder();
            if (data.getBuyPercentage() > 80) {
                noteBuilder.append("Extreme Buy-Dominanz ");
            } else if (data.getBuyPercentage() < 20) {
                noteBuilder.append("Extreme Sell-Dominanz ");
            }
            
            if (!data.isDataConsistent()) {
                noteBuilder.append("Inkonsistente Daten ");
            }
            
            this.notes = noteBuilder.toString().trim();
        }
        
        private String getSignalIcon(CurrencyPairData.TradingSignal signal) {
            switch (signal) {
                case BUY: return "📈";
                case SELL: return "📉";
                case NEUTRAL: return "↔️";
                default: return "❓";
            }
        }
        
        // Getter für TableView
        public CurrencyPairData getOriginalData() { return originalData; }
        public String getTimeString() { return timeString; }
        public String getBuyPercentageString() { return buyPercentageString; }
        public String getSellPercentageString() { return sellPercentageString; }
        public String getTradingSignalString() { return tradingSignalString; }
        public String getBuyPercentageChange() { return buyPercentageChange; }
        public String getNotes() { return notes; }
    }
}