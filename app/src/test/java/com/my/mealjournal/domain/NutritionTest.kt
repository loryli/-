package com.my.mealjournal.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NutritionTest {

    @Test
    fun `targets follow the fixed energy ratios`() {
        val t = Nutrition.targets(1800)
        // 蛋白 20% / 碳水 50% / 脂肪 30%，系数 4/4/9
        assertEquals(90.0, t.protein, 0.001)
        assertEquals(225.0, t.carbs, 0.001)
        assertEquals(60.0, t.fat, 0.001)
    }

    @Test
    fun `target ratios sum to one`() {
        assertEquals(1.0, Nutrition.PROTEIN_RATIO + Nutrition.CARB_RATIO + Nutrition.FAT_RATIO, 0.001)
    }

    @Test
    fun `targets reconstruct the calorie goal`() {
        val goal = 2200
        val t = Nutrition.targets(goal)
        assertEquals(goal.toDouble(), t.kcal, 0.5)
    }

    @Test
    fun `entry macro scales per100g by grams`() {
        val m = Nutrition.ofGrams(MacroPer100g(protein = 20.0, carbs = 50.0, fat = 5.0), grams = 200.0)
        assertEquals(40.0, m.protein, 0.001)
        assertEquals(100.0, m.carbs, 0.001)
        assertEquals(10.0, m.fat, 0.001)
    }

    @Test
    fun `unknown macro falls back to zero`() {
        val m = Nutrition.ofGrams(null, grams = 150.0)
        assertEquals(0.0, m.protein, 0.001)
        assertEquals(0.0, m.carbs, 0.001)
        assertEquals(0.0, m.fat, 0.001)
        assertFalse(MacroPer100g(null, null, null).known)
    }

    @Test
    fun `shares are energy based`() {
        // 蛋白 40g(160) 碳水 100g(400) 脂肪 20g(180)，总 740
        val s = Nutrition.shares(MacroGrams(protein = 40.0, carbs = 100.0, fat = 20.0))
        assertEquals(160.0 / 740.0, s.protein, 0.001)
        assertEquals(400.0 / 740.0, s.carbs, 0.001)
        assertEquals(180.0 / 740.0, s.fat, 0.001)
        assertEquals(1.0, s.protein + s.carbs + s.fat, 0.001)
    }

    @Test
    fun `zero macro gives zero shares`() {
        val s = Nutrition.shares(MacroGrams(0.0, 0.0, 0.0))
        assertTrue(s.kcal == 0.0 && s.protein == 0.0 && s.carbs == 0.0 && s.fat == 0.0)
    }
}
