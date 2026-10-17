package com.lemiepiante.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lemiepiante.LeMiePianteApp
import com.lemiepiante.ui.detail.PlantDetailViewModel
import com.lemiepiante.ui.edit.PlantEditViewModel
import com.lemiepiante.ui.list.PlantListViewModel

val AppViewModelFactory = viewModelFactory {
    initializer { PlantListViewModel(app()) }
    initializer { PlantDetailViewModel(createSavedStateHandle(), app()) }
    initializer { PlantEditViewModel(createSavedStateHandle(), app()) }
}

private fun CreationExtras.app(): LeMiePianteApp =
    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as LeMiePianteApp
