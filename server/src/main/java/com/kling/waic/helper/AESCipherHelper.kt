package com.kling.waic.helper

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

@Component
class AESCipherHelper(
    @Value("\${waic.cipher.key}")
    private val cipherKey: String,
    @Value("\${waic.cipher.iv}")
    private val cipherIV: String,
) {
    private fun getKeySpec(): SecretKeySpec {
        return SecretKeySpec(cipherKey.toByteArray(Charsets.UTF_8), "AES")
    }

    private fun getIvSpec(): IvParameterSpec {
        return IvParameterSpec(cipherIV.toByteArray(Charsets.UTF_8))
    }

    fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, getKeySpec(), getIvSpec())
        val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(encryptedBytes)
    }

    fun decrypt(cipherText: String): String {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, getKeySpec(), getIvSpec())
        val decryptedBytes = cipher.doFinal(Base64.getUrlDecoder().decode(cipherText))
        return String(decryptedBytes, Charsets.UTF_8)
    }

    fun hmacSHA256(data: String): String {
        val algorithm = "HmacSHA256"
        val keySpec = SecretKeySpec(cipherKey.toByteArray(Charsets.UTF_8), algorithm)
        val mac = Mac.getInstance(algorithm)
        mac.init(keySpec)
        val hmacBytes = mac.doFinal(data.toByteArray(Charsets.UTF_8))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hmacBytes)
    }
}