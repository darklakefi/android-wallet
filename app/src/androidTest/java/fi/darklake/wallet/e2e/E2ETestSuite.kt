package fi.darklake.wallet.e2e

import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * Test suite that runs all end-to-end tests
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    WalletCreationE2ETest::class,
    WalletMainE2ETest::class,
    SwapE2ETest::class,
    LiquidityE2ETest::class
)
class E2ETestSuite