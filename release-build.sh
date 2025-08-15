#!/bin/bash

# DarklakeWallet Release Build Script
# This script builds a signed release APK with proper configuration

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}DarklakeWallet Release Build Script${NC}"
echo "===================================="

# Check if signing.properties exists
if [ ! -f "signing.properties" ]; then
    echo -e "${RED}Error: signing.properties not found!${NC}"
    echo "Please copy signing.properties.example to signing.properties and configure it."
    exit 1
fi

# Check if keystore file exists
KEYSTORE_FILE=$(grep KEYSTORE_FILE signing.properties | cut -d'=' -f2)
if [ ! -f "$KEYSTORE_FILE" ]; then
    echo -e "${RED}Error: Keystore file not found at: $KEYSTORE_FILE${NC}"
    exit 1
fi

# Clean previous builds
echo -e "${YELLOW}Cleaning previous builds...${NC}"
./gradlew clean

# Set SOURCE_DATE_EPOCH for reproducible builds
export SOURCE_DATE_EPOCH=$(date +%s)
echo -e "${YELLOW}SOURCE_DATE_EPOCH set to: $SOURCE_DATE_EPOCH${NC}"

# Build release APK
echo -e "${YELLOW}Building release APK...${NC}"
./gradlew assembleRelease

# Find the generated APK
APK_PATH=$(find app/build/outputs/apk/release -name "*.apk" | head -n 1)

if [ -z "$APK_PATH" ]; then
    echo -e "${RED}Error: No APK file found!${NC}"
    exit 1
fi

echo -e "${GREEN}Build successful!${NC}"
echo -e "APK location: ${YELLOW}$APK_PATH${NC}"

# Get APK information
APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
echo -e "APK size: ${YELLOW}$APK_SIZE${NC}"

# Verify signature
echo -e "\n${YELLOW}Verifying APK signature...${NC}"
if command -v apksigner &> /dev/null; then
    apksigner verify --verbose "$APK_PATH"
else
    echo -e "${YELLOW}apksigner not found. Install Android SDK build-tools to verify signatures.${NC}"
fi

# Calculate checksums
echo -e "\n${YELLOW}Calculating checksums...${NC}"
echo -e "SHA256: $(sha256sum "$APK_PATH" | cut -d' ' -f1)"
echo -e "MD5: $(md5sum "$APK_PATH" | cut -d' ' -f1)"

# Create release directory
RELEASE_DIR="releases/$(date +%Y%m%d_%H%M%S)"
mkdir -p "$RELEASE_DIR"

# Copy APK to release directory
cp "$APK_PATH" "$RELEASE_DIR/"
echo -e "\n${GREEN}APK copied to: $RELEASE_DIR/${NC}"

# Generate release notes template
cat > "$RELEASE_DIR/release-notes.txt" << EOF
DarklakeWallet Release $(grep versionName app/build.gradle.kts | cut -d'"' -f2)
Build Date: $(date)
SOURCE_DATE_EPOCH: $SOURCE_DATE_EPOCH

SHA256: $(sha256sum "$APK_PATH" | cut -d' ' -f1)
MD5: $(md5sum "$APK_PATH" | cut -d' ' -f1)

Changes in this release:
- 

Known issues:
- 

EOF

echo -e "${GREEN}Release build complete!${NC}"
echo -e "Release directory: ${YELLOW}$RELEASE_DIR${NC}"
echo -e "\nNext steps:"
echo -e "1. Edit ${YELLOW}$RELEASE_DIR/release-notes.txt${NC}"
echo -e "2. Test the APK thoroughly"
echo -e "3. Upload to F-Droid or your distribution channel"