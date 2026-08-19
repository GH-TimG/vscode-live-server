package com.kaltrack.app.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.kaltrack.app.AppContainer
import com.kaltrack.app.KalTrackApp

/** Holt den [AppContainer] aus der Application – für alle ViewModel-Factories. */
val CreationExtras.appContainer: AppContainer
    get() = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as KalTrackApp).container
