package dev.brahmkshatriya.echo.ui.download

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.Extension
import dev.brahmkshatriya.echo.common.models.Progress
import dev.brahmkshatriya.echo.databinding.ItemDownloadBinding
import dev.brahmkshatriya.echo.databinding.ItemDownloadTaskBinding
import dev.brahmkshatriya.echo.download.Downloader
import dev.brahmkshatriya.echo.download.db.models.ContextEntity
import dev.brahmkshatriya.echo.download.db.models.DownloadEntity
import dev.brahmkshatriya.echo.download.db.models.TaskType
import dev.brahmkshatriya.echo.download.tasks.BaseTask.Companion.getTitle
import dev.brahmkshatriya.echo.ui.common.ExceptionUtils
import dev.brahmkshatriya.echo.ui.common.GridAdapter
import dev.brahmkshatriya.echo.utils.image.ImageUtils.loadAsCircle
import dev.brahmkshatriya.echo.utils.image.ImageUtils.loadInto
import dev.brahmkshatriya.echo.utils.ui.scrolling.ScrollAnimListAdapter
import dev.brahmkshatriya.echo.utils.ui.scrolling.ScrollAnimViewHolder

class DownloadsAdapter(
    private val listener: Listener,
) : ScrollAnimListAdapter<DownloadsAdapter.Item, DownloadsAdapter.ViewHolder>(
    object : DiffUtil.ItemCallback<Item>() {
        override fun areContentsTheSame(oldItem: Item, newItem: Item) = oldItem == newItem
        override fun areItemsTheSame(oldItem: Item, newItem: Item): Boolean {
            return when (oldItem) {
                is Download -> if (newItem !is Download) false
                else oldItem.downloadEntity.id == newItem.downloadEntity.id

                is Task -> if (newItem !is Task) false
                else oldItem.id == newItem.id
            }
        }
    }
), GridAdapter {

    interface Listener {
        fun onExceptionClicked(data: ExceptionUtils.Data)
        fun onCancel(trackId: Long)
        fun onRestart(trackId: Long)
    }

    sealed class ViewHolder(itemView: View) : ScrollAnimViewHolder(itemView)

    class DownloadViewHolder(
        parent: ViewGroup,
        private val listener: Listener,
        private val binding: ItemDownloadBinding = ItemDownloadBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        ),
    ) : ViewHolder(binding.root) {
        init {
            binding.imageView.clipToOutline = true
        }

        fun bind(item: Download) {
            val entity = item.downloadEntity
            binding.apply {
                val track = entity.track.getOrNull()
                title.text = track?.title ?: root.context.getString(R.string.unknown)
                track?.cover.loadInto(imageView, R.drawable.art_music)
                item.extension?.metadata?.icon?.loadAsCircle(
                    extensionIcon, R.drawable.ic_extension
                ) {
                    extensionIcon.setImageDrawable(it)
                }
                val sub = item.context?.mediaItem?.getOrNull()?.title
                subtitle.text = sub
                subtitle.isVisible = !sub.isNullOrEmpty()

                exception.text = entity.exception?.title
                exception.isVisible = exception.text.isNotEmpty()
                status.setText(
                    when {
                        entity.exception != null -> R.string.download_status_failed
                        entity.finalFile != null -> R.string.download_status_completed
                        else -> R.string.download_status_active
                    }
                )

                remove.setOnClickListener {
                    listener.onCancel(entity.id)
                }
                remove.isVisible = entity.finalFile == null

                retry.isVisible = entity.exception != null
                retry.contentDescription = root.context.getString(R.string.refresh)
                retry.setOnClickListener {
                    listener.onRestart(entity.id)
                }
                remove.contentDescription = root.context.getString(R.string.remove)
                root.setOnClickListener {
                    val data = entity.exception ?: return@setOnClickListener
                    listener.onExceptionClicked(data)
                }
            }
        }
    }

    class TaskViewHolder(
        parent: ViewGroup,
        private val binding: ItemDownloadTaskBinding = ItemDownloadTaskBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        ),
    ) : ViewHolder(binding.root) {
        fun bind(item: Task) = binding.apply {
            progressBar.isIndeterminate = item.progress.size == 0L
            progressBar.max = item.progress.size.toInt()
            progressBar.progress = item.progress.progress.toInt()
            subtitle.text = item.progress.toText()
            title.text = root.context.run {
                getTitle(item.taskType, getString(R.string.download))
            }
        }
    }


    sealed interface Item
    data class Download(
        val context: ContextEntity?,
        val downloadEntity: DownloadEntity,
        val extension: Extension<*>?,
    ) : Item

    data class Task(val taskType: TaskType, val progress: Progress, val id: Long) : Item

    enum class Filter {
        Active,
        Completed,
        Failed
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is Download -> 0
            is Task -> 1
        }
    }

    override val adapter = this
    override fun getSpanSize(position: Int, width: Int, count: Int) = 2.coerceAtLeast(count)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = when (viewType) {
        0 -> DownloadViewHolder(parent, listener)
        1 -> TaskViewHolder(parent)
        else -> throw IllegalArgumentException("Invalid view type")
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        when (holder) {
            is DownloadViewHolder -> {
                val item = getItem(position) as Download
                holder.bind(item)
            }

            is TaskViewHolder -> {
                val item = getItem(position) as Task
                holder.bind(item)
            }
        }
    }


    companion object {
        fun List<Downloader.Info>.toItems(
            extensions: List<Extension<*>>,
            filter: Filter = Filter.Active
        ) = filter {
            when (filter) {
                Filter.Active -> it.download.finalFile == null && it.download.exception == null
                Filter.Completed -> it.download.finalFile != null
                Filter.Failed -> it.download.exception != null
            }
        }.flatMap { info ->
            val download = info.download
            val extension = extensions.find { it.id == download.extensionId }
            val workers = if (filter == Filter.Active) info.workers else emptyList()
            listOf(Download(info.context, download, extension)) + workers.map {
                Task(it.first, it.second, download.id)
            }
        }

        private val SPEED_UNITS = arrayOf("", "KB", "MB", "GB")
        fun Progress.toText() = buildString {
            if (size > 0) append("%.2f%% • ".format(progress.toFloat() / size * 100))
            append(
                if (size > 0) "${convertBytes(progress)} / ${convertBytes(size)}"
                else convertBytes(progress)
            )
            if (speed > 0) append(" • ${convertBytes(speed)}/s")
        }

        private fun convertBytes(bytes: Long): String {
            var value = bytes.toFloat()
            var unitIndex = 0

            while (value >= 500 && unitIndex < SPEED_UNITS.size - 1) {
                value /= 1024
                unitIndex++
            }
            return "%.2f %s".format(value, SPEED_UNITS[unitIndex])
        }
    }
}
