package com.processorgateway

import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

class TripleDesCipher(key: ByteArray) {
    private val encrypter: Cipher
    private val decrypter: Cipher

    init {
        Security.addProvider(BouncyCastleProvider())
        val ALGORITHM = "DESede"
        val keySpec: SecretKey = SecretKeySpec(key, ALGORITHM)
        val BOUNCY_CASTLE_PROVIDER = "BC"
        val TRIPLE_DES_TRANSFORMATION = "DESede/ECB/Nopadding"
        encrypter = Cipher.getInstance(TRIPLE_DES_TRANSFORMATION, BOUNCY_CASTLE_PROVIDER)
        encrypter.init(Cipher.ENCRYPT_MODE, keySpec)
        decrypter = Cipher.getInstance(TRIPLE_DES_TRANSFORMATION, BOUNCY_CASTLE_PROVIDER)
        decrypter.init(Cipher.DECRYPT_MODE, keySpec)
    }

    @Throws(IllegalBlockSizeException::class, BadPaddingException::class)
    fun encode(input: ByteArray): ByteArray {
        return encrypter.doFinal(input)
    }

    @Throws(IllegalBlockSizeException::class, BadPaddingException::class)
    fun decode(input: ByteArray): ByteArray {
        return decrypter.doFinal(input)
    }
}
