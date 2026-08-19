package com.kaltrack.app.data.prefs

import android.content.Context
import com.kaltrack.app.data.model.Goals
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tagesziele. Für zwei Zahlen lohnt keine Datenbank – SharedPreferences reicht,
 * wird aber als [StateFlow] nach außen gegeben, damit die UI reagieren kann.
 */
class SettingsStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("kaltrack_settings", Context.MODE_PRIVATE)

    private val _goals = MutableStateFlow(read())
    val goals: StateFlow<Goals> = _goals.asStateFlow()

    fun setGoals(goals: Goals) {
        val sanitised = Goals(
            kcal = goals.kcal.coerceIn(500, 10_000),
            protein = goals.protein.coerceIn(10, 500),
        )
        prefs.edit()
            .putInt(KEY_KCAL, sanitised.kcal)
            .putInt(KEY_PROTEIN, sanitised.protein)
            .apply()
        _goals.value = sanitised
    }

    private fun read() = Goals(
        kcal = prefs.getInt(KEY_KCAL, Goals.DEFAULT_KCAL),
        protein = prefs.getInt(KEY_PROTEIN, Goals.DEFAULT_PROTEIN),
    )

    private companion object {
        const val KEY_KCAL = "goal_kcal"
        const val KEY_PROTEIN = "goal_protein"
    }
}
