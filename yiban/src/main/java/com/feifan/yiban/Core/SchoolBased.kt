package com.feifan.yiban.Core

import java.security.SecureRandom

object SchoolBased {
    private val token: String by lazy { generateToken() }

    @JvmStatic
    fun csrf(): String {
        return token
    }

    private fun generateToken(): String {
        fun bytesToHex(bytes: ByteArray): String {
            val hexChars = CharArray(bytes.size * 2)
            for (i in bytes.indices) {
                val v = bytes[i].toInt() and 0xFF
                hexChars[i * 2] = "0123456789ABCDEF"[v ushr 4]
                hexChars[i * 2 + 1] = "0123456789ABCDEF"[v and 0x0F]
            }
            return String(hexChars)
        }

        val random = SecureRandom()
        val tokenBytes = ByteArray(16)
        random.nextBytes(tokenBytes)
        val generatedToken = bytesToHex(tokenBytes)
        return generatedToken
    }

    @JvmStatic
    fun headers(): Map<String, String> = mapOf(
        "Origin" to "https://c.uyiban.com",
        "User-Agent" to "Yiban",
        "AppVersion" to "5.1.2"
    )
}
