package com.kaltrack.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kaltrack.app.data.model.Goals
import com.kaltrack.app.data.prefs.SettingsStore
import com.kaltrack.app.ui.appContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SettingsUiState(
    val kcalText: String = "",
    val proteinText: String = "",
    val savedHint: Boolean = false,
) {
    val kcal: Int? get() = kcalText.toIntOrNull()
    val protein: Int? get() = proteinText.toIntOrNull()
    val canSave: Boolean get() = kcal != null && protein != null
}

class SettingsViewModel(private val settings: SettingsStore) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            kcalText = settings.goals.value.kcal.toString(),
            proteinText = settings.goals.value.protein.toString(),
        ),
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun onKcalChange(value: String) {
        _uiState.value = _uiState.value.copy(kcalText = value.digits(), savedHint = false)
    }

    fun onProteinChange(value: String) {
        _uiState.value = _uiState.value.copy(proteinText = value.digits(), savedHint = false)
    }

    fun save() {
        val state = _uiState.value
        val kcal = state.kcal ?: return
        val protein = state.protein ?: return
        settings.setGoals(Goals(kcal = kcal, protein = protein))
        // Zurückschreiben, damit man die begrenzten Werte auch sieht.
        val stored = settings.goals.value
        _uiState.value = SettingsUiState(
            kcalText = stored.kcal.toString(),
            proteinText = stored.protein.toString(),
            savedHint = true,
        )
    }

    private fun String.digits(): String = filter { it.isDigit() }.take(5)

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { SettingsViewModel(appContainer.settings) }
        }
    }
}
