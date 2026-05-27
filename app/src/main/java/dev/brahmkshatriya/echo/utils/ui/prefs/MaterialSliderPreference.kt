package dev.brahmkshatriya.echo.utils.ui.prefs

import android.app.AlertDialog
import android.content.Context
import android.widget.EditText
import androidx.core.content.edit
import androidx.preference.Preference
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.brahmkshatriya.echo.R
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class MaterialSliderPreference(
    context: Context,
    private val from: Int,
    private val to: Int,
    private val steps: Int? = null,
    private val allowOverride: Boolean = false
) : Preference(context) {
    init {
        layoutResource = R.layout.preference
    }

    private var customSummary: CharSequence? = null
    private var defaultValue: Int? = null

    override fun onSetInitialValue(defaultValue: Any?) {
        customSummary = summary
        this.defaultValue = defaultValue as? Int
        updateSummary()
    }

    override fun onClick() {
        val current = getPersistedIntSafely(defaultValue ?: from)
        val min = if (allowOverride) min(from, current) else from
        var max = if (allowOverride) max(to, current) else to
        if (max <= min) {
            max = min + 1
        }
        val step = if (allowOverride) 0f else steps?.toFloat() ?: 1f
        val stepSize = if (step > 0f) {
            val range = max - min
            if (range % step == 0f) {
                step
            } else {
                1f
            }
        } else 0f
        showValueDialog(alignToStep(current, min, max, stepSize), min, max, stepSize)
    }

    private var dialogOpened = false
    private fun showValueDialog(value: Int, min: Int, max: Int, step: Float) {
        dialogOpened = true
        val dialog = MaterialAlertDialogBuilder(context)
            .setView(R.layout.item_edit_text)
            .setPositiveButton(R.string.okay, null)
            .setNegativeButton(R.string.cancel, null)
            .setTitle(title)
            .create()

        dialog.setOnShowListener {
            val editText = dialog.findViewById<EditText>(R.id.edit_text)
            editText?.inputType = android.text.InputType.TYPE_CLASS_NUMBER
            editText?.setText(value.toString())
            editText?.hint = customSummary

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val newValue = editText?.text?.toString()?.toIntOrNull()
                if (newValue != null && newValue in min..max) {
                    persistInt(alignToStep(newValue, min, max, step))
                    updateSummary()
                    dialog.dismiss()
                } else {
                    editText?.error = context.getString(R.string.error_x, "$min - $max")

                }
            }

            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener {
                dialog.dismiss()
            }
        }
        dialog.setOnDismissListener { dialogOpened = false }
        dialog.show()
    }

    private fun updateSummary() {
        val value = context.getString(R.string.value)
        val entry = getPersistedIntSafely(defaultValue ?: 0)
        val sum = customSummary?.let { "\n\n$it" } ?: ""
        summary = "$value : $entry$sum".trimIndent()
    }

    private fun getPersistedIntSafely(default: Int): Int {
        return runCatching { getPersistedInt(default) }
            .getOrElse {
                sharedPreferences?.edit { remove(key) }
                default
            }
    }

    private fun alignToStep(value: Int, min: Int, max: Int, step: Float): Int {
        val coerced = value.coerceIn(min, max)
        if (step <= 0f) return coerced
        val offset = ((coerced - min) / step).roundToInt()
        return (min + offset * step.toInt()).coerceIn(min, max)
    }
}
