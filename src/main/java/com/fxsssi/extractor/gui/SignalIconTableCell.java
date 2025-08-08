package com.fxsssi.extractor.gui;

import com.fxssi.extractor.model.CurrencyPairData;
import com.fxsssi.extractor.gui.MainWindowController.CurrencyPairTableRow;

import javafx.geometry.Pos;
import javafx.scene.control.TableCell;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;

/**
 * Custom TableCell für die Anzeige von Trading-Signal-Icons
 * Zeigt BUY (grün, nach oben), SELL (rot, nach unten), NEUTRAL (grau, seitlich) Icons an
 * 
 * @author Generated for FXSSI Data Extraction GUI
 * @version 1.0
 */
public class SignalIconTableCell extends TableCell<CurrencyPairTableRow, CurrencyPairTableRow> {
    
    private final StackPane iconContainer;
    
    private static final double ICON_SIZE = 20.0;
    private static final double CIRCLE_RADIUS = 12.0;
    
    // Signal-Farben
    private static final Color BUY_COLOR = Color.web("#27AE60");      // Grün
    private static final Color SELL_COLOR = Color.web("#E74C3C");     // Rot  
    private static final Color NEUTRAL_COLOR = Color.web("#95A5A6");  // Grau
    private static final Color UNKNOWN_COLOR = Color.web("#BDC3C7");  // Hellgrau
    
    public SignalIconTableCell() {
        iconContainer = new StackPane();
        iconContainer.setAlignment(Pos.CENTER);
        iconContainer.setPrefSize(ICON_SIZE * 2, ICON_SIZE * 2);
        this.setAlignment(Pos.CENTER);
    }
    
    @Override
    protected void updateItem(CurrencyPairTableRow item, boolean empty) {
        super.updateItem(item, empty);
        
        if (empty || item == null) {
            setGraphic(null);
            return;
        }
        
        CurrencyPairData.TradingSignal signal = item.getTradingSignal();
        iconContainer.getChildren().clear();
        
        // Erstelle Signal-Icon basierend auf Signal-Typ
        switch (signal) {
            case BUY:
                createBuyIcon();
                break;
            case SELL:
                createSellIcon();
                break;
            case NEUTRAL:
                createNeutralIcon();
                break;
            case UNKNOWN:
            default:
                createUnknownIcon();
                break;
        }
        
        setGraphic(iconContainer);
    }
    
    /**
     * Erstellt ein BUY-Icon (grüner Kreis mit Pfeil nach oben)
     */
    private void createBuyIcon() {
        // Erstelle Kreis-Hintergrund
        Circle background = new Circle(CIRCLE_RADIUS);
        background.setFill(BUY_COLOR);
        background.setStroke(BUY_COLOR.darker());
        background.setStrokeWidth(1.0);
        
        // Erstelle Pfeil nach oben
        Polygon arrow = createUpArrow();
        arrow.setFill(Color.WHITE);
        
        iconContainer.getChildren().addAll(background, arrow);
        
        // Tooltip für Accessibility
        setTooltip(new javafx.scene.control.Tooltip("BUY Signal"));
    }
    
    /**
     * Erstellt ein SELL-Icon (roter Kreis mit Pfeil nach unten)
     */
    private void createSellIcon() {
        // Erstelle Kreis-Hintergrund
        Circle background = new Circle(CIRCLE_RADIUS);
        background.setFill(SELL_COLOR);
        background.setStroke(SELL_COLOR.darker());
        background.setStrokeWidth(1.0);
        
        // Erstelle Pfeil nach unten
        Polygon arrow = createDownArrow();
        arrow.setFill(Color.WHITE);
        
        iconContainer.getChildren().addAll(background, arrow);
        
        // Tooltip für Accessibility
        setTooltip(new javafx.scene.control.Tooltip("SELL Signal"));
    }
    
    /**
     * Erstellt ein NEUTRAL-Icon (grauer Kreis mit horizontalem Pfeil)
     */
    private void createNeutralIcon() {
        // Erstelle Kreis-Hintergrund
        Circle background = new Circle(CIRCLE_RADIUS);
        background.setFill(NEUTRAL_COLOR);
        background.setStroke(NEUTRAL_COLOR.darker());
        background.setStrokeWidth(1.0);
        
        // Erstelle horizontalen Pfeil
        Polygon arrow = createRightArrow();
        arrow.setFill(Color.WHITE);
        
        iconContainer.getChildren().addAll(background, arrow);
        
        // Tooltip für Accessibility
        setTooltip(new javafx.scene.control.Tooltip("NEUTRAL Signal"));
    }
    
    /**
     * Erstellt ein UNKNOWN-Icon (hellgrauer Kreis mit Fragezeichen)
     */
    private void createUnknownIcon() {
        // Erstelle Kreis-Hintergrund
        Circle background = new Circle(CIRCLE_RADIUS);
        background.setFill(UNKNOWN_COLOR);
        background.setStroke(UNKNOWN_COLOR.darker());
        background.setStrokeWidth(1.0);
        
        // Erstelle Fragezeichen (vereinfacht als Text)
        javafx.scene.text.Text questionMark = new javafx.scene.text.Text("?");
        questionMark.setFill(Color.WHITE);
        questionMark.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        
        iconContainer.getChildren().addAll(background, questionMark);
        
        // Tooltip für Accessibility
        setTooltip(new javafx.scene.control.Tooltip("UNKNOWN Signal"));
    }
    
    /**
     * Erstellt einen Pfeil nach oben
     */
    private Polygon createUpArrow() {
        Polygon arrow = new Polygon();
        arrow.getPoints().addAll(new Double[]{
            0.0, -6.0,    // Spitze
            -4.0, 2.0,    // Links unten
            -2.0, 2.0,    // Links innen
            -2.0, 6.0,    // Links unten Schaft
            2.0, 6.0,     // Rechts unten Schaft
            2.0, 2.0,     // Rechts innen
            4.0, 2.0      // Rechts unten
        });
        return arrow;
    }
    
    /**
     * Erstellt einen Pfeil nach unten
     */
    private Polygon createDownArrow() {
        Polygon arrow = new Polygon();
        arrow.getPoints().addAll(new Double[]{
            0.0, 6.0,     // Spitze
            -4.0, -2.0,   // Links oben
            -2.0, -2.0,   // Links innen
            -2.0, -6.0,   // Links oben Schaft
            2.0, -6.0,    // Rechts oben Schaft
            2.0, -2.0,    // Rechts innen
            4.0, -2.0     // Rechts oben
        });
        return arrow;
    }
    
    /**
     * Erstellt einen Pfeil nach rechts
     */
    private Polygon createRightArrow() {
        Polygon arrow = new Polygon();
        arrow.getPoints().addAll(new Double[]{
            6.0, 0.0,     // Spitze
            -2.0, -4.0,   // Oben links
            -2.0, -2.0,   // Oben innen
            -6.0, -2.0,   // Links oben Schaft
            -6.0, 2.0,    // Links unten Schaft
            -2.0, 2.0,    // Unten innen
            -2.0, 4.0     // Unten links
        });
        return arrow;
    }
    
    /**
     * Fügt Hover-Effekte für bessere Interaktivität hinzu
     */
    private void addHoverEffects() {
        iconContainer.setOnMouseEntered(event -> {
            iconContainer.setScaleX(1.1);
            iconContainer.setScaleY(1.1);
        });
        
        iconContainer.setOnMouseExited(event -> {
            iconContainer.setScaleX(1.0);
            iconContainer.setScaleY(1.0);
        });
    }
}