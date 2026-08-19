package com.kaltrack.app.ui.today

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kaltrack.app.data.local.FoodEntryEntity
import com.kaltrack.app.data.local.kcal
import com.kaltrack.app.data.local.protein
import com.kaltrack.app.ui.common.MessageBlock
import com.kaltrack.app.ui.common.NutrientProgress
import com.kaltrack.app.ui.theme.CalorieAccent
import com.kaltrack.app.ui.theme.ProteinAccent
import com.kaltrack.app.util.formatDay
import com.kaltrack.app.util.formatGrams
import com.kaltrack.app.util.formatNumber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    onAdd: () -> Unit,
    onScan: () -> Unit,
    onSettings: () -> Unit,
    viewModel: TodayViewModel = viewModel(factory = TodayViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = "Rückgängig",
                withDismissAction = true,
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = formatDay(state.date),
                        modifier = Modifier.clickable { viewModel.jumpToToday() },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.shiftDay(-1) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Vorheriger Tag")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.shiftDay(1) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Nächster Tag")
                    }
                    IconButton(onClick = onScan) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Barcode scannen")
                    }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Einstellungen")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Hinzufügen") },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 96.dp),
        ) {
            item {
                SummaryCard(
                    kcal = state.kcal,
                    protein = state.protein,
                    goalKcal = state.goals.kcal,
                    goalProtein = state.goals.protein,
                )
            }

            if (state.entries.isEmpty()) {
                item {
                    MessageBlock(
                        title = "Noch nichts gegessen",
                        subtitle = "Tippe auf „Hinzufügen“ oder scanne einen Barcode.",
                    )
                }
            } else {
                items(state.entries, key = { it.id }) { entry ->
                    EntryRow(entry = entry, onDelete = { viewModel.delete(entry) })
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(
    kcal: Double,
    protein: Double,
    goalKcal: Int,
    goalProtein: Int,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            NutrientProgress(
                label = "Kalorien",
                value = kcal,
                goal = goalKcal,
                unit = "kcal",
                color = CalorieAccent,
            )
            NutrientProgress(
                label = "Eiweiß",
                value = protein,
                goal = goalProtein,
                unit = "g",
                color = ProteinAccent,
            )
        }
    }
}

@Composable
private fun EntryRow(
    entry: FoodEntryEntity,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = buildString {
                    entry.brand?.takeIf { it.isNotBlank() }?.let { append(it).append(" · ") }
                    append("${formatGrams(entry.amountGrams)} g")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${formatNumber(entry.kcal)} kcal",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "${formatNumber(entry.protein, 1)} g Eiweiß",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        IconButton(onClick = onDelete) {
            Icon(Icons.Default.DeleteOutline, contentDescription = "Eintrag löschen")
        }
    }
}
