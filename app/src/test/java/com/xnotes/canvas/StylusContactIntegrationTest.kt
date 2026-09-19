package com.xnotes.canvas

import android.view.InputDevice
import android.view.MotionEvent
import com.xnotes.core.FakeSurfaceFactory
import com.xnotes.core.FakeTextMeasurer
import com.xnotes.core.geometry.Pt
import com.xnotes.core.history.History
import com.xnotes.core.infinite.CanvasSelection
import com.xnotes.core.infinite.CanvasViewport
import com.xnotes.core.infinite.InfiniteDocument
import com.xnotes.core.model.Document
import com.xnotes.core.model.Page
import com.xnotes.core.model.Rgba
import com.xnotes.core.model.Stroke
import com.xnotes.core.stroke.Sample
import com.xnotes.core.tools.Tool
import com.xnotes.core.tools.ToolDefaults
import com.xnotes.ui.InfiniteInteraction
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
            spenThirdPartyButtons = true; penSecondaryButtonTool = Tool.SELECT
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
}
