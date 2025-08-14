#!/bin/bash

# Reproducible Build Verification Script for DarklakeWallet
# This script performs multiple builds and verifies they produce identical APKs

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}🔍 DarklakeWallet Reproducible Build Verification${NC}"
echo "=================================================="

# Number of build iterations
ITERATIONS=${1:-2}
BUILD_DIR="verification-builds"

# Clean up previous verification builds
if [ -d "$BUILD_DIR" ]; then
    echo -e "${YELLOW}🧹 Cleaning previous verification builds...${NC}"
    rm -rf "$BUILD_DIR"
fi
mkdir -p "$BUILD_DIR"

# Store checksums for comparison
declare -a checksums=()
declare -a build_times=()

echo -e "\n${YELLOW}🔄 Running $ITERATIONS build iterations...${NC}"

for i in $(seq 1 $ITERATIONS); do
    echo -e "\n${BLUE}--- Build Iteration $i ---${NC}"
    
    # Record start time
    start_time=$(date +%s)
    
    # Run reproducible build
    ./reproducible-build.sh
    
    # Record end time
    end_time=$(date +%s)
    build_time=$((end_time - start_time))
    build_times+=($build_time)
    
    # Copy APK and build info - find the actual APK file
    RELEASE_DIR="app/build/outputs/apk/release"
    APK_FILE=$(find "$RELEASE_DIR" -name "*.apk" -type f | head -n 1)
    
    if [ -n "$APK_FILE" ] && [ -f "$APK_FILE" ]; then
        cp "$APK_FILE" "$BUILD_DIR/build-$i.apk"
        cp build-info.json "$BUILD_DIR/build-info-$i.json"
    else
        echo "Error: No APK file found in $RELEASE_DIR"
        exit 1
    fi
    
    # Calculate checksum
    checksum=$(sha256sum "$BUILD_DIR/build-$i.apk" | cut -d' ' -f1)
    checksums+=($checksum)
    
    echo "Build $i completed in ${build_time}s"
    echo "Checksum: $checksum"
done

echo -e "\n${BLUE}📊 Verification Results${NC}"
echo "========================"

# Compare checksums
first_checksum=${checksums[0]}
all_identical=true

for i in $(seq 0 $((ITERATIONS-1))); do
    build_num=$((i+1))
    echo "Build $build_num: ${checksums[$i]} (${build_times[$i]}s)"
    
    if [ "${checksums[$i]}" != "$first_checksum" ]; then
        all_identical=false
    fi
done

echo

if [ "$all_identical" = true ]; then
    echo -e "${GREEN}✅ SUCCESS: All builds produced identical APKs!${NC}"
    echo -e "${GREEN}🔒 The build is reproducible!${NC}"
    echo
    echo "Identical checksum: $first_checksum"
    
    # Calculate average build time
    total_time=0
    for time in "${build_times[@]}"; do
        total_time=$((total_time + time))
    done
    avg_time=$((total_time / ITERATIONS))
    echo "Average build time: ${avg_time}s"
    
else
    echo -e "${RED}❌ FAILURE: Builds produced different APKs!${NC}"
    echo -e "${RED}🚨 The build is NOT reproducible!${NC}"
    echo
    echo "This indicates non-deterministic elements in the build process."
    echo "Common causes:"
    echo "- Timestamps embedded in the build"
    echo "- Non-deterministic ordering of resources"
    echo "- Environment-dependent values"
    echo "- Non-reproducible dependencies"
    
    exit 1
fi

# Generate verification report
REPORT_FILE="$BUILD_DIR/verification-report.json"
cat > "$REPORT_FILE" << EOF
{
  "verification_timestamp": "$(date -u --rfc-3339=seconds)",
  "iterations": $ITERATIONS,
  "reproducible": $all_identical,
  "common_checksum": "$first_checksum",
  "builds": [
EOF

for i in $(seq 0 $((ITERATIONS-1))); do
    build_num=$((i+1))
    cat >> "$REPORT_FILE" << EOF
    {
      "iteration": $build_num,
      "checksum": "${checksums[$i]}",
      "build_time_seconds": ${build_times[$i]}
    }$([ $i -lt $((ITERATIONS-1)) ] && echo "," || echo "")
EOF
done

cat >> "$REPORT_FILE" << EOF
  ],
  "average_build_time_seconds": $avg_time,
  "git_commit": "$(git rev-parse HEAD 2>/dev/null || echo 'unknown')",
  "environment": {
    "os": "$(uname -s)",
    "arch": "$(uname -m)",
    "kernel": "$(uname -r)",
    "java_version": "$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)",
    "gradle_version": "$(./gradlew --version | grep Gradle | cut -d' ' -f2)"
  }
}
EOF

echo
echo "Verification report saved to: $REPORT_FILE"
echo "Build artifacts stored in: $BUILD_DIR/"

if [ "$all_identical" = true ]; then
    echo -e "\n${GREEN}🎉 Reproducible build verification completed successfully!${NC}"
    exit 0
else
    echo -e "\n${RED}💥 Reproducible build verification failed!${NC}"
    exit 1
fi