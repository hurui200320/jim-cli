package info.skyblond.jim.http

import org.junit.jupiter.api.Test
import javax.crypto.spec.SecretKeySpec
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class CryptoTest {
    @Test
    fun `test sha`() {
        val string1 = "hello1"
        val key1 = (sha256KeyGen(string1) as SecretKeySpec).encoded
        val string2 = "hello2"
        val key2 = (sha256KeyGen(string2) as SecretKeySpec).encoded
        assertFalse("Different input gives same sha256") { key1.contentEquals(key2) }
    }

    @Test
    fun `test encryption and decryption`() {
        val key = sha256KeyGen("test key")
        val message = "Hello"
        val cipher = key.encrypt(message.toByteArray())
        val plain = key.decrypt(cipher).decodeToString()
        assertEquals(message, plain)
    }
}
