package com.kaltrack.app.ui.amount

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

data class AmountUiState(
    val isLoading: Boolean = true,
    val product: Product? = null,
    val amountText: String = "100",
    val error: String? = null,
    val saved: Boolean = false,
) {
    /** Eingegebene Menge in Gramm, oder null bei unbrauchbarer Eingabe. */
    val amountGrams: Double?
        get() = amountText.replace(',', '.').trim().toDoubleOrNull()?.takeIf { it > 0.0 }

    val kcal: Double get() = (product?.kcalPer100 ?: 0.0) * (amountGrams ?: 0.0) / 100.0

    val protein: Double get() = (product?.proteinPer100 ?: 0.0) * (amountGrams ?: 0.0) / 100.0

    val canSave: Boolean get() = product != null && amountGrams != null
}

class AmountViewModel(
    private val repository: FoodRepository,
    private val selectedDate: StateFlow<LocalDate>,
    private val barcode: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AmountUiState())
    val uiState: StateFlow<AmountUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val state = when (val result = repository.lookupBarcode(barcode)) {
                is FoodRepository.LookupResult.Found -> _uiState.value.copy(
                    isLoading = false,
                    product = result.product,
                    amountText = defaultAmount(result.product),
                    error = null,
                )

                FoodRepository.LookupResult.NotFound -> _uiState.value.copy(
                    isLoading = false,
                    product = null,
                    error = "Barcode $barcode ist nicht in Open Food Facts. " +
                        "Du kannst das Produkt manuell eintragen.",
                )

                FoodRepository.LookupResult.Offline -> _uiState.value.copy(
                    isLoading = false,
                    product = null,
                    error = "Keine Verbindung zu Open Food Facts. Prüf dein Internet und versuch es nochmal.",
                )
            }
            _uiState.value = state
        }
    }

    fun onAmountChange(text: String) {
        // Nur Ziffern, Komma und Punkt zulassen – spart Validierungsmeldungen.
        val filtered = text.filter { it.isDigit() || it == ',' || it == '.' }.take(7)
        _uiState.value = _uiState.value.copy(amountText = filtered)
    }

    fun save() {
        val state = _uiState.value
        val product = state.product ?: return
        val grams = state.amountGrams ?: return
        viewModelScope.launch {
            repository.addEntry(product, grams, selectedDate.value)
            _uiState.value = _uiState.value.copy(saved = true)
        }
    }

    private fun defaultAmount(product: Product): String {
        val serving = product.servingGrams
        return if (serving != null && serving > 0) {
            if (serving % 1.0 == 0.0) serving.toInt().toString() else serving.toString()
        } else {
            "100"
        }
    }

    companion object {
        fun factory(barcode: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = appContainer
                AmountViewModel(container.repository, container.selectedDate, barcode)
            }
        }
    }
}
