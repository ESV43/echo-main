package dev.brahmkshatriya.echo.utils.image

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import coil3.imageLoader
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import coil3.size.Size
import coil3.toBitmap
import dev.brahmkshatriya.echo.common.models.ImageHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toColorInt

suspend fun List<ImageHolder>.loadCollageBitmap(
    context: android.content.Context, size: Int = 320
): Bitmap? = withContext(Dispatchers.IO) {
    val covers = take(4)
    if (covers.isEmpty()) return@withContext null
    val halfSize = size / 2
    val loader = context.imageLoader
    val bitmaps = covers.mapIndexedNotNull { index, holder ->
        val request = ImageRequest.Builder(context)
            .apply { holder.createRequest(this) }
            .size(Size(halfSize, halfSize))
            .build()
        runCatching { loader.execute(request).image?.toBitmap() }.getOrNull()
    }
    if (bitmaps.isEmpty()) return@withContext null
    val result = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)
    val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply { isAntiAlias = true }
    val positions = listOf(
        Rect(0, 0, halfSize, halfSize),
        Rect(halfSize, 0, size, halfSize),
        Rect(0, halfSize, size - halfSize, size),
        Rect(halfSize, halfSize, size, size),
    )
    bitmaps.forEachIndexed { index, bmp ->
        val rect = positions[index]
        val src = Rect(0, 0, bmp.width, bmp.height)
        canvas.drawBitmap(bmp, src, rect, paint)
    }
    result
}

private fun ImageHolder.createRequest(builder: ImageRequest.Builder) {
    when (this) {
        is ImageHolder.ResourceUriImageHolder -> builder.data(uri)
        is ImageHolder.NetworkRequestImageHolder -> {
            val headerBuilder = NetworkHeaders.Builder()
            request.headers.forEach { (key, value) ->
                headerBuilder[key] = value
            }
            builder.httpHeaders(headerBuilder.build())
            builder.data(request.url)
        }
        is ImageHolder.ResourceIdImageHolder -> builder.data(resId)
        is ImageHolder.HexColorImageHolder -> builder.data(hex.toColorInt().toDrawable())
    }
}
