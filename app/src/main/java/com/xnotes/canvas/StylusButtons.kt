package com.xnotes.canvas

import android.view.KeyEvent
import android.view.MotionEvent
import com.xnotes.core.tools.Tool

/**
 * Which stylus side buttons are currently down.
 *
 * Pens do not agree on how to report this, and getting it right took several releases of
 * device-specific work, so both canvases use this shared state. Three delivery routes are covered:
 *
 *  - the touch stream's `buttonState`, which most pens use;
 *  - the hovering generic-motion stream, for pens that report the button only while hovering and
 *    never in a touch event;
 *  - a `KeyEvent`, which Bluetooth and USI pens use, including one vendor code with no standard
 *    mapping ([InteractionController.VENDOR_HELD_BUTTON_KEYCODE]).
 *
 * A fourth quirk is handled higher up and needs nothing here: some Samsung builds tag a
 * button-held stroke with proprietary action codes instead of DOWN/MOVE/UP, and `MainActivity`
 * rewrites those before dispatch, so any view in the tree gets a normal touch stream.
 *
 * Button masks are normalized here so the two canvases cannot drift apart. The existing vendor
 * key still uses the primary-button mapping.
 */
class StylusButtonLatch {
    private val keys = mutableSetOf<Int>()
    private var motionButtons = 0

    val held: Boolean get() = buttons() != 0

    fun onGenericMotion(e: MotionEvent): Boolean {
        if (!isPen(e.getToolType(0))) return false
        updateMotion(e.buttonState, e.actionMasked, e.actionButton)
        return !held
    }

    internal fun updateMotion(state: Int, action: Int, actionButton: Int = 0) {
        motionButtons = normalize(state)
        if (action == MotionEvent.ACTION_BUTTON_PRESS) {
            motionButtons = motionButtons or normalize(actionButton)
        }
        if (action == MotionEvent.ACTION_BUTTON_RELEASE) {
            val released = normalize(actionButton)
            keys.removeAll { keyMask(it) and released != 0 }
            motionButtons = motionButtons and released.inv()
        }
    }

    fun onKey(keyCode: Int, down: Boolean): Boolean {
        val mask = keyMask(keyCode)
        if (mask == 0) return false
        if (down) keys.add(keyCode) else {
            keys.remove(keyCode)
            // Some pens release on a different stream than the press.
            motionButtons = motionButtons and mask.inv()
        }
        return true
    }

    fun reset() {
        keys.clear()
        motionButtons = 0
    }

    fun toolFor(
        e: MotionEvent, primary: Tool?, secondary: Tool?, compatibility: Boolean = false,
        pointerIndex: Int = 0, hover: Boolean = false,
    ): Tool? = resolveTool(e.getToolType(pointerIndex), e.buttonState, primary, secondary, compatibility, hover)

    /** Wacom One's second side button can change toolType to ERASER with no button bits.
     * Resolve that before the ordinary eraser fallback, including a disabled (null) mapping.
     * With compatibility off, preserve the original single-button and physical-eraser behavior.
     * https://www.mindboardapps.com/posts/using-wacom-one-pen-on-android/ */
    internal fun resolveTool(
        toolType: Int, state: Int, primary: Tool?, secondary: Tool?, compatibility: Boolean,
        hover: Boolean = false,
    ): Tool? = when (toolType) {
        MotionEvent.TOOL_TYPE_ERASER -> if (compatibility) secondary else if (hover) null else Tool.ERASER
        MotionEvent.TOOL_TYPE_STYLUS -> toolForButtons(state, primary, if (compatibility) secondary else primary)
        else -> null
    }

    /** Secondary takes priority when both are held. A disabled button has no effect. */
    internal fun toolForButtons(state: Int, primary: Tool?, secondary: Tool?): Tool? {
        val active = buttons() or normalize(state)
        return when {
            active and SECONDARY != 0 && secondary != null -> secondary
            active and PRIMARY != 0 -> primary
            else -> null
        }
    }

    private fun buttons(): Int = keys.fold(motionButtons) { mask, key -> mask or keyMask(key) }

    companion object {
        fun isPen(toolType: Int): Boolean =
            toolType == MotionEvent.TOOL_TYPE_STYLUS || toolType == MotionEvent.TOOL_TYPE_ERASER
        private const val PRIMARY = 1
        private const val SECONDARY = 2

        private fun normalize(state: Int): Int =
            (if (state and (MotionEvent.BUTTON_STYLUS_PRIMARY or MotionEvent.BUTTON_SECONDARY) != 0) PRIMARY else 0) or
                (if (state and (MotionEvent.BUTTON_STYLUS_SECONDARY or MotionEvent.BUTTON_TERTIARY) != 0) SECONDARY else 0)

        private fun keyMask(key: Int): Int = when (key) {
            KeyEvent.KEYCODE_STYLUS_BUTTON_SECONDARY -> SECONDARY
            KeyEvent.KEYCODE_STYLUS_BUTTON_PRIMARY,
            KeyEvent.KEYCODE_STYLUS_BUTTON_TERTIARY,
            KeyEvent.KEYCODE_STYLUS_BUTTON_TAIL,
            InteractionController.VENDOR_HELD_BUTTON_KEYCODE -> PRIMARY
            else -> 0
        }

        fun isStylusButtonKey(keyCode: Int): Boolean = keyMask(keyCode) != 0
    }
}
