# FXSSI Data Extractor

[![Java](https://img.shields.io/badge/Java-11+-orange.svg)](https://openjdk.java.net/)  
[![JavaFX](https://img.shields.io/badge/JavaFX-17+-blue.svg)](https://openjfx.io/)  
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)  
[![Version](https://img.shields.io/badge/Version-2.2-brightgreen.svg)](CHANGELOG.md)  

An advanced Java tool for automated extraction and analysis of Forex sentiment data from **FXSSI.com**, with live monitoring, signal change detection, and email notifications.  

## 🌟 Features

### Dual-Mode Support
- **Console Mode**: Automated hourly data collection in the background  
- **GUI Mode**: Interactive JavaFX interface with live monitoring  

### Data Extraction & Storage
- ✅ Automatic extraction of Buy/Sell ratios from FXSSI.com  
- ✅ Four-fold data storage: Daily + currency pair specific + signal changes + email configuration  
- ✅ Robust HTML parsing with JSoup and fallback strategies  
- ✅ Thread-safe CSV file management with UTF-8 encoding  

### Live Monitoring & Analysis
- 🔄 **Signal Change Detection**: Automatic detection of BUY ↔ SELL switches  
- 📊 **Historical Data Analysis**: Complete CSV history per currency pair  
- 📈 **Visual Ratio Bars**: Horizontal Buy/Sell ratio visualization  
- 🎯 **Trading Signal Icons**: Visual BUY/SELL/NEUTRAL indicators  

### Email Notifications
- 📧 **GMX Server Integration**: Preconfigured for GMX accounts  
- 🚨 **Signal Change Alerts**: Automatic emails for critical changes  
- ⚠️ **Importance Filtering**: Critical, High, Medium, Low  
- 🛡️ **Spam Protection**: Configurable hourly email limits  

### GUI Features
- 🖥️ **Modern JavaFX UI**: Built programmatically (no FXML)  
- 🔄 **Auto-Refresh**: Configurable intervals (1–60 minutes)  
- 📊 **Historical Data Viewer**: Popup window with detailed CSV analysis  
- 🔧 **Email Configuration**: Full GMX setup window  
- 💾 **CSV Export**: Direct export for all data  

## 🛠️ Tech Stack

- **Java 11+** – Core programming language  
- **JavaFX 17+** – GUI framework  
- **JSoup** – Web scraping & HTML parsing  
- **Java Mail API** – Email functionality  
- **ScheduledExecutorService** – Task scheduling  
- **CompletableFuture** – Async operations  
- **Maven** – Build management  
- **JUnit 5** – Testing framework  

## 📋 Requirements

- Java 11 or higher  
- JavaFX 17+ (for GUI mode)  
- Internet connection for FXSSI.com  
- GMX email account (optional, for notifications)


## 🚀 Installation

### 1. Clone the Repository
```bash
git clone https://github.com/yourusername/fxssi-data-extractor.git
cd fxssi-data-extractor

