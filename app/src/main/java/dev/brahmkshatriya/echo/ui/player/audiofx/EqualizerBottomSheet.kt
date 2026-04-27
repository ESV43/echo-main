package dev.brahmkshatriya.echo.ui.player.audiofx

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.edit
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.databinding.DialogPlayerEqualizerBinding
import dev.brahmkshatriya.echo.databinding.ItemEqualizerBandBinding
import dev.brahmkshatriya.echo.playback.PlayerService.Companion.EQ_GAINS
import dev.brahmkshatriya.echo.ui.player.PlayerViewModel
import dev.brahmkshatriya.echo.utils.ui.AutoClearedValue.Companion.autoCleared
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class EqualizerBottomSheet : BottomSheetDialogFragment() {
    var binding by autoCleared<DialogPlayerEqualizerBinding>()
    private val viewModel by activityViewModel<PlayerViewModel>()

    private val bands = listOf("31", "62", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")
    private var currentGains = FloatArray(bands.size)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = DialogPlayerEqualizerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        loadGains()
        val adapter = EqualizerAdapter()
        binding.eqRecyclerView.adapter = adapter
        adapter.submitList(bands.indices.map { it })

        binding.topAppBar.setNavigationOnClickListener { dismiss() }
        binding.topAppBar.setOnMenuItemClickListener {
            if (it.itemId == R.id.menu_reset) {
                currentGains = FloatArray(bands.size)
                saveGains()
                adapter.notifyDataSetChanged()
                true
            } else false
        }
        binding.btnDone.setOnClickListener { dismiss() }
    }

    private fun loadGains() {
        val gainsStr = viewModel.settings.getString(EQ_GAINS, null)
        val gains = gainsStr?.split(",")?.mapNotNull { it.toFloatOrNull() }?.toFloatArray()
        if (gains != null && gains.size == bands.size) {
            currentGains = gains
        }
    }

    private fun saveGains() {
        val gainsStr = currentGains.joinToString(",")
        viewModel.settings.edit { putString(EQ_GAINS, gainsStr) }
    }

    inner class EqualizerAdapter : ListAdapter<Int, EqualizerAdapter.ViewHolder>(DiffCallback) {
        inner class ViewHolder(val binding: ItemEqualizerBandBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(ItemEqualizerBandBinding.inflate(layoutInflater, parent, false))
        }

        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.binding.frequencyValue.text = bands[position]
            holder.binding.bandSlider.value = currentGains[position]
            holder.binding.gainValue.text = "%.1f dB".format(currentGains[position])
            
            holder.binding.bandSlider.addOnChangeListener { _, value, fromUser ->
                if (fromUser) {
                    currentGains[position] = value
                    holder.binding.gainValue.text = "%.1f dB".format(value)
                    saveGains()
                }
            }
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<Int>() {
        override fun areItemsTheSame(oldItem: Int, newItem: Int) = oldItem == newItem
        override fun areContentsTheSame(oldItem: Int, newItem: Int) = true
    }
}