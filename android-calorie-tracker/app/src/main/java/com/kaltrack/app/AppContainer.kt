package com.kaltrack.app

import android.content.Context
import com.kaltrack.app.data.FoodRepository
import com.kaltrack.app.data.local.AppDatabase
import com.kaltrack.app.data.prefs.SettingsStore
import com.kaltrack.app.data.remote.OpenFoodFactsClient
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.LocalDate

/**
 * Handverdrahtete Abhängigkeiten. Bei dieser Größe wäre eine DI-Bibliothek
 * mehr Zeremonie als Nutzen.
 */
class AppContainer(context: Context) {

    private val database: AppDatabase by lazy { AppDatabase.build(context) }

    val settings: SettingsStore by lazy { SettingsStore(context) }

    /**
     * Der Tag, den die App gerade zeigt. Liegt hier, damit ein Eintrag auch
     * dann im richtigen Tag landet, wenn man vorher zurückgeblättert hat.
     */
    val selectedDate = MutableStateFlow(LocalDate.now())

    val repository: FoodRepository by lazy {
        FoodRepository(
            entryDao = database.foodEntryDao(),
            productDao = database.productDao(),
            off = OpenFoodFactsClient(),
        )
    }
}
