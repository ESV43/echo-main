package dev.brahmkshatriya.echo.ui.media.more

import android.text.Spannable
import android.text.SpannableString
import android.text.style.RelativeSizeSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import dev.brahmkshatriya.echo.databinding.ItemMoreButtonBinding
import dev.brahmkshatriya.echo.ui.common.GridAdapter
import dev.brahmkshatriya.echo.utils.ui.scrolling.ScrollAnimViewHolder

class MoreButtonAdapter
    : ListAdapter<MoreButton, MoreButtonAdapter.ViewHolder>(MoreButton.DiffCallback), GridAdapter {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(parent)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    override val adapter = this
    override fun getSpanSize(position: Int, width: Int, count: Int) = 1

    class ViewHolder(
        parent: ViewGroup,
        val binding: ItemMoreButtonBinding = ItemMoreButtonBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
    ) : ScrollAnimViewHolder(binding.root) {
        fun bind(item: MoreButton) = with(binding.root) {
            val textValue = if (item.subtitle != null) {
                val span = SpannableString("${item.title}\n${item.subtitle}")
                span.setSpan(
                    RelativeSizeSpan(0.8f),
                    item.title.length + 1,
                    span.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                span
            } else SpannableString(item.title)
            
            text = textValue
            setOnClickListener { item.onClick() }
            setIconResource(item.icon)
        }
    }
}