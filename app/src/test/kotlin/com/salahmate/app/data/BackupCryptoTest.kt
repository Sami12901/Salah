package com.salahmate.app.data

import com.salahmate.app.core.data.Crypto
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Backup envelope: AES-256-GCM with a PBKDF2-derived key. */
class BackupCryptoTest {

    private val plain = """{"version":1,"bookmarks":[{"surah":2,"ayah":255}]}""".toByteArray()

    @Test
    fun `encrypt then decrypt round trips`() {
        val sealed = Crypto.encrypt(plain, "correct horse battery")
        assertArrayEquals(plain, Crypto.decrypt(sealed, "correct horse battery"))
    }

    @Test
    fun `ciphertext is marked as an encrypted backup`() {
        val sealed = Crypto.encrypt(plain, "pw")
        assertTrue(Crypto.looksEncrypted(sealed))
        assertFalse(Crypto.looksEncrypted(plain))
    }

    @Test
    fun `the same payload encrypted twice is different`() {
        val a = Crypto.encrypt(plain, "pw")
        val b = Crypto.encrypt(plain, "pw")
        assertNotEquals(a.toList(), b.toList())
    }

    @Test
    fun `plaintext is not readable in the sealed payload`() {
        val sealed = Crypto.encrypt(plain, "pw")
        val asText = String(sealed, Charsets.ISO_8859_1)
        assertFalse(asText.contains("bookmarks"))
        assertFalse(asText.contains("255"))
    }

    @Test(expected = Exception::class)
    fun `wrong password fails authentication`() {
        val sealed = Crypto.encrypt(plain, "right")
        Crypto.decrypt(sealed, "wrong")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `decrypting a non encrypted file is rejected`() {
        Crypto.decrypt(plain, "pw")
    }
}
