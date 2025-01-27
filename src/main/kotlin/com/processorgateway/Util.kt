package com.processorgateway

import org.jpos.iso.ISOMsg
import org.jpos.iso.ISOUtil
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.security.NoSuchProviderException
import java.util.*
import javax.crypto.BadPaddingException
import javax.crypto.IllegalBlockSizeException
import javax.crypto.NoSuchPaddingException

class Util {
    companion object {
        fun createTMKIsoMessage(transDate: String, transTime: String, transDateTime: String): ISOMsg {
            val isoMessage = ISOMsg()
            isoMessage.setMTI("0800")
            isoMessage.set(3, "9A0000")
            isoMessage.set(7, transDateTime)
            isoMessage.set(11, generateStan())
            isoMessage.set(12, transTime)
            isoMessage.set(13, transDate)
            isoMessage.set(41, "2PSTAD61")
            return isoMessage
        }

        fun generateStan(): String {
            val random = Random()
            val stan = random.nextInt(999999)
            return ISOUtil.zeropad(stan.toString(), 6)
        }

        private fun fromHex(c: Char): Int {
            if (c in '0'..'9') {
                return c.code - '0'.code
            }
            if (c in 'A'..'F') {
                return c.code - 'A'.code + 10
            }
            if (c in 'a'..'f') {
                return c.code - 'a'.code + 10
            }
            throw IllegalArgumentException()
        }

        private fun toHex(nybble: Int): Char {
            require(!(nybble < 0 || nybble > 15))
            return "0123456789ABCDEF"[nybble]
        }

        private fun getXorOfKeyComponents(componentKey1: String, componentKey2: String): String {
            val chars = CharArray(componentKey1.length)
            for (i in chars.indices) {
                chars[i] =
                    toHex(fromHex(componentKey1[i]) xor fromHex(componentKey2[i]))
            }
            return String(chars)
        }


        @Throws(
            NoSuchPaddingException::class,
            NoSuchAlgorithmException::class,
            InvalidKeyException::class,
            NoSuchProviderException::class,
            BadPaddingException::class,
            IllegalBlockSizeException::class
        )
        fun getDecryptedTMKFromHost(fld53: String, componentKey1: String, componentKey2: String): String {

            val encryptedTmk = fld53.substring(0, 32)

            val bytesXORKeyComponents =
                ISOUtil.hex2byte(getXorOfKeyComponents(componentKey1, componentKey2))
            val bytesEncryptedTmk = ISOUtil.hex2byte(encryptedTmk)

            val cipher4KeyDecryption = TripleDesCipher(bytesXORKeyComponents)

            val plainKey: ByteArray = cipher4KeyDecryption.decode(bytesEncryptedTmk)
            return ISOUtil.hexString(plainKey)
        }
    }
}
