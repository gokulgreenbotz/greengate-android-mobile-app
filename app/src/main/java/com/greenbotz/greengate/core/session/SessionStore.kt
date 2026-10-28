package com.greenbotz.greengate.core.session

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private const val PREFS_NAME = "resident_session"
private const val KEY_REFRESH_TOKEN = "refresh_token"
private const val KEYSTORE = "AndroidKeyStore"
private const val KEY_ALIAS = "resident_session_key"
private const val TRANSFORMATION = "AES/GCM/NoPadding"
private const val IV_BYTES = 12

/**
 * What the resident sign-in keeps on the phone. The refresh token is encrypted with an
 * Android Keystore key that never leaves the device; the access token is only held in
 * memory by [ResidentSession]. The prefs file is excluded from backups (res/xml).
 * Call from a background thread.
 */
internal class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Null when signed out, or when the Keystore key is gone (e.g. data restored to a new phone). */
    var refreshToken: String?
        get() = prefs.getString(KEY_REFRESH_TOKEN, null)?.let(::decrypt)
        // commit, not apply: each refresh token works once, so a rotated one must not be lost.
        set(value) {
            prefs.edit().apply {
                if (value == null) remove(KEY_REFRESH_TOKEN) else putString(KEY_REFRESH_TOKEN, encrypt(value))
            }.commit()
        }

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )
        return generator.generateKey()
    }

    private fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key()) }
        val sealed = cipher.iv + cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(sealed, Base64.NO_WRAP)
    }

    private fun decrypt(stored: String): String? = runCatching {
        val sealed = Base64.decode(stored, Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, sealed, 0, IV_BYTES))
        String(cipher.doFinal(sealed, IV_BYTES, sealed.size - IV_BYTES), Charsets.UTF_8)
    }.getOrNull()
}
