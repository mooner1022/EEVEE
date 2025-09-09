package dev.mooner.eevee.utils

import android.util.Base64
import kr.go.mnd.mmsa.mnt.config.ConfigData
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object CipherUtils {

    fun decodeMDMAes(content: String, isSms: Boolean = false): String {
        return try {
            val initBytes = ByteArray(16)
            System.arraycopy(ConfigData.getinit(), 0, initBytes, 0, 16)

            val keySpec = SecretKeySpec(if (isSms) ConfigData.getsms() else ConfigData.getCommon(), "AES")
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding").apply {
                init(2, keySpec, IvParameterSpec(initBytes, 0, 16))
            }
            cipher.doFinal(Base64.decode(content.toByteArray(Charsets.UTF_8), 2)).toString(Charsets.UTF_8)
        } catch (e: Exception) {
            throw Exception("Failed to decode MDM-AES: " + e.message, e)
        }
    }
}