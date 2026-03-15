package com.fxssi.extractor.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages configuration for the Export Signals feature.
 */
public class ExportConfig {

    private static final Logger LOGGER = Logger.getLogger(ExportConfig.class.getName());
    private static final String CONFIG_DIR = "config";
    private static final String CONFIG_FILE = "export_config.properties";
    private static final String KEY_LAST_EXPORT_DIR = "lastExportDirectory";

    private final Path configPath;
    private final Properties properties;

    public ExportConfig(String dataDirectory) {
        Path basePath = Paths.get(dataDirectory != null ? dataDirectory : "data");
        this.configPath = basePath.resolve(CONFIG_DIR).resolve(CONFIG_FILE);
        this.properties = new Properties();
        loadConfig();
    }

    private void loadConfig() {
        if (Files.exists(configPath)) {
            try (InputStream is = Files.newInputStream(configPath)) {
                properties.load(is);
                LOGGER.info("Export configuration loaded from: " + configPath.toAbsolutePath());
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to load export configuration", e);
            }
        } else {
            // Default to user home if no config exists
            setLastExportDirectory(System.getProperty("user.home"));
        }
    }

    public void saveConfig() {
        try {
            if (!Files.exists(configPath.getParent())) {
                Files.createDirectories(configPath.getParent());
            }
            try (OutputStream os = Files.newOutputStream(configPath)) {
                properties.store(os, "FXSSI Data Extractor - Export Configuration");
                LOGGER.info("Export configuration saved to: " + configPath.toAbsolutePath());
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to save export configuration", e);
        }
    }

    public String getLastExportDirectory() {
        return properties.getProperty(KEY_LAST_EXPORT_DIR, System.getProperty("user.home"));
    }

    public void setLastExportDirectory(String path) {
        if (path != null && !path.trim().isEmpty()) {
            properties.setProperty(KEY_LAST_EXPORT_DIR, path);
            saveConfig();
        }
    }
}
