package com.fxsssi.extractor.gui;

import com.fxssi.extractor.model.CurrencyPairData;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Logger;

/**
 * Popup-Fenster für die Anzeige historischer CSV-Daten eines Währungspaares
 * Lädt die Daten über den GuiDataService und zeigt sie in einer detaillierten Tabelle an
 * 
 * @author Generated for FXSSI Historical Data Display
 * @version 1.0
 */
public class HistoricalDataWindow {
    
    private static final Logger LOGGER = Logger.getLogger(HistoricalDataWindow.class.getName());
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    
    private Stage stage;
    private Scene scene;
    private BorderPane root;
    
    // UI-Komponenten
    private Label titleLabel;
    private Label summaryLabel;
    private TableView<HistoricalDataTableRow> historicalTable;
    private ComboBox<DataRangeOption> dataRangeCombo;
    private Button refreshButton;
    private Button exportButton;
    private Button closeButton;
    private ProgressIndicator loadingIndicator;
    
    // Daten
    private final String currencyPair;
    private final GuiDataService dataService;
    private ObservableList<HistoricalDataTableRow> tableData;
    
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
        
        createWindow(parentStage);
        loadHistoricalData();
    }
    
    /**
     * Erstellt und konfiguriert das Popup-Fenster
     */
    private void createWindow(Stage parentStage) {
        stage = new Stage();
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(parentStage);
        stage.setTitle("Historische Daten: " + currencyPair);
        stage.setWidth(900);
        stage.setHeight(700);
        stage.setMinWidth(700);
        stage.setMinHeight(500);
        
        // Zentriere relativ zum Parent
        if (parentStage != null) {
            stage.setX(parentStage.getX() + 50);
            stage.setY(parentStage.getY() + 30);
        }
        
        // Erstelle Layout
        root = new BorderPane();
        root.setPadding(new Insets(15));
        
        // Erstelle UI-Bereiche
        VBox topArea = createTopArea();
        VBox centerArea = createCenterArea();
        HBox bottomArea = createBottomArea();
        
        root.setTop(topArea);
        root.setCenter(centerArea);
        root.setBottom(bottomArea);
        
        // Erstelle Scene
        scene = new Scene(root);
        stage.setScene(scene);
        
        LOGGER.info("Historische Daten-Fenster erstellt für: " + currencyPair);
    }
    
    /**
     * Erstellt den oberen Bereich (Titel + Filter)
     */
    private VBox createTopArea() {
        VBox topArea = new VBox(10);
        
        // Titel-Bereich
        HBox titleArea = new HBox(15);
        titleArea.setAlignment(Pos.CENTER_LEFT);
        
        titleLabel = new Label("📊 Historische Daten: " + currencyPair);
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // Loading Indicator
        loadingIndicator = new ProgressIndicator();
        loadingIndicator.setPrefSize(20, 20);
        loadingIndicator.setVisible(false);
        
        titleArea.getChildren().addAll(titleLabel, spacer, loadingIndicator);
        
        // Filter-Bereich
        HBox filterArea = new HBox(10);
        filterArea.setAlignment(Pos.CENTER_LEFT);
        
        Label filterLabel = new Label("Datenbereich:");
        filterLabel.setFont(Font.font(12));
        
        dataRangeCombo = new ComboBox<>();
        dataRangeCombo.getItems().addAll(DataRangeOption.values());
        dataRangeCombo.setValue(DataRangeOption.ALL);
        dataRangeCombo.setOnAction(e -> loadHistoricalData());
        
        refreshButton = new Button("🔄 Aktualisieren");
        refreshButton.setOnAction(e -> loadHistoricalData());
        
        exportButton = new Button("📄 CSV Export");
        exportButton.setOnAction(e -> exportToCSV());
        
        filterArea.getChildren().addAll(filterLabel, dataRangeCombo, refreshButton, exportButton);
        
        // Summary-Bereich
        summaryLabel = new Label("Lade Daten...");
        summaryLabel.setFont(Font.font(11));
        summaryLabel.setStyle("-fx-text-fill: #666666;");
        
        topArea.getChildren().addAll(titleArea, filterArea, summaryLabel, new Separator());
        return topArea;
    }
    
    /**
     * Erstellt den mittleren Bereich (Tabelle)
     */
    private VBox createCenterArea() {
        VBox centerArea = new VBox(10);
        VBox.setVgrow(centerArea, Priority.ALWAYS);
        
        // Tabellen-Header
        Label tableTitle = new Label("💾 CSV-Daten aus Dateien: " + currencyPair.replace("/", "_") + ".csv");
        tableTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
        
        // Historische Daten Tabelle
        historicalTable = createHistoricalDataTable();
        VBox.setVgrow(historicalTable, Priority.ALWAYS);
        
        centerArea.getChildren().addAll(tableTitle, historicalTable);
        return centerArea;
    }
    
    /**
     * Erstellt die Tabelle für historische Daten
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
        notesColumn.setPrefWidth(200);
        
        table.getColumns().addAll(timeColumn, buyColumn, sellColumn, signalColumn, changeColumn, notesColumn);
        
        // Row-Factory für abwechselnde Farben
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
     * Erstellt den unteren Bereich (Buttons)
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
     * Lädt die historischen Daten basierend auf dem gewählten Filter
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
                    updateTable(historicalData, range);
                    updateSummary(historicalData, range);
                    
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
     * Aktualisiert die Tabelle mit historischen Daten
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
     * Aktualisiert die Zusammenfassung
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
     * Exportiert die Daten als CSV
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
     * Zeigt Statistiken an
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
     * Enum für Datenbereich-Filter
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
     * Wrapper-Klasse für Tabellenzeilendaten
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