package com.pigeonpost.ui.screens.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

/**
 * Draws the Google Maps markers programmatically so the tracker keeps its hand-inked,
 * old-atlas look on top of real map tiles without shipping any image assets.
 */
internal object MapMarkerIcons {

    /**
     * A teardrop pin whose tip sits at the marker's anchor point, for the sender's and
     * the recipient's real coordinates.
     */
    fun locationPin(
        context: Context,
        fillColor: Int,
        ringColor: Int,
        sizeDp: Int = 26
    ): BitmapDescriptor {
        val width = context.px(sizeDp)
        val height = context.px((sizeDp * 1.4f).toInt())
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val radius = width / 2f - context.px(1)
        val centerX = width / 2f
        val centerY = radius + context.px(1)

        // Tail from the head down to the tip.
        val tail = Path().apply {
            moveTo(centerX - radius * 0.55f, centerY + radius * 0.65f)
            lineTo(centerX, height.toFloat() - context.px(1))
            lineTo(centerX + radius * 0.55f, centerY + radius * 0.65f)
            close()
        }
        paint.color = ringColor
        canvas.drawPath(tail, paint)

        // Head: dark ring, coloured body, pale centre.
        canvas.drawCircle(centerX, centerY, radius, paint)
        paint.color = fillColor
        canvas.drawCircle(centerX, centerY, radius * 0.78f, paint)
        paint.color = PALE_INK
        canvas.drawCircle(centerX, centerY, radius * 0.3f, paint)

        return bitmap.toDescriptor(context)
    }

    /**
     * The pigeon itself: a gilded glow with a simple winged body, centred on the bird's
     * real current position.
     */
    fun pigeon(
        context: Context,
        bodyColor: Int,
        glowColor: Int,
        sizeDp: Int = 34
    ): BitmapDescriptor {
        val size = context.px(sizeDp)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val center = size / 2f

        // Halo so the bird stays visible over busy map tiles.
        paint.color = glowColor
        paint.alpha = 90
        canvas.drawCircle(center, center, center * 0.95f, paint)
        paint.alpha = 160
        canvas.drawCircle(center, center, center * 0.6f, paint)

        // Body.
        paint.alpha = 255
        paint.color = bodyColor
        canvas.drawCircle(center, center, center * 0.3f, paint)

        // Wings.
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = context.px(2).toFloat().coerceAtLeast(2f)
        paint.strokeCap = Paint.Cap.ROUND
        val wing = center * 0.72f
        canvas.drawLine(center - wing, center - center * 0.28f, center, center, paint)
        canvas.drawLine(center + wing, center - center * 0.28f, center, center, paint)

        return bitmap.toDescriptor(context)
    }

    /**
     * A wax-red cross marking the exact spot where a doomed pigeon perished.
     */
    fun deathCross(
        context: Context,
        color: Int,
        sizeDp: Int = 30
    ): BitmapDescriptor {
        val size = context.px(sizeDp)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val center = size / 2f

        paint.color = PALE_INK
        paint.alpha = 200
        canvas.drawCircle(center, center, center * 0.92f, paint)

        paint.alpha = 255
        paint.color = color
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = context.px(3).toFloat().coerceAtLeast(3f)
        paint.strokeCap = Paint.Cap.ROUND
        val arm = center * 0.5f
        canvas.drawLine(center - arm, center - arm, center + arm, center + arm, paint)
        canvas.drawLine(center + arm, center - arm, center - arm, center + arm, paint)

        return bitmap.toDescriptor(context)
    }

    /** Parchment cream, matching the app's palette. */
    private const val PALE_INK = 0xFFF5E6D3.toInt()

    /**
     * [BitmapDescriptorFactory] needs the Maps SDK to have been initialised, which has
     * not necessarily happened yet the first time a marker icon is built, so make sure
     * of it here.
     */
    private fun Bitmap.toDescriptor(context: Context): BitmapDescriptor {
        MapsInitializer.initialize(context)
        return BitmapDescriptorFactory.fromBitmap(this)
    }

    private fun Context.px(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt().coerceAtLeast(1)
}
