package dev.brahmkshatriya.echo.utils.ui.prefs

import android.content.Context
import androidx.core.content.edit
import androidx.preference.SwitchPreferenceCompat

class MaterialSwitchPreference(context: Context) : SwitchPreferenceCompat(context) {
    override fun onSetInitialValue(defaultValue: Any?) {
        runCatching { super.onSetInitialValue(defaultValue) }
            .onFailure {
                sharedPreferences?.edit { remove(key) }
                isChecked = defaultValue as? Boolean ?: false
            }
    }
}
