fastlane documentation
----

# Installation

Make sure you have the latest version of the Xcode command line tools installed:

```sh
xcode-select --install
```

For _fastlane_ installation instructions, see [Installing _fastlane_](https://docs.fastlane.tools/#installing-fastlane)

# Available Actions

## Android

### android debug

```sh
[bundle exec] fastlane android debug
```

Build a debug APK

### android release

```sh
[bundle exec] fastlane android release
```

Build a release APK (unsigned for F-Droid)

### android reproducible

```sh
[bundle exec] fastlane android reproducible
```

Build a reproducible release APK for F-Droid

### android test

```sh
[bundle exec] fastlane android test
```

Run all tests

### android lint

```sh
[bundle exec] fastlane android lint
```

Run lint checks

### android prepare_fdroid_metadata

```sh
[bundle exec] fastlane android prepare_fdroid_metadata
```

Prepare F-Droid metadata

### android verify_reproducible

```sh
[bundle exec] fastlane android verify_reproducible
```

Verify reproducible build

### android clean

```sh
[bundle exec] fastlane android clean
```

Clean build artifacts

### android prepare_release

```sh
[bundle exec] fastlane android prepare_release
```

Prepare for a new release - update version numbers

### android signed_release

```sh
[bundle exec] fastlane android signed_release
```

Build a signed release APK (requires signing configuration)

----

This README.md is auto-generated and will be re-generated every time [_fastlane_](https://fastlane.tools) is run.

More information about _fastlane_ can be found on [fastlane.tools](https://fastlane.tools).

The documentation of _fastlane_ can be found on [docs.fastlane.tools](https://docs.fastlane.tools).
