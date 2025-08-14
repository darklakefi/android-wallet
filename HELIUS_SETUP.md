# Helius API Setup

To enable wallet balance, token, and NFT fetching, you need to configure the Helius API:

## Steps:

1. **Get Helius API Key:**
   - Go to [helius.dev](https://www.helius.dev/)
   - Sign up for an account
   - Create a new project
   - Copy your API key

2. **Configure API Key:**
   - Open `app/src/main/java/fi/darklake/wallet/data/api/HeliusApiService.kt`
   - Replace `"your-helius-api-key-here"` with your actual API key
   - **Important:** In production, store this securely using Android secrets or environment variables

## API Features Implemented:

- **SOL Balance:** Fetches native Solana balance via `getBalance` RPC call
- **Token Accounts:** Retrieves SPL token holdings via `getTokenAccountsByOwner`
- **NFTs:** Prepared for Digital Asset Standard (DAS) API integration via `getAssetsByOwner`

## Current Status:

- ✅ SOL balance fetching implemented
- ✅ Token account enumeration implemented  
- ⚠️ Token metadata fetching needs completion
- ⚠️ NFT fetching needs DAS API response parsing

## Testing:

The wallet screen will display:
- Total SOL balance with refresh capability
- Token list (currently shows balance without metadata)
- NFT grid (placeholder until metadata implemented)
- Proper loading states and error handling

## Security Notes:

- API calls are made over HTTPS
- No private keys are transmitted
- Only public wallet addresses are used for queries
- All data fetching is read-only