package com.kaltrack.app.ui.add

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kaltrack.app.data.model.Product
import com.kaltrack.app.ui.common.MessageBlock
import com.kaltrack.app.util.formatNumber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddScreen(
    onBack: () -> Unit,
    onScan: () -> Unit,
    onManual: () -> Unit,
    onProduct: (String) -> Unit,
    viewModel: AddViewModel = viewModel(factory = AddViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val recent by viewModel.recent.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lebensmittel hinzufügen") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            item {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = onScan,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                            Text("Scannen", modifier = Modifier.padding(start = 8.dp))
                        }
                        OutlinedButton(
                            onClick = onManual,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null)
                            Text("Manuell", modifier = Modifier.padding(start = 8.dp))
                        }
                    }

                    OutlinedTextField(
                        value = state.query,
                        onValueChange = viewModel::onQueryChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("In Open Food Facts suchen") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { viewModel.search() }),
                        trailingIcon = {
                            IconButton(onClick = { viewModel.search() }) {
                                Icon(Icons.Default.Search, contentDescription = "Suchen")
                            }
                        },
                    )

                    state.error?.let { error ->
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            if (state.isSearching) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp))
                    }
                }
            }

            val showResults = state.searched && !state.isSearching
            val list = if (showResults) state.results else recent

            if (showResults && state.results.isEmpty() && state.error == null) {
                item {
                    MessageBlock(
                        title = "Nichts gefunden",
                        subtitle = "Versuch einen anderen Begriff, scanne den Barcode " +
                            "oder trage die Werte manuell ein.",
                    )
                }
            }

            if (!showResults && recent.isNotEmpty()) {
                item { SectionHeader("Zuletzt benutzt") }
            }
            if (showResults && state.results.isNotEmpty()) {
                item { SectionHeader("Suchergebnisse") }
            }

            items(list) { product ->
                ProductRow(
                    product = product,
                    enabled = product.barcode != null,
                    onClick = { product.barcode?.let(onProduct) },
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
    )
}

@Composable
private fun ProductRow(
    product: Product,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = product.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = buildString {
                    product.brand?.takeIf { it.isNotBlank() }?.let { append(it).append(" · ") }
                    append("${formatNumber(product.kcalPer100)} kcal")
                    append(" · ")
                    append("${formatNumber(product.proteinPer100, 1)} g Eiweiß")
                    append(" / 100 g")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
