package com.example.meuassistente.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeModePref {
    SYSTEM,
    LIGHT,
    DARK
}

data class UserPreferences(
    val themeMode: ThemeModePref = ThemeModePref.SYSTEM,
    val fontScale: Float = 1.0f
)

private val Context.appPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "meu_assistente_prefs"
)

class AppPreferencesStore(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val FONT_SCALE = floatPreferencesKey("font_scale")
    }

    val preferencesFlow: Flow<UserPreferences> = context.appPreferencesDataStore.data.map { prefs ->
        val mode = prefs[Keys.THEME_MODE]?.let { raw ->
            runCatching { ThemeModePref.valueOf(raw) }.getOrDefault(ThemeModePref.SYSTEM)
        } ?: ThemeModePref.SYSTEM
        val scale = (prefs[Keys.FONT_SCALE] ?: 1.0f).coerceIn(0.85f, 1.35f)
        UserPreferences(themeMode = mode, fontScale = scale)
    }

    suspend fun setThemeMode(mode: ThemeModePref) {
        context.appPreferencesDataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.name
        }
    }

    suspend fun setFontScale(scale: Float) {
        context.appPreferencesDataStore.edit { prefs ->
            prefs[Keys.FONT_SCALE] = scale.coerceIn(0.85f, 1.35f)
        }
    }
}
