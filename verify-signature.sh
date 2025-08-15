#!/bin/bash

# DarklakeWallet APK Signature Verification Script
# This script verifies the signature of an APK file

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${GREEN}DarklakeWallet APK Signature Verification${NC}"
echo "========================================="

# Check if APK file is provided
if [ $# -eq 0 ]; then
    echo -e "${RED}Error: No APK file specified${NC}"
    echo "Usage: $0 <path-to-apk>"
    echo "Example: $0 app/build/outputs/apk/release/darklake-wallet-1.0.apk"
    exit 1
fi

APK_FILE="$1"

# Check if file exists
if [ ! -f "$APK_FILE" ]; then
    echo -e "${RED}Error: APK file not found: $APK_FILE${NC}"
    exit 1
fi

echo -e "Verifying: ${YELLOW}$APK_FILE${NC}"
echo

# Function to check if a command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Method 1: Using apksigner (preferred)
if command_exists apksigner; then
    echo -e "${BLUE}=== Using apksigner ===${NC}"
    apksigner verify --verbose --print-certs "$APK_FILE"
    
    if [ $? -eq 0 ]; then
        echo -e "\n${GREEN}✓ APK signature verified successfully with apksigner${NC}"
    else
        echo -e "\n${RED}✗ APK signature verification failed with apksigner${NC}"
        exit 1
    fi
else
    echo -e "${YELLOW}apksigner not found. Install Android SDK build-tools for best results.${NC}"
fi

echo

# Method 2: Using jarsigner (fallback)
if command_exists jarsigner; then
    echo -e "${BLUE}=== Using jarsigner ===${NC}"
    jarsigner -verify -verbose -certs "$APK_FILE"
    
    if [ $? -eq 0 ]; then
        echo -e "\n${GREEN}✓ APK signature verified successfully with jarsigner${NC}"
    else
        echo -e "\n${RED}✗ APK signature verification failed with jarsigner${NC}"
        exit 1
    fi
elif ! command_exists apksigner; then
    echo -e "${RED}Neither apksigner nor jarsigner found. Cannot verify signature.${NC}"
    echo "Install Android SDK build-tools or JDK to verify signatures."
    exit 1
fi

echo

# Extract certificate information
echo -e "${BLUE}=== Certificate Information ===${NC}"

# Create temporary directory
TEMP_DIR=$(mktemp -d)
trap "rm -rf $TEMP_DIR" EXIT

# Extract APK
unzip -q "$APK_FILE" -d "$TEMP_DIR"

# Find certificate files
CERT_FILES=$(find "$TEMP_DIR/META-INF" -name "*.RSA" -o -name "*.DSA" -o -name "*.EC" 2>/dev/null)

if [ -z "$CERT_FILES" ]; then
    echo -e "${YELLOW}No certificate files found in APK${NC}"
else
    for CERT in $CERT_FILES; do
        echo -e "\n${YELLOW}Certificate: $(basename "$CERT")${NC}"
        if command_exists keytool; then
            keytool -printcert -file "$CERT" | grep -E "Owner:|Issuer:|Serial number:|Valid from:"
        elif command_exists openssl; then
            openssl pkcs7 -inform DER -in "$CERT" -print_certs -text | grep -E "Subject:|Issuer:|Serial Number:|Not Before:|Not After:"
        else
            echo "keytool or openssl required to display certificate details"
        fi
    done
fi

echo

# Calculate checksums
echo -e "${BLUE}=== APK Checksums ===${NC}"
echo -e "SHA256: $(sha256sum "$APK_FILE" | cut -d' ' -f1)"
echo -e "SHA1:   $(sha1sum "$APK_FILE" | cut -d' ' -f1)"
echo -e "MD5:    $(md5sum "$APK_FILE" | cut -d' ' -f1)"

# File information
echo -e "\n${BLUE}=== File Information ===${NC}"
echo -e "Size: $(du -h "$APK_FILE" | cut -f1)"
echo -e "Modified: $(stat -c %y "$APK_FILE" 2>/dev/null || stat -f %Sm "$APK_FILE" 2>/dev/null)"

# Check if APK is signed with debug key
if command_exists apksigner; then
    echo -e "\n${BLUE}=== Debug Key Check ===${NC}"
    if apksigner verify --print-certs "$APK_FILE" | grep -q "CN=Android Debug"; then
        echo -e "${YELLOW}⚠ WARNING: This APK is signed with a debug key!${NC}"
        echo -e "${YELLOW}Debug-signed APKs should not be distributed publicly.${NC}"
    else
        echo -e "${GREEN}✓ APK is not signed with debug key${NC}"
    fi
fi

# V1/V2/V3/V4 signature scheme check
if command_exists apksigner; then
    echo -e "\n${BLUE}=== Signature Schemes ===${NC}"
    SCHEMES=$(apksigner verify --verbose "$APK_FILE" 2>&1 | grep -E "Verified using v[1-4] scheme" | sed 's/^/  /')
    if [ -n "$SCHEMES" ]; then
        echo "$SCHEMES"
    fi
fi

echo -e "\n${GREEN}Verification complete!${NC}"