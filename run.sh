#!/usr/bin/env bash
set -e

echo "=== Checking Ubuntu Prerequisites ==="

# Install Java if missing
if ! command -v java &> /dev/null; then
    echo "Java not found. Installing OpenJDK..."
    sudo apt update && sudo apt install -y default-jdk
fi

# Install Clojure CLI if missing
if ! command -v clojure &> /dev/null; then
    echo "Clojure CLI not found. Installing..."
    sudo apt update && sudo apt install -y curl
    curl -O https://download.clojure.org/install/linux-install-1.11.1.1413.sh
    chmod +x linux-install-1.11.1.1413.sh
    sudo ./linux-install-1.11.1.1413.sh
    rm linux-install-1.11.1.1413.sh
fi

echo ""
echo "=== Running Concurrent Session Store Simulation ==="
clojure session_store.clj
