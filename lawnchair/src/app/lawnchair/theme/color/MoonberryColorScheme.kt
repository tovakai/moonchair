package app.lawnchair.theme.color

import androidx.core.graphics.ColorUtils
import dev.kdrag0n.monet.theme.ColorScheme
import dev.kdrag0n.monet.theme.ColorSwatch

/** Fixed Moonberry swatches used by non-Compose Launcher3 UI. */
object MoonberryColorScheme : ColorScheme() {
    override val neutral1: ColorSwatch = swatch(0xFFFFF5F7.toInt(), 0xFF7B5A6C.toInt(), 0xFF21121C.toInt())
    override val neutral2: ColorSwatch = swatch(0xFFFFE0CF.toInt(), 0xFF79546D.toInt(), 0xFF2E1C29.toInt())
    override val accent1: ColorSwatch = swatch(0xFFFFE0EF.toInt(), 0xFFF5ACD8.toInt(), 0xFF432838.toInt())
    override val accent2: ColorSwatch = swatch(0xFFF8E5FF.toInt(), 0xFFE5B9F5.toInt(), 0xFF432838.toInt())
    override val accent3: ColorSwatch = swatch(0xFFFFE9DC.toInt(), 0xFFFFE0CF.toInt(), 0xFF6B4957.toInt())

    private fun swatch(light: Int, middle: Int, dark: Int): ColorSwatch {
        val shades = listOf(0, 10, 50, 100, 200, 300, 400, 500, 600, 700, 800, 900, 1000)
        return shades.associateWith { shade ->
            val argb = if (shade <= 500) {
                ColorUtils.blendARGB(light, middle, shade / 500f)
            } else {
                ColorUtils.blendARGB(middle, dark, (shade - 500) / 500f)
            }
            AndroidColor(argb)
        }
    }
}
