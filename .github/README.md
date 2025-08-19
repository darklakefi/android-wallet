# GitHub Actions Workflows

This directory contains the CI/CD workflows for DarklakeWallet, all powered by Fastlane.

## Workflows

### 🚀 CI/CD (`ci.yml`)
**Triggers:** Push/PR to main/develop branches

The main workflow that runs on every push and pull request:

- **Test & Lint**: Runs unit tests and lint checks using Fastlane
- **Build**: Creates both debug and release APKs
- **Reproducible Build**: Verifies build reproducibility 
- **Cross-Platform**: Tests builds on Ubuntu, macOS, and Windows
- **F-Droid Prep**: Generates F-Droid metadata for main branch

### 📦 Release (`release.yml`)
**Triggers:** Version tags (`v*`) or manual dispatch

Creates GitHub releases with F-Droid ready APKs:

- Runs full test suite before release
- Builds reproducible, unsigned APK for F-Droid
- Generates SHA256 checksums
- Creates GitHub release with artifacts
- Prepares F-Droid metadata

### 🔄 Weekly Reproducible Build (`reproducible-build.yml`)
**Triggers:** Weekly schedule (Mondays 2AM UTC) or manual dispatch

Scheduled verification of build reproducibility over time:

- Runs the reproducible build verification script
- Compares multiple builds for consistency
- Ensures long-term build stability

## Features

### ✅ Fastlane Integration
All workflows use Fastlane lanes instead of raw Gradle commands:
- `fastlane android test` - Run tests
- `fastlane android lint` - Run lint checks  
- `fastlane android debug` - Build debug APK
- `fastlane android release` - Build release APK
- `fastlane android reproducible` - Build reproducible APK

### 🔐 Reproducible Builds
All workflows use deterministic environment settings:
- `SOURCE_DATE_EPOCH=1734307200` - Fixed timestamp
- `TZ=UTC` - UTC timezone
- `LC_ALL=C` and `LANG=C` - Consistent locale
- Gradle daemon disabled for deterministic builds

### 📱 F-Droid Ready
Release workflow creates F-Droid compatible artifacts:
- Unsigned APKs (F-Droid signs them)
- Metadata structure for F-Droid inclusion
- SHA256 checksums for verification

### ⚡ Performance Optimized
- Gradle and Bundle caching for faster builds
- Conditional job execution
- Artifact retention policies
- Cross-platform parallel execution

## Manual Triggers

You can manually trigger workflows:

1. **Release Workflow**: Go to Actions → Release → Run workflow
   - Enter version (e.g., `v1.0.0`)
   
2. **Reproducible Build**: Go to Actions → Weekly Reproducible Build → Run workflow

## Artifacts

Workflows generate these artifacts:

### CI/CD Workflow
- `apk-debug` - Debug APK (30 days)
- `apk-release` - Release APK (30 days)
- `lint-reports` - Lint reports (30 days)
- `reproducible-builds` - Verification files (30 days)
- `build-{os}` - Cross-platform builds (7 days)

### Release Workflow
- GitHub Release with APK and checksums
- `fdroid-metadata-{version}` - F-Droid files (365 days)
- `release-artifacts-{version}` - Build files (365 days)

## Ruby/Fastlane Setup

All workflows handle Ruby/RVM setup automatically:

1. Install Ruby 3.3.6 using `ruby/setup-ruby`
2. Set up RVM PATH variables
3. Run `bundle install` to install Fastlane
4. Execute Fastlane lanes with proper PATH

## Security

- No secrets required for basic builds
- All builds are reproducible and verifiable
- F-Droid compatible (no Google dependencies)
- Unsigned releases for F-Droid to sign

## Development

To test workflows locally:
1. Install act: `curl https://raw.githubusercontent.com/nektos/act/master/install.sh | sudo bash`
2. Run workflow: `act push -W .github/workflows/ci.yml`

For Fastlane testing:
```bash
export PATH="$HOME/.rvm/gems/ruby-3.3.6/bin:$HOME/.rvm/rubies/ruby-3.3.6/bin:$PATH"
bundle exec fastlane android debug
```