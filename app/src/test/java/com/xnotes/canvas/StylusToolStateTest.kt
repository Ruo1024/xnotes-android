package com.xnotes.canvas

import com.xnotes.core.tools.Tool
import org.junit.Assert.*
import org.junit.Test

class StylusToolStateTest {
    @Test fun heldEraserTemporarilyOverridesToggledPan() {
        val state = StylusToolState()
        state.configure(Tool.ERASER, Tool.PAN, false, true)
        state.observe(2)
        state.observe(0)
        assertEquals(Tool.PAN, state.tool())
        state.observe(1)
        assertEquals(Tool.ERASER, state.tool())
        state.observe(0)
        assertEquals(Tool.PAN, state.tool())
        state.observe(2)
        assertNull(state.tool())
    }

    @Test fun bothToggleModesReplaceRatherThanStack() {
        val state = StylusToolState()
        state.configure(Tool.ERASER, Tool.SELECT, true, true)
        state.observe(1); state.observe(0)
        state.observe(2); state.observe(0)
        assertEquals(Tool.SELECT, state.tool())
        state.observe(2); state.observe(0)
        assertNull(state.tool())
    }

    @Test fun repeatedReportsAndQueriesDoNotRetoggle() {
        val state = StylusToolState()
        state.configure(Tool.ERASER, Tool.PAN, false, true)
        repeat(10) {
            state.observe(2)
            assertEquals(Tool.PAN, state.tool())
        }
        assertNull(state.heldTool()) // never pan merely by hovering in toggle mode
    }

    @Test fun manualChoiceSuppressesHeldButtonsUntilRelease() {
        val state = StylusToolState()
        state.configure(Tool.ERASER, Tool.PAN, false, true)
        state.observe(3)
        state.cancel()
        state.observe(3)
        assertNull(state.tool())
        state.observe(0)
        state.observe(2)
        assertEquals(Tool.PAN, state.tool())
    }

    @Test fun inverseMixedConfigurationAndDisabledButton() {
        val state = StylusToolState()
        state.configure(Tool.SELECT, Tool.PAN, true, false)
        state.observe(1); state.observe(0)
        state.observe(2)
        assertEquals(Tool.PAN, state.tool())
        state.observe(0)
        assertEquals(Tool.SELECT, state.tool())
        state.configure(Tool.ERASER, null, false, true)
        state.observe(2)
        assertNull(state.tool())
    }

    @Test fun resetClearsBothLatchedAndHeldTools() {
        val state = StylusToolState()
        state.configure(Tool.ERASER, Tool.PAN, false, true)
        state.observe(3)
        state.reset()
        assertNull(state.tool())
        assertNull(state.heldTool())
    }
}
