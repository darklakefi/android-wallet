#!/bin/bash

# DarklakeWallet Release Preparation Script
# This script helps prepare for a new release by updating version numbers

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${GREEN}DarklakeWallet Release Preparation Script${NC}"
echo "========================================"

# Function to get current version
get_current_version() {
    grep "versionName" app/build.gradle.kts | cut -d'"' -f2
}

# Function to get current version code
get_current_version_code() {
    grep "versionCode" app/build.gradle.kts | grep -o '[0-9]*'
}

# Display current version
CURRENT_VERSION=$(get_current_version)
CURRENT_VERSION_CODE=$(get_current_version_code)

echo -e "Current version: ${YELLOW}$CURRENT_VERSION${NC} (code: $CURRENT_VERSION_CODE)"

# Ask for new version
echo -e "\n${BLUE}Enter new version (e.g., 1.1.0):${NC}"
read -r NEW_VERSION

# Validate version format
if ! [[ $NEW_VERSION =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    echo -e "${RED}Error: Invalid version format. Please use semantic versioning (e.g., 1.1.0)${NC}"
    exit 1
fi

# Ask for version code increment
echo -e "\n${BLUE}Enter new version code (current: $CURRENT_VERSION_CODE, usually increment by 1):${NC}"
read -r NEW_VERSION_CODE

# Validate version code
if ! [[ $NEW_VERSION_CODE =~ ^[0-9]+$ ]]; then
    echo -e "${RED}Error: Version code must be a number${NC}"
    exit 1
fi

if [ "$NEW_VERSION_CODE" -le "$CURRENT_VERSION_CODE" ]; then
    echo -e "${YELLOW}Warning: New version code ($NEW_VERSION_CODE) is not greater than current ($CURRENT_VERSION_CODE)${NC}"
    echo -e "${BLUE}Continue anyway? (y/N):${NC}"
    read -r CONTINUE
    if [ "$CONTINUE" != "y" ] && [ "$CONTINUE" != "Y" ]; then
        exit 1
    fi
fi

# Show changes
echo -e "\n${YELLOW}Version changes:${NC}"
echo -e "Version: $CURRENT_VERSION → ${GREEN}$NEW_VERSION${NC}"
echo -e "Version Code: $CURRENT_VERSION_CODE → ${GREEN}$NEW_VERSION_CODE${NC}"

echo -e "\n${BLUE}Proceed with version update? (y/N):${NC}"
read -r CONFIRM

if [ "$CONFIRM" != "y" ] && [ "$CONFIRM" != "Y" ]; then
    echo "Version update cancelled."
    exit 0
fi

# Update build.gradle.kts
echo -e "\n${YELLOW}Updating build.gradle.kts...${NC}"
sed -i "s/versionName = \"$CURRENT_VERSION\"/versionName = \"$NEW_VERSION\"/" app/build.gradle.kts
sed -i "s/versionCode = $CURRENT_VERSION_CODE/versionCode = $NEW_VERSION_CODE/" app/build.gradle.kts

# Create changelog entry
CHANGELOG_DIR="changelogs"
CHANGELOG_FILE="$CHANGELOG_DIR/CHANGELOG-$NEW_VERSION.md"
mkdir -p "$CHANGELOG_DIR"

echo -e "${YELLOW}Creating changelog template...${NC}"
cat > "$CHANGELOG_FILE" << EOF
# DarklakeWallet v$NEW_VERSION

Released: $(date +%Y-%m-%d)

## What's New
- 

## Improvements
- 

## Bug Fixes
- 

## Technical Changes
- 

## Known Issues
- 

---

For the complete changelog, see [CHANGELOG.md](../CHANGELOG.md)
EOF

# Update main CHANGELOG.md if it exists
if [ -f "CHANGELOG.md" ]; then
    echo -e "${YELLOW}Updating CHANGELOG.md...${NC}"
    # Create temporary file with new entry
    cat > CHANGELOG.tmp << EOF
# Changelog

## [$NEW_VERSION] - $(date +%Y-%m-%d)

### Added
- 

### Changed
- 

### Fixed
- 

EOF
    # Append existing changelog content (skip the first line if it's a header)
    tail -n +2 CHANGELOG.md >> CHANGELOG.tmp
    mv CHANGELOG.tmp CHANGELOG.md
fi

# Git operations
echo -e "\n${YELLOW}Git status:${NC}"
git status --short

echo -e "\n${BLUE}Create git commit for version bump? (y/N):${NC}"
read -r CREATE_COMMIT

if [ "$CREATE_COMMIT" = "y" ] || [ "$CREATE_COMMIT" = "Y" ]; then
    git add app/build.gradle.kts
    [ -f "CHANGELOG.md" ] && git add CHANGELOG.md
    git add "$CHANGELOG_FILE"
    
    git commit -m "Bump version to $NEW_VERSION (version code: $NEW_VERSION_CODE)"
    echo -e "${GREEN}Git commit created${NC}"
    
    echo -e "\n${BLUE}Create git tag for this version? (y/N):${NC}"
    read -r CREATE_TAG
    
    if [ "$CREATE_TAG" = "y" ] || [ "$CREATE_TAG" = "Y" ]; then
        TAG_NAME="v$NEW_VERSION"
        git tag -a "$TAG_NAME" -m "Release version $NEW_VERSION"
        echo -e "${GREEN}Git tag '$TAG_NAME' created${NC}"
        echo -e "${YELLOW}Remember to push tags: git push origin $TAG_NAME${NC}"
    fi
fi

echo -e "\n${GREEN}Release preparation complete!${NC}"
echo -e "\nNext steps:"
echo -e "1. Edit ${YELLOW}$CHANGELOG_FILE${NC} with actual release notes"
echo -e "2. Review and test all changes"
echo -e "3. Run ${YELLOW}./release-build.sh${NC} to create signed APK"
echo -e "4. Push commits and tags to repository"