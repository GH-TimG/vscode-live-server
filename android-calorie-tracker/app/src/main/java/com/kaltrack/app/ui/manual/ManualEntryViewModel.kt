package com.kaltrack.app.ui.manual

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kaltrack.app.data.FoodRepository
import com.kaltrack.app.data.model.Product
import com.kaltrack.app.ui.appContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ManualUiState(
    val name: String = "",
    val kcalPer100Text: String = "",
    val proteinPer100Text: String = "",
    val amountText: String = "100",
    val saved: Boolean = false,
) {
    val kcalPer100: Double? get() = kcalPer100Text.toNumberOrNull()
    val proteinPer100: Double? get() = proteinPer100Text.toNumberOrNull()
    val amountGrams: Double? get() = amountText.toNumberOrNull()?.takeIf { it > 0.0 }

    val kcal: Double get() = (kcalPer100 ?: 0.0) * (amountGrams ?: 0.0) / 100.0
    val protein: Double get() = (proteinPer100 ?: 0.0) * (amountGrams ?: 0.0) / 100.0

    val canSave: Boolean
        get() = name.isNotBlank() && kcalPer100 != null && proteinPer100 != null && amountGrams != null
}

private fun String.toNumberOrNull(): Double? =
    replace(',', '.').trim().toDoubleOrNull()?.takeIf { it >= 0.0 }

class ManualEntryViewModel(
    private val repository: FoodRepository,
    private val selectedDate: StateFlow<LocalDate>,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ManualUiState())
    val uiState: StateFlow<ManualUiState> = _uiState.asStateFlow()

    fun onNameChange(value: String) {
        _uiState.value = _uiState.value.copy(name = value)
    }

    fun onKcalChange(value: String) {
        _uiState.value = _uiState.value.copy(kcalPer100Text = value.numeric())
    }

    fun onProteinChange(value: String) {
        _uiState.value = _uiState.value.copy(proteinPer100Text = value.numeric())
    }

    fun onAmountChange(value: String) {
        _uiState.value = _uiState.value.copy(amountText = value.numeric())
    }

    fun save() {
        val state = _uiState.value
        if (!state.canSave) return
        viewModelScope.launch {
            repository.addEntry(
                product = Product(
                    barcode = null,
                    name = state.name.trim(),
                    brand = null,
                    kcalPer100 = state.kcalPer100!!,
                    proteinPer100 = state.proteinPer100!!,
                ),
                amountGrams = state.amountGrams!!,
                date = selectedDate.value,
            )
            _uiState.value = _uiState.value.copy(saved = true)
        }
    }

    private fun String.numeric(): String =
        filter { it.isDigit() || it == ',' || it == '.' }.take(7)

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = appContainer
                ManualEntryViewModel(container.repository, container.selectedDate)
            }
        }
    }
}
