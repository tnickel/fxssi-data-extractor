# FXSSI Data Extractor - Project Documentation

## Overview

The **FXSSI Data Extractor** is a Java application for automated extraction of Forex sentiment data from the FXSSI.com website. The system collects Current Ratio data for various currency pairs hourly and stores them structurally in CSV files for further analysis.

### Main Features
- **Automated Data Extraction**: Hourly collection of Buy/Sell ratios from FXSSI.com
- **Web Scraping**: Robust HTML parsing with JSoup and fallback strategies
- **Data Management**: Automatic CSV file creation with daily segmentation
- **Trading Signal Generation**: Contrarian approach based on sentiment data
- **Scheduler System**: Time-controlled execution with configurable intervals
- **File Management**: Backup, cleanup, and validation functions

## Technology Stack

- **Java 11+** with java.util.logging
- **JSoup** for web scraping and HTML parsing
- **ScheduledExecutorService** for time control
- **CSV format** for data export
- **Log4j** for advanced logging features
- **JUnit 5** for unit testing

## Project Architecture

### Package Structure
```
com.fxssi.extractor/
├── model/           # Data models
├── scraper/         # Web scraping components
├── storage/         # File management
└── scheduler/       # Time control
```

### Architecture Patterns
- **Modular Architecture** with clear package separation
- **Scheduler Pattern** for time-controlled data collection
- **Facade Pattern** for scraper component
- **Safe Task Wrapper** for robust exception handling

## Class Overview

### 1. FXSSIDataExtractor (Main Class)
**Purpose**: Orchestrator and application entry point

**Functions**:
- Coordinates all components (Scraper, FileManager, Scheduler)
- Performs initial data extraction
- Starts hourly scheduler
- Handles shutdown hooks for clean termination

**Key Methods**:
- `start()`: Starts the system
- `stop()`: Stops the system properly
- `extractAndSaveData()`: Performs data extraction

### 2. CurrencyPairData (model/)
**Purpose**: Data model for currency pair sentiment data

**Properties**:
- `currencyPair`: Currency pair (e.g., "EUR/USD")
- `buyPercentage`: Buy percentage
- `sellPercentage`: Sell percentage
- `tradingSignal`: Trading signal (BUY/SELL/NEUTRAL)
- `timestamp`: Data capture timestamp

**Functions**:
- CSV import/export
- Data consistency validation
- Automatic trading signal calculation

### 3. FXSSIScraper (scraper/)
**Purpose**: Web scraper for FXSSI Current Ratio data

**Functions**:
- HTML parsing with JSoup
- Currency pair and percentage extraction
- Fallback strategies for parsing issues
- Connection testing to FXSSI website

**Key Methods**:
- `extractCurrentRatioData()`: Main data extraction method
- `testConnection()`: Connection test
- `parseCurrentRatioData()`: HTML parsing

### 4. DataFileManager (storage/)
**Purpose**: CSV file management with advanced features

**Functions**:
- Daily CSV file segmentation
- Data backup and cleanup
- Data validation
- Statistics about stored data

**Key Methods**:
- `appendDataToFile()`: Append data to today's file
- `readDataFromFile()`: Read data from file
- `cleanupOldFiles()`: Delete old files
- `validateDataFile()`: Check file integrity

### 5. HourlyScheduler (scheduler/)
**Purpose**: Time control for hourly execution

**Functions**:
- Hourly execution at the top of the hour
- SafeTaskWrapper for stability
- Daemon threads for clean shutdown
- Custom interval support for testing

**Key Methods**:
- `startScheduling()`: Start scheduler
- `stopScheduling()`: Stop scheduler
- `executeTaskManually()`: Manual execution

## Installation and Setup

### Prerequisites
- Java 11 or higher
- Maven for dependency management
- Internet connection for FXSSI.com

### Dependencies (pom.xml)
```xml
<dependency>
    <groupId>org.jsoup</groupId>
    <artifactId>jsoup</artifactId>
    <version>1.15.3</version>
</dependency>
<dependency>
    <groupId>org.apache.logging.log4j</groupId>
    <artifactId>log4j-core</artifactId>
    <version>2.20.0</version>
</dependency>
```

### Project Structure
```
fxssi-extractor/
├── src/
│   ├── main/java/
│   │   ├── FXSSIDataExtractor.java
│   │   └── com/fxssi/extractor/
│   │       ├── model/
│   │       ├── scraper/
│   │       ├── storage/
│   │       └── scheduler/
│   └── main/resources/
│       └── log4j.xml
├── data/                    # Created automatically
├── logs/                    # Created automatically
└── pom.xml
```

## Usage

### 1. Starting the Program
```bash
java -jar fxssi-extractor.jar
```

### 2. Manual Execution
The program can also be used programmatically:
```java
FXSSIDataExtractor extractor = new FXSSIDataExtractor();
extractor.start();
// ... runs automatically
extractor.stop();
```

### 3. Custom Scheduler for Testing
```java
HourlyScheduler customScheduler = HourlyScheduler.createCustomIntervalScheduler(
    () -> System.out.println("Test execution"), 
    5 // 5-minute interval
);
```

## Configuration

### Logging Configuration (log4j.xml)
- **Console Appender**: INFO level for console
- **File Appender**: DEBUG level for general logs
- **Error File Appender**: ERROR level for error logs
- **Data Extraction Appender**: Special logs for data extraction

### Directory Structure
- `data/`: CSV files with sentiment data
- `logs/`: Application log files

### CSV File Format
```
Zeitstempel;Währungspaar;Buy_Prozent;Sell_Prozent;Handelssignal
2025-08-08 14:30:00;EUR/USD;45.50;54.50;BUY
2025-08-08 14:30:00;GBP/USD;62.30;37.70;SELL
```

## Trading Signal Logic

The system uses a **Contrarian Approach**:
- **BUY Signal**: When Buy% < 40% (trade against the crowd)
- **SELL Signal**: When Buy% > 60% (trade against the crowd)
- **NEUTRAL**: When 40% ≤ Buy% ≤ 60%

## Monitoring and Diagnostics

### Log Levels
- **INFO**: Normal operation messages
- **WARNING**: Potential issues
- **ERROR**: Critical errors
- **DEBUG**: Detailed debugging information

### Status Monitoring
```java
// Check scheduler status
scheduler.getStatus();

// Get data statistics
fileManager.getDataStatistics();

// Connection test
scraper.testConnection();
```

## Maintenance and Administration

### Automatic Cleanup
```java
// Delete files older than 30 days
fileManager.cleanupOldFiles(30);
```

### Create Backup
```java
// Backup today's file
fileManager.backupTodayFile();
```

### Data Validation
```java
// Check file integrity
boolean isValid = fileManager.validateDataFile("fxssi_data_2025-08-08.csv");
```

## Error Handling

### Robust Execution
- **SafeTaskWrapper**: Prevents scheduler stop on exceptions
- **Fallback Parsing**: Alternative parsing methods for website changes
- **Timeout Handling**: 10-second timeout for web requests
- **Retry Logic**: Automatic retry for temporary failures

### Known Limitations
- Dependent on FXSSI website structure
- Requires stable internet connection
- User-agent dependent website access

## Extension Possibilities

### Additional Data Sources
- Other Forex sentiment websites
- API integration for real-time data
- Historical data analysis

### Advanced Features
- Web dashboard for data visualization
- Email notifications for signals
- Database integration (MySQL, PostgreSQL)
- RESTful API for data queries

## Support and Maintenance

### Log Analysis
```bash
# Show all logs
tail -f logs/fxssi-extractor.log

# Show only errors
tail -f logs/fxssi-extractor-error.log

# Follow data extraction
tail -f logs/data-extraction.log
```

### Performance Monitoring
- Average extraction time: ~10-30 seconds
- Memory usage: ~50-100 MB
- CPU load: Minimal outside extraction times

## Development and Testing

### Run Unit Tests
```bash
mvn test
```

### Debug Mode
```java
// Custom scheduler for faster testing
HourlyScheduler debugScheduler = HourlyScheduler.createCustomIntervalScheduler(task, 1); // 1 minute
```

### Code Quality
- Comprehensive exception handling
- Thread-safe implementation
- Modular, extensible architecture
- Extensive logging and diagnostics

## API Reference

### Core Classes

#### FXSSIDataExtractor
```java
public class FXSSIDataExtractor {
    public void start()                    // Start the system
    public void stop()                     // Stop the system
    private void extractAndSaveData()      // Extract and save data
}
```

#### CurrencyPairData
```java
public class CurrencyPairData {
    public String getCurrencyPair()       // Get currency pair
    public double getBuyPercentage()      // Get buy percentage
    public double getSellPercentage()     // Get sell percentage
    public TradingSignal getTradingSignal() // Get trading signal
    public String toCsvLine()             // Convert to CSV
    public static CurrencyPairData fromCsvLine(String csvLine) // Parse from CSV
}
```

#### FXSSIScraper
```java
public class FXSSIScraper {
    public List<CurrencyPairData> extractCurrentRatioData() // Extract data
    public boolean testConnection()       // Test connection
}
```

#### DataFileManager
```java
public class DataFileManager {
    public void appendDataToFile(List<CurrencyPairData> data) // Append data
    public List<CurrencyPairData> readDataFromFile(String filename) // Read data
    public void cleanupOldFiles(int daysToKeep) // Cleanup old files
    public boolean validateDataFile(String filename) // Validate file
}
```

#### HourlyScheduler
```java
public class HourlyScheduler {
    public void startScheduling()         // Start scheduler
    public void stopScheduling()          // Stop scheduler
    public void executeTaskManually()     // Manual execution
    public boolean isRunning()            // Check if running
    public String getStatus()             // Get status
}
```

## Troubleshooting

### Common Issues

#### Connection Problems
```java
// Test FXSSI connection
boolean connected = scraper.testConnection();
if (!connected) {
    // Check internet connection or proxy settings
}
```

#### Parsing Failures
- Website structure changes may require scraper updates
- Check log files for parsing error details
- Fallback methods provide basic functionality

#### File Access Issues
- Ensure write permissions for data/ directory
- Check disk space availability
- Verify file locking by other processes

### Debug Logging
Enable debug logging in log4j.xml:
```xml
<Logger name="com.fxssi.extractor" level="DEBUG" additivity="false">
    <AppenderRef ref="Console"/>
    <AppenderRef ref="FileAppender"/>
</Logger>
```

## Deployment

### Production Deployment
1. Build the application: `mvn clean package`
2. Copy JAR file to production server
3. Ensure Java 11+ is installed
4. Create data/ and logs/ directories
5. Configure log4j.xml for production
6. Start with: `java -jar fxssi-extractor.jar`

### Docker Deployment
```dockerfile
FROM openjdk:11-jre-slim
COPY target/fxssi-extractor.jar app.jar
VOLUME ["/app/data", "/app/logs"]
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Service Configuration
Configure as system service (systemd):
```ini
[Unit]
Description=FXSSI Data Extractor
After=network.target

[Service]
Type=simple
User=fxssi
WorkingDirectory=/opt/fxssi-extractor
ExecStart=/usr/bin/java -jar fxssi-extractor.jar
Restart=always

[Install]
WantedBy=multi-user.target
```

---

**Version**: 1.0.0  
**Last Updated**: August 2025  
**License**: Proprietary