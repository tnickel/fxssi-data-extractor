package com.fxssi.extractor.gui;

import com.fxssi.extractor.model.SignalChangeEvent;
import com.fxssi.extractor.storage.SignalChangeHistoryManager;
import com.fxsssi.extractor.gui.MainWindowController.CurrencyPairTableRow;

import javafx.animation.Animation;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
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
 * @version 2.0 - Verbesserte visuelle Hierarchie
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
        
        // Normale Größe wiederherstellen
        changeButton.setPrefSize(30, 30);
        changeButton.setMaxSize(30, 30);
        changeButton.setMinSize(30, 30);
        
        Tooltip tooltip = new Tooltip("Keine Signalwechsel erkannt\nKlicken für Details");
        changeButton.setTooltip(tooltip);
    }
    
    /**
     * Zeigt den Zustand mit Signalwechsel - VERBESSERTE VERSION mit deutlicher visueller Hierarchie
     */
    private void showChangeState(SignalChangeEvent change) {
        // Icon basierend auf Aktualität und Wichtigkeit
        String icon = change.getCombinedIcon();
        changeButton.setText(icon);
        
        // Styling basierend auf Aktualität - DEUTLICH VERBESSERT
        SignalChangeEvent.SignalChangeActuality actuality = change.getActuality();
        
        // Basis-Style
        String buttonStyle = "-fx-border-color: transparent; -fx-padding: 5; -fx-border-radius: 8; -fx-background-radius: 8;";
        String timeLabelStyle = "";
        
        switch (actuality) {
            case VERY_RECENT: // Heute - letzte 4 Stunden
                // SEHR AUFFÄLLIG: Leuchtend rote Hintergrundfarbe mit weißer Schrift
                buttonStyle += " -fx-background-color: #ff1744; -fx-text-fill: white; -fx-font-weight: bold;";
                timeLabelStyle = "-fx-text-fill: #ff1744; -fx-font-weight: bold; -fx-font-size: 10px;";
                
                // Button größer machen für maximale Aufmerksamkeit
                changeButton.setPrefSize(35, 35);
                changeButton.setMaxSize(35, 35);
                changeButton.setMinSize(35, 35);
                
                // Pulsierender Effekt für heute
                addPulsingEffect();
                break;
                
            case RECENT: // Heute - älter als 4 Stunden
                // DEUTLICH: Orangener Hintergrund mit weißer Schrift
                buttonStyle += " -fx-background-color: #ff6f00; -fx-text-fill: white; -fx-font-weight: bold;";
                timeLabelStyle = "-fx-text-fill: #ff6f00; -fx-font-weight: bold; -fx-font-size: 10px;";
                
                changeButton.setPrefSize(33, 33);
                changeButton.setMaxSize(33, 33);
                changeButton.setMinSize(33, 33);
                break;
                
            case THIS_WEEK: // Diese Woche
                // SICHTBAR: Grünlicher Hintergrund
                buttonStyle += " -fx-background-color: #4caf50; -fx-text-fill: white; -fx-font-weight: normal;";
                timeLabelStyle = "-fx-text-fill: #4caf50; -fx-font-weight: normal; -fx-font-size: 9px;";
                
                changeButton.setPrefSize(30, 30);
                changeButton.setMaxSize(30, 30);
                changeButton.setMinSize(30, 30);
                break;
                
            default: // Älter
                // DEZENT: Grauer Hintergrund
                buttonStyle += " -fx-background-color: #9e9e9e; -fx-text-fill: white; -fx-font-weight: normal;";
                timeLabelStyle = "-fx-text-fill: #757575; -fx-font-weight: normal; -fx-font-size: 9px;";
                
                changeButton.setPrefSize(28, 28);
                changeButton.setMaxSize(28, 28);
                changeButton.setMinSize(28, 28);
                break;
        }
        
        // Zusätzliche Hervorhebung für direkte Umkehrungen
        if (change.isDirectReversal()) {
            buttonStyle += " -fx-border-color: #ffeb3b; -fx-border-width: 2;";
            // Goldener Rand für direkte Umkehrungen
        }
        
        changeButton.setStyle(buttonStyle);
        
        // Zeit-Label stylen
        timeLabel.setStyle(timeLabelStyle);
        
        // Zeit-Label Text
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
     * Fügt einen pulsierenden Effekt für sehr aktuelle Änderungen hinzu
     */
    private void addPulsingEffect() {
        // Entferne eventuelle vorherige Animationen
        changeButton.getTransforms().clear();
        
        // Erstelle pulsierenden Effekt
        ScaleTransition pulse = new ScaleTransition(
            javafx.util.Duration.millis(1000), changeButton);
        pulse.setFromX(1.0);
        pulse.setFromY(1.0);
        pulse.setToX(1.1);
        pulse.setToY(1.1);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.setAutoReverse(true);
        
        pulse.play();
        
        // Animation nach 30 Sekunden stoppen um nicht zu nervig zu sein
        PauseTransition stopPulse = new PauseTransition(
            javafx.util.Duration.seconds(30));
        stopPulse.setOnFinished(e -> pulse.stop());
        stopPulse.play();
    }
    
    /**
     * Verbesserte Hover-Effekte mit Aktualitäts-spezifischen Reaktionen
     */
    private void addHoverEffects(SignalChangeEvent change) {
        SignalChangeEvent.SignalChangeActuality actuality = change.getActuality();
        
        changeButton.setOnMouseEntered(event -> {
            // Verschiedene Hover-Effekte je nach Aktualität
            double scaleIncrease = 1.0;
            String hoverColor = "";
            
            switch (actuality) {
                case VERY_RECENT:
                    scaleIncrease = 1.3; // Stärkere Vergrößerung für heute
                    hoverColor = "#d50000"; // Noch kräftigeres Rot
                    break;
                case RECENT:
                    scaleIncrease = 1.25;
                    hoverColor = "#e65100"; // Kräftigeres Orange
                    break;
                case THIS_WEEK:
                    scaleIncrease = 1.2;
                    hoverColor = "#388e3c"; // Kräftigeres Grün
                    break;
                default:
                    scaleIncrease = 1.15;
                    hoverColor = "#616161"; // Kräftigeres Grau
                    break;
            }
            
            // Vergrößerung
            changeButton.setScaleX(scaleIncrease);
            changeButton.setScaleY(scaleIncrease);
            
            // Farbe ändern beim Hover
            String currentStyle = changeButton.getStyle();
            String hoverStyle = currentStyle.replaceAll("-fx-background-color: [^;]+", 
                                                       "-fx-background-color: " + hoverColor);
            changeButton.setStyle(hoverStyle + " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 5, 0, 0, 2);");
            
            // Cursor ändern
            changeButton.getScene().setCursor(javafx.scene.Cursor.HAND);
        });
        
        changeButton.setOnMouseExited(event -> {
            // Zurück zur normalen Größe
            changeButton.setScaleX(1.0);
            changeButton.setScaleY(1.0);
            
            // Original-Styling wiederherstellen
            showChangeState(change);
            
            // Cursor zurücksetzen
            changeButton.getScene().setCursor(javafx.scene.Cursor.DEFAULT);
        });
    }
    
    /**
     * Zeigt den Fehlerzustand
     */
    private void showErrorState() {
        changeButton.setText("❌");
        changeButton.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-padding: 2; -fx-text-fill: #d32f2f;");
        timeLabel.setText("Fehler");
        
        // Normale Größe wiederherstellen
        changeButton.setPrefSize(30, 30);
        changeButton.setMaxSize(30, 30);
        changeButton.setMinSize(30, 30);
        
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
        tooltip.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");
        
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