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
            "caption_sensitivity" to "压感灵敏度",
            "caption_width" to "笔画宽度",
            "caption_opacity" to "不透明度",
            "caption_neon" to "发光效果",
            "caption_glow_intensity" to "发光强度",
            "view_single" to "单页",
            "view_double" to "双页",
            "default_for_new_canvases" to "设为新画布默认样式",
            "pref_spen_third_party" to "S Pen 第三方笔双按钮兼容",
            "pref_pen_primary_hold" to "笔主按钮（按住）",
            "pref_pen_secondary_hold" to "笔副按钮（按住）",
        ).forEach { (resource, expected) ->
            assertEquals(resource, expected, translated[resource])
        }
    }

}
