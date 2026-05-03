package dev.brahmkshatriya.echo.ui.player.more.lyrics

import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import dev.brahmkshatriya.echo.common.models.Lyrics
import dev.brahmkshatriya.echo.databinding.ItemLoadingBinding
import dev.brahmkshatriya.echo.databinding.ItemLyricBinding
import dev.brahmkshatriya.echo.ui.common.UiViewModel
import dev.brahmkshatriya.echo.ui.feed.FeedLoadingAdapter
import dev.brahmkshatriya.echo.ui.player.PlayerColors.Companion.defaultPlayerColors
import dev.brahmkshatriya.echo.utils.ui.AnimationUtils.applyTranslationYAnimation
import dev.brahmkshatriya.echo.utils.ui.scrolling.ScrollAnimListAdapter
import dev.brahmkshatriya.echo.utils.ui.scrolling.ScrollAnimViewHolder

class LyricAdapter(
    val uiViewModel: UiViewModel, val listener: Listener,
) : ScrollAnimListAdapter<LyricAdapter.LyricLine, LyricAdapter.ViewHolder>(DiffCallback) {
    
    data class LyricLine(
        val text: String,
        val startTime: Long,
        val endTime: Long,
        val words: List<Lyrics.Item>? = null
    )

    fun interface Listener {
        fun onLyricSelected(adapter: LyricAdapter, lyric: LyricLine)
    }

    object DiffCallback : DiffUtil.ItemCallback<LyricLine>() {
        override fun areItemsTheSame(oldItem: LyricLine, newItem: LyricLine) =
            oldItem.startTime == newItem.startTime && oldItem.text == newItem.text

        override fun areContentsTheSame(oldItem: LyricLine, newItem: LyricLine) =
            oldItem == newItem
    }

    inner class ViewHolder(val binding: ItemLyricBinding) : ScrollAnimViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                val lyrics = getItem(bindingAdapterPosition) ?: return@setOnClickListener
                listener.onLyricSelected(this@LyricAdapter, lyrics)
            }
        }
    }

    private fun ViewHolder.updateColors() {
        binding.root.run {
            val colors = uiViewModel.playerColors.value ?: context.defaultPlayerColors()
            val alphaStrippedColor = colors.onBackground or -0x1000000
            setTextColor(alphaStrippedColor)
        }
    }

    private fun getItemOrNull(position: Int) = runCatching { getItem(position) }.getOrNull()

    private var currentPos = -1
    private var currentProgress = 0L

    fun updateCurrent(currentPos: Int, progress: Long = 0L) {
        this.currentPos = currentPos
        this.currentProgress = progress
        onEachViewHolder { updateCurrent() }
    }

    private fun ViewHolder.updateCurrent() {
        val pos = bindingAdapterPosition
        val line = getItemOrNull(pos) ?: return
        val colors = uiViewModel.playerColors.value ?: itemView.context.defaultPlayerColors()
        val activeColor = colors.onBackground or -0x1000000
        val inactiveColor = Color.argb(100, Color.red(activeColor), Color.green(activeColor), Color.blue(activeColor))

        if (pos == currentPos) {
            binding.root.alpha = 1f
            binding.root.animate().scaleX(1.05f).scaleY(1.05f).setDuration(300).start()
            if (line.words != null) {
                val spannable = SpannableString(line.text)
                var startIndex = 0
                line.words.forEach { word ->
                    val wordIndex = line.text.indexOf(word.text, startIndex)
                    if (wordIndex != -1) {
                        val isActive = currentProgress in word.startTime..word.endTime
                        val color = if (currentProgress >= word.startTime) activeColor else inactiveColor
                        spannable.setSpan(
                            ForegroundColorSpan(color),
                            wordIndex,
                            wordIndex + word.text.length,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        if (isActive) {
                            spannable.setSpan(
                                android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                                wordIndex,
                                wordIndex + word.text.length,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                        }
                        startIndex = wordIndex + word.text.length
                    }
                }
                binding.root.text = spannable
            } else {
                binding.root.setTextColor(activeColor)
                binding.root.text = line.text
            }
        } else {
            binding.root.animate().scaleX(1f).scaleY(1f).setDuration(300).start()
            binding.root.setTextColor(inactiveColor)
            binding.root.alpha = if (pos < currentPos) 0.8f else 0.4f
            binding.root.text = line.text
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemLyricBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val line = getItem(position) ?: return
        holder.binding.root.text = line.text.trim().trim('\n').ifEmpty { "♪" }
        holder.updateColors()
        holder.updateCurrent()
        holder.itemView.applyTranslationYAnimation(scrollY)
    }

    override fun onViewAttachedToWindow(holder: ViewHolder) {
        holder.updateColors()
    }

    fun updateColors() {
        onEachViewHolder { updateColors() }
    }

    class Loading(
        parent: ViewGroup,
        val binding: ItemLoadingBinding = ItemLoadingBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
    ) : FeedLoadingAdapter.ViewHolder(binding.root)
}
