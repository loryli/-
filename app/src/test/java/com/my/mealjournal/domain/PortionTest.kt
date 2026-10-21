package com.my.mealjournal.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class PortionTest {

    @Test
    fun `个换算成克`() {
        assertEquals(50.0, Portion.gramsOf(1.0, 50.0), 0.0001)
        assertEquals(25.0, Portion.gramsOf(0.5, 50.0), 0.0001)
        assertEquals(75.0, Portion.gramsOf(1.5, 50.0), 0.0001)
    }

    @Test
    fun `克反算成个`() {
        assertEquals(2.0, Portion.portionOf(100.0, 50.0), 0.0001)
        assertEquals(1.4, Portion.portionOf(70.0, 50.0), 0.0001)
    }

    @Test
    fun `单位克数非法时不除零`() {
        assertEquals(0.0, Portion.portionOf(100.0, 0.0), 0.0001)
    }

    @Test
    fun `显示整数不带小数点`() {
        assertEquals("1", Portion.label(1.0))
        assertEquals("100", Portion.label(100.0))
        assertEquals("1.5", Portion.label(1.5))
        assertEquals("0.5", Portion.label(0.5))
    }

    @Test
    fun `有份量显示个和克`() {
        assertEquals("1 个 · 50 g", Portion.describe(50.0, "个", 50.0))
        assertEquals("0.5 个 · 25 g", Portion.describe(25.0, "个", 50.0))
        assertEquals("1.4 个 · 70 g", Portion.describe(70.0, "个", 50.0))
    }

    @Test
    fun `没有份量只显示克`() {
        assertEquals("100 g", Portion.describe(100.0, null, null))
        assertEquals("100 g", Portion.describe(100.0, "个", 0.0))
    }
}
