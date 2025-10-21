package com.fxssi.extractor.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility-Klasse zum Laden und Bereitstellen der Anwendungsversion
 * Die Version wird aus der version.properties-Datei gelesen, die zur Build-Zeit
 * von Maven mit den Werten aus der pom.xml gefüllt wird.
 *
 * @author FXSSI Data Extractor
 * @version 1.0
 */
public class AppVersion {

    private static final Logger LOGGER = Logger.getLogger(AppVersion.class.getName());

    private static final String VERSION_PROPERTIES_FILE = "/version.properties";
    private static final String FALLBACK_VERSION = "2.0-SNAPSHOT";
    private static final String FALLBACK_NAME = "FXSSI Data Extractor";

    private static String version;
    private static String appName;
    private static String artifactId;
    private static boolean initialized = false;

    /**
     * Lädt die Versionsinformationen aus der Properties-Datei
     */
    private static synchronized void initialize() {
        if (initialized) {
            return;
        }

        Properties props = new Properties();

        try (InputStream is = AppVersion.class.getResourceAsStream(VERSION_PROPERTIES_FILE)) {
            if (is != null) {
                props.load(is);

                version = props.getProperty("app.version", FALLBACK_VERSION);
                appName = props.getProperty("app.name", FALLBACK_NAME);
                artifactId = props.getProperty("app.artifactId", "fxssi-data-extractor");

                LOGGER.info("Version erfolgreich geladen: " + version);

            } else {
                LOGGER.warning("version.properties nicht gefunden - verwende Fallback-Version");
                version = FALLBACK_VERSION;
                appName = FALLBACK_NAME;
                artifactId = "fxssi-data-extractor";
            }

        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Fehler beim Laden der version.properties - verwende Fallback-Version", e);
            version = FALLBACK_VERSION;
            appName = FALLBACK_NAME;
            artifactId = "fxssi-data-extractor";
        }

        initialized = true;
    }

    /**
     * Gibt die Versionsnummer der Anwendung zurück
     * @return Versionsnummer (z.B. "2.0-SNAPSHOT")
     */
    public static String getVersion() {
        if (!initialized) {
            initialize();
        }
        return version;
    }

    /**
     * Gibt den Namen der Anwendung zurück
     * @return Anwendungsname
     */
    public static String getAppName() {
        if (!initialized) {
            initialize();
        }
        return appName;
    }

    /**
     * Gibt die Artifact-ID zurück
     * @return Artifact-ID
     */
    public static String getArtifactId() {
        if (!initialized) {
            initialize();
        }
        return artifactId;
    }

    /**
     * Gibt die vollständige Versionsinfo zurück (Name + Version)
     * @return Vollständige Versionsinfo (z.B. "FXSSI Data Extractor v2.0-SNAPSHOT")
     */
    public static String getFullVersionInfo() {
        if (!initialized) {
            initialize();
        }
        return appName + " v" + version;
    }

    /**
     * Gibt eine kurze Versionsinfo zurück (nur Version)
     * @return Kurze Versionsinfo (z.B. "v2.0-SNAPSHOT")
     */
    public static String getShortVersionInfo() {
        if (!initialized) {
            initialize();
        }
        return "v" + version;
    }
}
