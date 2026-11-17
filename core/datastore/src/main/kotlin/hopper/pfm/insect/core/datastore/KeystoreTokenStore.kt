package hopper.pfm.insect.core.datastore

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import hopper.pfm.insect.core.common.AuthTokens
import hopper.pfm.insect.core.common.TokenStore
import kotlinx.coroutines.flow.first
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tokens are encrypted with AES-GCM using a key stored in the Android Keystore (it cannot be exported), and only
 * the encrypted text is written to DataStore. If the key is lost or the data cannot be decrypted (for example after a
 * restore on another device) the tokens count as absent and the user signs in again.
 */
@Singleton
class KeystoreTokenStore @Inject constructor(
    private val store: DataStore<Preferences>,
) : TokenStore {

    override suspend fun get(): AuthTokens? {
        val stored = store.data.first()[TOKENS] ?: return null
        return try {
            val parts = decrypt(stored).split(SEPARATOR)
            if (parts.size == 2) AuthTokens(parts[0], parts[1]) else null
        } catch (e: java.security.GeneralSecurityException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    override suspend fun save(tokens: AuthTokens) {
        val encrypted = encrypt(tokens.accessToken + SEPARATOR + tokens.refreshToken)
        store.edit { it[TOKENS] = encrypted }
    }

    override suspend fun clear() {
        store.edit { it.remove(TOKENS) }
    }

    private fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val body = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(cipher.iv + body, Base64.NO_WRAP)
    }

    private fun decrypt(encoded: String): String {
        val bytes = Base64.decode(encoded, Base64.NO_WRAP)
        val iv = bytes.copyOfRange(0, IV_BYTES)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, iv))
        return String(cipher.doFinal(bytes, IV_BYTES, bytes.size - IV_BYTES), Charsets.UTF_8)
    }

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "pfm_token_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
        const val TAG_BITS = 128
        const val SEPARATOR = "\n"
        val TOKENS = stringPreferencesKey("tokens_encrypted")
    }
}
