// SPDX-License-Identifier: GPL-2.0-or-later

package org.citra.citra_emu.overlay

import kotlin.math.abs

/**
 * The "Resize" gesture of the edit layout for buttons, the D-pad and the circle pads.
 *
 * Only horizontal movement of the finger counts. The size change is measured from the place the
 * finger landed, so dragging right always grows the control and dragging left always shrinks it;
 * it never drops first just because the finger was not on the position that matches the current
 * size. The right edge of the screen is the biggest size for every control, so a control can
 * always be taken to the maximum without the value wrapping around.
 *
 * The size is the value of the scale slider (0..150, a size factor of (value + 50) / 100) and can
 * never leave that range, so nothing snaps back when the finger goes past the end.
 *
 * To keep it gentle, the size does not jump to the finger: it starts only after the finger moved
 * a little and then follows at a limited speed.
 */
class ResizeGesture {
    /** Value (0..150) of the control when the gesture started. */
    var startValue = 50f
        private set

    /** Value (0..150) that is shown right now. */
    var value = 50f
        private set

    private var startX = 0f
    private var screenWidth = 1f
    private var active = false
    private var lastTime = 0L
    var targetValue = 50f
        private set

    /** Current size relative to the size when the gesture started (1.0 = unchanged). */
    val ratio: Float
        get() = (value + BASE) / (startValue + BASE)

    fun begin(x: Float, screenWidth: Int, startValue: Int) {
        this.startValue = startValue.toFloat().coerceIn(MIN_VALUE, MAX_VALUE)
        value = this.startValue
        targetValue = this.startValue
        startX = x
        this.screenWidth = maxOf(screenWidth, 1).toFloat()
        active = false
        lastTime = 0L
    }

    /** Call for every move of the finger ([x] in view pixels, [timeMs] the event time). */
    fun update(x: Float, timeMs: Long) {
        // MotionEvent coordinates end at width - 1, so map that pixel to the exact maximum.
        val maxX = (screenWidth - 1f).coerceAtLeast(1f)
        val clampedX = x.coerceIn(0f, maxX)
        if (!active) {
            val reachedEdge = clampedX <= 0f || clampedX >= maxX
            if (abs(x - startX) < TOUCH_SLOP_PX && !reachedEdge) return
            active = true
            lastTime = timeMs
        }
        // The size follows the horizontal travel from where the finger landed, so dragging right
        // grows the control instead of first pulling it towards the absolute position's size.
        targetValue = (startValue + (clampedX - startX) * MAX_VALUE / maxX)
            .coerceIn(MIN_VALUE, MAX_VALUE)

        // Reaching an edge is deliberate: the edges of the screen are the limits, not a wrap.
        if (clampedX <= 0f) targetValue = MIN_VALUE
        if (clampedX >= maxX) targetValue = MAX_VALUE

        val maxStep = MAX_UNITS_PER_SECOND * (timeMs - lastTime).coerceAtLeast(0L) / 1000f
        lastTime = timeMs
        value += (targetValue - value).coerceIn(-maxStep, maxStep)

        if (targetValue <= MIN_VALUE) value = MIN_VALUE
        if (targetValue >= MAX_VALUE) value = MAX_VALUE
    }

    companion object {
        const val MIN_VALUE = 0f
        const val MAX_VALUE = 150f
        private const val BASE = 50f

        // How fast the size follows the finger, in scale slider units per second.
        private const val MAX_UNITS_PER_SECOND = 60f

        // Ignore small accidental movement when the control is first selected.
        private const val TOUCH_SLOP_PX = 24f
    }
}
