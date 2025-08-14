# Helius API Setup

To enable wallet balance, token, and NFT fetching, you need to configure the Helius API:

## Steps:

1. **Get Helius API Key:**
   - Go to [helius.dev](https://www.helius.dev/)
   - Sign up for an account
   - Create a new project
   - Copy your API key

2. **Configure API Key (Choose one method):**

   ### Method 1: Local Properties File (Recommended for Development)
   - Copy `app/src/main/assets/local.properties.template` to `app/src/main/assets/local.properties`
   - Replace `YOUR_API_KEY_HERE` with your actual API key
   - The app will automatically load this key on startup
   - This file is gitignored and won't be committed

   ### Method 2: In-App Settings
   - Open the app and go to Settings
   - Enter your API key in the "HELIUS API KEY" field
   - Tap "Save API Key"
   - The key will be stored in app preferences

## API Features Implemented:

- **SOL Balance:** Fetches native Solana balance via `getBalance` RPC call
- **Token Accounts:** Retrieves SPL token holdings via `getTokenAccountsByOwner`
- **Token Metadata:** Fetches token metadata (name, symbol, logo) via Jupiter's token list API
- **NFTs:** Fetches NFT assets and metadata via Helius Digital Asset Standard (DAS) API using `getAssetsByOwner`

## Current Status:

- ✅ SOL balance fetching implemented
- ✅ Token account enumeration implemented  
- ✅ Token metadata fetching implemented via Jupiter API
- ✅ NFT fetching implemented via Helius DAS API

## Testing:

The wallet screen will display:
- Total SOL balance with refresh capability
- Token list with full metadata (name, symbol, logo, balance)
- NFT grid with images, names, and collection information
- Proper loading states and error handling

**Note:** Token metadata requires no API key (uses public Jupiter API), but NFT fetching requires a valid Helius API key.

## Security Notes:

- API calls are made over HTTPS
- No private keys are transmitted
- Only public wallet addresses are used for queries
- All data fetching is read-only