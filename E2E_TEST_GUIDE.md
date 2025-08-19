# End-to-End Testing Guide

This guide explains how to run the end-to-end tests for DarklakeWallet.

## Prerequisites

1. **Android Emulator or Physical Device**
   - Android API level 24+ (Android 7.0+)
   - Recommended: Pixel Fold emulator with API 35

2. **Android SDK Setup**
   - Ensure Android SDK is installed
   - ADB should be in your PATH

## Test Structure

The E2E tests are organized into four main test classes:

### 1. WalletCreationE2ETest
Tests the complete wallet creation and import flow:
- New wallet creation
- Mnemonic display and verification
- Wallet import with valid/invalid mnemonics
- Navigation between onboarding screens

### 2. WalletMainE2ETest
Tests the main wallet functionality:
- Balance display
- Token and NFT lists
- Address copying
- Navigation between tabs
- Settings access
- Network status display

### 3. SwapE2ETest
Tests the token swap functionality:
- Token selection
- Amount input
- Slippage settings
- Quote display
- Swap execution flow
- Error handling

### 4. LiquidityE2ETest
Tests the liquidity provider functionality:
- Pool creation
- Adding liquidity
- Token pair selection
- Position management
- Slippage tolerance

## Running the Tests

### Start the Emulator

```bash
# List available AVDs
emulator -list-avds

# Start the emulator (replace with your AVD name)
emulator -avd Pixel_Fold_API_35 &

# Wait for device to be ready
adb wait-for-device
adb shell getprop sys.boot_completed
```

### Run All E2E Tests

```bash
# Run all E2E tests
./gradlew connectedAndroidTest

# Or run specific test class
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=fi.darklake.wallet.e2e.WalletCreationE2ETest

# Run test suite
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=fi.darklake.wallet.e2e.E2ETestSuite
```

### Run Individual Test Methods

```bash
# Run specific test method
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=fi.darklake.wallet.e2e.WalletCreationE2ETest#testCompleteWalletCreationFlow
```

### View Test Results

Test results are saved in:
```
app/build/reports/androidTests/connected/index.html
```

## Test Coverage

The E2E tests cover:

✅ **Onboarding Flow**
- Wallet creation
- Mnemonic backup and verification
- Wallet import
- Error handling

✅ **Main Wallet Features**
- Balance display
- Token inventory
- NFT collection
- Address management
- Network status

✅ **Swap Functionality**
- Token selection
- Amount input validation
- Slippage configuration
- Quote generation
- Transaction execution

✅ **Liquidity Provider**
- Pool creation
- Liquidity addition
- Position management
- Withdrawal flow

## Debugging Failed Tests

1. **Check Device/Emulator Status**
   ```bash
   adb devices
   ```

2. **View Device Logs**
   ```bash
   adb logcat | grep "DarklakeWallet"
   ```

3. **Take Screenshots During Tests**
   The tests automatically capture screenshots on failure, found in:
   ```
   app/build/outputs/connected_android_test_additional_output/
   ```

4. **Run Tests with Debugging**
   ```bash
   ./gradlew connectedAndroidTest --debug
   ```

## Common Issues and Solutions

### Issue: Emulator not starting
**Solution:** Ensure you have enough RAM (8GB+) and try with software rendering:
```bash
emulator -avd Pixel_Fold_API_35 -gpu swiftshader_indirect
```

### Issue: Tests timing out
**Solution:** Increase timeout values in tests or ensure network connectivity for API calls

### Issue: "No connected devices"
**Solution:** 
```bash
adb kill-server
adb start-server
adb devices
```

### Issue: Permission denied
**Solution:** Grant necessary permissions:
```bash
adb shell pm grant fi.darklake.wallet android.permission.INTERNET
```

## Writing New Tests

Use the `TestUtils` helper class for common operations:

```kotlin
import fi.darklake.wallet.e2e.TestUtils

@Test
fun myNewTest() {
    // Setup test wallet
    TestUtils.setupTestWallet()
    
    // Use helper functions
    composeTestRule.waitForText("Expected Text")
    composeTestRule.assertWalletScreenDisplayed()
}
```

## CI/CD Integration

To run tests in CI/CD pipelines:

```yaml
# Example GitHub Actions workflow
- name: Run E2E Tests
  uses: reactivecircus/android-emulator-runner@v2
  with:
    api-level: 35
    target: google_apis
    arch: x86_64
    script: ./gradlew connectedAndroidTest
```

## Performance Tips

1. **Use @LargeTest annotation** for long-running tests
2. **Disable animations** on test devices:
   ```bash
   adb shell settings put global window_animation_scale 0
   adb shell settings put global transition_animation_scale 0
   adb shell settings put global animator_duration_scale 0
   ```
3. **Run tests in parallel** when possible
4. **Use test orchestrator** for better isolation:
   ```kotlin
   android {
       testOptions {
           execution = "ANDROIDX_TEST_ORCHESTRATOR"
       }
   }
   ```

## Test Maintenance

- Update tests when UI changes
- Keep test data realistic
- Use descriptive test names
- Add comments for complex test logic
- Regular test execution to catch regressions early