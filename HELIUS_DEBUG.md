# Helius API Debug Guide

## Fixed Issues

1. **Added Helius API key configuration** - You can now configure your Helius API key in Settings
2. **Updated RPC endpoints** - The app now uses Helius RPC endpoints when an API key is configured
3. **Improved error handling** - Better error messages that indicate when an API key is missing

## To Fix the "Empty Response" Error

1. **Get a Helius API Key**:
   - Go to https://www.helius.dev/
   - Sign up for a free account
   - Create a new project
   - Copy your API key

2. **Configure the API Key in the App**:
   - Open the app
   - Go to Settings (gear icon)
   - Paste your Helius API key in the "HELIUS API KEY" field
   - Tap "Save API Key"

3. **Check the Logs**:
   - Run: `adb logcat | grep -E "(Helius|Solana|response)"`
   - Look for:
     - "Making request to:" - Shows the actual URL being used
     - "Response status:" - HTTP status code
     - "Raw response body:" - The actual response content

## What Changed

1. **NetworkSettings.kt** - Added `heliusApiKey` field and `getHeliusRpcUrl()` function
2. **SettingsManager.kt** - Updated to use Helius RPC URLs when API key is configured
3. **SettingsScreen.kt** - Added UI to configure Helius API key
4. **HeliusApiService.kt** - Enhanced error logging to show more details about empty responses

## Testing Without API Key

The app will fall back to public Solana RPC endpoints if no Helius API key is configured, but these have strict rate limits and may not work reliably.

## Common Issues

1. **Empty Response** - Usually means:
   - No API key configured
   - Invalid API key
   - Rate limit exceeded on public endpoints

2. **Network Error** - Check:
   - Internet connection
   - Firewall/proxy settings
   - VPN conflicts