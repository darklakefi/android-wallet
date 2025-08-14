#!/bin/bash

# Reproducible Build Script for DarklakeWallet
# This script ensures deterministic builds by controlling the build environment

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}🔒 DarklakeWallet Reproducible Build Script${NC}"
echo "=============================================="

# Set reproducible build environment variables
export SOURCE_DATE_EPOCH=${SOURCE_DATE_EPOCH:-1734307200} # 2024-12-16 00:00:00 UTC
export TZ=UTC
export LC_ALL=C
export LANG=C

# Set Java reproducible options
export JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8 -Djava.awt.headless=true"

# Set Gradle daemon off for reproducible builds
export GRADLE_OPTS="-Dorg.gradle.daemon=false -Dorg.gradle.caching=true -Dorg.gradle.parallel=false"

echo -e "${YELLOW}Build Configuration:${NC}"
echo "SOURCE_DATE_EPOCH: $(date -d @$SOURCE_DATE_EPOCH -u)"
echo "Java Version: $(java -version 2>&1 | head -n 1)"
echo "Gradle Version: $(./gradlew --version | grep Gradle | head -n 1)"

# Clean build
echo -e "\n${YELLOW}🧹 Cleaning previous builds...${NC}"
./gradlew clean

# Verify git status
if [ -d ".git" ]; then
    if [ -n "$(git status --porcelain)" ]; then
        echo -e "${YELLOW}⚠️  Warning: Working directory is not clean${NC}"
        git status --short
    else
        echo -e "${GREEN}✅ Working directory is clean${NC}"
        echo "Git commit: $(git rev-parse HEAD)"
        echo "Git tag: $(git describe --tags --exact-match 2>/dev/null || echo 'No tag')"
    fi
fi

# Build release APK
echo -e "\n${YELLOW}🔨 Building release APK...${NC}"
./gradlew assembleRelease \
    --no-daemon \
    --no-parallel \
    --no-configuration-cache

# Get build information - try different naming patterns
APK_PATH=""
RELEASE_DIR="app/build/outputs/apk/release"

# Look for APK files with different naming patterns
if [ -f "$RELEASE_DIR/darklake-wallet-1.0-release.apk" ]; then
    APK_PATH="$RELEASE_DIR/darklake-wallet-1.0-release.apk"
    APK_TYPE="signed"
elif [ -f "$RELEASE_DIR/darklake-wallet-1.0-release-unsigned.apk" ]; then
    APK_PATH="$RELEASE_DIR/darklake-wallet-1.0-release-unsigned.apk"
    APK_TYPE="unsigned"
elif [ -f "$RELEASE_DIR/app-release.apk" ]; then
    APK_PATH="$RELEASE_DIR/app-release.apk"
    APK_TYPE="signed"
elif [ -f "$RELEASE_DIR/app-release-unsigned.apk" ]; then
    APK_PATH="$RELEASE_DIR/app-release-unsigned.apk"
    APK_TYPE="unsigned"
else
    # Find any APK file in the release directory
    APK_PATH=$(find "$RELEASE_DIR" -name "*.apk" -type f | head -n 1)
    if [ -n "$APK_PATH" ]; then
        if [[ "$APK_PATH" == *"unsigned"* ]]; then
            APK_TYPE="unsigned"
        else
            APK_TYPE="signed"
        fi
    fi
fi

if [ -n "$APK_PATH" ] && [ -f "$APK_PATH" ]; then
    echo -e "\n${GREEN}✅ Build completed successfully! ($APK_TYPE APK)${NC}"
    echo "APK location: $APK_PATH"
    echo "APK size: $(du -h "$APK_PATH" | cut -f1)"
    echo "APK checksum (SHA256): $(sha256sum "$APK_PATH" | cut -d' ' -f1)"
    
    # Store build metadata
    BUILD_INFO_FILE="build-info.json"
    cat > "$BUILD_INFO_FILE" << EOF
{
  "build_timestamp": "$SOURCE_DATE_EPOCH",
  "build_date": "$(date -d @$SOURCE_DATE_EPOCH -u --rfc-3339=seconds)",
  "git_commit": "$(git rev-parse HEAD 2>/dev/null || echo 'unknown')",
  "git_tag": "$(git describe --tags --exact-match 2>/dev/null || echo 'none')",
  "gradle_version": "$(./gradlew --version | grep Gradle | cut -d' ' -f2)",
  "java_version": "$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)",
  "apk_sha256": "$(sha256sum "$APK_PATH" | cut -d' ' -f1)",
  "apk_size": $(stat -c%s "$APK_PATH")
}
EOF
    
    echo "Build metadata saved to: $BUILD_INFO_FILE"
    
    # Verify APK content (optional)
    if command -v aapt2 &> /dev/null; then
        echo -e "\n${YELLOW}📦 APK Information:${NC}"
        aapt2 dump badging "$APK_PATH" | grep -E "(package|versionCode|versionName|platformBuildVersionName)"
    fi
    
else
    echo -e "\n${RED}❌ Build failed - APK not found${NC}"
    exit 1
fi

echo -e "\n${GREEN}🎉 Reproducible build completed successfully!${NC}"
echo "To verify reproducibility:"
echo "1. Run this script again on a different machine"
echo "2. Compare the SHA256 checksums of the generated APKs"
echo "3. They should be identical if the build is truly reproducible"