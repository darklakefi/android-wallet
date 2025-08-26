# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

DarklakeWallet is an Android application built with Jetpack Compose and Material 3. Package name: `fi.darklake.wallet`

**Target Distribution**: F-Droid open source store. This project must avoid all Google dependencies to maintain F-Droid compatibility.

## Essential Commands

### Build Commands
```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install on device
./gradlew installDebug

# Clean build
./gradlew clean
```

### Testing Commands
```bash
# Run unit tests
./gradlew testDebugUnitTest

# Run instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest

# Run a single test class
./gradlew testDebugUnitTest --tests "fi.darklake.wallet.YourTestClass"

# Run tests with coverage
./gradlew testDebugUnitTestCoverage
```

### Development Commands
```bash
# Check for dependency updates
./gradlew dependencyUpdates

# Run lint checks
./gradlew lint

# Generate signed APK (requires signing config)
./gradlew assembleRelease
```

### Ruby/Fastlane Commands

**IMPORTANT**: This project uses RVM for Ruby management. Before running any Ruby/Fastlane commands, you must set the correct PATH:

```bash
# Set Ruby PATH for current session (required before any Ruby/gem/fastlane commands)
export PATH="$HOME/.rvm/gems/ruby-3.3.6/bin:$HOME/.rvm/rubies/ruby-3.3.6/bin:$PATH"

# Verify Ruby version (should show 3.3.6)
ruby --version

# Install Fastlane dependencies
bundle install

# Run Fastlane commands (examples)
fastlane android debug
fastlane android lint
fastlane android reproducible
```

**Available Fastlane Lanes**:
- `fastlane android debug` - Build debug APK
- `fastlane android release` - Build unsigned release APK for F-Droid
- `fastlane android signed_release` - Build signed release APK (requires signing.properties)
- `fastlane android reproducible` - Build reproducible APK with consistent timestamps
- `fastlane android test` - Run unit tests
- `fastlane android lint` - Run lint checks
- `fastlane android prepare_release` - Update version numbers and prepare for release
- `fastlane android prepare_fdroid_metadata` - Generate F-Droid metadata
- `fastlane android verify_reproducible` - Verify build reproducibility
- `fastlane android clean` - Clean build artifacts

**Release Process**:
1. `fastlane android prepare_release` - Update version, create changelog, git commit & tag
2. `fastlane android release` - Build unsigned APK for F-Droid
3. `fastlane android signed_release` - Build signed APK for Play Store (optional)

**Ruby Version File**: The project uses `.ruby-version` file set to `3.3.6`

## GitHub Actions / CI/CD

This project uses Fastlane-powered GitHub Actions workflows:

### Available Workflows

1. **CI/CD (`ci.yml`)** - Main build and test workflow
   - Triggers: Push/PR to main/develop branches
   - Jobs: Test, Lint, Build (debug/release), Reproducible build verification
   - Cross-platform testing on Ubuntu/macOS/Windows
   
2. **Release (`release.yml`)** - Create releases 
   - Triggers: Version tags (v*) or manual dispatch
   - Creates GitHub releases with unsigned APKs for F-Droid
   - Generates checksums and F-Droid metadata
   
3. **Weekly Reproducible Build (`reproducible-build.yml`)** - Scheduled verification
   - Triggers: Weekly schedule + manual dispatch
   - Verifies build reproducibility over time

### Workflow Features
- **Fastlane Integration**: All builds use Fastlane lanes
- **Ruby PATH Management**: Automatically configures RVM paths
- **Reproducible Builds**: Deterministic environment variables
- **F-Droid Ready**: Generates unsigned APKs and metadata
- **Cross-Platform**: Tests builds on multiple OS environments
- **Caching**: Gradle and Bundle caching for faster builds

## Architecture & Structure

### Technology Stack
- **UI Framework**: Jetpack Compose with Material 3
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 36
- **Kotlin**: 2.2.10
- **Java**: 11

### Design
- Use the files in `./assets` and follow the instructions on `./assets/style-guide.md`
- **Color Management**: All colors MUST be defined in `/app/src/main/java/fi/darklake/wallet/ui/theme/Color.kt` with semantic names (e.g., `DarklakeButtonBg`, `DarklakeTextPrimary`, `TokenSolBackground`). Never hardcode colors directly in components or screens (no `Color(0xFF...)` inline). Components should import colors from the theme: `import fi.darklake.wallet.ui.theme.*`

### Code Organization
```
app/src/main/java/fi/darklake/wallet/
├── MainActivity.kt          # Main entry point with Compose setup
└── ui/theme/               # Material 3 theming
    ├── Color.kt           # Color definitions
    ├── Theme.kt           # Theme configuration with dynamic colors
    └── Type.kt            # Typography definitions
```

### Key Architectural Decisions
1. **Single Activity Architecture**: Uses MainActivity with Jetpack Compose for all UI
2. **Material 3 Design System**: Implements Material You with dynamic color support
3. **Edge-to-Edge UI**: Modern Android UI that extends behind system bars
4. **Version Catalogs**: Dependencies managed via `gradle/libs.versions.toml`
5. **F-Droid Compliance**: No Google Play Services, Firebase, or proprietary dependencies
6. **Open Source Only**: All dependencies must be FOSS-compatible for F-Droid inclusion

### Testing Structure
- Unit tests: `app/src/test/java/fi/darklake/wallet/`
- Instrumented tests: `app/src/androidTest/java/fi/darklake/wallet/`
- Uses JUnit4 for unit tests and AndroidX Test for instrumented tests

## Important Files

- `app/build.gradle.kts` - App module build configuration
- `gradle/libs.versions.toml` - Centralized dependency versions
- `app/src/main/AndroidManifest.xml` - App manifest with permissions
- `app/proguard-rules.pro` - ProGuard configuration for release builds
