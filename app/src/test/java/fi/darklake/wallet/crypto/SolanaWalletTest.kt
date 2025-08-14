package fi.darklake.wallet.crypto

import org.junit.Test
import org.junit.Assert.*
import org.junit.Before

class SolanaWalletTest {

    @Test
    fun `generateMnemonic should return 12 words`() {
        val mnemonic = SolanaWalletManager.generateMnemonic()
        assertEquals(12, mnemonic.size)
    }

    @Test
    fun `generateMnemonic should return different mnemonics`() {
        val mnemonic1 = SolanaWalletManager.generateMnemonic()
        val mnemonic2 = SolanaWalletManager.generateMnemonic()
        
        assertNotEquals(mnemonic1, mnemonic2)
    }

    @Test
    fun `createWalletFromMnemonic should create valid wallet`() {
        val mnemonic = listOf(
            "abandon", "abandon", "abandon", "abandon", "abandon", "abandon",
            "abandon", "abandon", "abandon", "abandon", "abandon", "about"
        )
        
        val wallet = SolanaWalletManager.createWalletFromMnemonic(mnemonic)
        
        assertNotNull(wallet)
        assertNotNull(wallet.publicKey)
        assertNotNull(wallet.privateKey)
        assertEquals(mnemonic, wallet.mnemonic)
        assertTrue(wallet.privateKey.isNotEmpty())
        assertTrue(wallet.publicKey.isNotBlank())
    }

    @Test
    fun `createWalletFromMnemonic with same mnemonic should return same keys`() {
        val mnemonic = listOf(
            "abandon", "abandon", "abandon", "abandon", "abandon", "abandon",
            "abandon", "abandon", "abandon", "abandon", "abandon", "about"
        )
        
        val wallet1 = SolanaWalletManager.createWalletFromMnemonic(mnemonic)
        val wallet2 = SolanaWalletManager.createWalletFromMnemonic(mnemonic)
        
        assertEquals(wallet1.publicKey, wallet2.publicKey)
        assertArrayEquals(wallet1.privateKey, wallet2.privateKey)
    }

    @Test
    fun `validateMnemonic should return true for valid mnemonic`() {
        val validMnemonic = listOf(
            "abandon", "abandon", "abandon", "abandon", "abandon", "abandon",
            "abandon", "abandon", "abandon", "abandon", "abandon", "about"
        )
        
        assertTrue(SolanaWalletManager.validateMnemonic(validMnemonic))
    }

    @Test
    fun `validateMnemonic should return false for invalid mnemonic`() {
        val invalidMnemonic = listOf(
            "invalid", "mnemonic", "words", "that", "are", "not",
            "in", "the", "bip39", "word", "list", "test"
        )
        
        assertFalse(SolanaWalletManager.validateMnemonic(invalidMnemonic))
    }

    @Test
    fun `validateMnemonic should return false for wrong length`() {
        val shortMnemonic = listOf("abandon", "abandon", "abandon")
        val longMnemonic = listOf(
            "abandon", "abandon", "abandon", "abandon", "abandon", "abandon",
            "abandon", "abandon", "abandon", "abandon", "abandon", "abandon",
            "abandon", "abandon", "abandon"
        )
        
        assertFalse(SolanaWalletManager.validateMnemonic(shortMnemonic))
        assertFalse(SolanaWalletManager.validateMnemonic(longMnemonic))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `createWalletFromMnemonic should throw for invalid length`() {
        val shortMnemonic = listOf("abandon", "abandon", "abandon")
        SolanaWalletManager.createWalletFromMnemonic(shortMnemonic)
    }

    @Test
    fun `wallet equals should work correctly`() {
        val mnemonic = listOf(
            "abandon", "abandon", "abandon", "abandon", "abandon", "abandon",
            "abandon", "abandon", "abandon", "abandon", "abandon", "about"
        )
        
        val wallet1 = SolanaWalletManager.createWalletFromMnemonic(mnemonic)
        val wallet2 = SolanaWalletManager.createWalletFromMnemonic(mnemonic)
        val wallet3 = SolanaWalletManager.createWalletFromMnemonic(
            SolanaWalletManager.generateMnemonic()
        )
        
        assertEquals(wallet1, wallet2)
        assertNotEquals(wallet1, wallet3)
    }

    @Test
    fun `wallet hashCode should be consistent`() {
        val mnemonic = listOf(
            "abandon", "abandon", "abandon", "abandon", "abandon", "abandon",
            "abandon", "abandon", "abandon", "abandon", "abandon", "about"
        )
        
        val wallet1 = SolanaWalletManager.createWalletFromMnemonic(mnemonic)
        val wallet2 = SolanaWalletManager.createWalletFromMnemonic(mnemonic)
        
        assertEquals(wallet1.hashCode(), wallet2.hashCode())
    }
}