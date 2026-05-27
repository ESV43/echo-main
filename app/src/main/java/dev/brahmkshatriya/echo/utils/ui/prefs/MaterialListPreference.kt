package dev.brahmkshatriya.echo.utils.ui.prefs

import android.content.Context
import androidx.core.content.edit
import androidx.preference.ListPreference
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.brahmkshatriya.echo.R

class MaterialListPreference(context: Context) : ListPreference(context) {

    private var customSummary: CharSequence? = null

    override fun onSetInitialValue(defaultValue: Any?) {
        runCatching { super.onSetInitialValue(defaultValue) }
            .onFailure {
                sharedPreferences?.edit { remove(key) }
                value = defaultValue as? String
            }
        customSummary = summary
        updateSummary()
    }

    override fun onClick() {
        val currentEntryValues = entryValues ?: emptyArray()
        val currentEntries = entries ?: emptyArray()
        val selectedIndex = currentEntryValues.indexOf(value)
        MaterialAlertDialogBuilder(context)
            .setSingleChoiceItems(currentEntries, selectedIndex) { dialog, index ->
                val newValue = currentEntryValues.getOrNull(index)?.toString()
                if (newValue != null && callChangeListener(newValue)) runCatching {
                    setValueIndex(index)
                    updateSummary()
                }
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .setTitle(title)
            .create()
            .show()
    }

    private fun updateSummary() {
        val value = context.getString(R.string.value)
        val entry = entry.takeIf { !it.isNullOrEmpty() } ?: context.getString(R.string.value_not_set)
        val sum = customSummary?.let { "\n\n$it" } ?: ""
        summary = "$value : $entry$sum"
    }
}
