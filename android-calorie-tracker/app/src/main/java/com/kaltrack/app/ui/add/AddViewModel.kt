package com.kaltrack.app.ui.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kaltrack.app.data.FoodRepository
import com.kaltrack.app.data.local.toProduct
import com.kaltrack.app.data.model.Product
import com.kaltrack.app.ui.appContainer
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AddUiState(
    val query: String = "",
    val isSearching: Boolean = false,
    val results: List<Product> = emptyList(),
    val searched: Boolean = false,
    val error: String? = null,
)

class AddViewModel(private val repository: FoodRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(AddUiState())
    val uiState: StateFlow<AddUiState> = _uiState.asStateFlow()

    /** Zuletzt gescannte Produkte – der schnellste Weg für Alltagsessen. */
    val recent: StateFlow<List<Product>> = repository.observeRecentProducts()
        .map { list -> list.map { it.toProduct() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var searchJob: Job? = null

    fun onQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
    }

    fun search() {
        val query = _uiState.value.query.trim()
        if (query.length < 3) {
            _uiState.value = _uiState.value.copy(error = "Bitte mindestens 3 Zeichen eingeben.")
            return
        }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearching = true, error = null)
            val result = repository.search(query)
            _uiState.value = _uiState.value.copy(
                isSearching = false,
                searched = true,
                results = result.getOrDefault(emptyList()),
                error = if (result.isFailure) "Suche fehlgeschlagen – Internetverbindung prüfen." else null,
            )
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { AddViewModel(appContainer.repository) }
        }
    }
}
