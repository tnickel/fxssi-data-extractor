package com.fxsssi.extractor.gui;

import com.fxsssi.extractor.gui.MainWindowController.CurrencyPairTableRow;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

/**
 * Custom TableCell für die Anzeige von horizontalen Ratio-Balken
 * Zeigt Buy-Percentage (blau) und Sell-Percentage (rot) als horizontale Balken an
 * Verbesserte Behandlung von extremen Werten und sehr schmalen Balken
 * 
 * @author Generated for FXSSI Data Extraction GUI
 * @version 1.1 (verbesserte Behandlung extremer Werte)
 */
public class RatioBarTableCell extends TableCell<CurrencyPairTableRow, CurrencyPairTableRow> {
    
    private final HBox ratioContainer;
    private final Region buyBar;
    private final Region sellBar;
    private final Label buyLabel;
    private final Label sellLabel;
    private final StackPane buyBarContainer;
    private final StackPane sellBarContainer;
    
    private static final double BAR_HEIGHT = 25.0;
    private static final double CONTAINER_WIDTH = 350.0;
    private static final double MIN_VISIBLE_WIDTH = 8.0; // Minimum sichtbare Breite für sehr kleine Balken
    private static final String BUY_BAR_STYLE = "-fx-background-color: #5B9BD5; -fx-background-radius: 3 0 0 3;";
    private static final String SELL_BAR_STYLE = "-fx-background-color: #E74C3C; -fx-background-radius: 0 3 3 0;";
    private static final String LABEL_STYLE = "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px;";
    
    public RatioBarTableCell() {
        // Erstelle Container für die Balken
        ratioContainer = new HBox();
        ratioContainer.setAlignment(Pos.CENTER_LEFT);
        ratioContainer.setPrefWidth(CONTAINER_WIDTH);
        ratioContainer.setMaxWidth(CONTAINER_WIDTH);
        ratioContainer.setMinWidth(CONTAINER_WIDTH);
        
        // Erstelle Buy-Bar Container
        buyBarContainer = new StackPane();
        buyBar = new Region();
        buyBar.setPrefHeight(BAR_HEIGHT);
        buyBar.setMaxHeight(BAR_HEIGHT);
        buyBar.setMinHeight(BAR_HEIGHT);
        buyBar.setStyle(BUY_BAR_STYLE);
        
        buyLabel = new Label();
        buyLabel.setStyle(LABEL_STYLE);
        buyLabel.setAlignment(Pos.CENTER);
        
        buyBarContainer.getChildren().addAll(buyBar, buyLabel);
        buyBarContainer.setAlignment(Pos.CENTER_LEFT);
        
        // Erstelle Sell-Bar Container
        sellBarContainer = new StackPane();
        sellBar = new Region();
        sellBar.setPrefHeight(BAR_HEIGHT);
        sellBar.setMaxHeight(BAR_HEIGHT);
        sellBar.setMinHeight(BAR_HEIGHT);
        sellBar.setStyle(SELL_BAR_STYLE);
        
        sellLabel = new Label();
        sellLabel.setStyle(LABEL_STYLE);
        sellLabel.setAlignment(Pos.CENTER);
        
        sellBarContainer.getChildren().addAll(sellBar, sellLabel);
        sellBarContainer.setAlignment(Pos.CENTER_RIGHT);
        
        // Füge Container zusammen
        ratioContainer.getChildren().addAll(buyBarContainer, sellBarContainer);
        ratioContainer.setSpacing(0);
        
        // Styling für die gesamte Zelle
        this.setPadding(new Insets(2, 5, 2, 5));
        this.setAlignment(Pos.CENTER);
        
        // Füge Hover-Effekte hinzu
        addHoverEffects();
    }
    
    @Override
    protected void updateItem(CurrencyPairTableRow item, boolean empty) {
        super.updateItem(item, empty);
        
        if (empty || item == null) {
            setGraphic(null);
            return;
        }
        
        // Hole Percentage-Werte
        double buyPercentage = item.getBuyPercentage();
        double sellPercentage = item.getSellPercentage();
        
        // Stelle sicher, dass die Werte valid sind
        if (buyPercentage < 0) buyPercentage = 0;
        if (sellPercentage < 0) sellPercentage = 0;
        
        // Normalisiere die Werte falls sie nicht 100% ergeben
        double total = buyPercentage + sellPercentage;
        if (total > 0) {
            buyPercentage = (buyPercentage / total) * 100.0;
            sellPercentage = (sellPercentage / total) * 100.0;
        }
        
        // Berechne Balkenbreiten basierend auf Container-Breite
        double buyWidth = (buyPercentage / 100.0) * CONTAINER_WIDTH;
        double sellWidth = (sellPercentage / 100.0) * CONTAINER_WIDTH;
        
        // Behandlung für sehr extreme Werte (sehr schmale Balken)
        boolean buyVerySmall = buyWidth < MIN_VISIBLE_WIDTH;
        boolean sellVerySmall = sellWidth < MIN_VISIBLE_WIDTH;
        
        // Stelle sicher, dass sehr kleine Balken mindestens sichtbar sind
        if (buyVerySmall && buyPercentage > 0) {
            buyWidth = MIN_VISIBLE_WIDTH;
            sellWidth = CONTAINER_WIDTH - MIN_VISIBLE_WIDTH;
        } else if (sellVerySmall && sellPercentage > 0) {
            sellWidth = MIN_VISIBLE_WIDTH;
            buyWidth = CONTAINER_WIDTH - MIN_VISIBLE_WIDTH;
        }
        
        // Setze Balkenbreiten
        buyBar.setPrefWidth(buyWidth);
        buyBar.setMaxWidth(buyWidth);
        buyBar.setMinWidth(buyWidth);
        
        sellBar.setPrefWidth(sellWidth);
        sellBar.setMaxWidth(sellWidth);
        sellBar.setMinWidth(sellWidth);
        
        // Setze Container-Breiten
        buyBarContainer.setPrefWidth(buyWidth);
        buyBarContainer.setMaxWidth(buyWidth);
        buyBarContainer.setMinWidth(buyWidth);
        
        sellBarContainer.setPrefWidth(sellWidth);
        sellBarContainer.setMaxWidth(sellWidth);
        sellBarContainer.setMinWidth(sellWidth);
        
        // Setze Label-Texte
        buyLabel.setText(String.format("%.0f%%", item.getBuyPercentage())); // Verwende original Werte für Label
        sellLabel.setText(String.format("%.0f%%", item.getSellPercentage()));
        
        // Verstecke Labels bei sehr schmalen Balken, aber zeige sie bei extremen Werten in Tooltip
        boolean showBuyLabel = buyWidth > 35 || (!buyVerySmall && buyWidth > 20);
        boolean showSellLabel = sellWidth > 35 || (!sellVerySmall && sellWidth > 20);
        
        buyLabel.setVisible(showBuyLabel);
        sellLabel.setVisible(showSellLabel);
        
        // Spezielle Styling-Anpassungen basierend auf den Werten
        updateBarStyling(item.getBuyPercentage(), item.getSellPercentage(), buyVerySmall, sellVerySmall);
        
        // Erweiterte Tooltip-Informationen für extreme Werte
        updateTooltip(item);
        
        setGraphic(ratioContainer);
    }
    
    /**
     * Aktualisiert das Styling der Balken basierend auf den Werten und extremen Fällen
     */
    private void updateBarStyling(double buyPercentage, double sellPercentage, boolean buyVerySmall, boolean sellVerySmall) {
        // Basis-Styles
        String buyStyle = BUY_BAR_STYLE;
        String sellStyle = SELL_BAR_STYLE;
        
        // Spezielle Radius-Behandlung für extreme Werte
        if (buyVerySmall || buyPercentage < 3) {
            // Buy-Balken sehr klein, Sell-Balken bekommt kompletten Radius
            sellStyle = "-fx-background-color: #E74C3C; -fx-background-radius: 3;";
            buyStyle = "-fx-background-color: #5B9BD5; -fx-background-radius: 3;"; // Kompletter Radius für sichtbarkeit
        } else if (sellVerySmall || sellPercentage < 3) {
            // Sell-Balken sehr klein, Buy-Balken bekommt kompletten Radius
            buyStyle = "-fx-background-color: #5B9BD5; -fx-background-radius: 3;";
            sellStyle = "-fx-background-color: #E74C3C; -fx-background-radius: 3;"; // Kompletter Radius für sichtbarkeit
        }
        
        // Farbintensität basierend auf extremen Werten
        if (buyPercentage > 80) {
            buyStyle = buyStyle.replace("#5B9BD5", "#1F4E79"); // Sehr dunkles Blau für extreme Werte
        } else if (buyPercentage > 70) {
            buyStyle = buyStyle.replace("#5B9BD5", "#2E86AB"); // Dunkleres Blau
        }
        
        if (sellPercentage > 80) {
            sellStyle = sellStyle.replace("#E74C3C", "#922B21"); // Sehr dunkles Rot für extreme Werte
        } else if (sellPercentage > 70) {
            sellStyle = sellStyle.replace("#E74C3C", "#C0392B"); // Dunkleres Rot
        }
        
        // Spezielle Hervorhebung für sehr kleine aber sichtbare Balken
        if (buyVerySmall && buyPercentage > 0) {
            buyStyle += " -fx-border-color: #34495E; -fx-border-width: 1;"; // Border für bessere Sichtbarkeit
        }
        if (sellVerySmall && sellPercentage > 0) {
            sellStyle += " -fx-border-color: #34495E; -fx-border-width: 1;"; // Border für bessere Sichtbarkeit
        }
        
        buyBar.setStyle(buyStyle);
        sellBar.setStyle(sellStyle);
    }
    
    /**
     * Aktualisiert Tooltip mit detaillierten Informationen
     */
    private void updateTooltip(CurrencyPairTableRow item) {
        String tooltipText = String.format(
            "%s\nBuy: %.1f%% | Sell: %.1f%%\nSignal: %s\n\n" +
            "Linker Balken (Blau) = Buy/Long Positionen\n" +
            "Rechter Balken (Rot) = Sell/Short Positionen",
            item.getCurrencyPair(),
            item.getBuyPercentage(),
            item.getSellPercentage(),
            item.getTradingSignal().getDescription()
        );
        
        // Spezielle Hinweise für extreme Werte
        if (item.getBuyPercentage() > 80) {
            tooltipText += "\n⚠️ Sehr hohe Buy-Dominanz (>80%)";
        } else if (item.getSellPercentage() > 80) {
            tooltipText += "\n⚠️ Sehr hohe Sell-Dominanz (>80%)";
        }
        
        javafx.scene.control.Tooltip tooltip = new javafx.scene.control.Tooltip(tooltipText);
        tooltip.setShowDelay(javafx.util.Duration.millis(500));
        this.setTooltip(tooltip);
    }
    
    /**
     * Füge Hover-Effekte hinzu für bessere Interaktivität
     */
    private void addHoverEffects() {
        ratioContainer.setOnMouseEntered(event -> {
            // Sanfter Schatten-Effekt beim Hover
            buyBar.setStyle(buyBar.getStyle() + " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 3, 0, 0, 1);");
            sellBar.setStyle(sellBar.getStyle() + " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 3, 0, 0, 1);");
            
            // Leichte Vergrößerung
            ratioContainer.setScaleY(1.05);
        });
        
        ratioContainer.setOnMouseExited(event -> {
            // Entferne Effekte
            String buyStyle = buyBar.getStyle().replaceAll(" -fx-effect: dropshadow\\([^;]*\\);?", "");
            String sellStyle = sellBar.getStyle().replaceAll(" -fx-effect: dropshadow\\([^;]*\\);?", "");
            buyBar.setStyle(buyStyle);
            sellBar.setStyle(sellStyle);
            
            // Zurück zur normalen Größe
            ratioContainer.setScaleY(1.0);
        });
    }
    
    /**
     * Berechnet die optimale Textfarbe basierend auf der Hintergrundfarbe
     */
    private String getOptimalTextColor(double percentage, boolean isVerySmall) {
        if (isVerySmall) {
            // Bei sehr kleinen Balken ist weiße Schrift immer am besten sichtbar
            return "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 10px;";
        } else if (percentage > 70) {
            // Bei sehr dunklen Farben verwende weiße Schrift
            return "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px;";
        } else {
            // Standard weiße Schrift
            return "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px;";
        }
    }
}