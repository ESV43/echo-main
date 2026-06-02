package dev.brahmkshatriya.echo.utils.ui.prefs

import android.content.Context
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder

class TransitionPreference(
    context: Context
) : Preference(context) {
    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        holder.itemView.transitionName = key
    }
}