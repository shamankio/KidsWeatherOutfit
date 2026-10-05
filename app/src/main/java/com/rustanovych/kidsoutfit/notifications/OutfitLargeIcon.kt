package com.rustanovych.kidsoutfit.notifications

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.rustanovych.kidsoutfit.domain.model.ChildGender
import com.rustanovych.kidsoutfit.domain.model.OutfitSet
import com.rustanovych.kidsoutfit.ui.assets.outfitThumbDrawables
import kotlin.math.max
import kotlin.math.min

/**
 * The notification's large icon: the matching character, drawn into a square the size the system
 * asks for. A pair is drawn side by side. The illustrations are far larger than the icon, so they
 * are decoded already down-sampled rather than loaded at full size.
 */
fun outfitLargeIcon(context: Context, outfit: OutfitSet, gender: ChildGender): Bitmap {
    val resources = context.resources
    val size = resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_width)
    val drawables = outfitThumbDrawables(outfit, gender)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)

    val cellWidth = size.toFloat() / drawables.size
    drawables.forEachIndexed { index, res ->
        val source = decodeSampled(resources, res, targetWidth = cellWidth.toInt(), targetHeight = size)
        val scale = min(cellWidth / source.width, size.toFloat() / source.height)
        val width = source.width * scale
        val height = source.height * scale
        val left = index * cellWidth + (cellWidth - width) / 2
        val top = (size - height) / 2
        canvas.drawBitmap(source, null, RectF(left, top, left + width, top + height), paint)
        source.recycle()
    }
    return bitmap
}

private fun decodeSampled(resources: Resources, res: Int, targetWidth: Int, targetHeight: Int): Bitmap {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeResource(resources, res, bounds)

    var sample = 1
    while (bounds.outWidth / (sample * 2) >= targetWidth && bounds.outHeight / (sample * 2) >= targetHeight) {
        sample *= 2
    }
    val options = BitmapFactory.Options().apply { inSampleSize = max(1, sample) }
    return checkNotNull(BitmapFactory.decodeResource(resources, res, options)) { "Cannot decode drawable $res" }
}
