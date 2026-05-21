package com.baby.feedingtracker.ui.widget

import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object WidgetPreferenceKeys {
    val LAST_FEEDING_TS   = longPreferencesKey("last_feeding_ts")
    val LAST_FEEDING_TYPE = stringPreferencesKey("last_feeding_type")
}
