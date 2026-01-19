package com.fxssi.extractor.gui;

import com.fxssi.extractor.model.SignalChangeEvent;
import com.fxssi.extractor.storage.SignalChangeHistoryManager;

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
 * Popup-Fenster für die detaillierte Anzeige der Signalwechsel-Historie
 * Zeigt eine Timeline und detaillierte Tabelle der Signalwechsel für ein Währungspaar
 * 
 * @author Generated for FXSSI Signal Change Detection
 * @version 1.0
 */
public class SignalChangeHistoryWindow {
    
    private static final Logger LOGGER = Logger.getLogger(SignalChangeHistoryWindow.class.getName());
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    
    private Stage stage;
    private Scene scene;
    private BorderPane root;
    
    // UI-Komponenten
    private Label titleLabel;
    private Label summaryLabel;
    private TextArea timelineArea;
    private TableView<SignalChangeTableRow> changesTable;
    private ComboBox<TimeFilterOption> timeFilterCombo;
    private Button refreshButton;
    private Button closeButton;
    
    // Daten
    private final String currencyPair;
    private final SignalChangeHistoryManager historyManager;
    private ObservableList<SignalChangeTableRow> tableData;
    
    /**
     * Konstruktor
     * @param parentStage Parent-Fenster
     * @param currencyPair Das Währungspaar für das die Historie angezeigt werden soll
     * @param historyManager Der History-Manager für Datenzugriff
     */
    public SignalChangeHistoryWindow(Stage parentStage, String currencyPair, SignalChangeHistoryManager historyManager) {
        this.currencyPair = currencyPair;
        this.historyManager = historyManager;
        this.tableData = FXCollections.observableArrayList();
        
        createWindow(parentStage);
        loadData();
    }
    
    /**
     * Erstellt und konfiguriert das Popup-Fenster
     */
    private void createWindow(Stage parentStage) {
        stage = new Stage();
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(parentStage);
        stage.setTitle("Signalwechsel-Historie: " + currencyPair);
        stage.setWidth(800);
        stage.setHeight(600);
        stage.setMinWidth(600);
        stage.setMinHeight(400);
        
        // Zentriere relativ zum Parent
        if (parentStage != null) {
            stage.setX(parentStage.getX() + 100);
            stage.setY(parentStage.getY() + 50);
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
        
        LOGGER.info("Signalwechsel-Historie-Fenster erstellt für: " + currencyPair);
    }
    
    /**
     * Erstellt den oberen Bereich (Titel + Filter)
     */
    private VBox createTopArea() {
        VBox topArea = new VBox(10);
        
        // Titel-Bereich
        HBox titleArea = new HBox(15);
        titleArea.setAlignment(Pos.CENTER_LEFT);
        
        titleLabel = new Label("Signalwechsel-Historie: " + currencyPair);
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // Time Filter
        Label filterLabel = new Label("Zeitraum:");
        filterLabel.setFont(Font.font(12));
        
        timeFilterCombo = new ComboBox<>();
        timeFilterCombo.getItems().addAll(TimeFilterOption.values());
        timeFilterCombo.setValue(TimeFilterOption.ALL);
        timeFilterCombo.setOnAction(e -> loadData());
        
        refreshButton = new Button("🔄 Aktualisieren");
        refreshButton.setOnAction(e -> loadData());
        
        titleArea.getChildren().addAll(titleLabel, spacer, filterLabel, timeFilterCombo, refreshButton);
        
        // Summary-Bereich
        summaryLabel = new Label("Lade Daten...");
        summaryLabel.setFont(Font.font(11));
        summaryLabel.setStyle("-fx-text-fill: #666666;");
        
        topArea.getChildren().addAll(titleArea, summaryLabel, new Separator());
        return topArea;
    }
    
    /**
     * Erstellt den mittleren Bereich (Timeline + Tabelle)
     */
    private VBox createCenterArea() {
        VBox centerArea = new VBox(10);
        VBox.setVgrow(centerArea, Priority.ALWAYS);
        
        // Timeline-Bereich
        Label timelineTitle = new Label("📈 Signalwechsel-Timeline");
        timelineTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
        
        timelineArea = new TextArea();
        timelineArea.setPrefHeight(120);
        timelineArea.setMaxHeight(120);
        timelineArea.setEditable(false);
        timelineArea.setStyle("-fx-font-family: 'Courier New', monospace; -fx-font-size: 11px;");
        
        // Tabellen-Bereich
        Label tableTitle = new Label("📋 Detaillierte Historie");
        tableTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
        
        changesTable = createChangesTable();
        VBox.setVgrow(changesTable, Priority.ALWAYS);
        
        centerArea.getChildren().addAll(
            timelineTitle, timelineArea,
            new Separator(),
            tableTitle, changesTable
        );
        
        return centerArea;
    }
    
    /**
     * Erstellt die Tabelle für Signalwechsel-Details
     */
    private TableView<SignalChangeTableRow> createChangesTable() {
        TableView<SignalChangeTableRow> table = new TableView<>();
        table.setItems(tableData);
        
        // Zeitstempel-Spalte
        TableColumn<SignalChangeTableRow, String> timeColumn = new TableColumn<>("Zeit");
        timeColumn.setCellValueFactory(new PropertyValueFactory<>("timeString"));
        timeColumn.setPrefWidth(120);
        
        // Wechsel-Spalte
        TableColumn<SignalChangeTableRow, String> changeColumn = new TableColumn<>("Signalwechsel");
        changeColumn.setCellValueFactory(new PropertyValueFactory<>("changeDescription"));
        changeColumn.setPrefWidth(150);
        
        // Buy%-Änderung-Spalte
        TableColumn<SignalChangeTableRow, String> buyChangeColumn = new TableColumn<>("Buy% Änderung");
        buyChangeColumn.setCellValueFactory(new PropertyValueFactory<>("buyPercentageChange"));
        buyChangeColumn.setPrefWidth(120);
        
        // Wichtigkeit-Spalte
        TableColumn<SignalChangeTableRow, String> importanceColumn = new TableColumn<>("Wichtigkeit");
        importanceColumn.setCellValueFactory(new PropertyValueFactory<>("importanceString"));
        importanceColumn.setPrefWidth(100);
        
        // Aktualität-Spalte
        TableColumn<SignalChangeTableRow, String> actualityColumn = new TableColumn<>("Aktualität");
        actualityColumn.setCellValueFactory(new PropertyValueFactory<>("actualityString"));
        actualityColumn.setPrefWidth(100);
        
        // Detaillierte Beschreibung-Spalte
        TableColumn<SignalChangeTableRow, String> detailsColumn = new TableColumn<>("Details");
        detailsColumn.setCellValueFactory(new PropertyValueFactory<>("detailedDescription"));
        detailsColumn.setPrefWidth(200);
        
        table.getColumns().addAll(timeColumn, changeColumn, buyChangeColumn, importanceColumn, actualityColumn, detailsColumn);
        
        // Row-Factory für farbliche Hervorhebung
        table.setRowFactory(tv -> {
            TableRow<SignalChangeTableRow> row = new TableRow<SignalChangeTableRow>() {
                @Override
                protected void updateItem(SignalChangeTableRow item, boolean empty) {
                    super.updateItem(item, empty);
                    
                    if (empty || item == null) {
                        setStyle("");
                    } else {
                        // Farbliche Hervorhebung basierend auf Aktualität
                        SignalChangeEvent.SignalChangeActuality actuality = item.getOriginalEvent().getActuality();
                        switch (actuality) {
                            case VERY_RECENT:
                                setStyle("-fx-background-color: #ffebee;"); // Helles Rot
                                break;
                            case RECENT:
                                setStyle("-fx-background-color: #fff8e1;"); // Helles Gelb
                                break;
                            case THIS_WEEK:
                                setStyle("-fx-background-color: #e8f5e8;"); // Helles Grün
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
        
        // Placeholder
        Label placeholder = new Label("Keine Signalwechsel gefunden");
        placeholder.setStyle("-fx-text-fill: #888888;");
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
        
        Button exportButton = new Button("📊 Export CSV");
        exportButton.setOnAction(e -> exportToCSV());
        
        closeButton = new Button("Schließen");
        closeButton.setOnAction(e -> stage.close());
        closeButton.setDefaultButton(true);
        
        bottomArea.getChildren().addAll(exportButton, closeButton);
        return bottomArea;
    }
    
    /**
     * Lädt die Signalwechsel-Daten basierend auf dem gewählten Filter
     */
    private void loadData() {
        try {
            TimeFilterOption filter = timeFilterCombo.getValue();
            List<SignalChangeEvent> changes;
            
            // Lade Daten basierend auf Filter
            switch (filter) {
                case LAST_2_HOURS:
                    changes = historyManager.getSignalChangesWithinHours(currencyPair, 2);
                    break;
                case TODAY:
                    changes = historyManager.getSignalChangesWithinHours(currencyPair, 24);
                    break;
                case THIS_WEEK:
                    changes = historyManager.getSignalChangesWithinHours(currencyPair, 168);
                    break;
                case LAST_30_DAYS:
                    changes = historyManager.getSignalChangesWithinHours(currencyPair, 720);
                    break;
                case ALL:
                default:
                    changes = historyManager.getSignalChangeHistory(currencyPair);
                    break;
            }
            
            // Aktualisiere UI
            updateSummary(changes, filter);
            updateTimeline(changes);
            updateTable(changes);
            
            LOGGER.fine("Signalwechsel-Daten geladen: " + changes.size() + " Einträge für " + currencyPair);
            
        } catch (Exception e) {
            LOGGER.warning("Fehler beim Laden der Signalwechsel-Daten: " + e.getMessage());
            summaryLabel.setText("Fehler beim Laden der Daten: " + e.getMessage());
        }
    }
    
    /**
     * Aktualisiert die Zusammenfassung
     */
    private void updateSummary(List<SignalChangeEvent> changes, TimeFilterOption filter) {
        if (changes.isEmpty()) {
            summaryLabel.setText("Keine Signalwechsel im gewählten Zeitraum gefunden.");
            return;
        }
        
        // Statistiken berechnen
        long criticalChanges = changes.stream().filter(c -> c.getImportance() == SignalChangeEvent.SignalChangeImportance.CRITICAL).count();
        long highChanges = changes.stream().filter(c -> c.getImportance() == SignalChangeEvent.SignalChangeImportance.HIGH).count();
        long recentChanges = changes.stream().filter(c -> c.getActuality() == SignalChangeEvent.SignalChangeActuality.VERY_RECENT).count();
        
        String summaryText = String.format(
            "%d Signalwechsel gefunden (%s) | 🚨 %d Kritisch | ⚠️ %d Hoch | 🔴 %d Sehr aktuell",
            changes.size(), filter.getDescription(), criticalChanges, highChanges, recentChanges
        );
        
        summaryLabel.setText(summaryText);
    }
    
    /**
     * Aktualisiert die Timeline-Anzeige
     */
    private void updateTimeline(List<SignalChangeEvent> changes) {
        if (changes.isEmpty()) {
            timelineArea.setText("Keine Signalwechsel verfügbar.");
            return;
        }
        
        StringBuilder timeline = new StringBuilder();
        timeline.append(String.format("=== SIGNALWECHSEL-TIMELINE FÜR %s ===\n\n", currencyPair));
        
        for (int i = 0; i < Math.min(changes.size(), 10); i++) { // Zeige maximal 10 in Timeline
            SignalChangeEvent change = changes.get(i);
            timeline.append(change.getTimelineDescription()).append("\n");
        }
        
        if (changes.size() > 10) {
            timeline.append(String.format("\n... und %d weitere Signalwechsel (siehe Tabelle unten)", changes.size() - 10));
        }
        
        timelineArea.setText(timeline.toString());
    }
    
    /**
     * Aktualisiert die Tabellen-Daten
     */
    private void updateTable(List<SignalChangeEvent> changes) {
        tableData.clear();
        
        for (SignalChangeEvent change : changes) {
            SignalChangeTableRow row = new SignalChangeTableRow(change);
            tableData.add(row);
        }
    }
    
    /**
     * Exportiert die Daten als CSV
     */
    private void exportToCSV() {
        // Vereinfachte Implementation - in echter Anwendung würde man FileChooser verwenden
        try {
            List<SignalChangeEvent> allChanges = historyManager.getSignalChangeHistory(currencyPair);
            
            if (allChanges.isEmpty()) {
                showAlert("Keine Daten zum Exportieren vorhanden.");
                return;
            }
            
            // Erstelle CSV-Content
            StringBuilder csv = new StringBuilder();
            csv.append(SignalChangeEvent.getCsvHeader()).append("\n");
            
            for (SignalChangeEvent change : allChanges) {
                csv.append(change.toCsvLine()).append("\n");
            }
            
            // Zeige CSV-Content in neuem Fenster (vereinfacht)
            TextArea csvArea = new TextArea(csv.toString());
            csvArea.setEditable(false);
            csvArea.setPrefSize(600, 400);
            
            Stage csvStage = new Stage();
            csvStage.setTitle("CSV Export: " + currencyPair);
            csvStage.setScene(new Scene(new VBox(10, 
                new Label("CSV-Daten (kopieren Sie den Inhalt):"),
                csvArea
            ), 620, 450));
            csvStage.show();
            
        } catch (Exception e) {
            showAlert("Fehler beim Export: " + e.getMessage());
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
     * Enum für Zeit-Filter-Optionen
     */
    public enum TimeFilterOption {
        LAST_2_HOURS("Letzte 2 Stunden"),
        TODAY("Heute"),
        THIS_WEEK("Diese Woche"),
        LAST_30_DAYS("Letzte 30 Tage"),
        ALL("Alle");
        
        private final String description;
        
        TimeFilterOption(String description) {
            this.description = description;
        }
        
        public String getDescription() { return description; }
        
        @Override
        public String toString() { return description; }
    }
    
    /**
     * Wrapper-Klasse für Tabellenzeilendaten
     */
    public static class SignalChangeTableRow {
        private final SignalChangeEvent originalEvent;
        private final String timeString;
        private final String changeDescription;
        private final String buyPercentageChange;
        private final String importanceString;
        private final String actualityString;
        private final String detailedDescription;
        
        public SignalChangeTableRow(SignalChangeEvent event) {
            this.originalEvent = event;
            this.timeString = event.getChangeTime().format(DATE_TIME_FORMATTER);
            this.changeDescription = event.getChangeDescription();
            this.buyPercentageChange = String.format("%.1f%% → %.1f%%", 
                event.getFromBuyPercentage(), event.getToBuyPercentage());
            this.importanceString = event.getImportance().getIcon() + " " + event.getImportance().getDescription();
            this.actualityString = event.getActuality().getIcon() + " " + event.getActuality().getDescription();
            this.detailedDescription = event.getDetailedDescription();
        }
        
        // Getter für TableView
        public SignalChangeEvent getOriginalEvent() { return originalEvent; }
        public String getTimeString() { return timeString; }
        public String getChangeDescription() { return changeDescription; }
        public String getBuyPercentageChange() { return buyPercentageChange; }
        public String getImportanceString() { return importanceString; }
        public String getActualityString() { return actualityString; }
        public String getDetailedDescription() { return detailedDescription; }
    }
}