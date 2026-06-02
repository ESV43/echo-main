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
import com.google.android.material.chip.Chip
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

    private val presets = mapOf(
        "Custom" to null,
        "Flat" to floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
        "Classical" to floatArrayOf(5f, 3f, 2f, 2f, -2f, -2f, -1f, 2f, 4f, 5f),
        "Dance" to floatArrayOf(6f, 5f, 0f, -2f, -2f, 0f, 4f, 5f, 6f, 0f),
        "Heavy Metal" to floatArrayOf(4f, 5f, 6f, 3f, -2f, 2f, 5f, 1f, 1f, 0f),
        "Hip Hop" to floatArrayOf(5f, 3f, 0f, 3f, -1f, -1f, 2f, -1f, 3f, 5f),
        "Jazz" to floatArrayOf(4f, 2f, 1f, 2f, -2f, -2f, 0f, 2f, 4f, 4f),
        "Pop" to floatArrayOf(-2f, -1f, 0f, 2f, 4f, 4f, 2f, 0f, -1f, -2f),
        "Rock" to floatArrayOf(5f, 4f, 3f, 1f, -1f, -1f, 1f, 3f, 4f, 5f),
        "Vocal Booster" to floatArrayOf(-2f, -3f, -3f, 1f, 4f, 4f, 5f, 3f, 1f, -1f)
    )

    private val adapter by lazy { EqualizerAdapter() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = DialogPlayerEqualizerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        loadGains()
        binding.eqRecyclerView.adapter = adapter
        adapter.submitList(bands.indices.map { it })

        setupPresets()

        binding.topAppBar.setNavigationOnClickListener { dismiss() }
        binding.topAppBar.setOnMenuItemClickListener {
            if (it.itemId == R.id.menu_reset) {
                currentGains = FloatArray(bands.size)
                saveGains()
                adapter.notifyDataSetChanged()
                updatePresetChips()
                true
            } else false
        }
        binding.btnDone.setOnClickListener { dismiss() }
    }

    private fun setupPresets() {
        binding.presetChipGroup.removeAllViews()
        var matched = false
        presets.keys.forEach { name ->
            val chip = Chip(requireContext()).apply {
                text = name
                isCheckable = true
                isCheckedIconVisible = false
                
                val presetGains = presets[name]
                if (presetGains != null && currentGains.contentEquals(presetGains)) {
                    isChecked = true
                    matched = true
                } else if (presetGains == null && name == "Custom") {
                    isChecked = !matched
                }
                
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        val gains = presets[name]
                        if (gains != null && !currentGains.contentEquals(gains)) {
                            currentGains = gains.copyOf()
                            saveGains()
                            adapter.notifyDataSetChanged()
                            updatePresetChips()
                        }
                    }
                }
            }
            binding.presetChipGroup.addView(chip)
        }
    }

    private fun updatePresetChips() {
        var matched = false
        for (i in 0 until binding.presetChipGroup.childCount) {
            val chip = binding.presetChipGroup.getChildAt(i) as? Chip ?: continue
            val name = chip.text.toString()
            val gains = presets[name]
            if (gains != null && currentGains.contentEquals(gains)) {
                chip.isChecked = true
                matched = true
            } else if (gains == null && name == "Custom") {
                chip.isChecked = !matched
            } else {
                chip.isChecked = false
            }
        }
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
                    updatePresetChips()
                }
            }
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<Int>() {
        override fun areItemsTheSame(oldItem: Int, newItem: Int) = oldItem == newItem
        override fun areContentsTheSame(oldItem: Int, newItem: Int) = true
    }
}