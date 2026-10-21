package ru.urent.qatools.core_platform

import com.sun.jna.platform.win32.Crypt32Util

internal class DpapiTokenCipher : TokenCipher {
    override fun encrypt(plain: ByteArray): ByteArray = Crypt32Util.cryptProtectData(plain)

    override fun decrypt(encrypted: ByteArray): ByteArray = Crypt32Util.cryptUnprotectData(encrypted)
}
