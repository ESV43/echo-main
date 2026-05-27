package dev.brahmkshatriya.echo.utils.ui.prefs

import android.content.Context
import androidx.core.content.edit
import androidx.preference.MultiSelectListPreference
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.brahmkshatriya.echo.R

class MaterialMultipleChoicePreference(context: Context) : MultiSelectListPreference(context) {

    private var customSummary: CharSequence? = null

    override fun onSetInitialValue(defaultValue: Any?) {
        runCatching { super.onSetInitialValue(defaultValue) }
            .onFailure {
                sharedPreferences?.edit { remove(key) }
                values = (defaultValue as? Set<*>)?.filterIsInstance<String>()?.toSet() ?: emptySet()
            }
        customSummary = summary
        updateSummary()
    }

    override fun onClick() {
        val currentValues = values.toMutableSet()
        val selectedIndices = BooleanArray(entries.size) { index ->
            currentValues.contains(entryValues.getOrNull(index))
        }

        MaterialAlertDialogBuilder(context)
            .setMultiChoiceItems(entries, selectedIndices) { _, which, isChecked ->
                val value = entryValues.getOrNull(which)
                if (isChecked && value != null) {
                    currentValues.add(value.toString())
                } else {
                    currentValues.remove(value?.toString())
                }
            }
            .setPositiveButton(R.string.okay) { dialog, _ ->
                if (callChangeListener(currentValues)) {
                    setValues(currentValues)
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
        val entry = values.takeIf { it.isNotEmpty() }?.joinToString(", ") { v ->
            val index = entryValues.indexOf(v)
            if (index >= 0) entries.getOrNull(index).toString() else v
        } ?: context.getString(R.string.value_not_set)
        val sum = customSummary?.let { "\n\n$it" } ?: ""
        summary = "$value : $entry$sum"
    }
}
