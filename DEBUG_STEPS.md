# Debug Steps for Empty Response Error

## 1. Install and Run the App
```bash
./gradlew installDebug
```

## 2. Monitor Logs
Open a terminal and run:
```bash
adb logcat -c  # Clear previous logs
adb logcat | grep -E "(HELIUS|HTTP Client|Request body|Response|Error)"
```

## 3. What to Look For

The logs should show:
1. `=== HELIUS API REQUEST ===`
2. `Making request to: https://devnet.helius-rpc.com/?api-key=...`
3. `Using Helius RPC endpoint`
4. `Request body: {"jsonrpc":"2.0","id":"1","method":"getBalance",...}`
5. `HTTP Client:` messages showing the actual HTTP request/response
6. `Response status:` and `Response content-type:`
7. `Raw response first 500 chars:`

## 4. Possible Issues

### Issue A: Empty Response Body
If you see `Raw response body: ''` with status 200, it might mean:
- The request format is incorrect
- The API endpoint is wrong

### Issue B: HTML Response
If you see HTML in the response, it means:
- API key is invalid
- Wrong endpoint URL
- Rate limiting

### Issue C: No Helius URL
If you see `Using standard Solana RPC endpoint`, it means:
- The API key wasn't loaded from local.properties
- Check Settings screen to see if API key is shown

## 5. Quick Fixes

1. **Check Settings Screen**
   - Open the app
   - Go to Settings
   - Look for "API Key: b2905cfc..." under RPC ENDPOINT
   - If not shown, the key wasn't loaded

2. **Force Reload**
   - Clear app data: Settings > Apps > DarklakeWallet > Clear Data
   - Reinstall: `./gradlew installDebug`

3. **Manual API Key Entry**
   - If auto-load fails, manually enter the key in Settings
   - Save and check if it shows "Helius RPC"