package com.greenbotz.greengate.core.session

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class IconSet { ZERO, CLAY, TWO, THREE }
enum class AppTheme { ZERO, ONE, TWO, THREE, FOUR }

/** Theme for installs that haven't picked one in Profile. */
private val DefaultTheme = AppTheme.FOUR
enum class LoginExperience { NEW, WHITE, CURRENT }

private const val PREFS_NAME = "greengate_prefs"
private const val KEY_ICON_SET = "icon_set"
private const val KEY_ICON_SET_VERSION = "icon_set_version"
private const val KEY_SHOW_FEEDBACK = "show_feedback"
private const val KEY_REFERENCE_HOME_ARTWORK = "reference_home_artwork"
private const val KEY_LOGIN_EXPERIENCE = "login_experience"
private const val KEY_GREEN_BOT_VOICE = "green_bot_voice"
private const val KEY_FAVOURITE_FACILITIES = "favourite_facilities"

object AppPreferences {
    var loginExperience by mutableStateOf(LoginExperience.NEW)
        private set

    fun setLoginExperience(context: Context, value: LoginExperience) {
        loginExperience = value
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_LOGIN_EXPERIENCE, value.name).apply()
    }

    var theme by mutableStateOf(DefaultTheme)
        private set

    fun setTheme(context: Context, value: AppTheme) {
        theme = value
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString("theme", value.name).apply()
    }
    var iconSet by mutableStateOf(IconSet.ZERO)
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

    /** Saved as [greenBotVoice] when the resident picks "Phone default" in Profile. */
    const val PhoneDefaultVoice = "phone-default"

    /**
     * Text-to-speech voice Green Bot reads replies with, picked in Profile. Null until one is picked,
     * when Green Bot uses its own default voice (see greenBotVoiceFor).
     */
    var greenBotVoice by mutableStateOf<String?>(null)
        private set

    fun setGreenBotVoice(context: Context, name: String?) {
        greenBotVoice = name
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_GREEN_BOT_VOICE, name).apply()
    }

    /**
     * Ids of the facilities the resident hearted on their About page or in Book Facility, which lists
     * them first. Kept on the device until residents have a profile store.
     */
    var favouriteFacilities by mutableStateOf<Set<String>>(emptySet())
        private set

    fun toggleFavouriteFacility(context: Context, id: String) {
        favouriteFacilities = if (id in favouriteFacilities) favouriteFacilities - id else favouriteFacilities + id
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putStringSet(KEY_FAVOURITE_FACILITIES, favouriteFacilities).apply()
    }

    fun load(context: Context) {
        val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        greenBotVoice = preferences.getString(KEY_GREEN_BOT_VOICE, null)
        // Copied: the set SharedPreferences returns mustn't be kept or changed.
        favouriteFacilities = preferences.getStringSet(KEY_FAVOURITE_FACILITIES, null)?.toSet() ?: emptySet()
        // A theme picked in Profile is kept; installs that never picked one get the default.
        theme = runCatching { AppTheme.valueOf(preferences.getString("theme", DefaultTheme.name)!!) }
            .getOrDefault(DefaultTheme)
        // The original flat set was saved as TWO. Keep existing users on that artwork.
        if (preferences.getInt(KEY_ICON_SET_VERSION, 1) < 2) {
            val previous = preferences.getString(KEY_ICON_SET, IconSet.ZERO.name)
            preferences.edit()
                .putString(KEY_ICON_SET, if (previous == IconSet.TWO.name) IconSet.THREE.name else previous)
                .putInt(KEY_ICON_SET_VERSION, 2).apply()
        }
        // Set 1 was removed; anyone who had it saved falls back to Set 0.
        val stored = preferences.getString(KEY_ICON_SET, IconSet.ZERO.name)
        iconSet = runCatching { IconSet.valueOf(stored ?: IconSet.ZERO.name) }.getOrDefault(IconSet.ZERO)
        useReferenceArtwork = preferences.getBoolean(KEY_REFERENCE_HOME_ARTWORK, true)
        showFeedback = preferences.getBoolean(KEY_SHOW_FEEDBACK, false)
        loginExperience = runCatching {
            LoginExperience.valueOf(preferences.getString(KEY_LOGIN_EXPERIENCE, LoginExperience.NEW.name)!!)
        }.getOrDefault(LoginExperience.NEW)
    }

    fun setIconSet(context: Context, set: IconSet) {
        iconSet = set
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_ICON_SET, set.name).putInt(KEY_ICON_SET_VERSION, 2).apply()
        if (theme == AppTheme.ZERO) setUseReferenceArtwork(context, false)
    }
}
