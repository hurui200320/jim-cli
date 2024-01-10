package info.skyblond.jim.http

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

fun sha256KeyGen(input: String): SecretKey {
    val md = MessageDigest.getInstance("SHA-256")
    val hash = md.digest(input.toByteArray())
    return SecretKeySpec(hash, "AES")
}

private val secureRandom = SecureRandom()

fun SecretKey.encrypt(input: ByteArray): ByteArray {
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    // random iv to initialize cipher
    val iv = ByteArray(12).also { secureRandom.nextBytes(it) }
    cipher.init(Cipher.ENCRYPT_MODE, this, GCMParameterSpec(128, iv))
    // alloc result buffer, first 12 bytes (96bits) is IV/Nonce
    val result = ByteArray(12 + cipher.getOutputSize(input.size))
    // here we only copy 12 bytes of data
    iv.copyInto(result, 0, 0, 12)
    // directly output cipher into result buffer
    cipher.doFinal(input, 0, input.size, result, 12)
    return result
}

fun SecretKey.decrypt(input: ByteArray): ByteArray {
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(
        Cipher.DECRYPT_MODE, this,
        GCMParameterSpec(128, input, 0, 12)
    )
    return cipher.doFinal(input, 12, input.size - 12)
}
