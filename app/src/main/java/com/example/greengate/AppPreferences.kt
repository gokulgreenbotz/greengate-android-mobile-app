package com.example.greengate

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class IconSet { ZERO, CLAY, ONE, TWO, THREE }
enum class AppTheme { ZERO, ONE }

private const val PREFS_NAME = "greengate_prefs"
private const val KEY_ICON_SET = "icon_set"
private const val KEY_ICON_SET_VERSION = "icon_set_version"
private const val KEY_SHOW_FEEDBACK = "show_feedback"
private const val KEY_REFERENCE_HOME_ARTWORK = "reference_home_artwork"

object AppPreferences {
    var theme by mutableStateOf(AppTheme.ZERO)
        private set

    fun setTheme(context: Context, value: AppTheme) {
        theme = value
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString("theme", value.name).apply()
    }
    var iconSet by mutableStateOf(IconSet.ONE)
        private set

    var useReferenceArtwork by mutableStateOf(true)
        private set

    fun setUseReferenceArtwork(context: Context, useReference: Boolean) {
        useReferenceArtwork = useReference
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_REFERENCE_HOME_ARTWORK, useReference).apply()
    }

    /** Community took Feedback's home-screen slot; Feedback shows only once enabled in Profile. */
    var showFeedback by mutableStateOf(false)
        private set

    fun setShowFeedback(context: Context, show: Boolean) {
        showFeedback = show
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_SHOW_FEEDBACK, show).apply()
    }

    fun load(context: Context) {
        val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        theme = runCatching { AppTheme.valueOf(preferences.getString("theme", AppTheme.ZERO.name)!!) }
            .getOrDefault(AppTheme.ZERO)
        // The original flat set was saved as TWO. Keep existing users on that artwork.
        if (preferences.getInt(KEY_ICON_SET_VERSION, 1) < 2) {
            val previous = preferences.getString(KEY_ICON_SET, IconSet.ONE.name)
            preferences.edit()
                .putString(KEY_ICON_SET, if (previous == IconSet.TWO.name) IconSet.THREE.name else previous)
                .putInt(KEY_ICON_SET_VERSION, 2).apply()
        }
        val stored = preferences.getString(KEY_ICON_SET, IconSet.ONE.name)
        iconSet = runCatching { IconSet.valueOf(stored ?: IconSet.ONE.name) }.getOrDefault(IconSet.ONE)
        useReferenceArtwork = preferences.getBoolean(KEY_REFERENCE_HOME_ARTWORK, true)
        showFeedback = preferences.getBoolean(KEY_SHOW_FEEDBACK, false)
    }

    fun setIconSet(context: Context, set: IconSet) {
        iconSet = set
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_ICON_SET, set.name).putInt(KEY_ICON_SET_VERSION, 2).apply()
        if (theme == AppTheme.ZERO) setUseReferenceArtwork(context, false)
    }
}
