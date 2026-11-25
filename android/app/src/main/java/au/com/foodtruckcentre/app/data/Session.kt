package au.com.foodtruckcentre.app.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

@Serializable
data class Profile(val name: String = "", val email: String = "", val phone: String = "")

@Serializable
data class Session(val token: String, val profile: Profile)

/**
 * Keeps the logged-in customer's session token and profile on the device,
 * encrypted with an AES-GCM key held in the Android Keystore (the key never leaves secure hardware where available).
 * Nothing else from the forms is stored.
 */
class SessionStore private constructor(context: Context) {

    private val prefs = context.getSharedPreferences("ftc_session", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val _session = MutableStateFlow(load())
    val session: StateFlow<Session?> = _session.asStateFlow()

    val token: String? get() = _session.value?.token

    fun save(session: Session) {
        _session.value = session
        runCatching {
            prefs.edit().putString(KEY, encrypt(json.encodeToString(Session.serializer(), session))).apply()
        }
    }

    fun updateProfile(profile: Profile) {
        val current = _session.value ?: return
        save(current.copy(profile = profile))
    }

    fun clear() {
        _session.value = null
        prefs.edit().remove(KEY).apply()
    }

    private fun load(): Session? = runCatching {
        val stored = prefs.getString(KEY, null) ?: return null
        json.decodeFromString(Session.serializer(), decrypt(stored))
    }.getOrElse {
        prefs.edit().remove(KEY).apply() // unreadable (e.g. key reset) – start logged out
        null
    }

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return gen.generateKey()
    }

    private fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val ct = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" + Base64.encodeToString(ct, Base64.NO_WRAP)
    }

    private fun decrypt(stored: String): String {
        val (iv, ct) = stored.split(":", limit = 2).map { Base64.decode(it, Base64.NO_WRAP) }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
        return String(cipher.doFinal(ct), Charsets.UTF_8)
    }

    companion object {
        private const val KEY = "session"
        private const val ALIAS = "ftc_session_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"

        @Volatile private var instance: SessionStore? = null
        fun get(context: Context): SessionStore =
            instance ?: synchronized(this) { instance ?: SessionStore(context.applicationContext).also { instance = it } }
    }
}
