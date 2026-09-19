package com.xnotes.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class LocalizedTextTest {
    @Test
    fun resourceTranslationsPreserveForkCorrections() {
        val resources = java.io.File("src/main/res/values-b+zh+Hans/strings.xml")
            .takeIf { it.isFile }
            ?: java.io.File("app/src/main/res/values-b+zh+Hans/strings.xml")
        val document = javax.xml.parsers.DocumentBuilderFactory.newInstance()
            .newDocumentBuilder().parse(resources)
        val strings = document.getElementsByTagName("string")
        val translated = (0 until strings.length).associate { index ->
            val node = strings.item(index) as org.w3c.dom.Element
            node.getAttribute("name") to node.textContent
        }
        mapOf(
            "caption_sensitivity" to "SENSITIVITY",
            "caption_width" to "WIDTH",
            "caption_opacity" to "OPACITY",
            "caption_neon" to "NEON",
            "caption_glow_intensity" to "GLOW INTENSITY",
            "view_single" to "Single",
            "view_double" to "Double",
            "default_for_new_canvases" to "Default for new canvases",
            "pref_spen_third_party" to "S Pen third-party dual-button compatibility",
            "pref_pen_primary_hold" to "Stylus primary button (hold)",
            "pref_pen_secondary_hold" to "Stylus secondary button (hold)",
        ).forEach { (resource, source) ->
            assertEquals(resource, zhHans(source), translated[resource])
        }
    }

    @Test
    fun translatesStaticUiText() {
        assertEquals("取消", zhHans("Cancel"))
    }

    @Test
    fun translatesDynamicUiText() {
        assertEquals("第 3 页", zhHans("Page 3"))
        assertEquals("将“示例”分享为：", zhHans("Share “示例” as:"))
    }

    @Test
    fun keepsDocumentTextUnchanged() {
        assertEquals("用户文本", zhHans("用户文本"))
    }
}
