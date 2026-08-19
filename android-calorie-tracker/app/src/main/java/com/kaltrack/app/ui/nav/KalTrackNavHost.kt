package com.kaltrack.app.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kaltrack.app.ui.add.AddScreen
import com.kaltrack.app.ui.amount.AmountScreen
import com.kaltrack.app.ui.manual.ManualEntryScreen
import com.kaltrack.app.ui.scan.ScanScreen
import com.kaltrack.app.ui.settings.SettingsScreen
import com.kaltrack.app.ui.today.TodayScreen

object Routes {
    const val TODAY = "today"
    const val ADD = "add"
    const val SCAN = "scan"
    const val MANUAL = "manual"
    const val SETTINGS = "settings"

    const val AMOUNT_ARG_BARCODE = "barcode"
    const val AMOUNT = "amount/{$AMOUNT_ARG_BARCODE}"

    fun amount(barcode: String) = "amount/$barcode"
}

@Composable
fun KalTrackNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.TODAY) {

        composable(Routes.TODAY) {
            TodayScreen(
                onAdd = { navController.navigate(Routes.ADD) },
                onScan = { navController.navigate(Routes.SCAN) },
                onSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }

        composable(Routes.ADD) {
            AddScreen(
                onBack = { navController.popBackStack() },
                onScan = { navController.navigate(Routes.SCAN) },
                onManual = { navController.navigate(Routes.MANUAL) },
                onProduct = { barcode -> navController.navigate(Routes.amount(barcode)) },
            )
        }

        composable(Routes.SCAN) {
            ScanScreen(
                onBack = { navController.popBackStack() },
                onBarcode = { barcode ->
                    navController.navigate(Routes.amount(barcode)) {
                        // Nach dem Scan nicht in die Kamera zurückfallen.
                        popUpTo(Routes.SCAN) { inclusive = true }
                    }
                },
                onManual = {
                    navController.navigate(Routes.MANUAL) {
                        popUpTo(Routes.SCAN) { inclusive = true }
                    }
                },
            )
        }

        composable(
            route = Routes.AMOUNT,
            arguments = listOf(navArgument(Routes.AMOUNT_ARG_BARCODE) { type = NavType.StringType }),
        ) { entry ->
            AmountScreen(
                barcode = entry.arguments?.getString(Routes.AMOUNT_ARG_BARCODE).orEmpty(),
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack(Routes.TODAY, inclusive = false) },
                onManual = { navController.navigate(Routes.MANUAL) },
            )
        }

        composable(Routes.MANUAL) {
            ManualEntryScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack(Routes.TODAY, inclusive = false) },
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
