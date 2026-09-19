package com.xnotes.canvas

import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import com.xnotes.core.FakeSurfaceFactory
import com.xnotes.core.FakeTextMeasurer
import com.xnotes.core.geometry.Pt
import com.xnotes.core.history.History
import com.xnotes.core.infinite.CanvasSelection
import com.xnotes.core.infinite.CanvasViewport
import com.xnotes.core.infinite.InfiniteDocument
import com.xnotes.core.infinite.EraseSession
import com.xnotes.core.model.Document
import com.xnotes.core.model.Page
import com.xnotes.core.model.Rgba
import com.xnotes.core.model.Stroke
import com.xnotes.core.stroke.Sample
import com.xnotes.core.tools.Tool
import com.xnotes.core.tools.ToolDefaults
import com.xnotes.ui.InfiniteInteraction
import com.xnotes.ui.CanvasPointerMode
import com.xnotes.ui.theme.Palette
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Actual controller event sequences, not just button-to-tool mapping. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class StylusContactIntegrationTest {
    private var time = 100L

    private fun send(touch: (MotionEvent) -> Boolean, action: Int, x: Double, y: Double,
                     eraser: Boolean = false, buttons: Int = 0) {
        time += 20
        val properties = MotionEvent.PointerProperties().apply {
            id = 0
            toolType = if (eraser) MotionEvent.TOOL_TYPE_ERASER else MotionEvent.TOOL_TYPE_STYLUS
        }
        val coordinates = MotionEvent.PointerCoords().apply {
            this.x = x.toFloat(); this.y = y.toFloat(); pressure = 0.5f; size = 1f
        }
        val event = MotionEvent.obtain(100L, time, action, 1, arrayOf(properties),
            arrayOf(coordinates), 0, buttons, 1f, 1f, 0, 0, InputDevice.SOURCE_STYLUS, 0)
        try { touch(event) } finally { event.recycle() }
    }

    private fun dot() = Stroke(Tool.PEN, ToolDefaults.configFor(Tool.PEN),
        mutableListOf(Sample(100.0, 100.0, 1.0)))

    @Test fun pagedSelectionSurvivesReleaseAndFurtherContact() {
        val item = dot()
        val state = CanvasState(Document(mutableListOf(Page(400.0, 400.0, mutableListOf(item)))),
            FakeSurfaceFactory(), Palette.forAppearance("dark", Rgba(0, 230, 118))).apply {
            viewportW = 800; viewportH = 1000; relayout()
        }
        val controller = InteractionController(state, History(), FakeTextMeasurer(), {}).apply {
            setTool(Tool.PEN); spenThirdPartyButtons = true; penSecondaryButtonTool = Tool.SELECT
        }
        fun event(action: Int, x: Double, y: Double, eraser: Boolean) {
            val content = state.fromPageSpace(0, Pt(x, y))
            val point = state.contentToViewport(content)
            send(controller::onTouch, action, point.x, point.y, eraser)
        }
        event(MotionEvent.ACTION_DOWN, 30.0, 30.0, true)
        event(MotionEvent.ACTION_MOVE, 160.0, 160.0, true)
        event(MotionEvent.ACTION_MOVE, 170.0, 170.0, false)
        assertTrue("releasing SELECT must preserve the selection", controller.hasSelection)
        event(MotionEvent.ACTION_MOVE, 200.0, 200.0, false)
        event(MotionEvent.ACTION_UP, 210.0, 210.0, false)
        assertTrue(controller.hasSelection)
        assertEquals(1, state.document.pages[0].items.size)
    }

    @Test fun infiniteSelectionSurvivesReleaseAndFurtherContact() {
        val item = dot()
        val document = InfiniteDocument()
        val selection = CanvasSelection(document)
        val controller = InfiniteInteraction(CanvasViewport(), {}, selection = { selection },
            itemsIn = { listOf(item) }).apply {
            spenThirdPartyButtons = true; penSecondaryButtonTool = Tool.SELECT
        }
        send(controller::onTouch, MotionEvent.ACTION_DOWN, 30.0, 30.0, true)
        send(controller::onTouch, MotionEvent.ACTION_MOVE, 160.0, 160.0, true)
        send(controller::onTouch, MotionEvent.ACTION_MOVE, 170.0, 170.0)
        assertEquals(listOf(item), selection.items)
        send(controller::onTouch, MotionEvent.ACTION_MOVE, 200.0, 200.0)
        send(controller::onTouch, MotionEvent.ACTION_UP, 210.0, 210.0)
        assertEquals(listOf(item), selection.items)
    }

    @Test fun releasingPanDoesNotDrawUntilTheNextRealDown() {
        val committed = mutableListOf<Stroke>()
        val controller = InfiniteInteraction(CanvasViewport(), {}, onCommitStroke = { committed.add(it) }).apply {
            spenThirdPartyButtons = true; penSecondaryButtonTool = Tool.PAN
        }
        send(controller::onTouch, MotionEvent.ACTION_DOWN, 10.0, 10.0, true)
        send(controller::onTouch, MotionEvent.ACTION_MOVE, 20.0, 20.0, true)
        send(controller::onTouch, MotionEvent.ACTION_MOVE, 30.0, 30.0)
        send(controller::onTouch, MotionEvent.ACTION_MOVE, 50.0, 50.0)
        send(controller::onTouch, MotionEvent.ACTION_UP, 60.0, 60.0)
        assertTrue("release must not start an accidental stroke", committed.isEmpty())
        send(controller::onTouch, MotionEvent.ACTION_DOWN, 100.0, 100.0)
        send(controller::onTouch, MotionEvent.ACTION_MOVE, 120.0, 120.0)
        send(controller::onTouch, MotionEvent.ACTION_UP, 140.0, 140.0)
        assertEquals(1, committed.size)
    }

    @Test fun waitingForLiftIgnoresRepressAndMove() {
        val controller = InfiniteInteraction(CanvasViewport(), {}).apply {
            spenThirdPartyButtons = true; penSecondaryButtonTool = Tool.PAN
        }
        send(controller::onTouch, MotionEvent.ACTION_DOWN, 10.0, 10.0, true)
        send(controller::onTouch, MotionEvent.ACTION_MOVE, 20.0, 20.0)
        send(controller::onTouch, MotionEvent.ACTION_MOVE, 40.0, 40.0, true)
        assertEquals(CanvasPointerMode.IDLE, controller.mode)
        send(controller::onTouch, MotionEvent.ACTION_UP, 40.0, 40.0, true)
        send(controller::onTouch, MotionEvent.ACTION_DOWN, 60.0, 60.0, true)
        assertEquals(CanvasPointerMode.PAN, controller.mode)
    }

    @Test fun secondaryReleaseFallsBackToHeldPrimaryBeforeWaiting() {
        val controller = InfiniteInteraction(CanvasViewport(), {}).apply {
            spenThirdPartyButtons = true
            penButtonTool = Tool.PAN; penSecondaryButtonTool = Tool.SELECT
        }
        controller.onStylusButtonKey(KeyEvent.KEYCODE_STYLUS_BUTTON_PRIMARY, true)
        send(controller::onTouch, MotionEvent.ACTION_DOWN, 10.0, 10.0)
        controller.onStylusButtonKey(KeyEvent.KEYCODE_STYLUS_BUTTON_SECONDARY, true)
        send(controller::onTouch, MotionEvent.ACTION_MOVE, 20.0, 20.0)
        controller.onStylusButtonKey(KeyEvent.KEYCODE_STYLUS_BUTTON_SECONDARY, false)
        send(controller::onTouch, MotionEvent.ACTION_MOVE, 30.0, 30.0)
        assertEquals(CanvasPointerMode.PAN, controller.mode)
        controller.onStylusButtonKey(KeyEvent.KEYCODE_STYLUS_BUTTON_PRIMARY, false)
        send(controller::onTouch, MotionEvent.ACTION_MOVE, 40.0, 40.0)
        assertEquals(CanvasPointerMode.IDLE, controller.mode)
    }

    @Test fun disabledSecondaryDoesNotSplitTheStroke() {
        val committed = mutableListOf<Stroke>()
        val controller = InfiniteInteraction(CanvasViewport(), {}, onCommitStroke = { committed.add(it) }).apply {
            spenThirdPartyButtons = true; penSecondaryButtonTool = null
        }
        send(controller::onTouch, MotionEvent.ACTION_DOWN, 10.0, 10.0)
        send(controller::onTouch, MotionEvent.ACTION_MOVE, 20.0, 20.0, true)
        send(controller::onTouch, MotionEvent.ACTION_MOVE, 30.0, 30.0)
        send(controller::onTouch, MotionEvent.ACTION_UP, 40.0, 40.0)
        assertEquals(1, committed.size)
    }

    @Test fun compatibilityOffKeepsTheOriginalContactTool() {
        val controller = InfiniteInteraction(CanvasViewport(), {}).apply {
            spenThirdPartyButtons = false; penButtonTool = Tool.PAN
        }
        send(controller::onTouch, MotionEvent.ACTION_DOWN, 10.0, 10.0,
            buttons = MotionEvent.BUTTON_STYLUS_PRIMARY)
        send(controller::onTouch, MotionEvent.ACTION_MOVE, 20.0, 20.0)
        assertEquals(CanvasPointerMode.PAN, controller.mode)
    }

    @Test fun focusLossCancelsContactAndIgnoresTrailingMoveAndUp() {
        val committed = mutableListOf<Stroke>()
        val controller = InfiniteInteraction(CanvasViewport(), {}, onCommitStroke = { committed.add(it) }).apply {
            spenThirdPartyButtons = true; penSecondaryButtonTool = Tool.PAN
        }
        controller.onStylusButtonKey(KeyEvent.KEYCODE_STYLUS_BUTTON_SECONDARY, true)
        send(controller::onTouch, MotionEvent.ACTION_DOWN, 10.0, 10.0)
        controller.releaseStylusButtons() // same callback as window focus loss
        send(controller::onTouch, MotionEvent.ACTION_MOVE, 30.0, 30.0)
        send(controller::onTouch, MotionEvent.ACTION_UP, 50.0, 50.0)
        assertTrue(committed.isEmpty())
        send(controller::onTouch, MotionEvent.ACTION_DOWN, 60.0, 60.0)
        send(controller::onTouch, MotionEvent.ACTION_UP, 80.0, 80.0)
        assertEquals(1, committed.size)
    }

    @Test fun cancelledEraseStillProducesOneUndoAndNoTrailingStroke() {
        val document = InfiniteDocument()
        val item = dot()
        document.add(item)
        val history = History()
        val controller = InfiniteInteraction(CanvasViewport(), {},
            onEraseBegin = { EraseSession(document) },
            onEraseEnd = { it.buildCommand()?.let(history::push) }).apply {
            spenThirdPartyButtons = true; penSecondaryButtonTool = Tool.ERASER
        }
        send(controller::onTouch, MotionEvent.ACTION_DOWN, 100.0, 100.0, true)
        send(controller::onTouch, MotionEvent.ACTION_CANCEL, 100.0, 100.0, true)
        assertTrue(history.canUndo)
        history.undo()
        assertFalse(history.canUndo)
        assertTrue(document.itemsIn(item.paintBounds()).contains(item))
        send(controller::onTouch, MotionEvent.ACTION_DOWN, 200.0, 200.0)
        assertEquals(CanvasPointerMode.DRAW, controller.mode)
    }

    @Test fun pagedFocusLossDuringEraseKeepsUndoAndSuppressesTrailingInput() {
        val item = dot()
        val page = Page(400.0, 400.0, mutableListOf(item))
        val state = CanvasState(Document(mutableListOf(page)), FakeSurfaceFactory(),
            Palette.forAppearance("dark", Rgba(0, 230, 118))).apply {
            viewportW = 800; viewportH = 1000; relayout()
        }
        val history = History()
        val controller = InteractionController(state, history, FakeTextMeasurer(), {}).apply {
            setTool(Tool.PEN); spenThirdPartyButtons = true; penSecondaryButtonTool = Tool.ERASER
        }
        fun event(action: Int, x: Double, y: Double, eraser: Boolean = false) {
            val point = state.contentToViewport(state.fromPageSpace(0, Pt(x, y)))
            send(controller::onTouch, action, point.x, point.y, eraser)
        }
        event(MotionEvent.ACTION_DOWN, 100.0, 100.0, true)
        controller.releaseStylusButtons()
        event(MotionEvent.ACTION_MOVE, 180.0, 180.0)
        event(MotionEvent.ACTION_UP, 200.0, 200.0)
        assertTrue(history.canUndo)
        history.undo()
        assertEquals(listOf(item), page.items)
        assertFalse("erase must be a single undo step", history.canUndo)
        event(MotionEvent.ACTION_DOWN, 250.0, 250.0)
        event(MotionEvent.ACTION_UP, 280.0, 280.0)
        assertEquals(2, page.items.size)
    }

    @Test fun pressingPanCommitsTheOutgoingStrokeOnlyOnce() {
        val committed = mutableListOf<Stroke>()
        val controller = InfiniteInteraction(CanvasViewport(), {}, onCommitStroke = { committed.add(it) }).apply {
            spenThirdPartyButtons = true; penSecondaryButtonTool = Tool.PAN
        }
        send(controller::onTouch, MotionEvent.ACTION_DOWN, 10.0, 10.0)
        send(controller::onTouch, MotionEvent.ACTION_MOVE, 30.0, 30.0)
        send(controller::onTouch, MotionEvent.ACTION_MOVE, 50.0, 50.0, true)
        assertEquals(1, committed.size)
        assertEquals(CanvasPointerMode.PAN, controller.mode)
        send(controller::onTouch, MotionEvent.ACTION_MOVE, 70.0, 70.0, true)
        send(controller::onTouch, MotionEvent.ACTION_MOVE, 90.0, 90.0)
        send(controller::onTouch, MotionEvent.ACTION_UP, 110.0, 110.0)
        assertEquals(1, committed.size)
    }

    @Test fun hoverDuringContactCannotReplaceTheLiveGesture() {
        val controller = InfiniteInteraction(CanvasViewport(), {}).apply {
            spenThirdPartyButtons = true; penSecondaryButtonTool = Tool.PAN; penButtonHover = true
        }
        send(controller::onTouch, MotionEvent.ACTION_DOWN, 10.0, 10.0)
        send(controller::onHover, MotionEvent.ACTION_HOVER_MOVE, 20.0, 20.0, true)
        assertEquals(CanvasPointerMode.DRAW, controller.mode)
    }

    @Test fun replacingInfiniteDocumentDoesNotCommitTheOldEraseIntoNewHistory() {
        val document = InfiniteDocument().apply { add(dot()) }
        val newHistory = History()
        val controller = InfiniteInteraction(CanvasViewport(), {},
            onEraseBegin = { EraseSession(document) },
            onEraseEnd = { it.buildCommand()?.let(newHistory::push) }).apply {
            spenThirdPartyButtons = true
        }
        send(controller::onTouch, MotionEvent.ACTION_DOWN, 100.0, 100.0, true)
        // The editor has installed a new document and cleared its history before reset.
        newHistory.clear()
        controller.resetGestureState()
        send(controller::onTouch, MotionEvent.ACTION_MOVE, 200.0, 200.0)
        send(controller::onTouch, MotionEvent.ACTION_UP, 220.0, 220.0)
        assertFalse("old eraser must not enter the new document's history", newHistory.canUndo)
        assertEquals(CanvasPointerMode.IDLE, controller.mode)
    }

    @Test fun replacingPagedDocumentDoesNotCommitTheOldEraseIntoNewHistory() {
        val state = CanvasState(Document(mutableListOf(Page(400.0, 400.0, mutableListOf(dot())))),
            FakeSurfaceFactory(), Palette.forAppearance("dark", Rgba(0, 230, 118))).apply {
            viewportW = 800; viewportH = 1000; relayout()
        }
        val history = History()
        val controller = InteractionController(state, history, FakeTextMeasurer(), {}).apply {
            setTool(Tool.PEN); spenThirdPartyButtons = true
        }
        val point = state.contentToViewport(state.fromPageSpace(0, Pt(100.0, 100.0)))
        send(controller::onTouch, MotionEvent.ACTION_DOWN, point.x, point.y, true)
        state.document = Document(mutableListOf(Page(400.0, 400.0)))
        history.clear()
        controller.resetGestureState()
        send(controller::onTouch, MotionEvent.ACTION_MOVE, point.x, point.y)
        send(controller::onTouch, MotionEvent.ACTION_UP, point.x, point.y)
        assertFalse(history.canUndo)
        assertTrue(state.document.pages[0].items.isEmpty())
    }
}
