# Reproducible Builds for DarklakeWallet

## Overview

This document describes how to create reproducible builds of DarklakeWallet. Reproducible builds are crucial for cryptocurrency wallets as they allow anyone to independently verify that the published binaries match the source code.

## What are Reproducible Builds?

Reproducible builds ensure that given the same source code, build environment, and build instructions, anyone can generate bit-for-bit identical binaries. This provides several security benefits:

- **Transparency**: Anyone can verify that binaries match the source code
- **Trust**: No need to trust the build infrastructure
- **Supply Chain Security**: Prevents backdoors injected during the build process
- **F-Droid Compatibility**: Required for inclusion in F-Droid's main repository

## Prerequisites

### Software Requirements
- **Java 11 or 17** (OpenJDK recommended)
- **Git** (for source code management)
- **Unix-like environment** (Linux/macOS preferred, WSL for Windows)

### Environment Setup
```bash
# Install required packages (Ubuntu/Debian)
sudo apt update
sudo apt install openjdk-11-jdk git

# Verify Java version
java -version
```

## Quick Start

### 1. Clone and Build
```bash
git clone https://github.com/your-org/DarklakeWallet.git
cd DarklakeWallet

# Run reproducible build
./reproducible-build.sh
```

### 2. Verify Reproducibility
```bash
# Run multiple builds and compare
./verify-reproducible.sh 3
```

## Build Configuration

### Deterministic Settings

The build system uses several mechanisms to ensure determinism:

#### 1. Fixed Timestamps
- Uses `SOURCE_DATE_EPOCH` environment variable
- Falls back to fixed timestamp: `1734307200` (2024-12-16 00:00:00 UTC)

#### 2. Environment Normalization
- `TZ=UTC` - Fixed timezone
- `LC_ALL=C` - Fixed locale
- `LANG=C` - Fixed language

#### 3. Gradle Configuration
- Disabled daemon for consistency
- Fixed Java tool options
- Deterministic resource ordering

#### 4. APK Packaging
- Excluded variable metadata files
- Fixed file ordering in APK
- Deterministic naming scheme

### Build Scripts

#### `reproducible-build.sh`
Main build script that:
- Sets deterministic environment variables
- Cleans previous builds
- Builds unsigned release APK
- Generates build metadata
- Calculates checksums

#### `verify-reproducible.sh`
Verification script that:
- Runs multiple build iterations
- Compares checksums
- Generates verification report
- Validates reproducibility

## Manual Build Process

If you prefer to run builds manually:

```bash
# Set environment variables
export SOURCE_DATE_EPOCH=1734307200
export TZ=UTC
export LC_ALL=C
export LANG=C
export GRADLE_OPTS="-Dorg.gradle.daemon=false -Dorg.gradle.caching=true"

# Clean and build
./gradlew clean
./gradlew assembleRelease --no-daemon --no-parallel

# Calculate checksum
sha256sum app/build/outputs/apk/release/app-release-unsigned.apk
```

## Signing for Release

### Setup Signing Key

1. Create signing configuration:
```bash
cp signing-example.properties signing.properties
# Edit signing.properties with your keystore details
```

2. Generate keystore (if needed):
```bash
keytool -genkeypair -v \
  -keystore release-keystore.jks \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -alias darklake-wallet \
  -storepass "your_secure_password" \
  -keypass "your_secure_password" \
  -dname "CN=DarklakeWallet,OU=Development,O=Darklake,L=City,ST=State,C=FI"
```

### Build Signed APK
```bash
# Build with signing
./gradlew assembleRelease
```

**Note**: Signed builds may not be bit-for-bit identical due to signature timestamps, but the unsigned APK should be reproducible.

## Continuous Integration

### GitHub Actions

The repository includes a GitHub Actions workflow (`.github/workflows/reproducible-build.yml`) that:

- Runs reproducible build verification on each push
- Tests multiple Java versions
- Performs cross-platform builds
- Generates verification reports

### Running CI Locally

Use [act](https://github.com/nektos/act) to run GitHub Actions locally:
```bash
# Install act
curl https://raw.githubusercontent.com/nektos/act/master/install.sh | sudo bash

# Run reproducible build workflow
act push
```

## Verification

### Independent Verification

To verify a release independently:

1. **Check out the exact release tag**:
   ```bash
   git checkout v1.0.0  # Replace with actual version
   ```

2. **Run reproducible build**:
   ```bash
   ./reproducible-build.sh
   ```

3. **Compare with published checksum**:
   ```bash
   sha256sum app/build/outputs/apk/release/app-release-unsigned.apk
   # Compare with published checksum in release notes
   ```

### Expected Output

A successful reproducible build should produce:
- APK file: `app/build/outputs/apk/release/app-release-unsigned.apk`
- Build metadata: `build-info.json`
- Consistent SHA256 checksum across builds

### Troubleshooting

#### Different Checksums

If you get different checksums:

1. **Check Java version**: Ensure you're using the same JDK version
2. **Verify git checkout**: Make sure you're on the exact commit/tag
3. **Check environment**: Verify environment variables are set correctly
4. **Clean build**: Run `./gradlew clean` before building

#### Common Issues

- **Time zones**: Ensure `TZ=UTC` is set
- **Locale**: Use `LC_ALL=C` and `LANG=C`
- **Gradle daemon**: Disable with `--no-daemon`
- **Parallel builds**: Disable with `--no-parallel`

## Security Considerations

### Build Environment

- **Clean environment**: Use fresh Docker containers or VMs
- **Network isolation**: Builds should work offline after dependencies are cached
- **Trusted sources**: Only use official JDK and Gradle distributions

### Verification Practices

- **Multiple builders**: Have different people/organizations build independently
- **Cross-platform**: Verify builds work on different operating systems
- **Regular checks**: Set up automated verification on a schedule

## F-Droid Integration

For F-Droid inclusion:

1. **Reproducible builds are mandatory**
2. **All dependencies must be FOSS**
3. **No proprietary dependencies** (Google Play Services, Firebase, etc.)
4. **Build from source** using F-Droid's build system

The current configuration supports F-Droid requirements:
- ✅ No Google dependencies
- ✅ Reproducible builds
- ✅ Open source dependencies only
- ✅ Gradle build system

## Development vs Release Builds

### Development Builds
For development, you can disable reproducible build settings:
```bash
# Enable Gradle daemon for faster builds
export GRADLE_OPTS=""
./gradlew assembleDebug
```

### Release Builds
Always use reproducible build scripts for releases:
```bash
./reproducible-build.sh  # For releases
./verify-reproducible.sh  # For verification
```

## Contributing

When contributing to the project:

1. **Test reproducibility**: Run `./verify-reproducible.sh` before submitting PRs
2. **Don't break determinism**: Avoid adding sources of non-determinism
3. **Document changes**: Update this file if you modify build configuration

## Resources

- [Reproducible Builds Project](https://reproducible-builds.org/)
- [F-Droid Reproducible Builds](https://f-droid.org/docs/Reproducible_Builds/)
- [Android Reproducible Builds](https://source.android.com/docs/core/architecture/vndk/build-system)
- [Supply Chain Security](https://slsa.dev/)

## Support

If you encounter issues with reproducible builds:

1. Check this documentation
2. Search existing issues on GitHub
3. Open a new issue with:
   - Your operating system and version
   - Java version (`java -version`)
   - Build logs and error messages
   - Steps to reproduce the issue