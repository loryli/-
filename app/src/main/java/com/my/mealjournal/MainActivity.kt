package com.my.mealjournal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.my.mealjournal.ui.MealJournalApp
import com.my.mealjournal.ui.theme.MealJournalTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MealJournalTheme {
                MealJournalApp()
            }
        }
    }
}
