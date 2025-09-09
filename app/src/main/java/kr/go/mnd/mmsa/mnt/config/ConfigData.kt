package kr.go.mnd.mmsa.mnt.config

class ConfigData {
    companion object {
        init {
            System.loadLibrary("config")
        }

        @JvmStatic
        external fun getCommon(): ByteArray

        @JvmStatic
        external fun getHW(): String

        @JvmStatic
        external fun getKnoxBackward(): String

        @JvmStatic
        external fun getKnoxsdk(): String

        @JvmStatic
        external fun getLG(): String

        @JvmStatic
        external fun getMI(): String

        @JvmStatic
        external fun getOP(): String

        @JvmStatic
        external fun getRDataApk(): String

        @JvmStatic
        external fun getRDataApp(): String

        @JvmStatic
        external fun getRDataPath(): String

        @JvmStatic
        external fun getSG(): String

        @JvmStatic
        external fun getSS(): String

        @JvmStatic
        external fun getVI(): String

        @JvmStatic
        external fun getbdata(): ByteArray

        @JvmStatic
        external fun getinit(): ByteArray

        @JvmStatic
        external fun getiniv(): ByteArray

        @JvmStatic
        external fun getnnfc(): ByteArray

        @JvmStatic
        external fun getsms(): ByteArray

        @JvmStatic
        external fun getsnfc(): ByteArray
    }
}