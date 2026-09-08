package com.xnotes.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class LocalizedTextTest {
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
