package com.kaltrack.app.ui.amount

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kaltrack.app.data.model.Product
import com.kaltrack.app.ui.common.MessageBlock
import com.kaltrack.app.util.formatGrams
import com.kaltrack.app.util.formatNumber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmountScreen(
    barcode: String,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onManual: () -> Unit,
    viewModel: AmountViewModel = viewModel(factory = AmountViewModel.factory(barcode)),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.saved) {
        if (state.saved) onSaved()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Menge eintragen") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                state.product == null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        MessageBlock(
                            title = "Produkt nicht gefunden",
                            subtitle = state.error,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(onClick = { viewModel.load() }) { Text("Nochmal versuchen") }
                            Button(onClick = onManual) { Text("Manuell eintragen") }
                        }
                    }
                }

                else -> {
                    AmountForm(
                        state = state,
                        product = state.product!!,
                        onAmountChange = viewModel::onAmountChange,
                        onSave = viewModel::save,
                    )
                }
            }
        }
    }
}

@Composable
private fun AmountForm(
    state: AmountUiState,
    product: Product,
    onAmountChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column {
            Text(text = product.name, style = MaterialTheme.typography.headlineSmall)
            product.brand?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "${formatNumber(product.kcalPer100)} kcal · " +
                    "${formatNumber(product.proteinPer100, 1)} g Eiweiß je 100 g",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        OutlinedTextField(
            value = state.amountText,
            onValueChange = onAmountChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Menge in Gramm") },
            suffix = { Text("g") },
            singleLine = true,
            isError = state.amountGrams == null && state.amountText.isNotEmpty(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done,
            ),
        )

        QuickAmounts(
            servingGrams = product.servingGrams,
            current = state.amountText,
            onPick = onAmountChange,
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "Das ergibt",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "${formatNumber(state.kcal)} kcal",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "${formatNumber(state.protein, 1)} g Eiweiß",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }

        Button(
            onClick = onSave,
            enabled = state.canSave,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Zum Tag hinzufügen")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickAmounts(
    servingGrams: Double?,
    current: String,
    onPick: (String) -> Unit,
) {
    val options = buildList {
        if (servingGrams != null && servingGrams > 0) {
            add("1 Portion (${formatGrams(servingGrams)} g)" to formatGrams(servingGrams))
        }
        add("50 g" to "50")
        add("100 g" to "100")
        add("200 g" to "200")
    }

    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { (label, value) ->
            FilterChip(
                selected = current == value,
                onClick = { onPick(value) },
                label = { Text(label) },
            )
        }
    }
}
