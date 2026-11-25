package com.example.meuassistente.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class ApiKeyVault(context: Context) {

    private val prefs: SharedPreferences by lazy {
        runCatching {
            val masterKey = MasterKey.Builder(context.applicationContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context.applicationContext,
                "meu_assistente_secure_vault",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }.getOrElse {
            context.applicationContext.getSharedPreferences(
                "meu_assistente_fallback_vault",
                Context.MODE_PRIVATE
            )
        }
    }

    fun saveOptionalProviderKey(key: String?) {
        val normalized = key?.trim().orEmpty()
        if (normalized.isEmpty()) {
            clearProviderKey()
        } else {
            prefs.edit().putString(KEY_AI_PROVIDER_API, normalized).apply()
        }
    }

    fun getOptionalProviderKey(): String? {
        val value = prefs.getString(KEY_AI_PROVIDER_API, null)?.trim()
        return if (value.isNullOrEmpty()) null else value
    }

    fun clearProviderKey() {
        prefs.edit().remove(KEY_AI_PROVIDER_API).apply()
    }

    companion object {
        private const val KEY_AI_PROVIDER_API = "ai_provider_api_key"
    }
}
