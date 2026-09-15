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
