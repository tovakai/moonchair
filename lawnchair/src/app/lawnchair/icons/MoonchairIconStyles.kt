package app.lawnchair.icons

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import app.lawnchair.icons.shape.IconShape
import com.android.launcher3.model.data.WorkspaceItemInfo

/** Per-shortcut appearance without changing globally cached app icons. */
object MoonchairIconStyles {
    private const val MASK = 0x30000000
    private const val SHIFT = 28

    @JvmStatic fun getGroup(info: WorkspaceItemInfo): Int = (info.options and MASK) ushr SHIFT

    @JvmStatic fun setGroup(info: WorkspaceItemInfo, group: Int) {
        require(group in 0..3)
        info.options = (info.options and MASK.inv()) or (group shl SHIFT)
    }

    @JvmStatic fun wrap(icon: Drawable, group: Int): Drawable =
        if (group in 1..3) GroupDrawable(icon, group) else icon

    private class GroupDrawable(
        private val source: Drawable,
        group: Int,
    ) : Drawable(), Drawable.Callback {
        private val baseMask: Path = when (group) {
            1 -> IconShape.Meow.getMaskPath()
            2 -> IconShape.Hexagon.getMaskPath()
            else -> IconShape.Circle.getMaskPath()
        }
        private val background: Int = when (group) {
            1 -> 0xFFFFE0CF.toInt()
            2 -> 0xFF67465D.toInt()
            else -> 0xFFE5B9F5.toInt()
        }
        private val foreground: Int = when (group) {
            1 -> 0xFF432838.toInt()
            2 -> 0xFFF5ACD8.toInt()
            else -> 0xFF432838.toInt()
        }
        private val shapePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = background }
        private val tintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = foreground
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        }
        private val matrix = Matrix()
        private val drawMask = Path()
        private val iconRect = Rect()
        private val layerBounds = RectF()
        private var overallAlpha = 255

        init { source.callback = this }

        override fun draw(canvas: Canvas) {
            val b = bounds
            if (b.isEmpty) return
            matrix.reset()
            matrix.setScale(b.width() / 100f, b.height() / 100f)
            matrix.postTranslate(b.left.toFloat(), b.top.toFloat())
            drawMask.reset()
            baseMask.transform(matrix, drawMask)
            shapePaint.alpha = overallAlpha
            tintPaint.alpha = overallAlpha
            canvas.drawPath(drawMask, shapePaint)

            val width = (b.width() * 0.66f).toInt()
            val height = (b.height() * 0.66f).toInt()
            iconRect.set(
                b.centerX() - width / 2, b.centerY() - height / 2,
                b.centerX() + (width + 1) / 2, b.centerY() + (height + 1) / 2,
            )
            layerBounds.set(iconRect)
            val clip = canvas.save()
            canvas.clipPath(drawMask)
            val layer = canvas.saveLayer(layerBounds, null)
            source.bounds = iconRect
            source.draw(canvas)
            canvas.drawRect(layerBounds, tintPaint)
            canvas.restoreToCount(layer)
            canvas.restoreToCount(clip)
        }

        override fun setAlpha(alpha: Int) {
            overallAlpha = alpha.coerceIn(0, 255)
            invalidateSelf()
        }
        override fun setColorFilter(colorFilter: ColorFilter?) {
            source.colorFilter = colorFilter
            invalidateSelf()
        }
        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
        override fun getIntrinsicWidth(): Int = source.intrinsicWidth
        override fun getIntrinsicHeight(): Int = source.intrinsicHeight
        override fun isStateful(): Boolean = source.isStateful
        override fun onStateChange(state: IntArray): Boolean = source.setState(state)
        override fun onLevelChange(level: Int): Boolean = source.setLevel(level)
        override fun invalidateDrawable(who: Drawable) = invalidateSelf()
        override fun scheduleDrawable(who: Drawable, what: Runnable, whenMillis: Long) =
            scheduleSelf(what, whenMillis)
        override fun unscheduleDrawable(who: Drawable, what: Runnable) =
            unscheduleSelf(what)
    }
}
