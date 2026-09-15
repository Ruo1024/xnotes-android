package com.xnotes.canvas

import android.view.KeyEvent
import android.view.MotionEvent
import com.xnotes.core.tools.Tool
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StylusButtonsTest {
    @Test fun wacomEraserInputUsesConfiguredSecondaryToolWithoutButtonBits() {
        val buttons = StylusButtonLatch()
        for (configured in listOf(Tool.ERASER, Tool.PAN, Tool.SELECT)) {
            assertEquals(configured, buttons.resolveTool(
                MotionEvent.TOOL_TYPE_ERASER, 0, Tool.ERASER, configured, true))
        }
    }

    @Test fun disabledWacomSecondaryDoesNotFallBackToEraserOrLatchedPrimary() {
        val buttons = StylusButtonLatch()
        buttons.onKey(KeyEvent.KEYCODE_STYLUS_BUTTON_PRIMARY, true)
        assertNull(buttons.resolveTool(MotionEvent.TOOL_TYPE_ERASER, 0, Tool.ERASER, null, true))
    }

    @Test fun compatibilityOffPreservesTailEraserAndSingleButtonBehavior() {
        val buttons = StylusButtonLatch()
        assertEquals(Tool.ERASER, buttons.resolveTool(MotionEvent.TOOL_TYPE_ERASER, 0, Tool.PAN, Tool.SELECT, false))
        assertEquals(Tool.PAN, buttons.resolveTool(MotionEvent.TOOL_TYPE_STYLUS,
            MotionEvent.BUTTON_STYLUS_SECONDARY, Tool.PAN, Tool.SELECT, false))
        assertNull(buttons.resolveTool(MotionEvent.TOOL_TYPE_ERASER, 0, Tool.PAN, Tool.SELECT, false, hover = true))
    }

    @Test fun eraserTypeChangesResolveWithoutRequiringAButtonPressEvent() {
        val buttons = StylusButtonLatch()
        assertNull(buttons.resolveTool(MotionEvent.TOOL_TYPE_STYLUS, 0, Tool.ERASER, Tool.PAN, true))
        assertEquals(Tool.PAN, buttons.resolveTool(MotionEvent.TOOL_TYPE_ERASER, 0, Tool.ERASER, Tool.PAN, true))
        assertNull(buttons.resolveTool(MotionEvent.TOOL_TYPE_STYLUS, 0, Tool.ERASER, Tool.PAN, true))
    }

    @Test fun wacomHoverUsesSecondaryButDoesNotTreatOtherInputsAsPens() {
        val buttons = StylusButtonLatch()
        assertEquals(Tool.PAN, buttons.resolveTool(MotionEvent.TOOL_TYPE_ERASER, 0, Tool.ERASER, Tool.PAN, true, hover = true))
        assertTrue(StylusButtonLatch.isPen(MotionEvent.TOOL_TYPE_ERASER))
        assertTrue(StylusButtonLatch.isPen(MotionEvent.TOOL_TYPE_STYLUS))
        for (type in listOf(MotionEvent.TOOL_TYPE_FINGER, MotionEvent.TOOL_TYPE_MOUSE, MotionEvent.TOOL_TYPE_UNKNOWN)) {
            assertFalse(StylusButtonLatch.isPen(type))
            assertNull(buttons.resolveTool(type, MotionEvent.BUTTON_STYLUS_SECONDARY, Tool.ERASER, Tool.PAN, true))
        }
    }

    @Test fun actionButtonAloneCanLatchAndReleaseAButton() {
        val buttons = StylusButtonLatch()
        buttons.updateMotion(0, MotionEvent.ACTION_BUTTON_PRESS, MotionEvent.BUTTON_STYLUS_SECONDARY)
        assertEquals(Tool.PAN, buttons.resolveTool(MotionEvent.TOOL_TYPE_STYLUS, 0, Tool.ERASER, Tool.PAN, true))
        buttons.updateMotion(0, MotionEvent.ACTION_BUTTON_RELEASE, MotionEvent.BUTTON_STYLUS_SECONDARY)
        assertNull(buttons.resolveTool(MotionEvent.TOOL_TYPE_STYLUS, 0, Tool.ERASER, Tool.PAN, true))
    }

    @Test fun motionButtonsSelectIndependentTools() {
        val buttons = StylusButtonLatch()
        assertEquals(Tool.ERASER, buttons.toolForButtons(MotionEvent.BUTTON_STYLUS_PRIMARY, Tool.ERASER, Tool.PAN))
        assertEquals(Tool.PAN, buttons.toolForButtons(MotionEvent.BUTTON_STYLUS_SECONDARY, Tool.ERASER, Tool.PAN))
        assertNull(buttons.toolForButtons(MotionEvent.BUTTON_STYLUS_SECONDARY, Tool.ERASER, null))
        assertNull(buttons.toolForButtons(0, Tool.ERASER, Tool.PAN))
    }

    @Test fun simultaneousButtonsPreferSecondaryUntilReleased() {
        val buttons = StylusButtonLatch()
        buttons.onKey(KeyEvent.KEYCODE_STYLUS_BUTTON_PRIMARY, true)
        buttons.onKey(KeyEvent.KEYCODE_STYLUS_BUTTON_SECONDARY, true)
        assertEquals(Tool.PAN, buttons.toolForButtons(0, Tool.ERASER, Tool.PAN))
        buttons.onKey(KeyEvent.KEYCODE_STYLUS_BUTTON_SECONDARY, false)
        assertEquals(Tool.ERASER, buttons.toolForButtons(0, Tool.ERASER, Tool.PAN))
    }

    @Test fun hoverOnlyButtonSurvivesTouchWithoutButtonBits() {
        val buttons = StylusButtonLatch()
        buttons.updateMotion(MotionEvent.BUTTON_STYLUS_SECONDARY, MotionEvent.ACTION_BUTTON_PRESS)
        assertEquals(Tool.PAN, buttons.toolForButtons(0, Tool.ERASER, Tool.PAN))
        buttons.updateMotion(0, MotionEvent.ACTION_BUTTON_RELEASE, MotionEvent.BUTTON_STYLUS_SECONDARY)
        assertNull(buttons.toolForButtons(0, Tool.ERASER, Tool.PAN))
    }

    @Test fun keyOnlyButtonSurvivesHoverWithoutButtonBits() {
        val buttons = StylusButtonLatch()
        buttons.onKey(KeyEvent.KEYCODE_STYLUS_BUTTON_SECONDARY, true)
        buttons.updateMotion(0, MotionEvent.ACTION_HOVER_MOVE)
        assertEquals(Tool.PAN, buttons.toolForButtons(0, Tool.ERASER, Tool.PAN))
        buttons.reset()
        assertNull(buttons.toolForButtons(0, Tool.ERASER, Tool.PAN))
    }

    @Test fun releaseAcrossStreamsClearsOnlyReleasedButton() {
        val buttons = StylusButtonLatch()
        buttons.onKey(KeyEvent.KEYCODE_STYLUS_BUTTON_PRIMARY, true)
        buttons.onKey(KeyEvent.KEYCODE_STYLUS_BUTTON_SECONDARY, true)
        buttons.updateMotion(MotionEvent.BUTTON_STYLUS_PRIMARY, MotionEvent.ACTION_BUTTON_RELEASE,
            MotionEvent.BUTTON_STYLUS_SECONDARY)
        assertEquals(Tool.ERASER, buttons.toolForButtons(0, Tool.ERASER, Tool.PAN))
        buttons.onKey(KeyEvent.KEYCODE_STYLUS_BUTTON_PRIMARY, false)
        assertNull(buttons.toolForButtons(0, Tool.ERASER, Tool.PAN))
    }

    @Test fun stylusMouseAliasesDoNotClaimLeftClickOrKeyboardKeys() {
        val buttons = StylusButtonLatch()
        assertEquals(Tool.ERASER, buttons.toolForButtons(MotionEvent.BUTTON_SECONDARY, Tool.ERASER, Tool.PAN))
        assertEquals(Tool.PAN, buttons.toolForButtons(MotionEvent.BUTTON_TERTIARY, Tool.ERASER, Tool.PAN))
        assertNull(buttons.toolForButtons(MotionEvent.BUTTON_PRIMARY, Tool.ERASER, Tool.PAN))
        assertFalse(buttons.onKey(KeyEvent.KEYCODE_PAGE_UP, true))
    }

    @Test fun releasingSecondaryDoesNotReleasePrimary() {
        val buttons = StylusButtonLatch()
        buttons.onKey(KeyEvent.KEYCODE_STYLUS_BUTTON_PRIMARY, true)
        buttons.onKey(KeyEvent.KEYCODE_STYLUS_BUTTON_SECONDARY, true)
        buttons.onKey(KeyEvent.KEYCODE_STYLUS_BUTTON_SECONDARY, false)
        assertTrue(buttons.held)
        buttons.onKey(KeyEvent.KEYCODE_STYLUS_BUTTON_PRIMARY, false)
        assertFalse(buttons.held)
    }
}
