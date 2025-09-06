package com.fxsssi.extractor.gui;

import com.fxssi.extractor.model.SignalChangeEvent;
import com.fxssi.extractor.storage.SignalChangeHistoryManager;
import com.fxsssi.extractor.gui.MainWindowController.CurrencyPairTableRow;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Logger;

/**
 * Custom TableCell für die Anzeige von Signalwechsel-Informationen
 * Zeigt ein Icon mit Aktualitäts-Information und öffnet bei Klick das Detail-Popup
 * 
 * @author Generated for FXSSI Signal Change Detection
 * @version 1.0
 */
public class SignalChangeTableCell extends TableCell<CurrencyPairTableRow, CurrencyPairTableRow> {
    
    private static final Logger LOGGER = Logger.getLogger(SignalChangeTableCell.class.getName());
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM");
    
    private final HBox container;
    private final Button changeButton;
    private final Label timeLabel;
    private SignalChangeHistoryManager historyManager;
    private Stage parentStage;
    
    public SignalChangeTableCell(SignalChangeHistoryManager historyManager, Stage parentStage) {
        this.historyManager = historyManager;
        this.parentStage = parentStage;
        
        // Erstelle Container
        container = new HBox(5);
        container.setAlignment(Pos.CENTER);
        
        // Erstelle Change-Button (wird als Icon verwendet)
        changeButton = new Button();
        changeButton.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-padding: 2;");
        changeButton.setFont(Font.font(16));
        changeButton.setPrefSize(30, 30);
        changeButton.setMaxSize(30, 30);
        changeButton.setMinSize(30, 30);
        
        // Erstelle Zeit-Label
        timeLabel = new Label();
        timeLabel.setFont(Font.font(9));
        timeLabel.setStyle("-fx-text-fill: #666666;");
        
        // Layout
        VBox buttonContainer = new VBox(changeButton, timeLabel);
        buttonContainer.setAlignment(Pos.CENTER);
        container.getChildren().add(buttonContainer);
        
        // Click-Handler für Detail-Popup
        changeButton.setOnAction(event -> {
            CurrencyPairTableRow item = getTableRow().getItem();
            if (item != null) {
                openSignalChangeHistory(item.getCurrencyPair());
            }
        });
        
        // Styling
        this.setAlignment(Pos.CENTER);
    }
    
    @Override
    protected void updateItem(CurrencyPairTableRow item, boolean empty) {
        super.updateItem(item, empty);
        
        if (empty || item == null) {
            setGraphic(null);
            return;
        }
        
        try {
            // Hole aktuellsten Signalwechsel für dieses Währungspaar
            SignalChangeEvent mostRecentChange = historyManager.getMostRecentChangeForPair(item.getCurrencyPair());
            
            if (mostRecentChange == null) {
                // Keine Signalwechsel vorhanden
                showNoChangesState();
            } else {
                // Zeige Signalwechsel-Information
                showChangeState(mostRecentChange);
            }
            
            setGraphic(container);
            
        } catch (Exception e) {
            LOGGER.warning("Fehler beim Aktualisieren der Signalwechsel-Zelle: " + e.getMessage());
            showErrorState();
            setGraphic(container);
        }
    }
    
    /**
     * Zeigt den Zustand ohne Signalwechsel
     */
    private void showNoChangesState() {
        changeButton.setText("⚪");
        changeButton.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-padding: 2; -fx-text-fill: #cccccc;");
        timeLabel.setText("Kein Wechsel");
        
        Tooltip tooltip = new Tooltip("Keine Signalwechsel erkannt\nKlicken für Details");
        changeButton.setTooltip(tooltip);
    }
    
    /**
     * Zeigt den Zustand mit Signalwechsel
     */
    private void showChangeState(SignalChangeEvent change) {
        // Icon basierend auf Aktualität und Wichtigkeit
        String icon = change.getCombinedIcon();
        changeButton.setText(icon);
        
        // Styling basierend auf Aktualität
        String buttonStyle = "-fx-background-color: transparent; -fx-border-color: transparent; -fx-padding: 2;";
        SignalChangeEvent.SignalChangeActuality actuality = change.getActuality();
        
        switch (actuality) {
            case VERY_RECENT:
                buttonStyle += " -fx-text-fill: #d32f2f;"; // Kräftiges Rot
                break;
            case RECENT:
                buttonStyle += " -fx-text-fill: #f57c00;"; // Orange
                break;
            case THIS_WEEK:
                buttonStyle += " -fx-text-fill: #388e3c;"; // Grün
                break;
            default:
                buttonStyle += " -fx-text-fill: #757575;"; // Grau
                break;
        }
        
        changeButton.setStyle(buttonStyle);
        
        // Zeit-Label
        String timeText = formatChangeTime(change);
        timeLabel.setText(timeText);
        
        // Tooltip mit Details
        String tooltipText = createDetailedTooltip(change);
        Tooltip tooltip = new Tooltip(tooltipText);
        tooltip.setShowDelay(javafx.util.Duration.millis(300));
        changeButton.setTooltip(tooltip);
        
        // Hover-Effekte hinzufügen
        addHoverEffects(change);
    }
    
    /**
     * Zeigt den Fehlerzustand
     */
    private void showErrorState() {
        changeButton.setText("❌");
        changeButton.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-padding: 2; -fx-text-fill: #d32f2f;");
        timeLabel.setText("Fehler");
        
        Tooltip tooltip = new Tooltip("Fehler beim Laden der Signalwechsel");
        changeButton.setTooltip(tooltip);
    }
    
    /**
     * Formatiert die Zeit des Signalwechsels für Anzeige
     */
    private String formatChangeTime(SignalChangeEvent change) {
        SignalChangeEvent.SignalChangeActuality actuality = change.getActuality();
        
        switch (actuality) {
            case VERY_RECENT:
                return "Heute " + change.getChangeTime().format(TIME_FORMATTER);
            case RECENT:
                return "Heute " + change.getChangeTime().format(TIME_FORMATTER);
            case THIS_WEEK:
                return getGermanDayOfWeek(change) + " " + change.getChangeTime().format(TIME_FORMATTER);
            default:
                return change.getChangeTime().format(DATE_FORMATTER);
        }
    }
    
    /**
     * Erstellt einen detaillierten Tooltip-Text
     */
    private String createDetailedTooltip(SignalChangeEvent change) {
        StringBuilder tooltip = new StringBuilder();
        
        tooltip.append("🔄 SIGNALWECHSEL ERKANNT\n");
        tooltip.append("═══════════════════════════\n\n");
        
        tooltip.append("Währungspaar: ").append(change.getCurrencyPair()).append("\n");
        tooltip.append("Wechsel: ").append(change.getDetailedDescription()).append("\n");
        tooltip.append("Zeit: ").append(change.getChangeTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))).append("\n");
        tooltip.append("Wichtigkeit: ").append(change.getImportance().getIcon()).append(" ").append(change.getImportance().getDescription()).append("\n");
        tooltip.append("Aktualität: ").append(change.getActuality().getIcon()).append(" ").append(change.getActuality().getDescription()).append("\n\n");
        
        if (change.isDirectReversal()) {
            tooltip.append("⚠️ DIREKTE UMKEHRUNG - Besonders wichtig!\n\n");
        }
        
        tooltip.append("📊 Klicken für vollständige Historie");
        
        return tooltip.toString();
    }
    
    /**
     * Fügt Hover-Effekte hinzu
     */
    private void addHoverEffects(SignalChangeEvent change) {
        changeButton.setOnMouseEntered(event -> {
            // Vergrößere Button leicht beim Hover
            changeButton.setScaleX(1.2);
            changeButton.setScaleY(1.2);
            
            // Ändere Cursor
            changeButton.getScene().setCursor(javafx.scene.Cursor.HAND);
        });
        
        changeButton.setOnMouseExited(event -> {
            // Zurück zur normalen Größe
            changeButton.setScaleX(1.0);
            changeButton.setScaleY(1.0);
            
            // Cursor zurücksetzen
            changeButton.getScene().setCursor(javafx.scene.Cursor.DEFAULT);
        });
    }
    
    /**
     * Öffnet das Signalwechsel-Historie-Fenster
     */
    private void openSignalChangeHistory(String currencyPair) {
        try {
            LOGGER.info("Öffne Signalwechsel-Historie für: " + currencyPair);
            
            SignalChangeHistoryWindow historyWindow = new SignalChangeHistoryWindow(
                parentStage, 
                currencyPair, 
                historyManager
            );
            
            historyWindow.show();
            
        } catch (Exception e) {
            LOGGER.severe("Fehler beim Öffnen der Signalwechsel-Historie: " + e.getMessage());
            
            // Zeige Fehlermeldung
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("Fehler");
            alert.setHeaderText("Signalwechsel-Historie konnte nicht geöffnet werden");
            alert.setContentText("Fehler: " + e.getMessage());
            alert.showAndWait();
        }
    }
    
    /**
     * Hilfsmethode für deutsche Wochentage
     */
    private String getGermanDayOfWeek(SignalChangeEvent change) {
        switch (change.getChangeTime().getDayOfWeek()) {
            case MONDAY: return "Mo";
            case TUESDAY: return "Di";
            case WEDNESDAY: return "Mi";
            case THURSDAY: return "Do";
            case FRIDAY: return "Fr";
            case SATURDAY: return "Sa";
            case SUNDAY: return "So";
            default: return "??";
        }
    }
    
    /**
     * Aktualisiert den History-Manager (für Runtime-Updates)
     */
    public void updateHistoryManager(SignalChangeHistoryManager newHistoryManager) {
        this.historyManager = newHistoryManager;
    }
    
    /**
     * Erzwingt eine Aktualisierung der Zelle
     */
    public void forceRefresh() {
        CurrencyPairTableRow item = getTableRow() != null ? getTableRow().getItem() : null;
        if (item != null) {
            updateItem(item, false);
        }
    }
}