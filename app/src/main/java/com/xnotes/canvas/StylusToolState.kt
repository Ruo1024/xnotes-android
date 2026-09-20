package com.xnotes.canvas

import com.xnotes.core.tools.Tool

/** Per-editor button policy. Input edges mutate state; querying a tool never does. */
class StylusToolState {
    private var primary: Tool? = null
    private var secondary: Tool? = null
    private var primaryToggle = false
    private var secondaryToggle = false
    private var pressed = 0
    private var suppressed = 0
    private var latched = 0
    var revision = 0
        private set

    fun configure(primary: Tool?, secondary: Tool?, primaryToggle: Boolean, secondaryToggle: Boolean) {
        if (this.primary == primary && this.secondary == secondary &&
            this.primaryToggle == primaryToggle && this.secondaryToggle == secondaryToggle) return
        cancel()
        this.primary = primary
        this.secondary = secondary
        this.primaryToggle = primaryToggle
        this.secondaryToggle = secondaryToggle
    }

    fun observe(mask: Int) {
        suppressed = suppressed and mask
        val down = mask and pressed.inv() and suppressed.inv()
        pressed = mask
        // Deterministic secondary priority if both arrive in the same event.
        val trigger = when {
            down and 2 != 0 && secondaryToggle && secondary != null -> 2
            down and 1 != 0 && primaryToggle && primary != null -> 1
            else -> 0
        }
        if (trigger != 0) {
            latched = if (latched == trigger) 0 else trigger
            revision++
        }
    }

    fun heldTool(): Tool? {
        val active = pressed and suppressed.inv()
        return when {
            active and 2 != 0 && !secondaryToggle && secondary != null -> secondary
            active and 1 != 0 && !primaryToggle -> primary
            else -> null
        }
    }

    fun tool(): Tool? = heldTool() ?: when (latched) {
        1 -> primary
        2 -> secondary
        else -> null
    }

    /** A manual tool choice wins even while a physical button remains pressed. */
    fun cancel() {
        suppressed = pressed
        latched = 0
        revision++
    }

    fun reset() {
        pressed = 0
        suppressed = 0
        latched = 0
        revision++
    }
}
