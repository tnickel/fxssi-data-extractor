package com.fxsssi.extractor.gui;

import com.fxssi.extractor.config.ExportConfig;
import com.fxssi.extractor.storage.CurrencyPairDataManager;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Path;

public class ExportSignalsWindow {

    private final CurrencyPairDataManager dataManager;
    private final ExportConfig exportConfig;
    private Stage stage;
    private Label statusLabel;
    private ProgressBar progressBar;
    private TextField directoryField;
    private Button chooseButton;
    private Button startButton;
    private Button closeButton;
    private Path selectedDirectory;

    public ExportSignalsWindow(Stage parentStage, CurrencyPairDataManager dataManager, ExportConfig exportConfig) {
        this.dataManager = dataManager;
        this.exportConfig = exportConfig;
        
        // Initialize with saved config
        String lastDirStr = exportConfig.getLastExportDirectory();
        File lastDir = new File(lastDirStr);
        if (lastDir.exists() && lastDir.isDirectory()) {
            this.selectedDirectory = lastDir.toPath();
        }

        createWindow(parentStage);
    }

    private void createWindow(Stage parentStage) {
        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(parentStage);
        stage.setTitle("Signale exportieren");

        VBox layout = new VBox(15);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(20));
        layout.setPrefWidth(500);

        Label titleLabel = new Label("CSV Signale exportieren");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 16));

        Label infoLabel = new Label("Wähle ein Zielverzeichnis aus. Alle aktuellen Signaldaten-Dateien\nwerden dorthin exportiert und ggf. überschrieben.");
        infoLabel.setWrapText(true);
        infoLabel.setAlignment(Pos.CENTER);

        // Directory Selection
        HBox dirBox = new HBox(10);
        dirBox.setAlignment(Pos.CENTER);
        
        directoryField = new TextField();
        directoryField.setEditable(false);
        directoryField.setPrefWidth(300);
        if (selectedDirectory != null) {
            directoryField.setText(selectedDirectory.toAbsolutePath().toString());
        }

        chooseButton = new Button("Verzeichnis wählen...");
        chooseButton.setOnAction(e -> chooseDirectory());
        
        dirBox.getChildren().addAll(directoryField, chooseButton);

        statusLabel = new Label("Wähle ein Verzeichnis und drücke 'Exportieren'.");
        
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(450);

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);

        startButton = new Button("Exportieren");
        startButton.setStyle("-fx-base: #4CAF50; -fx-text-fill: white; font-weight: bold;");
        startButton.setOnAction(e -> startExport());
        startButton.setDisable(selectedDirectory == null); // Disable if no dir chosen

        closeButton = new Button("Schließen");
        closeButton.setOnAction(e -> stage.close());
        
        buttonBox.getChildren().addAll(startButton, closeButton);

        layout.getChildren().addAll(titleLabel, infoLabel, dirBox, statusLabel, progressBar, buttonBox);

        Scene scene = new Scene(layout);
        stage.setScene(scene);
        stage.setResizable(false);
    }

    private void chooseDirectory() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Zielverzeichnis für den Export auswählen");
        
        if (selectedDirectory != null && selectedDirectory.toFile().exists()) {
            directoryChooser.setInitialDirectory(selectedDirectory.toFile());
        }

        File dir = directoryChooser.showDialog(stage);
        if (dir != null) {
            selectedDirectory = dir.toPath();
            directoryField.setText(selectedDirectory.toAbsolutePath().toString());
            exportConfig.setLastExportDirectory(selectedDirectory.toAbsolutePath().toString());
            startButton.setDisable(false);
            statusLabel.setText("Bereit zum Export.");
        }
    }

    public void show() {
        stage.showAndWait();
    }

    private void startExport() {
        if (selectedDirectory == null) {
            return;
        }

        startButton.setDisable(true);
        chooseButton.setDisable(true);
        closeButton.setDisable(true);
        statusLabel.setText("Starte Export...");
        progressBar.setProgress(-1);

        Task<String> exportTask = new Task<String>() {
            @Override
            protected String call() throws Exception {
                return dataManager.exportAllSignals(
                    selectedDirectory,
                    (current, total) -> {
                        updateProgress(current, total);
                    },
                    (message) -> {
                        updateMessage(message);
                    }
                );
            }
        };

        exportTask.messageProperty().addListener((obs, oldMsg, newMsg) -> {
            statusLabel.setText(newMsg);
        });

        exportTask.progressProperty().addListener((obs, oldProg, newProg) -> {
            progressBar.setProgress(newProg.doubleValue());
        });

        exportTask.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                String report = exportTask.getValue();
                statusLabel.setText("✅ Export erfolgreich abgeschlossen!");
                progressBar.setProgress(1.0);
                
                closeButton.setDisable(false);
                closeButton.setText("Fertig");
                chooseButton.setDisable(false);
                
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Export-Bericht");
                alert.setHeaderText("Zusammenfassung des Exports");
                
                TextArea textArea = new TextArea(report);
                textArea.setEditable(false);
                textArea.setWrapText(false);
                textArea.setFont(Font.font("Monospaced", 12));
                
                alert.getDialogPane().setContent(textArea);
                alert.getDialogPane().setPrefWidth(400);
                alert.showAndWait();
            });
        });

        exportTask.setOnFailed(e -> {
            Platform.runLater(() -> {
                statusLabel.setText("❌ Fehler beim Export aufgetreten.");
                progressBar.setStyle("-fx-accent: red;");
                closeButton.setDisable(false);
                chooseButton.setDisable(false);
                Throwable exception = exportTask.getException();
                if(exception != null) {
                    exception.printStackTrace();
                }
            });
        });

        Thread thread = new Thread(exportTask);
        thread.setDaemon(true);
        thread.start();
    }
}
