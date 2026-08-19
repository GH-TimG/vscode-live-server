package com.kaltrack.app.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kaltrack.app.data.FoodRepository
import com.kaltrack.app.data.local.FoodEntryEntity
import com.kaltrack.app.data.local.kcal
import com.kaltrack.app.data.local.protein
import com.kaltrack.app.data.model.Goals
import com.kaltrack.app.data.prefs.SettingsStore
import com.kaltrack.app.ui.appContainer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class TodayUiState(
    val date: LocalDate = LocalDate.now(),
    val entries: List<FoodEntryEntity> = emptyList(),
    val goals: Goals = Goals(),
    val kcal: Double = 0.0,
    val protein: Double = 0.0,
)

class TodayViewModel(
    private val repository: FoodRepository,
    settings: SettingsStore,
    private val selectedDate: MutableStateFlow<LocalDate>,
) : ViewModel() {

    private val _messages = MutableSharedFlow<String>()
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    /** Zuletzt gelöschter Eintrag, damit "Rückgängig" funktioniert. */
    private var lastDeleted: FoodEntryEntity? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<TodayUiState> = combine(
        selectedDate,
        selectedDate.flatMapLatest { repository.observeDay(it) },
        settings.goals,
    ) { date, entries, goals ->
        TodayUiState(
            date = date,
            entries = entries,
            goals = goals,
            kcal = entries.sumOf { it.kcal },
            protein = entries.sumOf { it.protein },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TodayUiState(),
    )

    fun shiftDay(days: Long) {
        selectedDate.value = selectedDate.value.plusDays(days)
    }

    fun jumpToToday() {
        selectedDate.value = LocalDate.now()
    }

    fun delete(entry: FoodEntryEntity) {
        viewModelScope.launch {
            repository.deleteEntry(entry.id)
            lastDeleted = entry
            _messages.emit("${entry.name} gelöscht")
        }
    }

    fun undoDelete() {
        val entry = lastDeleted ?: return
        lastDeleted = null
        viewModelScope.launch { repository.restoreEntry(entry) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = appContainer
                TodayViewModel(container.repository, container.settings, container.selectedDate)
            }
        }
    }
}
